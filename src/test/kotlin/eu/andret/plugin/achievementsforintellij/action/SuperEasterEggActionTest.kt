package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

class SuperEasterEggActionTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private lateinit var action: SuperEasterEggAction

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        action = SuperEasterEggAction()

        // Configure test dialog to return OK (0) instead of throwing exception
        TestDialogManager.setTestDialog(TestDialog.OK)
    }

    fun `test actionPerformed increments super easter egg achievement`() {
        val initialCount = service.get(AchievementIds.SUPER_EASTER_EGG)
        assertEquals(0L, initialCount)

        val event = TestActionEvent.createTestEvent(action)
        action.actionPerformed(event)

        val newCount = service.get(AchievementIds.SUPER_EASTER_EGG)
        assertEquals(1L, newCount)
    }

    fun `test multiple invocations increment achievement`() {
        val event = TestActionEvent.createTestEvent(action)

        repeat(5) {
            action.actionPerformed(event)
        }

        val count = service.get(AchievementIds.SUPER_EASTER_EGG)
        assertEquals(5L, count)
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
