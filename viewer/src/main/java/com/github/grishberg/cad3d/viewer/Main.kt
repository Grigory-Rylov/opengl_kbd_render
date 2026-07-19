package com.github.grishberg.cad3d.viewer

import com.github.grishberg.cad3d.debug.DebugCmd
import com.github.grishberg.cad3d.keyboard.ControlPointsController
import com.github.grishberg.cad3d.plugin.Cad3dPlugin
import com.github.grishberg.cad3d.plugin.ResultListener
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.plugins.PluginManager
import com.github.grishberg.cad3d.plugins.PluginManagerImpl
import com.github.grishberg.cad3d.viewer.debug.DebugVisualizerImpl
import com.github.grishberg.cad3d.viewer.dialog.ConfigEditor
import com.github.grishberg.cad3d.viewer.dialog.StlExportDialog
import com.github.grishberg.cad3d.util.fromModelNative
import com.github.grishberg.cad3d.viewer.dialog.ScriptEditorPanel
import com.github.grishberg.scripting.ScriptEvaluator
import com.github.grishberg.javascad.manifold.Manifold3dEngine
import com.github.grishberg.javascad.utils.Color as JavascadColor
import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.fixedfunc.GLLightingFunc
import com.jogamp.opengl.glu.GLU
import com.jogamp.opengl.util.Animator
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Main(title: String?) : JFrame(title), GLEventListener {

    //    protected GLWindow window;
    private val animator: Animator = Animator()

    //public Caps caps;
    // Установка позиции источника света
    var lightPosition = floatArrayOf(0.0f, 1f, 0.5f, 1.0f)
    private val settingsHolder = SettingsHolder(SETTINGS_FILE)

    private val vertexHolderList: MutableList<VertexHolder> = ArrayList()
    private val glu = GLU()
    private var viewportWidth = 1200
    private var viewportHeight = 800
    private var prevMouseX = 0
    private var prevMouseY = 0
    private val pointsController = ControlPointsController()
    private var glCanvas: GLCanvas? = null
    private var splitPane: javax.swing.JSplitPane? = null

    //    private val sceneBuilder: SceneBuilder
    private val debugVisualizer = DebugVisualizerImpl()
    private val debugCommands = mutableListOf<DebugCmd>()

    private var showDebugInfo = false
    private var currentDebugCommandIndex = 0
    private lateinit var debugNavigationPanel: JPanel
    private lateinit var debugInfoLabel: JLabel
    private lateinit var helpLabel: JLabel
    private lateinit var statusLabel: JLabel
    private lateinit var scriptEditorButton: JButton
    private lateinit var prevDebugButton: JButton
    private lateinit var nextDebugButton: JButton
    private var pluginManager: PluginManager? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var plugins: List<Cad3dPlugin> = emptyList()
    private lateinit var scriptEditorPanel: ScriptEditorPanel
    private var scriptModelEnabled = false
    private val scriptEvaluator: ScriptEvaluator by lazy {
        ScriptEvaluator(filterScriptClasspath())
    }

    private fun filterScriptClasspath(): List<String> {
        val full = System.getProperty("java.class.path")
            .split(java.io.File.pathSeparator)
            .map { p -> java.io.File(p) }
            .filter { f -> f.exists() }
            .map { f -> f.absolutePath }
        val keep = listOf(
            "scripting", "cad3d", "plugin", "kbd_core", "javascad", "common",
            "kotlin-stdlib", "kotlin-script-runtime",
        )
        val filtered = full.filter { p -> keep.any { name -> p.contains(name) } }
        // Fallback: if filtering dropped essential libs, use the full classpath.
        return if (filtered.any { it.contains("javascad") } &&
            filtered.any { it.contains("kotlin-stdlib") }
        ) filtered else full
    }

    init {
        settingsHolder.loadSettings()

        val pluginsDir = findPluginsDir()
        if (!pluginsDir.exists()) {
            println("WARNING: plugins dir not found: ${pluginsDir.absolutePath}")
        }
        setup()


        pluginManager = PluginManagerImpl(pluginsDir)
        pluginManager!!.setOnPluginLoadedListener(object : PluginManager.OnPluginLoadedListener {
            override fun onPluginsLoaded(newPlugins: List<Cad3dPlugin>) {
                plugins = newPlugins
                println("LOADED plugins count=${newPlugins.size} from ${pluginsDir.absolutePath}")
                if (newPlugins.isEmpty()) {
                    println("WARNING: no plugins loaded! Check plugins path.")
                }
                rebuildConfigAndRequestRendering(plugins, emptySet())
            }
        })

        pluginManager!!.start()
    }

    private fun findPluginsDir(): java.io.File {
        var dir = java.io.File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = dir.resolve("cad3d/build/libs")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate
            }
            dir = dir.parentFile ?: return java.io.File("../cad3d/build/libs")
        }
        return java.io.File("../cad3d/build/libs")
    }

    private fun scriptTemplate(): String = """// DSL script — F5 to run
// Available: cube/sphere/cylinder/prism/hull/union, importStl, v3(), move/rotate/withColor

// --- 5U+Vertical+Post (converted from OpenSCAD) ---
val nutDiameter = 8.79
val holeDiameter = 7.56 * 0.75776
val w = 20.0
val l = 222.0
val h = 6.0

// Y positions of the screw holes (offset_y + accumulated large/small steps)
val ys = doubleArrayOf(
    6.34, 22.22, 38.10, 50.77, 66.65, 82.53, 95.20, 111.08,
    126.96, 139.63, 155.51, 171.39, 184.06, 199.94, 215.82, 228.49
)

// Base body
val body = cube(w, l, h).move(10.0, 0.0, 0.0).withColor(Color.RED)

// Hex nuts (prism with 6 sides) placed at every screw position, to subtract
val nuts = repeat(ys.size) { i ->
    prism(4.0, nutDiameter / 2.0, 6).move(22.9, ys[i], 2.0).withColor(Color.YELLOW)
}

// Round holes placed at every screw position, to subtract
val holes = repeat(ys.size) { i ->
    cylinder(2.0, holeDiameter / 2.0).move(22.9, ys[i], 0.0).withColor(Color.GREEN)
}

// Imported STL placed above the body
val post = importStl("5U+Vertical+Post.stl").withColor(Color.CYAN).move(0.0, 0.0, 100.0)

body.subtractModel(nuts).subtractModel(holes).addModel(post)
"""

    private fun findUserScriptFile(): java.io.File? {
        var dir = java.io.File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = dir.resolve("scripting/examples/user_script.kt")
            if (candidate.exists() && candidate.isFile) {
                return candidate
            }
            dir = dir.parentFile ?: return null
        }
        return null
    }

    private fun loadMatrixRightText(): String {
        val file = findUserScriptFile()
        return if (file != null) {
            try {
                file.readText()
            } catch (e: Exception) {
                scriptTemplate()
            }
        } else {
            scriptTemplate()
        }
    }

    private fun createScriptEditorPanel(initialScript: String = loadMatrixRightText()): ScriptEditorPanel {
        val classPaths = filterScriptClasspath()

        return ScriptEditorPanel(classPaths, { holders ->
            vertexHolderList.clear()
            vertexHolderList.addAll(holders)
            requestRender()
        }, initialScript)
    }

    fun setup() {
        layout = BorderLayout()
        // Создаем меню
        jMenuBar = createMenuBar()
        // Создаем панель управления
        val controlPanel = createControlPanel()

        // Создаем панель навигации по debug командам
        createDebugNavigationPanel()

        val glProfile = GLProfile.get(GLProfile.GL2)
        val glCapabilities = GLCapabilities(glProfile)
        glCapabilities.depthBits = 24

        // 2. Создание GLCanvas с явным конструктором
        glCanvas = GLCanvas(glCapabilities)
        glCanvas!!.addGLEventListener(this)
        val mouseListener = GlCanvasMouseListener()
        glCanvas!!.addMouseListener(mouseListener)
        glCanvas!!.addMouseMotionListener(mouseListener)
        glCanvas!!.addMouseWheelListener(mouseListener)
        glCanvas!!.addKeyListener(GlCanvasKeyListener())
        defaultCloseOperation = EXIT_ON_CLOSE
        animator.add(glCanvas)
        animator.start()
        contentPane.add(glCanvas, BorderLayout.CENTER)
        contentPane.add(controlPanel, BorderLayout.NORTH)
        contentPane.add(debugNavigationPanel, BorderLayout.SOUTH)

        // Обработка закрытия окна
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                pluginManager?.stop()
                settingsHolder.saveSettings()
                if (animator.isAnimating) {
                    animator.stop()
                }
                dispose()
            }
        })
        setSize(1200, 800)
        isVisible = true
        requestRender()
        // При старте восстанавливаем состояние панели скриптов из настроек
        settingsHolder.loadScriptPanelState()
        val lastScript = settingsHolder.lastScriptFile
        if (lastScript.isNotEmpty()) {
            val f = java.io.File(lastScript)
            if (f.exists()) {
                try {
                    scriptEditorPanel = createScriptEditorPanel(f.readText())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (settingsHolder.showScriptPanel) {
            showScriptPanel()
        }
    }

    private fun createMenuBar(): javax.swing.JMenuBar {
        val menuBar = javax.swing.JMenuBar()
        val fileMenu = javax.swing.JMenu("Файл")

        val openItem = javax.swing.JMenuItem("Открыть скрипт...")
        val shortcutMask = java.awt.Toolkit.getDefaultToolkit().menuShortcutKeyMaskEx
        openItem.accelerator = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, shortcutMask)
        openItem.addActionListener { openScript() }
        fileMenu.add(openItem)

        menuBar.add(fileMenu)
        return menuBar
    }

    private fun openScript() {
        val chooser = javax.swing.JFileChooser()
        chooser.dialogTitle = "Открыть скрипт"
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("Kotlin scripts (*.kt, *.kts)", "kt", "kts")
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return
        }
        val file = chooser.selectedFile
        try {
            val text = file.readText()
            if (!::scriptEditorPanel.isInitialized) {
                scriptEditorPanel = createScriptEditorPanel(text)
            } else {
                scriptEditorPanel.loadScript(text)
            }
            settingsHolder.lastScriptFile = file.absolutePath
            settingsHolder.saveSettings()
            showScriptPanel()
        } catch (e: Exception) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "Не удалось открыть файл: ${e.message}",
                "Ошибка",
                javax.swing.JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showScriptPanel() {
        if (!::scriptEditorPanel.isInitialized) {
            scriptEditorPanel = createScriptEditorPanel()
        }
        if (splitPane == null) {
            // Переносим GLCanvas из CENTER в сплиттер вместе с панелью кода
            contentPane.remove(glCanvas)

            val split = javax.swing.JSplitPane(
                javax.swing.JSplitPane.HORIZONTAL_SPLIT,
                glCanvas,
                scriptEditorPanel,
            )
            split.isContinuousLayout = true
            split.resizeWeight = 1.0 // при ресайзе окна растёт левая часть, правая держит ширину
            split.dividerSize = 8
            glCanvas!!.minimumSize = Dimension(100, 100)
            scriptEditorPanel.minimumSize = Dimension(150, 100)
            // Ставим позицию разделителя так, чтобы правая панель имела сохранённую ширину
            split.addComponentListener(object : java.awt.event.ComponentAdapter() {
                override fun componentResized(e: java.awt.event.ComponentEvent?) {
                    applyScriptPanelWidth(split)
                    split.removeComponentListener(this)
                }
            })
            // Сохраняем ширину правой панели при перетаскивании разделителя
            split.addPropertyChangeListener(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY) {
                val total = split.width
                if (total > 0) {
                    val rightWidth = total - split.dividerLocation - split.dividerSize
                    if (rightWidth > 0) {
                        settingsHolder.scriptPanelWidth = rightWidth
                    }
                }
            }
            splitPane = split
            contentPane.add(split, BorderLayout.CENTER)
            contentPane.revalidate()
            contentPane.repaint()
            javax.swing.SwingUtilities.invokeLater { applyScriptPanelWidth(split) }
        }
        settingsHolder.showScriptPanel = true
        scriptEditorButton.text = "Скрипты ✓"
        settingsHolder.saveScriptPanelState()
        // Компилируем текст редактора при открытии панели
        scriptEditorPanel.runScript()
    }

    private fun applyScriptPanelWidth(split: javax.swing.JSplitPane) {
        val total = split.width
        if (total <= 0) return
        val width = settingsHolder.scriptPanelWidth.coerceIn(150, (total - 100).coerceAtLeast(150))
        split.dividerLocation = total - width - split.dividerSize
    }

    private fun hideScriptPanel() {
        splitPane?.let { split ->
            // Возвращаем GLCanvas обратно в CENTER
            split.remove(glCanvas)
            contentPane.remove(split)
            splitPane = null
            contentPane.add(glCanvas, BorderLayout.CENTER)
            contentPane.revalidate()
            contentPane.repaint()
        }
        settingsHolder.showScriptPanel = false
        scriptEditorButton.text = "Скрипты"
        settingsHolder.saveScriptPanelState()
        // Script panel closed: render the keyboard from the plugin.
        rebuildConfigAndRequestRendering(plugins, emptySet())
    }

    private fun toggleScriptPanel() {
        val visible = splitPane != null
        if (visible) hideScriptPanel() else showScriptPanel()
    }

    private fun createControlPanel(): JPanel {
        // Создаем основную панель с вертикальным BoxLayout
        val controlPanel = JPanel()
        controlPanel.layout = BoxLayout(controlPanel, BoxLayout.Y_AXIS)

        // Создаем две панели для строк
        val row1 = JPanel(FlowLayout(FlowLayout.LEFT))
        val row2 = JPanel(FlowLayout(FlowLayout.LEFT))

        // Создаем кнопки (код создания кнопок остается прежним)
        val keysButton = createToggleButton("Клавиши", settingsHolder.settingsShowCaps) {
            settingsHolder.settingsShowCaps = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val caseButton = createToggleButton("Корпус", settingsHolder.settingsShowCase) {
            settingsHolder.settingsShowCase = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val matrixButton = createToggleButton("Матрица", settingsHolder.settingsShowMatrix) {
            settingsHolder.settingsShowMatrix = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val plateButton = createToggleButton("Поддон", settingsHolder.settingsShowPlate) {
            settingsHolder.settingsShowPlate = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val wristRestButton = createToggleButton("Держатель рук", settingsHolder.settingsShowWristRest) {
            settingsHolder.settingsShowWristRest = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val trackballButton = createToggleButton("Трэкбол", settingsHolder.settingsTrackball) {
            settingsHolder.settingsTrackball = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val trackballSensorButton = createToggleButton("Сенсор ТБ", settingsHolder.showTrackballSensor) {
            settingsHolder.showTrackballSensor = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val trackballSensorCapButton = createToggleButton("Крышка сенсора ТБ", settingsHolder.showTrackballSensorCap) {
            settingsHolder.showTrackballSensorCap = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val showControllerHolderButton =
            createToggleButton("Держатель контроллера", settingsHolder.showControllerHolder) {
                settingsHolder.showControllerHolder = it
                rebuildConfigAndRequestRendering(plugins, emptySet())
            }
        val showControllerButton = createToggleButton("Контроллера", settingsHolder.showController) {
            settingsHolder.showController = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val showAmoebaButton = createToggleButton("Амебы", settingsHolder.showAmoeba) {
            settingsHolder.showAmoeba = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val showTrackballCaseButton = createToggleButton("trackball case", settingsHolder.showTrackballCase) {
            settingsHolder.showTrackballCase = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val showTrackballCasePlateButton = createToggleButton("trackball case plate", settingsHolder.showTrackballCasePlate) {
            settingsHolder.showTrackballCasePlate = it
            rebuildConfigAndRequestRendering(plugins, emptySet())
        }
        val debugButton = createToggleButton("Debug", showDebugInfo) {
            showDebugInfo = it
            if (!it) {
                debugVisualizer.clearVisualization()
            } else {
                addDebugCommands()
                updateDebugDisplay()
            }
            updateDebugNavigationState()
        }

        val configButton = JButton("Конфигурации")
        configButton.addActionListener {
            showConfigDialog()
        }

        val exportStlButton = JButton("Экспорт STL")
        exportStlButton.addActionListener {
            val dialog = StlExportDialog(this)
            plugins.forEach { plugin ->
                plugin.exportStl(settingsHolder.settings, dialog)
            }
            dialog.isVisible = true
        }

        scriptEditorButton = JButton("Скрипты")
        scriptEditorButton.addActionListener {
            toggleScriptPanel()
        }

        // --- Распределяем кнопки по строкам ---
        // Вы можете изменить это распределение в зависимости от того, какие кнопки вам
        // нужны чаще и должны быть на верхнем ряду.

        // Верхний ряд (row1)
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
        // row1.add(trackballSensorCapButton) // Можно перенести на вторую строку, если не помещается

        // Нижний ряд (row2)
        // Добавим оставшиеся кнопки на вторую строку
        row2.add(trackballSensorCapButton) // Перенесли сюда
        row2.add(showControllerHolderButton)
        row2.add(showControllerButton)
        row2.add(showAmoebaButton)
        row2.add(showTrackballCaseButton)
        row2.add(showTrackballCasePlateButton)

        row2.add(debugButton)

        // Добавляем строки на основную панель
        controlPanel.add(row1)
        controlPanel.add(row2)
        return controlPanel
    }

    private fun createDebugNavigationPanel() {
        debugNavigationPanel = JPanel()
        debugNavigationPanel.layout = FlowLayout(FlowLayout.CENTER)
        debugNavigationPanel.preferredSize = Dimension(1200, 40)

        // Кнопка "Предыдущая"
        prevDebugButton = JButton("◀ Пред.")
        prevDebugButton.preferredSize = Dimension(80, 30)
        prevDebugButton.addActionListener {
            if (debugCommands.isNotEmpty()) {
                currentDebugCommandIndex = (currentDebugCommandIndex - 1 + debugCommands.size) % debugCommands.size
                updateDebugDisplay()
            }
        }

        // Кнопка "Следующая"  
        nextDebugButton = JButton("След. ▶")
        nextDebugButton.preferredSize = Dimension(80, 30)
        nextDebugButton.addActionListener {
            if (debugCommands.isNotEmpty()) {
                currentDebugCommandIndex = (currentDebugCommandIndex + 1) % debugCommands.size
                updateDebugDisplay()
            }
        }

        // Информационная метка
        debugInfoLabel = JLabel("Debug: выключен")
        debugInfoLabel.preferredSize = Dimension(350, 30)

        // Метка статуса рендеринга
        statusLabel = JLabel("Готово")
        statusLabel.preferredSize = Dimension(200, 30)

        // Подсказка о горячих клавишах
        helpLabel = JLabel("Горячие клавиши: R - вкл/выкл debug, Q/E - переключение команд")
        helpLabel.preferredSize = Dimension(400, 30)

        debugNavigationPanel.add(statusLabel)
        debugNavigationPanel.add(prevDebugButton)
        debugNavigationPanel.add(debugInfoLabel)
        debugNavigationPanel.add(nextDebugButton)
        debugNavigationPanel.add(helpLabel)

        // Изначально кнопки отключены
        updateDebugNavigationState()

        // Панель не видна, если debug выключен
        // Панель всегда видима, статус слева, debug-инфо по флагу
        debugNavigationPanel.isVisible = true
    }

    private fun showConfigDialog() {
        // Создание и отображение редактора
        val configDialog = ConfigEditor(
            settingsHolder.settings, onKeyboardSettingsChanged = {
            settingsHolder.updateSettings(it)
            rebuildConfigAndRequestRendering(
                plugins, setOf(
                    KeyboardPart.KeyMatrix,
                    KeyboardPart.KeyCaps, KeyboardPart.Case, KeyboardPart.Plate,
                )
            )
        },

            onThumbClusterSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering(
                    plugins, setOf(
                        KeyboardPart.KeyMatrix,
                        KeyboardPart.KeyCaps, KeyboardPart.Case, KeyboardPart.Plate,
                    )
                )
            }, onTrackballSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering(
                    plugins, setOf(
                        KeyboardPart.TrackBall,
                        KeyboardPart.TrackBallSensor, KeyboardPart.TrackBallHolder, KeyboardPart.TrackBallSensorCap,
                        KeyboardPart.Case,
                    )
                )
            })
        configDialog.isVisible = true
    }

    private fun isScriptPanelVisible(): Boolean =
        ::scriptEditorPanel.isInitialized && scriptEditorPanel.parent != null

    private fun rebuildConfigAndRequestRendering(plugins: List<Cad3dPlugin>, modifiedKeyboardParts: Set<KeyboardPart>) {
        plugins.forEach {
            println("Request from ${it.name} , ver ${it.version}")
            // Показать статус: Рендеринг
            setRenderingStatus(true)
            it.requestModels(
                settingsHolder.settings, modifiedKeyboardParts, object : ResultListener {
                    override fun onReady(result: List<VertexHolder>, complete: Boolean) {
                        // Plugin (keyboard) model is shown only when the script panel is hidden.
                        // When the script panel is open, the script has rendering priority.
                        if (!isScriptPanelVisible()) {
                            vertexHolderList.clear()
                            vertexHolderList.addAll(result)
                            if (complete) {
                                setRenderingStatus(false)
                            }
                            requestRender()
                        }
                    }
                })
        }
    }

    private fun addDebugCommands() {
        currentDebugCommandIndex = 0
        updateDebugNavigationState()
    }

    private fun updateDebugNavigationState() {
        prevDebugButton.isEnabled = showDebugInfo
        nextDebugButton.isEnabled = showDebugInfo
        debugInfoLabel.isVisible = showDebugInfo
        prevDebugButton.isVisible = showDebugInfo
        nextDebugButton.isVisible = showDebugInfo
        helpLabel.isVisible = showDebugInfo

        if (showDebugInfo && debugCommands.isNotEmpty()) {
            val currentCmd = debugCommands[currentDebugCommandIndex]
            debugInfoLabel.text =
                "Debug (${currentDebugCommandIndex + 1}/${debugCommands.size}): ${currentCmd.description}"
        } else {
            debugInfoLabel.text = "Debug: выключен"
        }

        // Обновляем layout окна при изменении видимости панели
        debugNavigationPanel.revalidate()
        debugNavigationPanel.repaint()
        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun setRenderingStatus(isRendering: Boolean) {
        statusLabel.text = if (isRendering) "Рендеринг" else "Готово"
        statusLabel.background = if (isRendering) Color.ORANGE else Color.GREEN
    }

    private fun updateDebugDisplay() {
        debugVisualizer.clearVisualization()

        if (showDebugInfo && debugCommands.isNotEmpty()) {
            // Рендерим статические debug объекты

            // Рендерим только текущую debug команду
            val currentCmd = debugCommands[currentDebugCommandIndex]
            debugVisualizer.applyDebugVisualization(currentCmd)
        }

        updateDebugNavigationState()
    }

    private fun requestRender() {
        glCanvas?.display()
    }

    private fun createToggleButton(text: String, initialState: Boolean, onChanged: (Boolean) -> Unit): JCheckBox {
        val button = JCheckBox(text, initialState)
        button.addActionListener { onChanged(button.isSelected) }

        // Автоматический расчет ширины на основе текста
        val metrics = button.getFontMetrics(button.font)
        val textWidth = metrics.stringWidth(text)
        val preferredWidth = minOf(textWidth + 40, 300) // Максимум 300px

        button.preferredSize = Dimension(preferredWidth, 30)
        button.minimumSize = Dimension(100, 30)

        return button
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT or GL2.GL_DEPTH_BUFFER_BIT)
        gl.glLoadIdentity()

        // Устанавливаем GL контекст для debug визуализатора
        debugVisualizer.setGL(gl)

        // Установка материала для куба
        val materialDiffuse = floatArrayOf(0.7f, 0.7f, 0.7f, 1.0f)
        gl.glMaterialfv(GL2.GL_FRONT, GLLightingFunc.GL_DIFFUSE, materialDiffuse, 0)

        // Перемещение куба в нужное место
        gl.glTranslatef(settingsHolder.translateX, settingsHolder.translateY, settingsHolder.translateZ)
        gl.glPushMatrix()
        gl.glRotatef(settingsHolder.rotateX, 1.0f, 0.0f, 0.0f)
        gl.glRotatef(settingsHolder.rotateY, 0.0f, 1.0f, 0.0f)
        gl.glRotatef(settingsHolder.rotateZ, 0.0f, 0.0f, 1.0f)
        for (vertexHolder in vertexHolderList) {
            gl.glBegin(GL2.GL_TRIANGLES)
            var normalArrayIndex = 0
            var vertexArrayIndex = 0
            val vert = vertexHolder.vertex
            val normals = vertexHolder.normals
            for (i in 0 until vertexHolder.verticesCount) {
                val x = vert[vertexArrayIndex++]
                val y = vert[vertexArrayIndex++]
                val z = vert[vertexArrayIndex++]
                gl.glColor4f(
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++],
                    vert[vertexArrayIndex++]
                )
                gl.glNormal3f(
                    normals[normalArrayIndex++], normals[normalArrayIndex++], normals[normalArrayIndex++]
                )
                gl.glVertex3f(x, y, z)
            }
            gl.glEnd()
        }

        // Рендерим debug объекты если они включены (ВНУТРИ трансформаций)
        if (showDebugInfo) {
            debugVisualizer.renderDebugObjects()
        }

        gl.glPopMatrix() // Возвращаемся к исходной матрице

        renderAxisGizmo(gl)

        gl.glFlush()
    }

    private fun renderAxisGizmo(gl: GL2) {
        val size = 270
        val margin = 10
        // Левый нижний угол (в GL Y растёт вверх)
        val vpX = margin
        val vpY = margin

        // Сохраняем текущие атрибуты/матрицы
        val savedLighting = gl.glIsEnabled(GLLightingFunc.GL_LIGHTING)
        val savedColorMaterial = gl.glIsEnabled(GL2.GL_COLOR_MATERIAL)
        val currentProgram = IntArray(1)
        gl.glGetIntegerv(GL2.GL_CURRENT_PROGRAM, currentProgram, 0)
        // Отключаем шейдерную программу (иначе её освещение затемняет gizmo)
        gl.glUseProgram(0)
        // Отключаем свет и color-material, чтобы gizmo был всегда ярким
        gl.glDisable(GLLightingFunc.GL_LIGHTING)
        gl.glDisable(GL2.GL_COLOR_MATERIAL)
        gl.glDisable(GL2.GL_DEPTH_TEST)

        gl.glViewport(vpX, vpY, size, size)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPushMatrix()
        gl.glLoadIdentity()
        val range = 1.6
        gl.glOrtho(-range, range, -range, range, -10.0, 10.0)

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glLoadIdentity()

        // Те же вращения, что и у модели (без переноса)
        gl.glRotatef(settingsHolder.rotateX, 1.0f, 0.0f, 0.0f)
        gl.glRotatef(settingsHolder.rotateY, 0.0f, 1.0f, 0.0f)
        gl.glRotatef(settingsHolder.rotateZ, 0.0f, 0.0f, 1.0f)

        val red = floatArrayOf(1.0f, 0.3f, 0.3f)
        val green = floatArrayOf(0.35f, 1.0f, 0.35f)
        val blue = floatArrayOf(0.45f, 0.6f, 1.0f)

        gl.glLineWidth(3.0f)
        gl.glBegin(GL2.GL_LINES)
        // X - красный
        gl.glColor3f(red[0], red[1], red[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(1f, 0f, 0f)
        // Y - зелёный
        gl.glColor3f(green[0], green[1], green[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(0f, 1f, 0f)
        // Z - синий
        gl.glColor3f(blue[0], blue[1], blue[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(0f, 0f, 1f)
        gl.glEnd()

        // Подписи осей (буквы отрисованы отрезками у конца каждой оси)
        gl.glLineWidth(2.5f)
        drawAxisLabel(gl, 'X', 1.18f, 0f, 0f, red)
        drawAxisLabel(gl, 'Y', 0f, 1.18f, 0f, green)
        drawAxisLabel(gl, 'Z', 0f, 0f, 1.18f, blue)
        gl.glLineWidth(1.0f)

        // Восстанавливаем матрицы и viewport
        gl.glPopMatrix()
        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPopMatrix()
        gl.glMatrixMode(GL2.GL_MODELVIEW)

        gl.glViewport(0, 0, viewportWidth, viewportHeight)
        gl.glEnable(GL2.GL_DEPTH_TEST)
        if (savedLighting) {
            gl.glEnable(GLLightingFunc.GL_LIGHTING)
        }
        if (savedColorMaterial) {
            gl.glEnable(GL2.GL_COLOR_MATERIAL)
        }
        // Восстанавливаем шейдерную программу
        gl.glUseProgram(currentProgram[0])
    }

    // Рисует букву-подпись оси, всегда развёрнутую к экрану (billboard),
    // компенсируя вращение сцены обратным поворотом.
    private fun drawAxisLabel(gl: GL2, letter: Char, x: Float, y: Float, z: Float, color: FloatArray) {
        gl.glColor3f(color[0], color[1], color[2])
        gl.glPushMatrix()
        gl.glTranslatef(x, y, z)
        // Разворот к экрану: обратный порядок и знак вращений сцены
        gl.glRotatef(-settingsHolder.rotateZ, 0.0f, 0.0f, 1.0f)
        gl.glRotatef(-settingsHolder.rotateY, 0.0f, 1.0f, 0.0f)
        gl.glRotatef(-settingsHolder.rotateX, 1.0f, 0.0f, 0.0f)
        val s = 0.16f
        gl.glScalef(s, s, s)
        gl.glBegin(GL2.GL_LINES)
        when (letter) {
            'X' -> {
                gl.glVertex3f(-0.6f, 1f, 0f); gl.glVertex3f(0.6f, -1f, 0f)
                gl.glVertex3f(0.6f, 1f, 0f); gl.glVertex3f(-0.6f, -1f, 0f)
            }
            'Y' -> {
                gl.glVertex3f(-0.6f, 1f, 0f); gl.glVertex3f(0f, 0f, 0f)
                gl.glVertex3f(0.6f, 1f, 0f); gl.glVertex3f(0f, 0f, 0f)
                gl.glVertex3f(0f, 0f, 0f); gl.glVertex3f(0f, -1f, 0f)
            }
            'Z' -> {
                gl.glVertex3f(-0.6f, 1f, 0f); gl.glVertex3f(0.6f, 1f, 0f)
                gl.glVertex3f(0.6f, 1f, 0f); gl.glVertex3f(-0.6f, -1f, 0f)
                gl.glVertex3f(-0.6f, -1f, 0f); gl.glVertex3f(0.6f, -1f, 0f)
            }
        }
        gl.glEnd()
        gl.glPopMatrix()
    }

    override fun dispose(drawable: GLAutoDrawable) {
        // TODO Auto-generated method stub
    }

    private fun initializeProgram(gl: GL2) {
        println("initializeProgram")
        val vertexShaderSource = ShaderLoader.loadShader("/shader_vertex_104.txt")
        val fragmentShaderSource = ShaderLoader.loadShader("/shader_fragment_104.txt")
        val shaderProgram = ShaderProgram(gl, vertexShaderSource, fragmentShaderSource)
        shaderProgram.use(gl)
        val error = gl.glGetError()
        if (error != 0) {
            println("Error while setting shaders : $error")
        }
    }

    override fun init(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2
        init(gl)
    }

    protected fun init(gl: GL2) {
        println("init gl2")
        gl.glShadeModel(GL2.GL_SMOOTH)
        gl.glClearColor(0.2f, 0.2f, 0.2f, 1.0f)
        gl.glClearDepth(1.0)
        gl.glEnable(GL2.GL_DEPTH_TEST)
        gl.glDepthFunc(GL2.GL_LEQUAL)
        gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST)

        // Включение освещения
        gl.glEnable(GLLightingFunc.GL_LIGHTING)
        gl.glEnable(GLLightingFunc.GL_LIGHT0)
        gl.glEnable(GL2.GL_COLOR_MATERIAL)
        gl.glColorMaterial(GL2.GL_FRONT_AND_BACK, GL2.GL_AMBIENT_AND_DIFFUSE)

        // Настройка света
        val lightAmbient = floatArrayOf(0.2f, 0.2f, 0.2f, 1.0f)
        val lightDiffuse = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_AMBIENT, lightAmbient, 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_DIFFUSE, lightDiffuse, 0)
        gl.glLightfv(GLLightingFunc.GL_LIGHT0, GLLightingFunc.GL_POSITION, lightPosition, 0)
        initializeProgram(gl)
    }

    override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) {
        var height = height
        val gl = drawable.gl.gL2
        if (height <= 0) {
            height = 1
        }
        val h = width.toFloat() / height.toFloat()
        viewportWidth = width
        viewportHeight = height
        gl.glViewport(0, 0, width, height)
        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glLoadIdentity()
        val aspect = width.toFloat() / height
        val fov = 45.0f
        val near = 0.1f
        val far = 1400.0f
        glu.gluPerspective(fov, aspect, near, far)
        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glLoadIdentity()
    }

    // ------------------------------------------------
    private inner class GlCanvasMouseListener : MouseListener, MouseMotionListener, MouseWheelListener {

        override fun mouseClicked(mouseEvent: MouseEvent) {}
        override fun mouseEntered(mouseEvent: MouseEvent) {}
        override fun mouseExited(mouseEvent: MouseEvent) {}
        override fun mousePressed(mouseEvent: MouseEvent) {
            prevMouseX = mouseEvent.x
            prevMouseY = mouseEvent.y
        }

        override fun mouseReleased(mouseEvent: MouseEvent) {}

        /// motion
        override fun mouseDragged(e: MouseEvent) {
            val currentMouseX = e.x
            val currentMouseY = e.y
            val deltaX = currentMouseX - prevMouseX
            val deltaY = currentMouseY - prevMouseY
            if (e.modifiersEx and InputEvent.CTRL_DOWN_MASK != 0) {
                // Смещение объекта при зажатом Control
                settingsHolder.translateX += deltaX * MOUSE_TRANSLATE_SENSITIVITY
                settingsHolder.translateY -= deltaY * MOUSE_TRANSLATE_SENSITIVITY
            } else {
                settingsHolder.rotateX += deltaY.toFloat()
                settingsHolder.rotateZ += deltaX.toFloat()
            }
            prevMouseX = currentMouseX
            prevMouseY = currentMouseY
            requestRender()
        }

        override fun mouseMoved(e: MouseEvent) {}
        override fun mouseWheelMoved(e: MouseWheelEvent) {
            val notches = e.wheelRotation

            // Управление смещением с помощью Ctrl
            if (e.modifiersEx and InputEvent.CTRL_DOWN_MASK != 0) {
                // При зажатом Ctrl - изменение масштаба
            } else {
                // Без Ctrl - перемещение по осям
                settingsHolder.translateZ -= notches * ZOOM_SENSITIVITY
            }

            // Ограничиваем диапазон значений (опционально)
            settingsHolder.translateZ = Math.max(ZOOM_MIN_OFFSET, Math.min(settingsHolder.translateZ, ZOOM_MAX_OFFSET))
            requestRender()
        }
    }

    private inner class GlCanvasKeyListener : KeyListener {

        override fun keyTyped(e: KeyEvent) {}
        override fun keyPressed(e: KeyEvent) {
            val keyCode = e.keyCode
            when (keyCode) {
                KeyEvent.VK_A, KeyEvent.VK_LEFT -> settingsHolder.translateX -= TRANSLATE_STEP
                KeyEvent.VK_D, KeyEvent.VK_RIGHT -> settingsHolder.translateX += TRANSLATE_STEP
                KeyEvent.VK_W, KeyEvent.VK_UP -> settingsHolder.translateY += TRANSLATE_STEP
                KeyEvent.VK_S, KeyEvent.VK_DOWN -> settingsHolder.translateY -= TRANSLATE_STEP

                // Горячие клавиши для debug навигации
                KeyEvent.VK_Q -> {
                    if (showDebugInfo && debugCommands.isNotEmpty()) {
                        currentDebugCommandIndex =
                            (currentDebugCommandIndex - 1 + debugCommands.size) % debugCommands.size
                        updateDebugDisplay()
                    }
                }

                KeyEvent.VK_E -> {
                    if (showDebugInfo && debugCommands.isNotEmpty()) {
                        currentDebugCommandIndex = (currentDebugCommandIndex + 1) % debugCommands.size
                        updateDebugDisplay()
                    }
                }

                KeyEvent.VK_R -> {
                    // Переключение debug режима
                    showDebugInfo = !showDebugInfo
                    if (!showDebugInfo) {
                        debugVisualizer.clearVisualization()
                        //debugCommands.clear()
                    } else {
                        addDebugCommands()
                        updateDebugDisplay()
                    }
                    updateDebugNavigationState()
                }
            }
            requestRender()
        }

        override fun keyReleased(e: KeyEvent) {}
    }

    companion object {

        private const val ZOOM_SENSITIVITY = 5.0f
        private const val ZOOM_MIN_OFFSET = -1200.0f
        private const val ZOOM_MAX_OFFSET = 0.0f
        private const val SETTINGS_FILE = "settings.json"
        private const val MOUSE_TRANSLATE_SENSITIVITY = 0.5f // Чувствительность смещения
        private const val TRANSLATE_STEP = 5.0f // Шаг смещения

        @JvmStatic
        fun main(args: Array<String>) {
            Main("Генератор эргономичной клавиатуры от Grishberg")
        }
    }
}
