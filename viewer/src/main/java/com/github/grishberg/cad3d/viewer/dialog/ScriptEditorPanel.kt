package com.github.grishberg.cad3d.viewer.dialog

import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModelNative
import com.github.grishberg.scripting.ScriptEvaluator
import com.github.grishberg.scripting.ScriptResult
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.utils.Color
import java.awt.BorderLayout
import java.awt.Color as AwtColor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder as SwingEmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ScriptEditorPanel(
    private val classPaths: List<String>,
    private val onModelReady: (VertexHolder) -> Unit,
) : JPanel(BorderLayout()) {

    private val scriptText = JTextArea()
    private val statusLabel = JLabel("Готово")
    private val runButton = JButton("▶ Run (F5)")
    private val evaluator = ScriptEvaluator(classPaths)

    private var isModified = false

    init {
        border = BorderFactory.createTitledBorder("Script Editor")

        // Setup text area
        scriptText.font = Font("Monospaced", Font.PLAIN, 12)
        scriptText.lineWrap = false
        scriptText.wrapStyleWord = false
        scriptText.margin = java.awt.Insets(5, 5, 5, 5)
        scriptText.text = """// Script editor — F5 to run
// Available: bindings, Abstract3dModel, V3d

bindings.cube(50.0)
"""
        scriptText.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) { markModified() }
            override fun removeUpdate(e: DocumentEvent?) { markModified() }
            override fun changedUpdate(e: DocumentEvent?) { markModified() }
        })

        val scrollPane = JScrollPane(scriptText)
        scrollPane.preferredSize = Dimension(400, 300)

        // Control panel
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(runButton)
        runButton.addActionListener { runScript() }

        statusLabel.border = SwingEmptyBorder(0, 10, 0, 0)
        statusLabel.foreground = AwtColor.GREEN
        controlPanel.add(statusLabel)

        // Main layout
        add(scrollPane, BorderLayout.CENTER)
        add(controlPanel, BorderLayout.NORTH)

        // F5 key binding
        scriptText.registerKeyboardAction(
            ActionListener { runScript() },
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F5, 0),
            javax.swing.JComponent.WHEN_FOCUSED
        )
    }

    private fun markModified() {
        isModified = true
    }

    private fun runScript() {
        runButton.isEnabled = false
        statusLabel.text = "Рендеринг..."
        statusLabel.foreground = AwtColor.ORANGE

        Thread {
            try {
                Manifold3dEngine.initialize()
            } catch (e: Exception) {
                // already initialized
            }

            val source = scriptText.text
            val result = evaluator.evaluate(source)
            val err = result.error
            val mdl = result.model

            SwingUtilities.invokeLater {
                runButton.isEnabled = true
                if (err != null) {
                    statusLabel.text = "Ошибка: ${err.lines().firstOrNull()?.take(100)}"
                    statusLabel.foreground = AwtColor.RED
                } else if (mdl != null) {
                    try {
                        val vertexHolder = fromModelNative(mdl, Color.GRAY, 20)
                        onModelReady(vertexHolder)
                        statusLabel.text = "OK (${result.compilationTimeMs}ms, ${vertexHolder.verticesCount} вершин)"
                        statusLabel.foreground = AwtColor.GREEN
                        isModified = false
                    } catch (e: Exception) {
                        statusLabel.text = "Ошибка конвертации: ${e.message?.take(100)}"
                        statusLabel.foreground = AwtColor.RED
                    }
                } else {
                    statusLabel.text = "Null model (${result.compilationTimeMs}ms)"
                    statusLabel.foreground = AwtColor.ORANGE
                }
            }
        }.start()
    }

    fun loadScript(text: String) {
        scriptText.text = text
        isModified = false
    }

    fun getScript(): String = scriptText.text
}
