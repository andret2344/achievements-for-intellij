package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import org.assertj.core.api.Assertions.assertThat

class LongFileOpenListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: LongFileOpenListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = LongFileOpenListener()
    }

    fun `test opening file with 1000+ lines unlocks achievement`() {
        // Create a file with 1001 lines
        val lines = (1..1001).map { "Line $it" }
        val content = lines.joinToString("\n") + "\n"

        val count = openWithListener("LongFile.txt", content)

        assertThat(count).isEqualTo(1L)
    }

    fun `test opening file with less than 1000 lines does not unlock achievement`() {
        // Create a file with 999 lines (no trailing newline = 999 lines in Document)
        val lines = (1..999).map { "Line $it" }
        val content = lines.joinToString("\n")

        val count = openWithListener("ShortFile.txt", content)

        assertThat(count).isZero()
    }

    fun `test opening exactly 1000 lines unlocks achievement`() {
        // Create a file with exactly 1000 lines
        val lines = (1..1000).map { "Line $it" }
        val content = lines.joinToString("\n") + "\n"

        val count = openWithListener("ExactFile.txt", content)

        assertThat(count).isEqualTo(1L)
    }

    private fun openWithListener(fileName: String, content: String): Long {
        val file = myFixture.configureByText(fileName, content).virtualFile
        assertThat(FileDocumentManager.getInstance().getDocument(file)).isNotNull()

        // configureByText already opened the file, so the registered listener may have unlocked it
        service.clearAll()
        listener.fileOpened(FileEditorManager.getInstance(project), file)

        return service.get(AchievementIds.MILLENNIUM_FILE)
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
