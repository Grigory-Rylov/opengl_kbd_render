package com.github.grishberg.cad3d.viewer.render

import com.jogamp.opengl.GL2
import com.jogamp.opengl.fixedfunc.GLLightingFunc

/**
 * Рисует поворотный gizmo осей X/Y/Z в левом нижнем углу.
 * Оси вращаются синхронно со сценой, подписи всегда развёрнуты к экрану.
 */
class AxisGizmoRenderer(
    private val size: Int = 270,
    private val margin: Int = 10,
) {
    private val red = floatArrayOf(1.0f, 0.3f, 0.3f)
    private val green = floatArrayOf(0.35f, 1.0f, 0.35f)
    private val blue = floatArrayOf(0.45f, 0.6f, 1.0f)

    fun render(
        gl: GL2,
        rotateX: Float,
        rotateY: Float,
        rotateZ: Float,
        viewportWidth: Int,
        viewportHeight: Int,
    ) {
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

        gl.glViewport(margin, margin, size, size)

        gl.glMatrixMode(GL2.GL_PROJECTION)
        gl.glPushMatrix()
        gl.glLoadIdentity()
        val range = 1.6
        gl.glOrtho(-range, range, -range, range, -10.0, 10.0)

        gl.glMatrixMode(GL2.GL_MODELVIEW)
        gl.glPushMatrix()
        gl.glLoadIdentity()

        // Те же вращения, что и у модели (без переноса)
        gl.glRotatef(rotateX, 1.0f, 0.0f, 0.0f)
        gl.glRotatef(rotateY, 0.0f, 1.0f, 0.0f)
        gl.glRotatef(rotateZ, 0.0f, 0.0f, 1.0f)

        drawAxes(gl)

        // Подписи осей (буквы отрисованы отрезками у конца каждой оси)
        gl.glLineWidth(2.5f)
        drawAxisLabel(gl, 'X', 1.18f, 0f, 0f, red, rotateX, rotateY, rotateZ)
        drawAxisLabel(gl, 'Y', 0f, 1.18f, 0f, green, rotateX, rotateY, rotateZ)
        drawAxisLabel(gl, 'Z', 0f, 0f, 1.18f, blue, rotateX, rotateY, rotateZ)
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

    private fun drawAxes(gl: GL2) {
        gl.glLineWidth(3.0f)
        gl.glBegin(GL2.GL_LINES)
        gl.glColor3f(red[0], red[1], red[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(1f, 0f, 0f)
        gl.glColor3f(green[0], green[1], green[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(0f, 1f, 0f)
        gl.glColor3f(blue[0], blue[1], blue[2])
        gl.glVertex3f(0f, 0f, 0f)
        gl.glVertex3f(0f, 0f, 1f)
        gl.glEnd()
    }

    // Рисует букву-подпись оси, всегда развёрнутую к экрану (billboard),
    // компенсируя вращение сцены обратным поворотом.
    private fun drawAxisLabel(
        gl: GL2,
        letter: Char,
        x: Float,
        y: Float,
        z: Float,
        color: FloatArray,
        rotateX: Float,
        rotateY: Float,
        rotateZ: Float,
    ) {
        gl.glColor3f(color[0], color[1], color[2])
        gl.glPushMatrix()
        gl.glTranslatef(x, y, z)
        // Разворот к экрану: обратный порядок и знак вращений сцены
        gl.glRotatef(-rotateZ, 0.0f, 0.0f, 1.0f)
        gl.glRotatef(-rotateY, 0.0f, 1.0f, 0.0f)
        gl.glRotatef(-rotateX, 1.0f, 0.0f, 0.0f)
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
}
