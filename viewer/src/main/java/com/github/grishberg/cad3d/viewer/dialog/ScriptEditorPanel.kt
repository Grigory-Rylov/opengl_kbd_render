package com.github.grishberg.cad3d.viewer.dialog

import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModelNative
import com.github.grishberg.scripting.ScriptEvaluator
import com.github.grishberg.javascad.manifold.Manifold3dEngine
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
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory
import org.fife.ui.rtextarea.RTextScrollPane

class ScriptEditorPanel(
    private val classPaths: List<String>,
    private val onModelReady: (List<VertexHolder>) -> Unit,
    initialScript: String = "",
    scriptDir: String? = null,
) : JPanel(BorderLayout()) {

    companion object {
        init {
            // Register the custom token maker BEFORE any RSyntaxTextArea is created,
            // otherwise the default factory is cached and our maker is ignored.
            TokenMakerFactory.setDefaultInstance(object : AbstractTokenMakerFactory() {
                override fun initTokenMakerMap() {
                    putMapping(SyntaxConstants.SYNTAX_STYLE_KOTLIN, KotlinHighlightTokenMaker::class.java.name)
                    putMapping("text/x-kotlin-hl", KotlinHighlightTokenMaker::class.java.name)
                }
            })
        }
    }

    private val scriptText = RSyntaxTextArea(20, 60)
    private val statusLabel = JLabel("Готово")
    private val errorArea = JTextArea()
    private val runButton = JButton("▶ Run (F5)")
    private val saveButton = JButton("💾 Save")
    private val exportButton = JButton("⤓ Export STL (F7)")
    private val evaluator = ScriptEvaluator(classPaths)

    private var isModified = false
    private var scriptDirectory: String? = scriptDir
    private var lastHolders: List<VertexHolder> = emptyList()

    init {
        border = BorderFactory.createTitledBorder("Script Editor")

        // Setup syntax-highlighted text area (custom Kotlin highlighter that
        // colors class / interface / function names distinctly).
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
        // Custom colors for class / interface / function names (over the dark theme).
        // getSyntaxScheme() returns a copy, so we must set it back after editing.
        val scheme = scriptText.syntaxScheme
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.DATA_TYPE,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(90, 170, 255)) // classes - blue
        )
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.RESERVED_WORD_2,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(120, 220, 140)) // interfaces - light green
        )
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.FUNCTION,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(255, 165, 60)) // method/function calls - orange
        )
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.IDENTIFIER,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(255, 215, 90)) // variables - yellow
        )
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_DECIMAL_INT,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(200, 170, 255)) // numbers - light purple
        )
        scheme.setStyle(
            org.fife.ui.rsyntaxtextarea.TokenTypes.LITERAL_NUMBER_FLOAT,
            org.fife.ui.rsyntaxtextarea.Style(AwtColor(200, 170, 255)) // numbers - light purple
        )
        scriptText.syntaxScheme = scheme
        scriptText.text = if (initialScript.isNotEmpty()) initialScript else """// Script editor — F5 to run, F7 to export STL
 // Available: bindings, Model, V3d

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
        scrollPane.maximumSize = Dimension(520, 280)

        // Auto-completion
        val autoCompletion = AutoCompletion(KotlinCompletionProvider())
        autoCompletion.install(scriptText)

        // Control panel
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        controlPanel.add(runButton)
        runButton.addActionListener { runScript() }
        controlPanel.add(saveButton)
        saveButton.addActionListener { saveScript() }
        controlPanel.add(exportButton)
        exportButton.addActionListener { exportStl() }

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
        errorScroll.maximumSize = Dimension(520, 150)
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

        // F7 key binding - export to STL
        scriptText.registerKeyboardAction(
            ActionListener { exportStl() },
            javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F7, 0),
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

    private fun concatenateSiblingScripts(mainSource: String): String {
        if (scriptDirectory.isNullOrEmpty()) return mainSource
        val dir = java.io.File(scriptDirectory)
        if (!dir.isDirectory) return mainSource

        val siblingFiles = dir.listFiles { _, name ->
            name.endsWith(".kt") && name != "user_script.kt"
        }?.sortedBy { it.name } ?: emptyList()

        if (siblingFiles.isEmpty()) return mainSource

        return siblingFiles.joinToString("\n\n") { it.readText() } + "\n\n" + mainSource
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
            val concatenatedSource = concatenateSiblingScripts(source)
            val result = evaluator.evaluate(concatenatedSource)
            val err = result.error
            val models = result.models

            SwingUtilities.invokeLater {
                runButton.isEnabled = true
                if (err != null) {
                    statusLabel.text = "Ошибка компиляции"
                    statusLabel.foreground = AwtColor.RED
                    setError(err)
                } else if (models.isNotEmpty()) {
                    try {
                        val holders = models.map { fromModelNative(it, 20) }
                        lastHolders = holders
                        val totalVerts = holders.sumOf { it.verticesCount }
                        onModelReady(holders)
                        statusLabel.text = "OK (${result.compilationTimeMs}ms, $totalVerts вершин, ${holders.size} фигур)"
                        statusLabel.foreground = AwtColor.GREEN
                        setOutput("OK (${result.compilationTimeMs}ms, $totalVerts вершин, ${holders.size} фигур)")
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

    fun loadScript(text: String, directory: String? = scriptDirectory) {
        scriptText.text = text
        scriptDirectory = directory
        isModified = false
    }

    private fun findSaveDir(): java.io.File {
        var dir = java.io.File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = dir.resolve("scripting/sandbox")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate
            }
            dir = dir.parentFile ?: return java.io.File(".")
        }
        return java.io.File(".")
    }

    private fun saveScript() {
        val dir = scriptDirectory?.let { java.io.File(it) }.takeIf { it?.isDirectory == true } ?: findSaveDir()
        val file = dir.resolve("user_script.kt")
        try {
            file.writeText(scriptText.text)
            isModified = false
            statusLabel.text = "Сохранено: ${file.name}"
            statusLabel.foreground = AwtColor.GREEN
        } catch (e: Exception) {
            statusLabel.text = "Ошибка сохранения"
            statusLabel.foreground = AwtColor.RED
            setError("Ошибка сохранения: ${e.message}")
        }
    }

    private fun exportStl() {
        val holders = lastHolders
        if (holders.isEmpty()) {
            statusLabel.text = "Нет фигур для экспорта"
            statusLabel.foreground = AwtColor.ORANGE
            setError("Сначала запустите скрипт (F5), чтобы построить фигуры.")
            return
        }

        val chooser = javax.swing.JFileChooser(findSaveDir())
        chooser.dialogTitle = "Экспорт в STL"
        chooser.selectedFile = java.io.File("export.stl")
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("STL files (*.stl)", "stl")
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return
        }
        var file = chooser.selectedFile
        if (!file.name.lowercase().endsWith(".stl")) {
            file = java.io.File(file.parentFile, file.name + ".stl")
        }

        try {
            val triangleCount = writeBinaryStl(holders, file)
            statusLabel.text = "STL сохранён: ${file.name}"
            statusLabel.foreground = AwtColor.GREEN
            setOutput("Экспортировано ${holders.size} фигур ($triangleCount треугольников) в ${file.absolutePath}")
        } catch (e: Exception) {
            statusLabel.text = "Ошибка экспорта STL"
            statusLabel.foreground = AwtColor.RED
            setError("Ошибка экспорта STL: ${e.message}")
        }
    }

    private fun writeBinaryStl(holders: List<VertexHolder>, file: java.io.File): Int {
        var triangleCount = 0
        for (holder in holders) {
            triangleCount += holder.verticesCount / 3
        }

        val buffer = java.nio.ByteBuffer.allocate(84 + triangleCount * 50)
        buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN)
        buffer.put(ByteArray(80))
        buffer.putInt(triangleCount)

        for (holder in holders) {
            val vert = holder.vertex
            val normals = holder.normals
            var vi = 0
            var ni = 0
            val triangles = holder.verticesCount / 3
            for (t in 0 until triangles) {
                var nx = 0f
                var ny = 0f
                var nz = 0f
                val positions = FloatArray(9)
                for (v in 0 until 3) {
                    positions[v * 3] = vert[vi++]
                    positions[v * 3 + 1] = vert[vi++]
                    positions[v * 3 + 2] = vert[vi++]
                    vi += 4 // skip r,g,b,a
                    nx += normals[ni++]
                    ny += normals[ni++]
                    nz += normals[ni++]
                }
                buffer.putFloat(nx / 3f)
                buffer.putFloat(ny / 3f)
                buffer.putFloat(nz / 3f)
                for (f in positions) {
                    buffer.putFloat(f)
                }
                buffer.putShort(0)
            }
        }

        file.outputStream().use { it.write(buffer.array()) }
        return triangleCount
    }

    fun setStatus(text: String, success: Boolean) {
        statusLabel.text = text
        statusLabel.foreground = if (success) AwtColor.GREEN else AwtColor.RED
        if (success) setOutput(text) else setError(text)
    }

    fun getScript(): String = scriptText.text
}
