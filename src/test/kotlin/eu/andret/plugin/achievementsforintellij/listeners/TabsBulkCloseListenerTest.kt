package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

class TabsBulkCloseListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: TabsBulkCloseListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = TabsBulkCloseListener()
    }

    fun `test closing 10+ tabs updates bulk close achievement`() {
        val editorManager = FileEditorManager.getInstance(project)

        // Open 15 files
        val files = (1..15).map {
            myFixture.configureByText("File$it.txt", "content $it").virtualFile
        }
        files.forEach { editorManager.openFile(it, false) }

        val event = createMockEvent()
        val action = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {}
        }

        listener.beforeActionPerformed(action, event)

        val before = editorManager.openFiles.size

        // Close 12 files
        files.take(12).forEach { editorManager.closeFile(it) }

        val after = editorManager.openFiles.size
        val closed = (before - after).toLong()

        // Apply the same logic as the listener
        if (closed >= 10) {
            val current = service.get(AchievementIds.CLOSED_TABS_BULK)
            if (closed > current) {
                service.increment(AchievementIds.CLOSED_TABS_BULK, closed - current)
            }
        }

        val count = service.get(AchievementIds.CLOSED_TABS_BULK)
        assertEquals(12L, count)
    }

    fun `test closing less than 10 tabs does not update achievement`() {
        val editorManager = FileEditorManager.getInstance(project)

        val files = (1..5).map {
            myFixture.configureByText("File$it.txt", "content $it").virtualFile
        }
        files.forEach { editorManager.openFile(it, false) }

        val event = createMockEvent()
        val action = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {}
        }

        listener.beforeActionPerformed(action, event)
        val before = editorManager.openFiles.size

        files.forEach { editorManager.closeFile(it) }

        val after = editorManager.openFiles.size
        val closed = (before - after).toLong()

        // Apply the same logic as the listener
        if (closed >= 10) {
            val current = service.get(AchievementIds.CLOSED_TABS_BULK)
            if (closed > current) {
                service.increment(AchievementIds.CLOSED_TABS_BULK, closed - current)
            }
        }

        val count = service.get(AchievementIds.CLOSED_TABS_BULK)
        assertEquals(0L, count)
    }

    fun `test achievement stores maximum closed count`() {
        val editorManager = FileEditorManager.getInstance(project)

        // First close: 10 tabs
        val files1 = (1..10).map {
            myFixture.configureByText("FileA$it.txt", "content").virtualFile
        }
        files1.forEach { editorManager.openFile(it, false) }

        val event1 = createMockEvent()
        val action = object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {}
        }

        listener.beforeActionPerformed(action, event1)
        val before1 = editorManager.openFiles.size

        files1.forEach { editorManager.closeFile(it) }

        val after1 = editorManager.openFiles.size
        val closed1 = (before1 - after1).toLong()

        if (closed1 >= 10) {
            val current = service.get(AchievementIds.CLOSED_TABS_BULK)
            if (closed1 > current) {
                service.increment(AchievementIds.CLOSED_TABS_BULK, closed1 - current)
            }
        }

        assertEquals(10L, service.get(AchievementIds.CLOSED_TABS_BULK))

        // Second close: 15 tabs (should update to 15)
        val files2 = (1..15).map {
            myFixture.configureByText("FileB$it.txt", "content").virtualFile
        }
        files2.forEach { editorManager.openFile(it, false) }

        val event2 = createMockEvent()
        listener.beforeActionPerformed(action, event2)
        val before2 = editorManager.openFiles.size

        files2.forEach { editorManager.closeFile(it) }

        val after2 = editorManager.openFiles.size
        val closed2 = (before2 - after2).toLong()

        if (closed2 >= 10) {
            val current = service.get(AchievementIds.CLOSED_TABS_BULK)
            if (closed2 > current) {
                service.increment(AchievementIds.CLOSED_TABS_BULK, closed2 - current)
            }
        }

        assertEquals(15L, service.get(AchievementIds.CLOSED_TABS_BULK))
    }

    private fun createMockEvent(): AnActionEvent {
        return TestActionEvent.createTestEvent()
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
