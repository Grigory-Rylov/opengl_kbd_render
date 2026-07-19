package com.github.grishberg.cad3d.viewer.render

import com.github.grishberg.cad3d.plugin.VertexHolder
import com.jogamp.opengl.GL2

/**
 * Отрисовывает список [VertexHolder] треугольниками.
 * Формат вершины: 7 float (x, y, z, r, g, b, a), нормали: 3 float на вершину.
 */
class MeshRenderer {
    fun render(gl: GL2, holders: List<VertexHolder>) {
        for (holder in holders) {
            drawHolder(gl, holder)
        }
    }

    private fun drawHolder(gl: GL2, holder: VertexHolder) {
        gl.glBegin(GL2.GL_TRIANGLES)
        var normalIndex = 0
        var vertexIndex = 0
        val vert = holder.vertex
        val normals = holder.normals
        for (i in 0 until holder.verticesCount) {
            val x = vert[vertexIndex++]
            val y = vert[vertexIndex++]
            val z = vert[vertexIndex++]
            gl.glColor4f(
                vert[vertexIndex++],
                vert[vertexIndex++],
                vert[vertexIndex++],
                vert[vertexIndex++],
            )
            gl.glNormal3f(
                normals[normalIndex++], normals[normalIndex++], normals[normalIndex++],
            )
            gl.glVertex3f(x, y, z)
        }
        gl.glEnd()
    }
}
