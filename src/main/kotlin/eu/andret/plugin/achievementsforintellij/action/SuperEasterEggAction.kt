package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import eu.andret.plugin.achievementsforintellij.AchievementsBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService

internal class SuperEasterEggAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        // Grant the Super Easter Egg achievement
        AchievementsService.getInstance().raiseTo(AchievementIds.SUPER_EASTER_EGG, 1)

        Messages.showDialog(
            AchievementsBundle.message("achievement.super-easter-egg.modal.content"),
            AchievementsBundle.message("achievement.super-easter-egg.modal.title"),
            arrayOf(AchievementsBundle.message("ok")),
            0,
            null
        )
    }
}
