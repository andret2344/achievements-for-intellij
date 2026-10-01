package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import eu.andret.plugin.achievementsforintellij.AchievementsIcons
import eu.andret.plugin.achievementsforintellij.ui.AchievementsDialog

// Text and description come from the bundle via plugin.xml
internal class ShowAchievementsAction : DumbAwareAction(AchievementsIcons.PluginIcon) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        AchievementsDialog.showOrFocus(e.project)
    }
}
