package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import org.assertj.core.api.Assertions.assertThat

class EasterEggActionTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var action: EasterEggAction

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        action = EasterEggAction()

        // Configure test dialog to return OK (0) instead of throwing exception
        TestDialogManager.setTestDialog(TestDialog.OK)
    }

    fun `test actionPerformed increments easter egg achievement`() {
        val initialCount = service.get(AchievementIds.EASTER_EGG)
        assertThat(initialCount).isZero()

        val event = TestActionEvent.createTestEvent(action)
        action.actionPerformed(event)

        val newCount = service.get(AchievementIds.EASTER_EGG)
        assertThat(newCount).isEqualTo(1L)
    }

    fun `test multiple clicks unlock achievement only once`() {
        val event = TestActionEvent.createTestEvent(action)

        action.actionPerformed(event)
        action.actionPerformed(event)
        action.actionPerformed(event)

        val count = service.get(AchievementIds.EASTER_EGG)
        assertThat(count).isEqualTo(1L)
    }

    override fun tearDown() {
        try {
            service.clearAll()
            TestDialogManager.setTestDialog(TestDialog.DEFAULT)
        } finally {
            super.tearDown()
        }
    }
}
