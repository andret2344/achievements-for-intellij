package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.FileVoyagerService
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

/**
 * Listener that tracks when files of different types are opened.
 */
internal class FileVoyagerListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val extension = file.extension ?: "no-extension"
        val service = FileVoyagerService.getInstance()
        val count = service.addExtension(extension)

        AchievementsService.getInstance().increment(AchievementIds.FILE_VOYAGER, 0)

        val achievementsService = AchievementsService.getInstance()
        val currentCount = achievementsService.get(AchievementIds.FILE_VOYAGER)
        if (count > currentCount) {
            achievementsService.increment(AchievementIds.FILE_VOYAGER, count - currentCount)
        }
    }
}
