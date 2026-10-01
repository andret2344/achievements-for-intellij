package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import eu.andret.plugin.achievementsforintellij.AchievementsBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService

internal class EasterEggAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        AchievementsService.getInstance().raiseTo(AchievementIds.EASTER_EGG, 1)

        Messages.showDialog(
            AchievementsBundle.message("achievement.easter-egg.modal.content"),
            AchievementsBundle.message("achievement.easter-egg.modal.title"),
            arrayOf(AchievementsBundle.message("ok")),
            0,
            null
        )
    }
}
