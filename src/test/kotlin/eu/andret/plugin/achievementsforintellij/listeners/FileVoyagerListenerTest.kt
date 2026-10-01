package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import org.assertj.core.api.Assertions.assertThat

class FileVoyagerListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: FileVoyagerListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = FileVoyagerListener()
    }

    fun `test opening files of different types counts each type`() {
        open("Main.kt", "Main.java", "layout.xml")

        assertThat(service.get(AchievementIds.FILE_VOYAGER)).isEqualTo(3L)
    }

    fun `test opening files of the same type counts it once`() {
        open("First.kt", "Second.kt")

        assertThat(service.get(AchievementIds.FILE_VOYAGER)).isEqualTo(1L)
    }

    fun `test extensions differing only in case count as one type`() {
        open("Main.java", "Legacy.JAVA")

        assertThat(service.get(AchievementIds.FILE_VOYAGER)).isEqualTo(1L)
    }

    fun `test files without an extension do not count`() {
        open("Makefile", "Dockerfile")

        assertThat(service.get(AchievementIds.FILE_VOYAGER)).isZero()
    }

    // addFileToProject does not open the file, so only the listener under test sees it
    private fun open(vararg fileNames: String) {
        val editorManager = FileEditorManager.getInstance(project)
        fileNames.forEach { listener.fileOpened(editorManager, myFixture.addFileToProject(it, "").virtualFile) }
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
