package eu.andret.plugin.achievementsforintellij.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import eu.andret.plugin.achievementsforintellij.achievements.AchievementIds
import org.assertj.core.api.Assertions.assertThat

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

        assertThat(changes).isEqualTo(1)
    }

    fun `test increment by zero does not publish change`() {
        service.increment(AchievementIds.FILE_VOYAGER, 0)

        assertThat(changes).isZero()
    }

    fun `test clearAll publishes change`() {
        service.clearAll()

        assertThat(changes).isEqualTo(1)
    }

    fun `test clearAll clears file voyager extensions`() {
        val fileVoyagerService = FileVoyagerService.getInstance()
        fileVoyagerService.addExtension("kt")
        fileVoyagerService.addExtension("java")

        service.clearAll()

        assertThat(fileVoyagerService.state.openedExtensions).isEmpty()
    }

    fun `test getState returns a snapshot not affected by later changes`() {
        service.increment(AchievementIds.EASTER_EGG)
        val snapshot = service.state

        service.increment(AchievementIds.EASTER_EGG)

        assertThat(snapshot.achievements).containsEntry(AchievementIds.EASTER_EGG, 1L)
        assertThat(snapshot.logs[AchievementIds.EASTER_EGG]).hasSize(1)
    }

    fun `test getAllLogs returns one entry per reached step`() {
        service.increment(AchievementIds.TAB_AVALANCHE, 25)

        val logs = service.getAllLogs()[AchievementIds.TAB_AVALANCHE]

        assertThat(logs?.map { it.stepIndex }).containsExactly(0, 1)
    }

    fun `test raiseTo raises counter to a higher value`() {
        service.raiseTo(AchievementIds.TAB_AVALANCHE, 12)

        assertThat(service.get(AchievementIds.TAB_AVALANCHE)).isEqualTo(12L)
        assertThat(changes).isEqualTo(1)
    }

    fun `test raiseTo keeps counter and does not publish for a lower value`() {
        service.raiseTo(AchievementIds.TAB_AVALANCHE, 12)

        service.raiseTo(AchievementIds.TAB_AVALANCHE, 7)

        assertThat(service.get(AchievementIds.TAB_AVALANCHE)).isEqualTo(12L)
        assertThat(changes).isEqualTo(1)
    }

    fun `test raiseTo logs each newly reached step`() {
        service.raiseTo(AchievementIds.TAB_AVALANCHE, 10)
        service.raiseTo(AchievementIds.TAB_AVALANCHE, 30)

        val logs = service.getAllLogs()[AchievementIds.TAB_AVALANCHE]

        assertThat(logs?.map { it.stepIndex }).containsExactly(0, 1)
    }

    fun `test loadState stores the current version`() {
        service.loadState(AchievementsService.State())

        assertThat(service.state.version).isEqualTo(1)
    }

    override fun tearDown() {
        try {
            service.clearAll()
        } finally {
            super.tearDown()
        }
    }
}
