package eu.andret.plugin.achievementsforintellij.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import eu.andret.plugin.achievementsforintellij.MyBundle
import eu.andret.plugin.achievementsforintellij.achievements.AchievementsRegistry
import eu.andret.plugin.achievementsforintellij.achievements.entity.AchievementDefinition
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService
import eu.andret.plugin.achievementsforintellij.storage.AchievementsService.AchievementProgress
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
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
import javax.swing.plaf.basic.BasicProgressBarUI

internal class AchievementsDialog(project: Project?) : DialogWrapper(project, true, IdeModalityType.MODELESS) {

    private val service = AchievementsService.getInstance()
    private lateinit var scrollPane: JBScrollPane
    private val completedMessage by lazy { MyBundle.message("progress.completed") }
    private val uncompletedMessage by lazy { MyBundle.message("progress.uncompleted") }
    private val stepsCompletedMessage by lazy { MyBundle.message("progress.steps.completed") }
    private val hiddenNameMessage by lazy { MyBundle.message("achievements.hidden.name") }
    private val hiddenDescMessage by lazy { MyBundle.message("achievements.hidden.description") }

    init {
        title = MyBundle.message("dialog.title")
        init()
        setSize(800, 600)
    }

    companion object {
        private val COLOR_GREEN = JBColor(Color(0, 128, 0), Color(76, 175, 80))
        private val COLOR_ORANGE = JBColor(Color(204, 102, 0), Color(255, 165, 0))
        private val COLOR_RED = JBColor(Color(204, 0, 0), Color(244, 67, 54))
    }

    override fun createActions(): Array<Action> {
        val resetAction = object : AbstractAction(MyBundle.message("dialog.button.reset.caption")) {
            override fun actionPerformed(e: ActionEvent) {
                if (Messages.showYesNoDialog(
                        MyBundle.message("dialog.modal.reset.message"),
                        MyBundle.message("dialog.modal.reset.title"),
                        Messages.getWarningIcon()
                    ) == Messages.YES
                ) {
                    service.clearAll()
                    refreshContent()
                }
            }
        }
        return arrayOf(resetAction, okAction)
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
        }

        // Add overall progress bar at the top
        mainPanel.add(createOverallProgressPanel(), BorderLayout.NORTH)

        // Add scrollable achievements list
        scrollPane = JBScrollPane(createAchievementsListPanel()).apply {
            border = JBUI.Borders.empty()
        }
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        return mainPanel
    }

    private fun refreshContent() {
        val mainPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty()
        }
        mainPanel.add(createOverallProgressPanel(), BorderLayout.NORTH)
        scrollPane.setViewportView(createAchievementsListPanel())
        mainPanel.add(scrollPane, BorderLayout.CENTER)
        contentPane.removeAll()
        (contentPane as JPanel).add(mainPanel, BorderLayout.CENTER)
        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun createAchievementsListPanel() = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(8)
        getSortedAchievements().forEach { add(createAchievementItem(it)) }
    }

    private fun getSortedAchievements() = AchievementsRegistry.all().sortedWith(
        compareBy(
            { service.getProgress(it.id).nextThreshold == null },
            { -service.getProgress(it.id).lastStepIndex },
            { -service.getProgress(it.id).percentToNext },
            { it.hidden },
            { MyBundle.message(it.nameKey) }
        )
    )

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
                add(createProgressBar(progress))
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
            MyBundle.message(def.nameKey)
        }

        return JLabel(text).apply {
            font = font.deriveFont(Font.BOLD)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    private fun createHiddenBadge(): JLabel {
        return JLabel("SECRET").apply {
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

    private fun createProgressBar(progress: AchievementProgress) = JProgressBar(0, 100).apply {
        alignmentX = Component.LEFT_ALIGNMENT
        value = progress.percentToNext
        isStringPainted = true
        string = if (progress.nextThreshold == null) {
            completedMessage
        } else {
            "${progress.percentToNext}%  (${progress.count} / ${progress.nextThreshold})"
        }
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
            isCompleted -> "✓ $completedMessage"
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

            add(JLabel(MyBundle.message("progress.overall.title")).apply {
                font = font.deriveFont(Font.BOLD, font.size.toFloat() + 2)
                alignmentX = Component.LEFT_ALIGNMENT
            })

            add(Box.createVerticalStrut(6))

            add(object : JProgressBar(0, 100) {
                init {
                    alignmentX = LEFT_ALIGNMENT
                    maximumSize = java.awt.Dimension(Integer.MAX_VALUE, preferredSize.height)
                    value = percentage
                    isStringPainted = true
                    string = "$percentage%"
                    foreground = JBColor(Color(51, 122, 183), Color(255, 165, 0))
                }

                override fun updateUI() {
                    super.updateUI()
                    // selectionForeground = text color on filled portion (always white)
                    // selectionBackground = text color on unfilled portion (black in light, white in dark)
                    setUI(object : BasicProgressBarUI() {
                        override fun getSelectionForeground() = JBColor.WHITE
                        override fun getSelectionBackground() = JBColor(Color.BLACK, Color.WHITE)
                    })
                }
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
