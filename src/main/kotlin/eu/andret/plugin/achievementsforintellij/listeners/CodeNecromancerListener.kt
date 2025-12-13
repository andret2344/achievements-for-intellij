package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

internal class CodeNecromancerListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val project = source.project
        if (project.isDisposed) return

        val fileStatus = FileStatusManager.getInstance(project).getStatus(file)
        if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.IGNORED) return

        val filePath = VcsUtil.getFilePath(file)
        val vcs = VcsUtil.getVcsFor(project, filePath) ?: return
        val historyProvider = vcs.vcsHistoryProvider ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val session = historyProvider.createSessionFor(filePath) ?: return@executeOnPooledThread
                val revisionList = session.revisionList
                if (revisionList.isEmpty()) return@executeOnPooledThread

                val lastRevision = revisionList.first()
                val revisionDate = lastRevision.revisionDate ?: return@executeOnPooledThread

                val twoYearsAgo = System.currentTimeMillis() - (2L * 365 * 24 * 60 * 60 * 1000)
                if (revisionDate.time < twoYearsAgo) {
                    AchievementsService.getInstance().increment(AchievementIds.CODE_NECROMANCER)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
