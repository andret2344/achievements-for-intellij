package eu.andret.plugin.achievementsforintellij.action

import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ShowAchievementsActionTest : BasePlatformTestCase() {

    private lateinit var action: ShowAchievementsAction

    override fun setUp() {
        super.setUp()
        action = ShowAchievementsAction()
    }

    fun `test actionPerformed does not throw exception`() {
        val event = TestActionEvent.createTestEvent(action)

        // Should not throw
        action.actionPerformed(event)
    }
}
