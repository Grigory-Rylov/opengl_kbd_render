package com.github.grishberg.cad3d.viewer.dialog

import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModelNative
import com.github.grishberg.scripting.ScriptEvaluator
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.utils.Color
import java.awt.BorderLayout
import java.awt.Color as AwtColor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder as SwingEmptyBorder
import javax.swing.border.TitledBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane

class ScriptEditorPanel(
    private val classPaths: List<String>,
    private val onModelReady: (VertexHolder) -> Unit,
    initialScript: String = "",
) : JPanel(BorderLayout()) {

    private val scriptText = RSyntaxTextArea(20, 60)
    private val statusLabel = JLabel("Готово")
    private val errorArea = JTextArea()
    private val runButton = JButton("▶ Run (F5)")
    private val saveButton = JButton("💾 Save")
    private val evaluator = ScriptEvaluator(classPaths)

    private var isModified = false

    init {
        border = BorderFactory.createTitledBorder("Script Editor")

        // Setup syntax-highlighted text area (Kotlin)
        scriptText.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_KOTLIN
        scriptText.isCodeFoldingEnabled = true
        scriptText.margin = java.awt.Insets(5, 5, 5, 5)
        try {
            Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"))
                ?.apply(scriptText)
        } catch (e: Exception) {
            // keep default theme if resource missing
        }
        // Font must be set AFTER the theme, otherwise the theme resets it.
        scriptText.font = Font("Monospaced", Font.PLAIN, 16)
        scriptText.text = if (initialScript.isNotEmpty()) initialScript else """// Script editor — F5 to run
 // Available: bindings, Abstract3dModel, V3d

 bindings.cube(50.0) // fallback if matrix_right not found
 """
        scriptText.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                markModified()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                markModified()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                markModified()
            }
        })

        val scrollPane = RTextScrollPane(scriptText)
        scrollPane.preferredSize = Dimension(520, 280)

        // Control panel
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(runButton)
        runButton.addActionListener { runScript() }
        controlPanel.add(saveButton)
        saveButton.addActionListener { saveScript() }

        statusLabel.border = SwingEmptyBorder(0, 10, 0, 0)
        statusLabel.foreground = AwtColor.GREEN
        controlPanel.add(statusLabel)

        // Error/output panel (under the code)
        errorArea.font = Font("Monospaced", Font.PLAIN, 16)
        errorArea.lineWrap = true
        errorArea.wrapStyleWord = true
        errorArea.isEditable = false
        errorArea.background = AwtColor(40, 40, 40)
        errorArea.foreground = AwtColor(255, 140, 140)
        errorArea.text = ""
        val errorScroll = JScrollPane(errorArea)
        errorScroll.preferredSize = Dimension(520, 150)
        errorScroll.border = TitledBorder("Ошибки / вывод")

        // Main layout: code (center), errors (south), controls (north)
        add(scrollPane, BorderLayout.CENTER)
        add(errorScroll, BorderLayout.SOUTH)
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

    fun setError(text: String) {
        errorArea.foreground = AwtColor(255, 140, 140)
        errorArea.text = text
    }

    fun setOutput(text: String) {
        errorArea.foreground = AwtColor(180, 220, 180)
        errorArea.text = text
    }

    fun runScript() {
        runButton.isEnabled = false
        statusLabel.text = "Рендеринг..."
        statusLabel.foreground = AwtColor.ORANGE
        errorArea.foreground = AwtColor(180, 220, 180)
        errorArea.text = "Компиляция..."

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
                    statusLabel.text = "Ошибка компиляции"
                    statusLabel.foreground = AwtColor.RED
                    setError(err)
                } else if (mdl != null) {
                    try {
                        val vertexHolder = fromModelNative(mdl, Color.GRAY, 20)
                        onModelReady(vertexHolder)
                        statusLabel.text = "OK (${result.compilationTimeMs}ms, ${vertexHolder.verticesCount} вершин)"
                        statusLabel.foreground = AwtColor.GREEN
                        setOutput("OK (${result.compilationTimeMs}ms, ${vertexHolder.verticesCount} вершин)")
                        isModified = false
                    } catch (e: Exception) {
                        val msg = "Ошибка конвертации: ${e.message}"
                        statusLabel.text = "Ошибка"
                        statusLabel.foreground = AwtColor.RED
                        setError(msg)
                    }
                } else {
                    statusLabel.text = "Null model"
                    statusLabel.foreground = AwtColor.ORANGE
                    setOutput("Модель не построена (null). ${result.compilationTimeMs}ms")
                }
            }
        }.start()
    }

    fun loadScript(text: String) {
        scriptText.text = text
        isModified = false
    }

    private fun findSaveDir(): java.io.File {
        var dir = java.io.File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = dir.resolve("scripting/examples")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate
            }
            dir = dir.parentFile ?: return java.io.File(".")
        }
        return java.io.File(".")
    }

    private fun saveScript() {
        val file = findSaveDir().resolve("user_script.kt")
        try {
            file.writeText(scriptText.text)
            isModified = false
            statusLabel.text = "Сохранено: ${file.absolutePath}"
            statusLabel.foreground = AwtColor.GREEN
            setOutput("Сохранено: ${file.absolutePath}")
        } catch (e: Exception) {
            statusLabel.text = "Ошибка сохранения"
            statusLabel.foreground = AwtColor.RED
            setError("Ошибка сохранения: ${e.message}")
        }
    }

    fun setStatus(text: String, success: Boolean) {
        statusLabel.text = text
        statusLabel.foreground = if (success) AwtColor.GREEN else AwtColor.RED
        if (success) setOutput(text) else setError(text)
    }

    fun getScript(): String = scriptText.text
}
