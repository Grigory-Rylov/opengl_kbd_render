package com.github.grishberg.cad3d.viewer

import com.github.grishberg.cad3d.plugin.Cad3dPlugin
import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.viewer.dialog.StlExportDialog
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JPanel

interface ControlPanelActions {
    val settings: SettingsHolder
    val plugins: List<Cad3dPlugin>
    var debugEnabled: Boolean
    fun rebuild()
    fun showConfigDialog()
    fun toggleScriptPanel()
    fun onDebugToggled(enabled: Boolean)
}

class ControlPanel(
    private val owner: JFrame,
    private val actions: ControlPanelActions,
) {
    lateinit var scriptEditorButton: JButton
        private set

    fun build(): JPanel {
        val controlPanel = JPanel()
        controlPanel.layout = BoxLayout(controlPanel, BoxLayout.Y_AXIS)

        val row1 = JPanel(FlowLayout(FlowLayout.LEFT))
        val row2 = JPanel(FlowLayout(FlowLayout.LEFT))

        val keysButton = createToggleButton("Клавиши", actions.settings.settingsShowCaps) {
            actions.settings.settingsShowCaps = it
            actions.rebuild()
        }
        val caseButton = createToggleButton("Корпус", actions.settings.settingsShowCase) {
            actions.settings.settingsShowCase = it
            actions.rebuild()
        }
        val matrixButton = createToggleButton("Матрица", actions.settings.settingsShowMatrix) {
            actions.settings.settingsShowMatrix = it
            actions.rebuild()
        }
        val plateButton = createToggleButton("Поддон", actions.settings.settingsShowPlate) {
            actions.settings.settingsShowPlate = it
            actions.rebuild()
        }
        val wristRestButton = createToggleButton("Держатель рук", actions.settings.settingsShowWristRest) {
            actions.settings.settingsShowWristRest = it
            actions.rebuild()
        }
        val trackballButton = createToggleButton("Трэкбол", actions.settings.settingsTrackball) {
            actions.settings.settingsTrackball = it
            actions.rebuild()
        }
        val trackballSensorButton = createToggleButton("Сенсор ТБ", actions.settings.showTrackballSensor) {
            actions.settings.showTrackballSensor = it
            actions.rebuild()
        }
        val trackballSensorCapButton = createToggleButton("Крышка сенсора ТБ", actions.settings.showTrackballSensorCap) {
            actions.settings.showTrackballSensorCap = it
            actions.rebuild()
        }
        val showControllerHolderButton =
            createToggleButton("Держатель контроллера", actions.settings.showControllerHolder) {
                actions.settings.showControllerHolder = it
                actions.rebuild()
            }
        val showControllerButton = createToggleButton("Контроллера", actions.settings.showController) {
            actions.settings.showController = it
            actions.rebuild()
        }
        val showAmoebaButton = createToggleButton("Амебы", actions.settings.showAmoeba) {
            actions.settings.showAmoeba = it
            actions.rebuild()
        }
        val showTrackballCaseButton = createToggleButton("trackball case", actions.settings.showTrackballCase) {
            actions.settings.showTrackballCase = it
            actions.rebuild()
        }
        val showTrackballCasePlateButton =
            createToggleButton("trackball case plate", actions.settings.showTrackballCasePlate) {
                actions.settings.showTrackballCasePlate = it
                actions.rebuild()
            }
        val debugButton = createToggleButton("Debug", actions.debugEnabled) {
            actions.onDebugToggled(it)
        }

        val configButton = JButton("Конфигурации")
        configButton.addActionListener {
            actions.showConfigDialog()
        }

        val exportStlButton = JButton("Экспорт STL")
        exportStlButton.addActionListener {
            val dialog = StlExportDialog(owner)
            actions.plugins.forEach { plugin ->
                plugin.exportStl(actions.settings.settings, dialog)
            }
            dialog.isVisible = true
        }

        scriptEditorButton = JButton("Скрипты")
        scriptEditorButton.addActionListener {
            actions.toggleScriptPanel()
        }

        row1.add(configButton)
        row1.add(exportStlButton)
        row1.add(scriptEditorButton)
        row1.add(keysButton)
        row1.add(caseButton)
        row1.add(matrixButton)
        row1.add(plateButton)
        row1.add(wristRestButton)
        row1.add(trackballButton)
        row1.add(trackballSensorButton)

        row2.add(trackballSensorCapButton)
        row2.add(showControllerHolderButton)
        row2.add(showControllerButton)
        row2.add(showAmoebaButton)
        row2.add(showTrackballCaseButton)
        row2.add(showTrackballCasePlateButton)
        row2.add(debugButton)

        controlPanel.add(row1)
        controlPanel.add(row2)
        return controlPanel
    }

    private fun createToggleButton(text: String, initialState: Boolean, onChanged: (Boolean) -> Unit): JCheckBox {
        val button = JCheckBox(text, initialState)
        button.addActionListener { onChanged(button.isSelected) }

        val metrics = button.getFontMetrics(button.font)
        val textWidth = metrics.stringWidth(text)
        val preferredWidth = minOf(textWidth + 40, 300)

        button.preferredSize = Dimension(preferredWidth, 30)
        button.minimumSize = Dimension(100, 30)

        return button
    }
}
