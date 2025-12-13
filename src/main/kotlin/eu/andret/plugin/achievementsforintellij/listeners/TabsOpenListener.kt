package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

/**
 * Tracks the maximum number of concurrent open tabs.
 */
internal class TabsOpenListener : FileEditorManagerListener {

    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val service = AchievementsService.getInstance()
        val openTabsCount = source.openFiles.size.toLong()
        val current = service.get(AchievementIds.OPEN_TABS_CONCURRENT)

        if (openTabsCount > current) {
            service.increment(AchievementIds.OPEN_TABS_CONCURRENT, openTabsCount - current)
        }
    }
}
