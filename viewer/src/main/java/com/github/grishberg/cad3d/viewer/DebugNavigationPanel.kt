package com.github.grishberg.cad3d.viewer

import com.github.grishberg.cad3d.debug.DebugCmd
import com.github.grishberg.cad3d.viewer.debug.DebugVisualizerImpl
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class DebugNavigationPanel(
    private val onPrev: () -> Unit,
    private val onNext: () -> Unit,
) {
    private lateinit var panel: JPanel
    private lateinit var debugInfoLabel: JLabel
    private lateinit var statusLabel: JLabel
    private lateinit var helpLabel: JLabel
    private lateinit var prevDebugButton: JButton
    private lateinit var nextDebugButton: JButton

    val component: JPanel
        get() = panel

    fun build(): JPanel {
        panel = JPanel()
        panel.layout = FlowLayout(FlowLayout.CENTER)
        panel.preferredSize = Dimension(1200, 40)

        prevDebugButton = JButton("◀ Пред.")
        prevDebugButton.preferredSize = Dimension(80, 30)
        prevDebugButton.addActionListener { onPrev() }

        nextDebugButton = JButton("След. ▶")
        nextDebugButton.preferredSize = Dimension(80, 30)
        nextDebugButton.addActionListener { onNext() }

        debugInfoLabel = JLabel("Debug: выключен")
        debugInfoLabel.preferredSize = Dimension(350, 30)

        statusLabel = JLabel("Готово")
        statusLabel.preferredSize = Dimension(200, 30)

        helpLabel = JLabel("Горячие клавиши: R - вкл/выкл debug, Q/E - переключение команд")
        helpLabel.preferredSize = Dimension(400, 30)

        panel.add(statusLabel)
        panel.add(prevDebugButton)
        panel.add(debugInfoLabel)
        panel.add(nextDebugButton)
        panel.add(helpLabel)

        updateDebugNavigationState(false, 0, 0, null)
        panel.isVisible = true
        return panel
    }

    fun updateDebugNavigationState(
        showDebugInfo: Boolean,
        commandCount: Int,
        currentIndex: Int,
        description: String?,
    ) {
        prevDebugButton.isEnabled = showDebugInfo
        nextDebugButton.isEnabled = showDebugInfo
        debugInfoLabel.isVisible = showDebugInfo
        prevDebugButton.isVisible = showDebugInfo
        nextDebugButton.isVisible = showDebugInfo
        helpLabel.isVisible = showDebugInfo

        debugInfoLabel.text = if (showDebugInfo && commandCount > 0) {
            "Debug (${currentIndex + 1}/$commandCount): $description"
        } else {
            "Debug: выключен"
        }

        panel.revalidate()
        panel.repaint()
    }

    fun setRenderingStatus(isRendering: Boolean) {
        statusLabel.text = if (isRendering) "Рендеринг" else "Готово"
        statusLabel.background = if (isRendering) Color.ORANGE else Color.GREEN
    }
}
