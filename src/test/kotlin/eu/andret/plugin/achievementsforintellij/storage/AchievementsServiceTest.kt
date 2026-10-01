package eu.andret.plugin.achievementsforintellij.storage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds

class AchievementsServiceTest : BasePlatformTestCase() {

    private lateinit var service: AchievementsService
    private var changes = 0

    override fun setUp() {
        super.setUp()
        service = AchievementsService.getInstance()
        service.clearAll()
        ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
            .subscribe(AchievementsService.TOPIC, AchievementsService.AchievementListener { changes++ })
    }

    fun `test increment publishes change`() {
        service.increment(AchievementIds.EASTER_EGG)

        assertEquals(1, changes)
    }

    fun `test increment by zero does not publish change`() {
        service.increment(AchievementIds.FILE_VOYAGER, 0)

        assertEquals(0, changes)
    }

    fun `test reset publishes change`() {
        service.reset(AchievementIds.EASTER_EGG)

        assertEquals(1, changes)
    }

    fun `test clearAll publishes change`() {
        service.clearAll()

        assertEquals(1, changes)
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
