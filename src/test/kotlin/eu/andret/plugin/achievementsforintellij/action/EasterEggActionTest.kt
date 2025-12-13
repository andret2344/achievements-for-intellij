package eu.andret.plugin.achievementsforintellij.action

import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.ui.TestDialogManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService

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
        assertEquals(0L, initialCount)

        val event = TestActionEvent.createTestEvent(action)
        action.actionPerformed(event)

        val newCount = service.get(AchievementIds.EASTER_EGG)
        assertEquals(1L, newCount)
    }

    fun `test multiple clicks increment achievement`() {
        val event = TestActionEvent.createTestEvent(action)

        action.actionPerformed(event)
        action.actionPerformed(event)
        action.actionPerformed(event)

        val count = service.get(AchievementIds.EASTER_EGG)
        assertEquals(3L, count)
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
