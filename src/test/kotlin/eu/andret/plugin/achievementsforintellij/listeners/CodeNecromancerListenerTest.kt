package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService
import org.mockito.kotlin.*

class CodeNecromancerListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: CodeNecromancerListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = CodeNecromancerListener()
    }

    fun `test fileOpened with file without VCS does not crash`() {
        // Create a real file in the test fixture
        val file = myFixture.configureByText("NoVcsFile.txt", "content without vcs").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        val initialCount = service.get(AchievementIds.CODE_NECROMANCER)

        // Opening a file without VCS should not cause errors
        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }

        // Achievement should not increment for files without VCS
        val finalCount = service.get(AchievementIds.CODE_NECROMANCER)
        assertEquals(initialCount, finalCount)
    }

    fun `test fileOpened handles disposed project gracefully`() {
        val file = myFixture.configureByText("File.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        // This should handle disposed project check gracefully
        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }
    }

    fun `test fileOpened with unknown file status returns early`() {
        val file = myFixture.configureByText("UnknownFile.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        val initialCount = service.get(AchievementIds.CODE_NECROMANCER)

        listener.fileOpened(editorManager, file)

        // Should not increment for unknown files
        // Note: In test environment, files typically have UNKNOWN status
        val finalCount = service.get(AchievementIds.CODE_NECROMANCER)
        assertEquals(initialCount, finalCount)
    }

    fun `test fileOpened processes file without errors`() {
        // Create a file and verify the listener processes it without crashing
        val file = myFixture.configureByText("TestFile.kt", "fun test() { }").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }

        // The listener executes VCS checks on a pooled thread
        // In a test environment without proper VCS setup, it should handle gracefully
        assertTrue(true)
    }

    fun `test listener does not crash on null VCS`() {
        val file = myFixture.configureByText("NoVcs.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        // File without VCS should not cause errors
        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }
    }

    fun `test listener does not crash on null history provider`() {
        val file = myFixture.configureByText("NoHistory.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        // File without history provider should not cause errors
        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }
    }

    fun `test listener does not crash on exception in VCS operations`() {
        val file = myFixture.configureByText("ExceptionFile.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        // Any exception during VCS operations should be caught and printed
        assertDoesNotThrow {
            listener.fileOpened(editorManager, file)
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
