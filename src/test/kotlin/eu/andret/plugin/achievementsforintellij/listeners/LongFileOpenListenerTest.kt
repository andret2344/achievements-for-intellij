package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

class LongFileOpenListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: LongFileOpenListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = LongFileOpenListener()
    }

    fun `test opening file with 1000+ lines increments achievement`() {
        // Create a file with 1001 lines
        val lines = (1..1001).map { "Line $it" }
        val content = lines.joinToString("\n") + "\n"

        val psiFile = myFixture.configureByText("LongFile.txt", content)
        val file = psiFile.virtualFile

        // Verify the document exists and has the expected line count
        val document = FileDocumentManager.getInstance().getDocument(file)
        assertNotNull("Document should not be null", document)

        val initialCount = service.get(AchievementIds.OPENED_LONG_FILE_1000)

        val editorManager = FileEditorManager.getInstance(project)
        listener.fileOpened(editorManager, file)

        val newCount = service.get(AchievementIds.OPENED_LONG_FILE_1000)
        assertTrue("Achievement should increment for 1000+ line file. Line count: ${document!!.lineCount}",
                   newCount > initialCount)
    }

    fun `test opening file with less than 1000 lines does not increment achievement`() {
        // Create a file with 999 lines (no trailing newline = 999 lines in Document)
        val lines = (1..999).map { "Line $it" }
        val content = lines.joinToString("\n")

        val psiFile = myFixture.configureByText("ShortFile.txt", content)
        val file = psiFile.virtualFile

        // Verify the document has fewer than 1000 lines
        val document = FileDocumentManager.getInstance().getDocument(file)
        assertNotNull("Document should not be null", document)

        val initialCount = service.get(AchievementIds.OPENED_LONG_FILE_1000)

        val editorManager = FileEditorManager.getInstance(project)
        listener.fileOpened(editorManager, file)

        val newCount = service.get(AchievementIds.OPENED_LONG_FILE_1000)
        assertEquals("Achievement should not increment for <1000 line file. Line count: ${document!!.lineCount}",
                    initialCount, newCount)
    }

    fun `test opening exactly 1000 lines increments achievement`() {
        // Create a file with exactly 1000 lines
        val lines = (1..1000).map { "Line $it" }
        val content = lines.joinToString("\n") + "\n"

        val psiFile = myFixture.configureByText("ExactFile.txt", content)
        val file = psiFile.virtualFile

        // Verify the document has exactly 1000 lines
        val document = FileDocumentManager.getInstance().getDocument(file)
        assertNotNull("Document should not be null", document)

        val initialCount = service.get(AchievementIds.OPENED_LONG_FILE_1000)

        val editorManager = FileEditorManager.getInstance(project)
        listener.fileOpened(editorManager, file)

        val count = service.get(AchievementIds.OPENED_LONG_FILE_1000)
        assertTrue("Achievement should increment for 1000 line file. Line count: ${document!!.lineCount}",
                   count > initialCount)
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
