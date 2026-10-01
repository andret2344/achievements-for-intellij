package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService

/**
 * Tracks the maximum number of concurrent open tabs.
 */
internal class TabsOpenListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        AchievementsService.getInstance().raiseTo(AchievementIds.TAB_AVALANCHE, source.openFiles.size.toLong())
    }
}
