package org.example

import com.github.grishberg.cad3d.keyboard.ControlPointsController
import com.github.grishberg.cad3d.keyboard.cfg.SettingsHolder
import com.github.grishberg.cad3d.util.SceneBuilder
import com.github.grishberg.cad3d.util.SceneBuilderKeyboard
import com.jogamp.opengl.GL2
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLCanvas
import com.jogamp.opengl.fixedfunc.GLLightingFunc
import com.jogamp.opengl.glu.GLU
import com.jogamp.opengl.util.Animator
import eu.printingin3d.javascad.vrl.VertexHolder
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.ActionEvent
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
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JPanel
import org.example.dialog.ConfigEditor

class Main(title: String?) : JFrame(title), GLEventListener {

    //    protected GLWindow window;
    protected var animator: Animator? = null

    //public Caps caps;
    // Установка позиции источника света
    var lightPosition = floatArrayOf(0.0f, 1f, 0.5f, 1.0f)
    private val settingsHolder = SettingsHolder(SETTINGS_FILE)

    private val vertexHolderList: MutableList<VertexHolder> = ArrayList()
    private val glu = GLU()
    private var prevMouseX = 0
    private var prevMouseY = 0
    private val pointsController = ControlPointsController()
    private var glCanvas: GLCanvas? = null
    private val sceneBuilder: SceneBuilder

    private var requestRenderingTime = 0L

    init {
        settingsHolder.loadSettings()
        sceneBuilder = SceneBuilderKeyboard(settingsHolder.settings.getKeyboardConfig(), pointsController)
        sceneBuilder.setListener { buffers: List<VertexHolder>, isReady: Boolean ->
            if (isReady) {
                val timeDelta = System.currentTimeMillis() - requestRenderingTime
                println("Rendering time = $timeDelta ms")
            }
            vertexHolderList.clear()
            vertexHolderList.addAll(buffers)
        }
        rebuildConfigAndRequestRendering()
        setup()
    }

    fun setup() {
        layout = BorderLayout()
        // Создаем панель управления
        val controlPanel = JPanel()
        controlPanel.layout = FlowLayout(FlowLayout.LEFT)
        controlPanel.preferredSize = Dimension(800, 40)

        // Добавляем переключатели
        val keysButton = createToggleButton("Клавиши", settingsHolder.settingsShowCaps) {
            settingsHolder.settingsShowCaps = it
            rebuildConfigAndRequestRendering()
        }
        val caseButton = createToggleButton("Корпус", settingsHolder.settingsShowCase) {
            settingsHolder.settingsShowCase = it
            rebuildConfigAndRequestRendering()
        }
        val matrixButton = createToggleButton("Матрица", settingsHolder.settingsShowMatrix) {
            settingsHolder.settingsShowMatrix = it
            rebuildConfigAndRequestRendering()
        }
        val plateButton = createToggleButton("Поддон", settingsHolder.settingsShowPlate) {
            settingsHolder.settingsShowPlate = it
            rebuildConfigAndRequestRendering()
        }
        val wristRestButton = createToggleButton("Держатель рук", settingsHolder.settingsShowWristRest) {
            settingsHolder.settingsShowWristRest = it
            rebuildConfigAndRequestRendering()
        }
        val trackballButton = createToggleButton("Трэкбол", settingsHolder.settingsTrackball) {
            settingsHolder.settingsTrackball = it
            rebuildConfigAndRequestRendering()
        }
        val trackballSensorButton = createToggleButton("Сенсор ТБ", settingsHolder.showTrackballSensor) {
            settingsHolder.showTrackballSensor = it
            rebuildConfigAndRequestRendering()
        }
        val trackballSensorCapButton = createToggleButton("Крышка сенсора ТБ", settingsHolder.showTrackballSensorCap) {
            settingsHolder.showTrackballSensorCap = it
            rebuildConfigAndRequestRendering()
        }
        val showControllerHolderButton =
            createToggleButton("Держатель контроллера", settingsHolder.showControllerHolder) {
                settingsHolder.showControllerHolder = it
                rebuildConfigAndRequestRendering()
            }

        val showControllerButton = createToggleButton("Контроллера", settingsHolder.showController) {
            settingsHolder.showController = it
            rebuildConfigAndRequestRendering()
        }

        val showAmoebaButton = createToggleButton("Амебы", settingsHolder.showAmoeba) {
            settingsHolder.showAmoeba = it
            rebuildConfigAndRequestRendering()
        }

        controlPanel.add(keysButton)
        controlPanel.add(caseButton)
        controlPanel.add(matrixButton)
        controlPanel.add(plateButton)
        controlPanel.add(wristRestButton)
        controlPanel.add(trackballButton)
        controlPanel.add(trackballSensorButton)
        controlPanel.add(trackballSensorCapButton)
        controlPanel.add(showControllerHolderButton)
        controlPanel.add(showControllerButton)
        controlPanel.add(showAmoebaButton)

        val configButton = JButton("Конфигурации")
        configButton.addActionListener {
            showConfigDialog()
        }
        controlPanel?.add(configButton)

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
        animator = Animator()
        animator!!.add(glCanvas)
        animator!!.start()
        contentPane.add(glCanvas, BorderLayout.CENTER)
        contentPane.add(controlPanel, BorderLayout.NORTH)

        // Обработка закрытия окна
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                settingsHolder.saveSettings()
                animator!!.stop()
                dispose()
            }
        })
        setSize(1200, 800)
        isVisible = true
    }

    private fun showConfigDialog() {
        // Создание и отображение редактора
        val configDialog = ConfigEditor(settingsHolder.settings, onKeyboardSettingsChanged = {
            settingsHolder.updateSettings(it)
            rebuildConfigAndRequestRendering()
        },

            onThumbClusterSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering()
            }, onTrackballSettingsChanged = {
                settingsHolder.updateSettings(it)
                rebuildConfigAndRequestRendering()
            })
        configDialog.isVisible = true
    }

    private fun rebuildConfigAndRequestRendering() {
        sceneBuilder.setConfig(settingsHolder.settings.getKeyboardConfig())
        requestRenderingTime = System.currentTimeMillis()
        sceneBuilder.requestBuffers()
    }

    private fun createToggleButton(text: String, initialState: Boolean, onChanged: (Boolean) -> Unit): JCheckBox {
        val button = JCheckBox(text, initialState)
        button.addActionListener { e: ActionEvent? -> onChanged(button.isSelected) }
        button.preferredSize = Dimension(100, 30)
        return button
    }

    override fun display(drawable: GLAutoDrawable) {
        val gl = drawable.gl.gL2
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT or GL2.GL_DEPTH_BUFFER_BIT)
        gl.glLoadIdentity()

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
        gl.glPopMatrix() // Возвращаемся к исходной матрице
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
        gl.glClearColor(0f, 0f, 0f, 0f)
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
        println(
            "reshape x=$x, y=$y, widht=$width, height=$height"
        )
        // TODO Auto-generated method stub
        val gl = drawable.gl.gL2
        if (height <= 0) {
            height = 1
        }
        val h = width.toFloat() / height.toFloat()
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
            }
            //window.reparentWindow(); // Обновляем отображение
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
