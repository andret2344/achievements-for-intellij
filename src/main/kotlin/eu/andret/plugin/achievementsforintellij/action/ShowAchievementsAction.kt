package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import eu.andret.plugin.achievementsforintellij.AchievementsIcons
import eu.andret.plugin.achievementsforintellij.ui.AchievementsDialog

internal class ShowAchievementsAction : DumbAwareAction(
    "Achievements",
    "Show achievements",
    AchievementsIcons.PluginIcon
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val dialog = AchievementsDialog(project)
        dialog.show()
    }
}
