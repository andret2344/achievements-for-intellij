package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import eu.andret.plugin.achievementsforintellij.MyBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

internal class SuperEasterEggAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        // Grant the Super Easter Egg achievement
        AchievementsService.getInstance().increment(AchievementIds.SUPER_EASTER_EGG)

        Messages.showDialog(
            MyBundle.message("achievement.super-easter-egg.modal.content"),
            MyBundle.message("achievement.super-easter-egg.modal.title"),
            arrayOf(MyBundle.message("ok")),
            1,
            null
        )
    }
}
