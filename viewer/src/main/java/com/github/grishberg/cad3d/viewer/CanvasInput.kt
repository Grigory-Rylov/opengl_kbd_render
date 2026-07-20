package com.github.grishberg.cad3d.viewer

import com.github.grishberg.cad3d.debug.DebugCmd
import com.github.grishberg.cad3d.viewer.debug.DebugVisualizerImpl
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import java.awt.event.KeyListener

/**
 * Surface the canvas input listeners need from the host viewer. Implemented by [Main]
 * so the listeners can stay in their own file without being inner classes.
 */
interface CanvasInteraction {
    val settings: SettingsHolder
    var showDebugInfo: Boolean
    val debugCommands: MutableList<DebugCmd>
    val debugVisualizer: DebugVisualizerImpl

    fun requestRender()
    fun updateDebugDisplay()
    fun updateDebugNavigationState()
    fun addDebugCommands()

    fun cycleDebugBackward()
    fun cycleDebugForward()
    fun toggleDebug()
}

class GlCanvasMouseListener(
    private val interaction: CanvasInteraction,
) : MouseListener, MouseMotionListener, MouseWheelListener {

    private var prevMouseX = 0
    private var prevMouseY = 0

    override fun mouseClicked(mouseEvent: MouseEvent) {}
    override fun mouseEntered(mouseEvent: MouseEvent) {}
    override fun mouseExited(mouseEvent: MouseEvent) {}

    override fun mousePressed(mouseEvent: MouseEvent) {
        prevMouseX = mouseEvent.x
        prevMouseY = mouseEvent.y
    }

    override fun mouseReleased(mouseEvent: MouseEvent) {}

    override fun mouseDragged(e: MouseEvent) {
        val currentMouseX = e.x
        val currentMouseY = e.y
        val deltaX = currentMouseX - prevMouseX
        val deltaY = currentMouseY - prevMouseY
        val settings = interaction.settings
        if (e.modifiersEx and InputEvent.CTRL_DOWN_MASK != 0) {
            // Смещение объекта при зажатом Control
            settings.translateX += deltaX * MOUSE_TRANSLATE_SENSITIVITY
            settings.translateY -= deltaY * MOUSE_TRANSLATE_SENSITIVITY
        } else {
            settings.rotateX += deltaY.toFloat()
            settings.rotateZ += deltaX.toFloat()
        }
        prevMouseX = currentMouseX
        prevMouseY = currentMouseY
        interaction.requestRender()
    }

    override fun mouseMoved(e: MouseEvent) {}

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        val notches = e.wheelRotation
        val settings = interaction.settings

        // Управление смещением с помощью Ctrl
        if (e.modifiersEx and InputEvent.CTRL_DOWN_MASK != 0) {
            // При зажатом Ctrl - изменение масштаба
        } else {
            // Без Ctrl - перемещение по осям
            settings.translateZ -= notches * ZOOM_SENSITIVITY
        }

        // Ограничиваем диапазон значений (опционально)
        settings.translateZ = Math.max(ZOOM_MIN_OFFSET, Math.min(settings.translateZ, ZOOM_MAX_OFFSET))
        interaction.requestRender()
    }
}

class GlCanvasKeyListener(
    private val interaction: CanvasInteraction,
) : KeyListener {

    override fun keyTyped(e: KeyEvent) {}

    override fun keyPressed(e: KeyEvent) {
        val settings = interaction.settings
        when (e.keyCode) {
            KeyEvent.VK_A, KeyEvent.VK_LEFT -> settings.translateX -= TRANSLATE_STEP
            KeyEvent.VK_D, KeyEvent.VK_RIGHT -> settings.translateX += TRANSLATE_STEP
            KeyEvent.VK_W, KeyEvent.VK_UP -> settings.translateY += TRANSLATE_STEP
            KeyEvent.VK_S, KeyEvent.VK_DOWN -> settings.translateY -= TRANSLATE_STEP

            // Горячие клавиши для debug навигации
            KeyEvent.VK_Q -> interaction.cycleDebugBackward()

            KeyEvent.VK_E -> interaction.cycleDebugForward()

            KeyEvent.VK_R -> interaction.toggleDebug()
        }
        interaction.requestRender()
    }

    override fun keyReleased(e: KeyEvent) {}
}

private const val ZOOM_SENSITIVITY = 5.0f
private const val ZOOM_MIN_OFFSET = -1200.0f
private const val ZOOM_MAX_OFFSET = 0.0f
private const val MOUSE_TRANSLATE_SENSITIVITY = 0.5f // Чувствительность смещения
private const val TRANSLATE_STEP = 5.0f // Шаг смещения
