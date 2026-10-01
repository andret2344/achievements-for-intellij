package eu.andret.plugin.achievementsforintellij.listeners

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat

class CodeNecromancerListenerTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var listener: CodeNecromancerListener

    private val now = TimeUnit.DAYS.toMillis(10_000)

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        listener = CodeNecromancerListener()
    }

    fun `test file changed over two years ago qualifies`() {
        val lastChange = now - TimeUnit.DAYS.toMillis(2 * 365 + 1)

        assertThat(isOlderThanTwoYears(lastChange, now)).isTrue()
    }

    fun `test file changed exactly two years ago does not qualify`() {
        val lastChange = now - TimeUnit.DAYS.toMillis(2 * 365)

        assertThat(isOlderThanTwoYears(lastChange, now)).isFalse()
    }

    fun `test recently changed file does not qualify`() {
        val lastChange = now - TimeUnit.DAYS.toMillis(30)

        assertThat(isOlderThanTwoYears(lastChange, now)).isFalse()
    }

    fun `test opening file without VCS does not increment achievement`() {
        val file = myFixture.configureByText("NoVcsFile.txt", "content without vcs").virtualFile

        listener.fileOpened(FileEditorManager.getInstance(project), file)

        assertThat(service.get(AchievementIds.CODE_NECROMANCER)).isZero()
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
