package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

internal class LongFileOpenListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val document: Document = FileDocumentManager.getInstance().getDocument(file) ?: return
        if (document.lineCount >= 1000) {
            AchievementsService.getInstance().increment(AchievementIds.OPENED_LONG_FILE_1000)
        }
    }
}
