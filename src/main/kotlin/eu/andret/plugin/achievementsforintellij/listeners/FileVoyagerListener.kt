package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import eu.andret.plugin.achievementsforintellij.services.FileVoyagerService

/**
 * Listener that tracks when files of different types are opened.
 */
internal class FileVoyagerListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val extension = file.extension ?: "no-extension"
        val count = FileVoyagerService.getInstance().addExtension(extension)
        AchievementsService.getInstance().raiseTo(AchievementIds.FILE_VOYAGER, count.toLong())
    }
}
