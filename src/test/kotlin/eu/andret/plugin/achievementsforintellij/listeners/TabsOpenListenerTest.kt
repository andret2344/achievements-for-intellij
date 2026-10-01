package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import org.assertj.core.api.Assertions.assertThat

class TabsOpenListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: TabsOpenListener

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = TabsOpenListener()
    }

    fun `test opening file updates max concurrent tabs achievement`() {
        val file1 = myFixture.configureByText("File1.txt", "content1").virtualFile
        val file2 = myFixture.configureByText("File2.txt", "content2").virtualFile

        val editorManager = FileEditorManager.getInstance(project)

        editorManager.openFile(file1, true)
        listener.fileOpened(editorManager, file1)

        val countAfterOne = service.get(AchievementIds.TAB_AVALANCHE)
        assertThat(countAfterOne).isGreaterThanOrEqualTo(1L)

        editorManager.openFile(file2, true)
        listener.fileOpened(editorManager, file2)

        val countAfterTwo = service.get(AchievementIds.TAB_AVALANCHE)
        assertThat(countAfterTwo).isGreaterThanOrEqualTo(countAfterOne)
    }

    fun `test achievement only increases when new max is reached`() {
        val file1 = myFixture.configureByText("File1.txt", "content").virtualFile
        val editorManager = FileEditorManager.getInstance(project)

        editorManager.openFile(file1, true)
        listener.fileOpened(editorManager, file1)
        val firstCount = service.get(AchievementIds.TAB_AVALANCHE)

        // Opening same file again shouldn't increase beyond actual open tabs
        listener.fileOpened(editorManager, file1)
        val secondCount = service.get(AchievementIds.TAB_AVALANCHE)

        assertThat(secondCount).isEqualTo(firstCount)
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
