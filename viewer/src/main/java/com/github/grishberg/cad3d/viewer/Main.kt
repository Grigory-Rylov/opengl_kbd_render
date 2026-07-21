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
import com.github.grishberg.cad3d.viewer.dialog.ScriptEditorPanel
import com.github.grishberg.scripting.ScriptEvaluator
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
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class Main(title: String?) : JFrame(title), GLEventListener, CanvasInteraction, ControlPanelActions {

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
    private val pointsController = ControlPointsController()
    private var glCanvas: GLCanvas? = null
    private var splitPane: javax.swing.JSplitPane? = null
    private val meshRenderer = com.github.grishberg.cad3d.viewer.render.MeshRenderer()
    private val axisGizmoRenderer = com.github.grishberg.cad3d.viewer.render.AxisGizmoRenderer()

    override var showDebugInfo = false
    private var currentDebugCommandIndex = 0
    private lateinit var controlPanel: ControlPanel
    private lateinit var debugNavigation: DebugNavigationPanel
    private var pluginManager: PluginManager? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var loadedPlugins: List<Cad3dPlugin> = emptyList()
    override val plugins: List<Cad3dPlugin> get() = loadedPlugins
    private lateinit var scriptEditorPanel: ScriptEditorPanel
    private var scriptModelEnabled = false
    private val scriptEvaluator: ScriptEvaluator by lazy {
        ScriptEvaluator(filterScriptClasspath())
    }

    override val settings: SettingsHolder = settingsHolder
    override val debugVisualizer = DebugVisualizerImpl()
    override val debugCommands = mutableListOf<DebugCmd>()

    private fun filterScriptClasspath(): List<String> {
        val full = System.getProperty("java.class.path").split(java.io.File.pathSeparator).map { p -> java.io.File(p) }
            .filter { f -> f.exists() }.map { f -> f.absolutePath }
        val keep = listOf(
            "scripting", "cad3d", "plugin", "kbd_core", "javascad", "common",
            "kotlin-stdlib", "kotlin-script-runtime",
        )
        val filtered = full.filter { p -> keep.any { name -> p.contains(name) } }
        // Fallback: if filtering dropped essential libs, use the full classpath.
        return if (filtered.any { it.contains("javascad") } && filtered.any { it.contains("kotlin-stdlib") }) filtered else full
    }

    init {
        settingsHolder.loadSettings()

        val loadedPluginsDir = findPluginsDir()
        if (!loadedPluginsDir.exists()) {
            println("WARNING: loadedPlugins dir not found: ${loadedPluginsDir.absolutePath}")
        }
        setup()


        pluginManager = PluginManagerImpl(loadedPluginsDir)
        pluginManager!!.setOnPluginLoadedListener(object : PluginManager.OnPluginLoadedListener {
            override fun onPluginsLoaded(newPlugins: List<Cad3dPlugin>) {
                loadedPlugins = newPlugins
                println("LOADED loadedPlugins count=${newPlugins.size} from ${loadedPluginsDir.absolutePath}")
                if (newPlugins.isEmpty()) {
                    println("WARNING: no loadedPlugins loaded! Check loadedPlugins path.")
                }
                rebuildConfigAndRequestRendering(loadedPlugins, emptySet())
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

    private fun findScriptDir(): String? {
        var dir = java.io.File(System.getProperty("user.dir"))
        repeat(6) {
            val candidate = dir.resolve("scripting/examples")
            if (candidate.exists() && candidate.isDirectory) {
                return candidate.absolutePath
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

    private fun createScriptEditorPanel(initialScript: String = loadMatrixRightText(), scriptDirectory: String? = null): ScriptEditorPanel {
        val classPaths = filterScriptClasspath()

        return ScriptEditorPanel(classPaths, { holders ->
            vertexHolderList.clear()
            vertexHolderList.addAll(holders)
            requestRender()
        }, initialScript, scriptDirectory)
    }

    fun setup() {
        layout = BorderLayout()
        // Создаем меню
        jMenuBar = createMenuBar()
        // Создаем панель управления
        controlPanel = ControlPanel(this, this)
        val controlPanelComponent = controlPanel.build()

        // Создаем панель навигации по debug командам
        debugNavigation = DebugNavigationPanel(
            onPrev = {
                if (showDebugInfo && debugCommands.isNotEmpty()) {
                    currentDebugCommandIndex = (currentDebugCommandIndex - 1 + debugCommands.size) % debugCommands.size
                    updateDebugDisplay()
                }
            },
            onNext = {
                if (showDebugInfo && debugCommands.isNotEmpty()) {
                    currentDebugCommandIndex = (currentDebugCommandIndex + 1) % debugCommands.size
                    updateDebugDisplay()
                }
            },
        )
        val debugNavComponent = debugNavigation.build()

        val glProfile = GLProfile.get(GLProfile.GL2)
        val glCapabilities = GLCapabilities(glProfile)
        glCapabilities.depthBits = 24

        // 2. Создание GLCanvas с явным конструктором
        glCanvas = GLCanvas(glCapabilities)
        glCanvas!!.addGLEventListener(this)
        val mouseListener = GlCanvasMouseListener(this)
        glCanvas!!.addMouseListener(mouseListener)
        glCanvas!!.addMouseMotionListener(mouseListener)
        glCanvas!!.addMouseWheelListener(mouseListener)
        glCanvas!!.addKeyListener(GlCanvasKeyListener(this))
        defaultCloseOperation = EXIT_ON_CLOSE
        animator.add(glCanvas)
        animator.start()
        contentPane.add(glCanvas, BorderLayout.CENTER)
        contentPane.add(controlPanelComponent, BorderLayout.NORTH)
        contentPane.add(debugNavComponent, BorderLayout.SOUTH)

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
                    scriptEditorPanel = createScriptEditorPanel(f.readText(), f.parent)
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
        chooser.fileFilter =
            javax.swing.filechooser.FileNameExtensionFilter("Kotlin scripts (*.kt, *.kts)", "kt", "kts")
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return
        }
        val file = chooser.selectedFile
        try {
            val text = file.readText()
            if (!::scriptEditorPanel.isInitialized) {
                scriptEditorPanel = createScriptEditorPanel(text, file.parent)
            } else {
                scriptEditorPanel.loadScript(text)
            }
            settingsHolder.lastScriptFile = file.absolutePath
            settingsHolder.saveSettings()
            showScriptPanel()
        } catch (e: Exception) {
            javax.swing.JOptionPane.showMessageDialog(
                this, "Не удалось открыть файл: ${e.message}", "Ошибка", javax.swing.JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showScriptPanel() {
        if (!::scriptEditorPanel.isInitialized) {
            scriptEditorPanel = createScriptEditorPanel(scriptDirectory = findScriptDir())
        }
        if (splitPane == null) {
            // Переносим GLCanvas из CENTER в сплиттер вместе с панелью кода
            contentPane.remove(glCanvas)
            val split = com.github.grishberg.cad3d.viewer.render.ResizableSidePanel(
                left = glCanvas!!,
                right = scriptEditorPanel,
                widthProvider = { settingsHolder.scriptPanelWidth },
                onWidthChanged = { settingsHolder.scriptPanelWidth = it },
            )
            splitPane = split
            contentPane.add(split, BorderLayout.CENTER)
            contentPane.revalidate()
            contentPane.repaint()
        }
        settingsHolder.showScriptPanel = true
        controlPanel.scriptEditorButton.text = "Скрипты ✓"
        settingsHolder.saveScriptPanelState()
        // Компилируем текст редактора при открытии панели
        scriptEditorPanel.runScript()
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
        controlPanel.scriptEditorButton.text = "Скрипты"
        settingsHolder.saveScriptPanelState()
        // Script panel closed: render the keyboard from the plugin.
        rebuildConfigAndRequestRendering(loadedPlugins, emptySet())
    }

    override fun toggleScriptPanel() {
        val visible = splitPane != null
        if (visible) hideScriptPanel() else showScriptPanel()
    }

    override fun showConfigDialog() {
        // Создание и отображение редактора
        val configDialog = ConfigEditor(
            settingsHolder.settings, onKeyboardSettingsChanged = {
            settingsHolder.updateSettings(it)
            rebuildConfigAndRequestRendering(
                loadedPlugins, setOf(
                    KeyboardPart.KeyMatrix,
                    KeyboardPart.KeyCaps, KeyboardPart.Case, KeyboardPart.Plate,
                )
            )
        },

            onThumbClusterSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering(
                    loadedPlugins, setOf(
                        KeyboardPart.KeyMatrix,
                        KeyboardPart.KeyCaps, KeyboardPart.Case, KeyboardPart.Plate,
                    )
                )
            }, onTrackballSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering(
                    loadedPlugins, setOf(
                        KeyboardPart.TrackBall,
                        KeyboardPart.TrackBallSensor, KeyboardPart.TrackBallHolder, KeyboardPart.TrackBallSensorCap,
                        KeyboardPart.Case,
                    )
                )
            })
        configDialog.isVisible = true
    }

    private fun isScriptPanelVisible(): Boolean = ::scriptEditorPanel.isInitialized && scriptEditorPanel.parent != null

    private fun rebuildConfigAndRequestRendering(
        loadedPlugins: List<Cad3dPlugin>,
        modifiedKeyboardParts: Set<KeyboardPart>
    ) {
        loadedPlugins.forEach {
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

    override fun addDebugCommands() {
        currentDebugCommandIndex = 0
        updateDebugNavigationState()
    }

    override fun cycleDebugBackward() {
        if (showDebugInfo && debugCommands.isNotEmpty()) {
            currentDebugCommandIndex = (currentDebugCommandIndex - 1 + debugCommands.size) % debugCommands.size
            updateDebugDisplay()
        }
    }

    override fun cycleDebugForward() {
        if (showDebugInfo && debugCommands.isNotEmpty()) {
            currentDebugCommandIndex = (currentDebugCommandIndex + 1) % debugCommands.size
            updateDebugDisplay()
        }
    }

    override fun toggleDebug() {
        applyDebugEnabled(!showDebugInfo)
    }

    override var debugEnabled: Boolean
        get() = showDebugInfo
        set(value) {
            applyDebugEnabled(value)
        }

    override fun onDebugToggled(enabled: Boolean) {
        applyDebugEnabled(enabled)
    }

    private fun applyDebugEnabled(enabled: Boolean) {
        showDebugInfo = enabled
        if (!enabled) {
            debugVisualizer.clearVisualization()
            //debugCommands.clear()
        } else {
            addDebugCommands()
            updateDebugDisplay()
        }
        updateDebugNavigationState()
    }

    override fun rebuild() {
        rebuildConfigAndRequestRendering(loadedPlugins, emptySet())
    }

    override fun updateDebugNavigationState() {
        val description = if (showDebugInfo && debugCommands.isNotEmpty()) {
            debugCommands[currentDebugCommandIndex].description
        } else {
            null
        }
        debugNavigation.updateDebugNavigationState(
            showDebugInfo, debugCommands.size, currentDebugCommandIndex, description
        )
        contentPane.revalidate()
        contentPane.repaint()
    }

    private fun setRenderingStatus(isRendering: Boolean) {
        debugNavigation.setRenderingStatus(isRendering)
    }

    override fun updateDebugDisplay() {
        debugVisualizer.clearVisualization()

        if (showDebugInfo && debugCommands.isNotEmpty()) {
            val currentCmd = debugCommands[currentDebugCommandIndex]
            debugVisualizer.applyDebugVisualization(currentCmd)
        }

        updateDebugNavigationState()
    }

    override fun requestRender() {
        glCanvas?.display()
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

        meshRenderer.render(gl, vertexHolderList)

        // Рендерим debug объекты если они включены (ВНУТРИ трансформаций)
        if (showDebugInfo) {
            debugVisualizer.renderDebugObjects()
        }

        gl.glPopMatrix() // Возвращаемся к исходной матрице

        axisGizmoRenderer.render(
            gl,
            settingsHolder.rotateX,
            settingsHolder.rotateY,
            settingsHolder.rotateZ,
            viewportWidth,
            viewportHeight,
        )

        gl.glFlush()
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

    companion object {

        private const val SETTINGS_FILE = "settings.json"

        @JvmStatic
        fun main(args: Array<String>) {
            Main("Генератор эргономичной клавиатуры от Grishberg")
        }
    }
}
