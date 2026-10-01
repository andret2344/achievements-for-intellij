package eu.andret.plugin.achievementsforintellij.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.messages.Topic
import eu.andret.plugin.achievementsforintellij.AchievementsBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementsRegistry
import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementDefinition

private val LOG = logger<AchievementsService>()

@State(
    name = "achievements",
    storages = [Storage("achievements.xml")]
)
@Service(Service.Level.APP)
class AchievementsService : PersistentStateComponent<AchievementsService.State> {

    data class AchievementLog(
        var stepIndex: Int = -1,
        var count: Long = 0,
        var timestamp: Long = 0,
    )

    data class State(
        // Not persisted while equal to the default; for the first migration change the default to 0 and set the new version explicitly
        var version: Int = 1,
        var achievements: MutableMap<String, Long> = LinkedHashMap(),
        // Logs of reached steps, at most one per step index
        var logs: MutableMap<String, MutableList<AchievementLog>> = LinkedHashMap()
    )

    private var myState: State = State()

    // Returns a snapshot: the platform serializes it on its own thread, outside of our lock
    override fun getState(): State = synchronized(this) {
        myState.copy(
            achievements = LinkedHashMap(myState.achievements),
            logs = myState.logs.mapValuesTo(LinkedHashMap()) { (_, logs) -> logs.mapTo(mutableListOf()) { it.copy() } }
        )
    }

    override fun loadState(state: State) = synchronized(this) {
        myState = state
    }

    fun interface AchievementListener {
        fun onAchievementsChanged()
    }

    fun increment(achievementId: String, delta: Long = 1): Long {
        val newValue = synchronized(this) { incrementLocked(achievementId, delta) }
        if (delta != 0L) {
            publishChange()
        }
        return newValue
    }

    /**
     * Raises the counter to [value] when it beats the current one, for "most at once" records.
     * Compares and updates under one lock, so concurrent callers cannot double-count.
     */
    fun raiseTo(achievementId: String, value: Long): Long {
        var raised = false
        val newValue = synchronized(this) {
            val current = myState.achievements[achievementId] ?: 0L
            if (value > current) {
                raised = true
                incrementLocked(achievementId, value - current)
            } else {
                current
            }
        }
        if (raised) {
            publishChange()
        }
        return newValue
    }

    private fun incrementLocked(achievementId: String, delta: Long): Long {
        val def: AchievementDefinition? = AchievementsRegistry.get(achievementId)
        val oldValue = myState.achievements[achievementId] ?: 0L
        val newValue = oldValue + delta
        myState.achievements[achievementId] = newValue

        if (def == null) {
            LOG.warn("Increment for unknown achievement id: $achievementId")
            return newValue
        }
        if (def.steps.isEmpty()) {
            return newValue
        }
        val oldStepIdx = def.steps.indexOfLast { oldValue >= it.threshold }
        val newStepIdx = def.steps.indexOfLast { newValue >= it.threshold }

        if (newStepIdx > oldStepIdx) {
            val now = System.currentTimeMillis()
            ensureLogList(achievementId)
            for (i in (oldStepIdx + 1)..newStepIdx) {
                if (myState.logs[achievementId]?.none { it.stepIndex == i } ?: true) {
                    myState.logs[achievementId]?.add(AchievementLog(i, newValue, now))
                }
                showNotification(def, i)
            }
        }
        return newValue
    }

    fun get(achievementId: String): Long = synchronized(this) { myState.achievements[achievementId] ?: 0L }

    fun clearAll() {
        synchronized(this) {
            myState.achievements.clear()
            myState.logs.clear()
        }
        // File Voyager keeps its own state; leaving it would re-unlock the achievement on the next file open
        FileVoyagerService.getInstance().clear()
        publishChange()
    }

    data class AchievementProgress(
        val count: Long,
        val lastStepIndex: Int,
        val nextThreshold: Long?,
        val percentToNext: Int,
    )

    fun getProgress(achievementId: String): AchievementProgress {
        val def = AchievementsRegistry.get(achievementId)
        val count = get(achievementId)
        if (def == null || def.steps.isEmpty()) {
            return AchievementProgress(count, -1, null, 100)
        }
        val lastIdx = def.steps.indexOfLast { count >= it.threshold }
        val nextIdx = lastIdx + 1
        val nextThreshold = def.steps.getOrNull(nextIdx)?.threshold

        // For non-progressive achievements, do not show gradual percentage: either 0 (not yet) or 100 (reached).
        if (!def.progressive) {
            val percentNonProgressive = if (nextThreshold == null) 100 else if (count >= nextThreshold) 100 else 0
            return AchievementProgress(count, lastIdx, nextThreshold, percentNonProgressive)
        }

        val percent = if (nextThreshold == null) 100 else {
            ((count.toDouble() / nextThreshold.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        }
        return AchievementProgress(count, lastIdx, nextThreshold, percent)
    }

    fun getAllLogs(): Map<String, List<AchievementLog>> = synchronized(this) {
        myState.logs.mapValues { (_, logs) -> logs.map { it.copy() } }
    }

    private fun ensureLogList(achievementId: String) {
        if (myState.logs[achievementId] == null) {
            myState.logs[achievementId] = mutableListOf()
        }
    }

    // Must be called outside the lock: subscribers run synchronously on the caller's thread
    private fun publishChange() {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onAchievementsChanged()
    }

    private fun showNotification(def: AchievementDefinition, stepIndex: Int) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATIONS_GROUP_ID)
        val title = AchievementsBundle.message("notification.achievement.unlocked.title")
        val achievementName = AchievementsBundle.message(def.nameKey)
        val description = renderDescription(def.id, stepIndex)
        val content = "<b>$achievementName</b><br/>$description"
        group.createNotification(title, content, NotificationType.INFORMATION).notify(null)
    }

    companion object {
        const val NOTIFICATIONS_GROUP_ID: String = "achievements.notifications"

        @JvmField
        val TOPIC: Topic<AchievementListener> = Topic.create(
            "AchievementsChanged",
            AchievementListener::class.java
        )

        fun getInstance(): AchievementsService =
            ApplicationManager.getApplication().getService(AchievementsService::class.java)
    }

    /**
     * Renders the achievement description for a specific step.
     * If stepIndex is null, uses the next threshold (or last achieved if completed).
     */
    fun renderDescription(achievementId: String, stepIndex: Int? = null): String {
        val def = AchievementsRegistry.get(achievementId) ?: return ""

        val threshold: Long? = if (stepIndex != null) {
            def.steps.getOrNull(stepIndex)?.threshold
        } else {
            val progress = getProgress(achievementId)
            progress.nextThreshold ?: def.steps.lastOrNull()?.threshold
        }

        return if (threshold != null) {
            AchievementsBundle.message(def.descriptionKey, threshold)
        } else {
            AchievementsBundle.message(def.descriptionKey)
        }
    }
}
