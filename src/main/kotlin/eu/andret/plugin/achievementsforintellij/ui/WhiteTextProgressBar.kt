package eu.andret.plugin.achievementsforintellij.ui

import com.intellij.ui.JBColor
import java.awt.Color
import javax.swing.JProgressBar
import javax.swing.plaf.basic.BasicProgressBarUI

/**
 * A progress bar with white text on both filled and unfilled portions for better visibility.
 */
class WhiteTextProgressBar(min: Int = 0, max: Int = 100) : JProgressBar(min, max) {

    init {
        isStringPainted = true
    }

    override fun updateUI() {
        super.updateUI()
        setUI(object : BasicProgressBarUI() {
            override fun getSelectionForeground() = JBColor(Color.BLUE, Color(0, 0, 200))
            override fun getSelectionBackground() = JBColor(Color.BLUE, Color.WHITE)
        })
    }
}
