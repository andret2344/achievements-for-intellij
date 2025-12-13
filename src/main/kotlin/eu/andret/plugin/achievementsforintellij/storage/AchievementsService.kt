package eu.andret.plugin.achievementsforintellij.storage

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic
import eu.andret.plugin.achievementsforintellij.MyBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementsRegistry
import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementDefinition

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
        var version: Int = 1,
        var achievements: MutableMap<String, Long> = LinkedHashMap(),
        // Logs of reached steps, at most one per step index
        var logs: MutableMap<String, MutableList<AchievementLog>> = LinkedHashMap()
    )

    private var myState: State = State()
    private val LOG: Logger = Logger.getInstance(AchievementsService::class.java)

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun increment(achievementId: String, delta: Long = 1): Long = synchronized(this) {
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
                val event = AchievementEvent(achievementId, i, newValue, now)
                publishEvent(event)
                showNotification(def, i)
            }
        }
        newValue
    }

    fun get(achievementId: String): Long = myState.achievements[achievementId] ?: 0L

    fun getAll(): Map<String, Long> = HashMap(myState.achievements)

    fun reset(achievementId: String) = synchronized(this) {
        myState.achievements.remove(achievementId)
        myState.logs.remove(achievementId)
    }

    fun clearAll() = synchronized(this) {
        myState.achievements.clear()
        myState.logs.clear()
    }

    fun getDefinition(achievementId: String): AchievementDefinition? = AchievementsRegistry.get(achievementId)

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
            val prevThreshold = if (lastIdx >= 0) def.steps[lastIdx].threshold else 0L
            val span = (nextThreshold - prevThreshold).coerceAtLeast(1)
            val done = (count - prevThreshold).coerceAtLeast(0)
            ((done.toDouble() / span.toDouble()) * 100.0).toInt().coerceIn(0, 100)
        }
        return AchievementProgress(count, lastIdx, nextThreshold, percent)
    }

    fun getLogs(achievementId: String): List<AchievementLog> = synchronized(this) {
        ensureLogList(achievementId)
        myState.logs[achievementId]?.toList() ?: ArrayList()
    }

    fun clearLogs(achievementId: String) = synchronized(this) {
        myState.logs.remove(achievementId)
    }

    private fun ensureLogList(achievementId: String) {
        if (myState.logs[achievementId] == null) {
            myState.logs[achievementId] = mutableListOf()
        }
    }

    private fun publishEvent(event: AchievementEvent) {
        ApplicationManager.getApplication().messageBus.syncPublisher(TOPIC).onStepReached(event)
    }

    private fun showNotification(def: AchievementDefinition, stepIndex: Int) {
        val group = NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATIONS_GROUP_ID)
        val title = "🏆 Achievement Unlocked!"
        val achievementName = MyBundle.message(def.nameKey)
        val description = renderDescription(def.id, stepIndex)
        val content = "<b>$achievementName</b><br/>$description"
        group.createNotification(title, content, NotificationType.INFORMATION).notify(null)
    }

    companion object {
        const val NOTIFICATIONS_GROUP_ID: String = "achievements.notifications"

        interface AchievementListener {
            fun onStepReached(event: AchievementEvent)
        }

        data class AchievementEvent(
            val id: String,
            val stepIndex: Int,
            val count: Long,
            val timestamp: Long,
        )

        @JvmField
        val TOPIC: Topic<AchievementListener> = Topic.create(
            "AchievementsStepReached",
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
            MyBundle.message(def.descriptionKey, threshold)
        } else {
            MyBundle.message(def.descriptionKey)
        }
    }
}
