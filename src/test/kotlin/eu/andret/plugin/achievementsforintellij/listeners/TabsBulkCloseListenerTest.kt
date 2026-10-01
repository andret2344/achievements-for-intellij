package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import org.assertj.core.api.Assertions.assertThat

class TabsBulkCloseListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: TabsBulkCloseListener

    private val action = object : AnAction() {
        override fun actionPerformed(e: AnActionEvent) {}
    }

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = TabsBulkCloseListener()
    }

    fun `test closing 10+ tabs updates bulk close achievement`() {
        val files = openFiles("File", 15)

        closeInOneAction(files.take(12))

        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isEqualTo(12L)
    }

    fun `test closing less than 10 tabs does not update achievement`() {
        val files = openFiles("File", 5)

        closeInOneAction(files)

        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isZero()
    }

    fun `test achievement stores maximum closed count`() {
        closeInOneAction(openFiles("FileA", 10))
        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isEqualTo(10L)

        closeInOneAction(openFiles("FileB", 15))
        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isEqualTo(15L)

        closeInOneAction(openFiles("FileC", 11))
        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isEqualTo(15L)
    }

    fun `test action without project is ignored`() {
        val event = TestActionEvent.createTestEvent()

        listener.beforeActionPerformed(action, event)
        listener.afterActionPerformed(action, event, AnActionResult.PERFORMED)

        assertThat(service.get(AchievementIds.CLEAN_SWEEP)).isZero()
    }

    private fun openFiles(prefix: String, count: Int): List<VirtualFile> {
        val editorManager = FileEditorManager.getInstance(project)
        return (1..count).map {
            myFixture.configureByText("$prefix$it.txt", "content $it").virtualFile
        }.onEach { editorManager.openFile(it, false) }
    }

    private fun closeInOneAction(files: List<VirtualFile>) {
        val editorManager = FileEditorManager.getInstance(project)
        val event = TestActionEvent.createTestEvent(SimpleDataContext.getProjectContext(project))

        listener.beforeActionPerformed(action, event)
        files.forEach { editorManager.closeFile(it) }
        listener.afterActionPerformed(action, event, AnActionResult.PERFORMED)
    }

    override fun tearDown() {
        try {
            service.clearAll()
            FileEditorManager.getInstance(project).openFiles.forEach {
                FileEditorManager.getInstance(project).closeFile(it)
            }
        } finally {
            super.tearDown()
        }
    }
}
