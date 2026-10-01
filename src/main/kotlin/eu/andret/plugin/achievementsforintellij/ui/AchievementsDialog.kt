package eu.andret.plugin.achievementsforintellij.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBColor
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import eu.andret.plugin.achievementsforintellij.AchievementsBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementsRegistry
import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementDefinition
import eu.andret.plugin.achievementsforintellij.services.AchievementsService
import eu.andret.plugin.achievementsforintellij.services.AchievementsService.AchievementLog
import eu.andret.plugin.achievementsforintellij.services.AchievementsService.AchievementProgress
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.JSeparator

internal class AchievementsDialog private constructor(project: Project?) :
    DialogWrapper(project, true, IdeModalityType.MODELESS) {

    private val service = AchievementsService.getInstance()
    private lateinit var mainPanel: JPanel
    private lateinit var scrollPane: JBScrollPane
    private lateinit var logsPanel: JPanel
    private val logsModel = CollectionListModel<String>()
    private val completedMessage by lazy { AchievementsBundle.message("progress.completed") }
    private val uncompletedMessage by lazy { AchievementsBundle.message("progress.uncompleted") }
    private val stepsCompletedMessage by lazy { AchievementsBundle.message("progress.steps.completed") }
    private val hiddenNameMessage by lazy { AchievementsBundle.message("achievements.hidden.name") }
    private val hiddenDescMessage by lazy { AchievementsBundle.message("achievements.hidden.description") }

    init {
        title = AchievementsBundle.message("dialog.title")
        init()
        ApplicationManager.getApplication().messageBus.connect(disposable)
            .subscribe(AchievementsService.TOPIC, AchievementsService.AchievementListener {
                ApplicationManager.getApplication().invokeLater({
                    if (!isDisposed) {
                        refreshContent()
                    }
                }, ModalityState.any())
            })
    }

    companion object {
        private val COLOR_GREEN = JBColor(Color(0, 128, 0), Color(76, 175, 80))
        private val COLOR_ORANGE = JBColor(Color(204, 102, 0), Color(255, 200, 87))
        private val COLOR_RED = JBColor(Color(204, 0, 0), Color(244, 67, 54))

        // Achievements are app-wide, so one dialog is enough; accessed on EDT only
        private var openDialog: AchievementsDialog? = null

        fun showOrFocus(project: Project?) {
            openDialog?.let {
                it.window.toFront()
                return
            }
            AchievementsDialog(project).apply {
                openDialog = this
                Disposer.register(disposable) { openDialog = null }
                show()
            }
        }
    }

    // Lets the platform remember the size and position the user left the dialog with
    override fun getDimensionServiceKey(): String = "eu.andret.plugin.achievementsforintellij.AchievementsDialog"

    override fun createActions(): Array<Action> {
        val resetAction = object : AbstractAction(AchievementsBundle.message("dialog.button.reset.caption")) {
            override fun actionPerformed(e: ActionEvent) {
                if (Messages.showYesNoDialog(
                        AchievementsBundle.message("dialog.modal.reset.message"),
                        AchievementsBundle.message("dialog.modal.reset.title"),
                        Messages.getWarningIcon()
                    ) == Messages.YES
                ) {
                    service.clearAll()
                }
            }
        }
        return arrayOf(resetAction, okAction)
    }

    override fun createCenterPanel(): JComponent {
        mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
            preferredSize = JBUI.size(800, 600)
        }

        // Add overall progress bar at the top
        mainPanel.add(createOverallProgressPanel(), BorderLayout.NORTH)

        // Add scrollable achievements list
        scrollPane = JBScrollPane(createAchievementsListPanel()).apply {
            border = JBUI.Borders.empty()
        }
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // Add collapsible logs below the list
        logsPanel = createLogsPanel()
        mainPanel.add(logsPanel, BorderLayout.SOUTH)
        updateLogs()

        return mainPanel
    }

    // Rebuilds only the center panel: contentPane is the dialog root and also holds the buttons
    private fun refreshContent() {
        val viewPosition = scrollPane.viewport.viewPosition
        mainPanel.removeAll()
        mainPanel.add(createOverallProgressPanel(), BorderLayout.NORTH)
        scrollPane.setViewportView(createAchievementsListPanel())
        scrollPane.viewport.viewPosition = viewPosition
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        mainPanel.add(logsPanel, BorderLayout.SOUTH)
        updateLogs()
        mainPanel.revalidate()
        mainPanel.repaint()
    }

    // The expanded list takes height from the achievements list above, so the toggle moves up
    private fun createLogsPanel(): JPanel {
        val logsScrollPane = JBScrollPane(JBList(logsModel).apply {
            emptyText.text = AchievementsBundle.message("logs.empty")
        }).apply {
            preferredSize = JBUI.size(0, 150)
            isVisible = false
        }
        val toggle = ActionLink(AchievementsBundle.message("logs.show")).apply {
            icon = AllIcons.General.ArrowRight
        }
        toggle.addActionListener {
            logsScrollPane.isVisible = !logsScrollPane.isVisible
            toggle.text = AchievementsBundle.message(if (logsScrollPane.isVisible) "logs.hide" else "logs.show")
            toggle.icon = if (logsScrollPane.isVisible) AllIcons.General.ArrowDown else AllIcons.General.ArrowRight
            mainPanel.revalidate()
            mainPanel.repaint()
        }

        return JPanel(BorderLayout()).apply {
            add(JPanel(BorderLayout()).apply {
                border = JBUI.Borders.empty(0, 12, 4, 12)
                add(JSeparator(), BorderLayout.NORTH)
                add(toggle.apply { border = JBUI.Borders.emptyTop(4) }, BorderLayout.WEST)
            }, BorderLayout.NORTH)
            add(logsScrollPane, BorderLayout.CENTER)
        }
    }

    private fun updateLogs() {
        val lines = service.getAllLogs()
            .flatMap { (id, logs) -> logs.map { id to it } }
            .sortedWith(compareByDescending<Pair<String, AchievementLog>> { it.second.timestamp }
                .thenByDescending { it.second.stepIndex })
            .map { (id, log) -> formatLog(id, log) }
        logsModel.replaceAll(lines)
    }

    private fun formatLog(achievementId: String, log: AchievementLog): String {
        val name = AchievementsRegistry.get(achievementId)
            ?.let { AchievementsBundle.message(it.nameKey) } ?: achievementId
        val step = service.renderDescription(achievementId, log.stepIndex)
        return "${DateFormatUtil.formatDateTime(log.timestamp)}   $name: $step"
    }

    private fun createAchievementsListPanel() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(8)
        getSortedAchievements().forEach { add(createAchievementItem(it)) }
    }

    private fun getSortedAchievements(): List<AchievementDefinition> {
        val progress = AchievementsRegistry.all().associate { it.id to service.getProgress(it.id) }
        return AchievementsRegistry.all().sortedWith(
            compareBy(
                { progress.getValue(it.id).nextThreshold == null },
                { -progress.getValue(it.id).lastStepIndex },
                { -progress.getValue(it.id).percentToNext },
                { it.hidden },
                { AchievementsBundle.message(it.nameKey) }
            )
        )
    }

    private fun createAchievementItem(def: AchievementDefinition): JPanel {
        val progress = service.getProgress(def.id)
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(8)

            // Add name with optional hidden badge
            val namePanel = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                alignmentX = Component.LEFT_ALIGNMENT
                add(createNameLabel(def, progress))
                if (def.hidden) {
                    add(Box.createHorizontalStrut(8))
                    add(createHiddenBadge())
                }
            }
            add(namePanel)

            add(Box.createVerticalStrut(4))
            add(createDescriptionLabel(def, progress))
            add(Box.createVerticalStrut(8))
            if (def.progressive) {
                if (progress.nextThreshold != null) {
                    add(createProgressBar(progress.percentToNext))
                }
                add(createCountLabel(progress))
            } else {
                add(createStatusLabel(progress, def.steps.size))
            }
        }.let { wrapWithSeparator(it) }
    }

    private fun createNameLabel(def: AchievementDefinition, progress: AchievementProgress): JLabel {
        val text = if (def.hidden && progress.lastStepIndex < 0) {
            hiddenNameMessage
        } else {
            AchievementsBundle.message(def.nameKey)
        }

        return JLabel(text).apply {
            font = font.deriveFont(Font.BOLD)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    private fun createHiddenBadge(): JLabel {
        return JLabel(AchievementsBundle.message("achievements.hidden.badge")).apply {
            font = font.deriveFont(Font.BOLD, font.size.toFloat() - 2)
            foreground = JBColor(Color.WHITE, Color.BLACK)
            isOpaque = true
            background = JBColor(Color(153, 102, 204), Color(153, 102, 204))
            border = JBUI.Borders.empty(2, 6)
        }
    }

    private fun createDescriptionLabel(def: AchievementDefinition, progress: AchievementProgress) = JLabel(
        if (def.hidden && progress.lastStepIndex < 0) hiddenDescMessage else service.renderDescription(def.id)
    ).apply {
        alignmentX = Component.LEFT_ALIGNMENT
    }

    // No text inside the bar: themes do not style it, so it becomes unreadable over the unfilled part
    private fun createProgressBar(percent: Int) = JProgressBar(0, 100).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        value = percent
    }

    private fun createCountLabel(progress: AchievementProgress) = JLabel(
        if (progress.nextThreshold == null) completedMessage else "${progress.count} / ${progress.nextThreshold}"
    ).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        border = JBUI.Borders.emptyTop(4)
        font = font.deriveFont(Font.ITALIC, font.size.toFloat() + 1)
        foreground = if (progress.nextThreshold == null) COLOR_GREEN else COLOR_ORANGE
    }

    private fun createStatusLabel(progress: AchievementProgress, totalSteps: Int) = JLabel().apply {
        val achievedSteps = progress.lastStepIndex + 1
        val isCompleted = achievedSteps == totalSteps && progress.lastStepIndex >= 0
        val isPartial = progress.lastStepIndex >= 0 && !isCompleted

        text = when {
            isCompleted -> completedMessage
            isPartial -> "$achievedSteps / $totalSteps $stepsCompletedMessage"
            else -> uncompletedMessage
        }
        alignmentX = Component.LEFT_ALIGNMENT
        font = font.deriveFont(Font.ITALIC, font.size.toFloat() + 1)
        foreground = when {
            isCompleted -> COLOR_GREEN
            isPartial -> COLOR_ORANGE
            else -> COLOR_RED
        }
    }

    private fun wrapWithSeparator(itemPanel: JPanel) = JPanel(BorderLayout()).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        add(itemPanel, BorderLayout.CENTER)
        add(JSeparator(), BorderLayout.SOUTH)
    }

    private fun createOverallProgressPanel(): JPanel {
        val percentage = calculateOverallProgress()

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(12, 12, 8, 12)

            add(JLabel(AchievementsBundle.message("progress.overall.title")).apply {
                font = font.deriveFont(Font.BOLD, font.size.toFloat() + 2)
                alignmentX = Component.LEFT_ALIGNMENT
            })

            add(Box.createVerticalStrut(6))

            add(JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
                alignmentX = Component.LEFT_ALIGNMENT
                add(createProgressBar(percentage), BorderLayout.CENTER)
                add(JLabel("$percentage%"), BorderLayout.EAST)
                maximumSize = Dimension(Integer.MAX_VALUE, preferredSize.height)
            })

            add(Box.createVerticalStrut(8))
            add(JSeparator())
        }
    }

    private fun calculateOverallProgress(): Int {
        val achievements = AchievementsRegistry.all()
        if (achievements.isEmpty()) return 0

        var totalPercentage = 0.0

        achievements.forEach { achievement ->
            val progress = service.getProgress(achievement.id)
            val achievementSteps = achievement.steps.size.coerceAtLeast(1)
            val completedSteps = (progress.lastStepIndex + 1).coerceIn(0, achievementSteps)

            // Each achievement contributes equally (100 / number of achievements)
            // Within each achievement, steps are weighted equally
            val achievementProgress = (completedSteps.toDouble() / achievementSteps.toDouble()) * 100.0
            totalPercentage += achievementProgress
        }

        return (totalPercentage / achievements.size).toInt()
    }
}
