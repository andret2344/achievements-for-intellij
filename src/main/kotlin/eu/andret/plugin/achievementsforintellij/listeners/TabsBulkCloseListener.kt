package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks bulk tab closing via actions (e.g., Close Others/All).
 * Updates achievement when 10+ tabs are closed in one action.
 */
internal class TabsBulkCloseListener : AnActionListener {

    private val beforeCounts = ConcurrentHashMap<Project, Int>()

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
        event.project?.let { project ->
            beforeCounts[project] = FileEditorManager.getInstance(project).openFiles.size
        }
    }

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
        val project = event.project ?: return
        val before = beforeCounts.remove(project) ?: return
        val after = FileEditorManager.getInstance(project).openFiles.size
        val closed = (before - after).toLong()

        if (closed >= 10) {
            val service = AchievementsService.getInstance()
            val current = service.get(AchievementIds.CLOSED_TABS_BULK)
            if (closed > current) {
                service.increment(AchievementIds.CLOSED_TABS_BULK, closed - current)
            }
        }
    }
}
