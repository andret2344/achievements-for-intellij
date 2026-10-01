package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcsUtil.VcsUtil
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService

private val LOG = logger<CodeNecromancerListener>()
private const val TWO_YEARS_MILLIS = 2L * 365 * 24 * 60 * 60 * 1000

internal fun isOlderThanTwoYears(lastChangeMillis: Long, nowMillis: Long): Boolean =
    lastChangeMillis < nowMillis - TWO_YEARS_MILLIS

internal class CodeNecromancerListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val project = source.project
        if (project.isDisposed) return
        // Reading the file history is expensive, skip it once the achievement is complete
        if (AchievementsService.getInstance().getProgress(AchievementIds.CODE_NECROMANCER).nextThreshold == null) return

        val fileStatus = FileStatusManager.getInstance(project).getStatus(file)
        if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.IGNORED) return

        val filePath = VcsUtil.getFilePath(file)
        val vcs = VcsUtil.getVcsFor(project, filePath) ?: return
        val historyProvider = vcs.vcsHistoryProvider ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            if (project.isDisposed) return@executeOnPooledThread
            try {
                val session = historyProvider.createSessionFor(filePath) ?: return@executeOnPooledThread
                val revisionList = session.revisionList
                if (revisionList.isEmpty()) return@executeOnPooledThread

                val lastRevision = revisionList.first()
                val revisionDate = lastRevision.revisionDate ?: return@executeOnPooledThread

                if (isOlderThanTwoYears(revisionDate.time, System.currentTimeMillis())) {
                    AchievementsService.getInstance().raiseTo(AchievementIds.CODE_NECROMANCER, 1)
                }
            } catch (e: Exception) {
                if (e is ControlFlowException) throw e
                LOG.warn("Failed to read VCS history of ${file.path}", e)
            }
        }
    }
}
