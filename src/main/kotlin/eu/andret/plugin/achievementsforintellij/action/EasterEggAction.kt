package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import eu.andret.plugin.achievementsforintellij.MyBundle

internal class EasterEggAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        Messages.showDialog(
            MyBundle.message("EasterEgg.found"),
            MyBundle.message("EasterEgg.name"),
            arrayOf(MyBundle.message("Ok")),
            1,
            null
        )
    }
}
