package com.github.grishberg.javascad.utils

import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.coords2d.Coords2d
import com.github.grishberg.javascad.manifold.Manifold3dEngine
import com.github.grishberg.javascad.models.Empty3dModel
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.StlModel
import com.github.grishberg.javascad.vrl.FacetGenerationContext
import com.github.grishberg.javascad.vrl.Polygon

/**
 * Projects a 3D model onto the XY plane and extrudes the resulting 2D silhouette
 * by the given thickness along Z.
 *
 * Uses marching squares on rasterized silhouette for correct concave contours.
 *
 * Usage in scripts:
 *   cube(50, 30, 20).projectAndExtrude(2.0)
 *   case.projectAndExtrude(2.0)
 */
fun Model.projectAndExtrude(thickness: Double): Model {
    val context = FacetGenerationContext.DEFAULT
    val mesh = this.toNativeMesh(context)
    val result = try {
        doProjectAndExtrude(mesh, thickness)
    } catch (e: Exception) {
        e.printStackTrace()
        this
    } finally {
        Manifold3dEngine.delete(mesh)
    }
    return result
}

private fun doProjectAndExtrude(mesh: Long, thickness: Double): Model {
    val mb = Manifold3dEngine.bindings()
    val data = mb.exportMeshGL64(mesh)
    val verts = data.vertices()
    val tris = data.triangles()
    val inputTriCount = data.triCount().toInt()

    if (inputTriCount == 0) return Empty3dModel()

    // Build edge-to-faces map
    val edgeFaces = HashMap<Long, ArrayList<Int>>()
    fun edgeKey(v0: Int, v1: Int): Long {
        val minV = Math.min(v0, v1).toLong()
        val maxV = Math.max(v0, v1).toLong()
        return minV * 1000000000L + maxV
    }
    fun edgeVertices(key: Long): kotlin.Pair<Int, Int> {
        val minV = (key / 1000000000L).toInt()
        val maxV = (key % 1000000000L).toInt()
        return minV to maxV
    }

    for (i in 0 until inputTriCount) {
        val i0 = tris[i * 3].toInt()
        val i1 = tris[i * 3 + 1].toInt()
        val i2 = tris[i * 3 + 2].toInt()

        for (edge in listOf(kotlin.Pair(i0, i1), kotlin.Pair(i1, i2), kotlin.Pair(i0, i2))) {
            val key = edgeKey(edge.first, edge.second)
            edgeFaces.computeIfAbsent(key) { ArrayList() }.add(i)
        }
    }

    // Normal Z-component per face
    val faceNormalZ = DoubleArray(inputTriCount)
    for (i in 0 until inputTriCount) {
        val vi0 = tris[i * 3].toInt() * 3
        val vi1 = tris[i * 3 + 1].toInt() * 3
        val vi2 = tris[i * 3 + 2].toInt() * 3

        val ax = verts[vi1] - verts[vi0]
        val ay = verts[vi1 + 1] - verts[vi0 + 1]
        val az = verts[vi1 + 2] - verts[vi0 + 2]
        val bx = verts[vi2] - verts[vi0]
        val by = verts[vi2 + 1] - verts[vi0 + 1]
        val bz = verts[vi2 + 2] - verts[vi0 + 2]

        val cz = ax * by - ay * bx
        val cx = ay * bz - az * by
        val cy = az * bx - ax * bz
        val len = Math.sqrt(cx * cx + cy * cy + cz * cz)
        faceNormalZ[i] = if (len > 1e-12) cz / len else 0.0
    }

    // Collect 2D silhouette edges
    val silhouetteEdges = ArrayList<Quad2d>()
    for ((edgeKeyVal, faces) in edgeFaces) {
        if (faces.size >= 2) {
            val hasUp = faces.any { faceNormalZ[it] > 1e-4 }
            val hasDown = faces.any { faceNormalZ[it] < -1e-4 }
            if (hasUp && hasDown) {
                val (v0, v1) = edgeVertices(edgeKeyVal)
                val a = Coords2d(verts[v0 * 3], verts[v0 * 3 + 1])
                val b = Coords2d(verts[v1 * 3], verts[v1 * 3 + 1])
                silhouetteEdges.add(Quad2d(a.x, a.y, b.x, b.y))
            }
        }
    }

    if (silhouetteEdges.isEmpty()) {
        return projectViaConvexHull(verts, thickness)
    }

    // Rasterize silhouette and trace contour via marching squares
    val contour = traceContourFromEdges(silhouetteEdges)

    if (contour.size < 3) {
        return projectViaConvexHull(verts, thickness)
    }

    return buildExtrudedMesh(contour, thickness)
}

private data class Quad2d(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

private fun projectViaConvexHull(verts: DoubleArray, thickness: Double): Model {
    val projected = (0 until verts.size step 3).map { i ->
        Coords2d(verts[i].toDouble(), verts[i + 1].toDouble())
    }
    val hull = convexHull2d(projected)
    return buildExtrudedMesh(hull, thickness)
}

private fun traceContourFromEdges(edges: List<Quad2d>): List<Coords2d> {
    var minX = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    for (e in edges) {
        if (e.x0 < minX) minX = e.x0
        if (e.x0 > maxX) maxX = e.x0
        if (e.y0 < minY) minY = e.y0
        if (e.y0 > maxY) maxY = e.y0
        if (e.x1 < minX) minX = e.x1
        if (e.x1 > maxX) maxX = e.x1
        if (e.y1 < minY) minY = e.y1
        if (e.y1 > maxY) maxY = e.y1
    }

    val padding = 1.0
    minX -= padding
    maxX += padding
    minY -= padding
    maxY += padding

    val gridSize = 0.5
    val cols = ((maxX - minX) / gridSize).toInt() + 1
    val rows = ((maxY - minY) / gridSize).toInt() + 1

    if (cols < 3 || rows < 3) return emptyList()

    val inside = BooleanArray(rows * cols)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val px = minX + (c + 0.5) * gridSize
            val py = minY + (r + 0.5) * gridSize
            if (computeWindingNumber(px, py, edges) != 0) {
                inside[r * cols + c] = true
            }
        }
    }

    // Collect boundary cells (inside cells adjacent to outside)
    val boundaryGrid = BooleanArray(rows * cols)
    var boundaryCount = 0

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val idx = r * cols + c
            if (!inside[idx]) continue
            val isBoundary = (c > 0 && !inside[idx - 1]) ||
                (c < cols - 1 && !inside[idx + 1]) ||
                (r > 0 && !inside[idx - cols]) ||
                (r < rows - 1 && !inside[idx + cols])
            if (isBoundary) {
                boundaryGrid[idx] = true
                boundaryCount++
            }
        }
    }

    if (boundaryCount < 3) return emptyList()

    // Collect unique boundary grid points and sort by angle from centroid
    val points = ArrayList<Coords2d>()
    var cx = 0.0
    var cy = 0.0
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            if (boundaryGrid[r * cols + c]) {
                val px = minX + (c + 0.5) * gridSize
                val py = minY + (r + 0.5) * gridSize
                points.add(Coords2d(px, py))
                cx += px
                cy += py
            }
        }
    }

    if (points.size < 3) return emptyList()

    cx /= points.size
    cy /= points.size

    // Sort by angle from centroid, then by distance (for same angle)
    points.sortWith(compareBy<Coords2d> {
            Math.atan2(it.y - cy, it.x - cx)
        }.thenBy {
            Math.hypot(it.x - cx, it.y - cy)
        }
    )

    return simplifyContour(points)
}

private fun computeWindingNumber(px: Double, py: Double, edges: List<Quad2d>): Int {
    var wn = 0
    for (e in edges) {
        val y0 = e.y0
        val y1 = e.y1
        if (y0 <= py) {
            if (y1 > py && isLeft(e.x0, y0, e.x1, y1, px, py) > 0) wn++
        } else {
            if (y1 <= py && isLeft(e.x0, y0, e.x1, y1, px, py) < 0) wn--
        }
    }
    return wn
}

private fun isLeft(x0: Double, y0: Double, x1: Double, y1: Double, px: Double, py: Double): Double {
    return (x1 - x0) * (py - y0) - (y1 - y0) * (px - x0)
}

private fun simplifyContour(points: List<Coords2d>): List<Coords2d> {
    if (points.size <= 3) return points

    // Remove consecutive duplicates
    val deduped = ArrayList<Coords2d>()
    for (i in 0 until points.size) {
        val prev = deduped.lastOrNull()
        if (prev == null || prev.x != points[i].x || prev.y != points[i].y) {
            deduped.add(points[i])
        }
    }

    if (deduped.size <= 4) return deduped

    // Remove collinear points
    val final = ArrayList<Coords2d>()
    final.add(deduped[0])
    for (i in 1 until deduped.size - 1) {
        val a = final[final.size - 1]
        val b = deduped[i]
        val c = deduped[i + 1]
        val cross = (b.x - a.x) * (c.y - b.y) - (b.y - a.y) * (c.x - b.x)
        if (Math.abs(cross) > 0.1) {
            final.add(b)
        }
    }
    final.add(deduped[deduped.size - 1])

    return if (final.size >= 3) final else deduped
}

private fun buildExtrudedMesh(contour: List<Coords2d>, thickness: Double): Model {
    val allVerts = ArrayList<Double>()
    val allTris = ArrayList<Long>()
    val n = contour.size
    val bottomZ = 0.0
    val topZ = thickness

    fun addVert(v: V3d): Long {
        val idx = (allVerts.size / 3).toLong()
        allVerts.add(v.x)
        allVerts.add(v.y)
        allVerts.add(v.z)
        return idx
    }

    val bottomVerts = contour.map { addVert(V3d(it.x, it.y, bottomZ)) }
    val topVerts = contour.map { addVert(V3d(it.x, it.y, topZ)) }

    // Bottom face (reversed winding)
    for (i in n - 1 downTo 1) {
        allTris.add(bottomVerts[0])
        allTris.add(bottomVerts[i])
        allTris.add(bottomVerts[i - 1])
    }

    // Top face (CCW)
    for (i in 1 until n - 1) {
        allTris.add(topVerts[0])
        allTris.add(topVerts[i])
        allTris.add(topVerts[i + 1])
    }

    // Side faces
    for (i in 0 until n) {
        val j = (i + 1) % n
        allTris.add(bottomVerts[i])
        allTris.add(bottomVerts[j])
        allTris.add(topVerts[j])
        allTris.add(bottomVerts[i])
        allTris.add(topVerts[j])
        allTris.add(topVerts[i])
    }

    val vertArray = DoubleArray(allVerts.size)
    for (i in allVerts.indices) vertArray[i] = allVerts[i]

    val triCount = allTris.size / 3
    val triArray = LongArray(allTris.size)
    for (i in allTris.indices) triArray[i] = allTris[i]

    val mb = Manifold3dEngine.bindings()
    val resultMesh = mb.importMeshGL64(
        vertArray, triArray,
        (vertArray.size / 3).toLong(), triCount.toLong()
    )

    if (mb.isEmpty(resultMesh)) {
        Manifold3dEngine.delete(resultMesh)
        return Empty3dModel()
    }

    val polygons = manifoldToPolygons(mb, resultMesh)
    Manifold3dEngine.delete(resultMesh)

    return if (polygons.isEmpty()) Empty3dModel() else StlModel(polygons)
}

private fun manifoldToPolygons(mb: com.cadoodlecad.manifold.ManifoldBindings, manifold: Long): List<Polygon> {
    val data = mb.exportMeshGL64(manifold)
    val verts = data.vertices()
    val tris = data.triangles()
    val triCount = data.triCount().toInt()
    val result = ArrayList<Polygon>()
    val c = com.github.grishberg.javascad.utils.Color.GRAY

    for (i in 0 until triCount) {
        val vi0 = tris[i * 3].toInt() * 3
        val vi1 = tris[i * 3 + 1].toInt() * 3
        val vi2 = tris[i * 3 + 2].toInt() * 3
        result.add(Polygon.fromPolygons(
            V3d(verts[vi0], verts[vi0 + 1], verts[vi0 + 2]),
            V3d(verts[vi1], verts[vi1 + 1], verts[vi1 + 2]),
            V3d(verts[vi2], verts[vi2 + 1], verts[vi2 + 2]),
            c
        ))
    }
    return result
}

private fun convexHull2d(points: List<Coords2d>): List<Coords2d> {
    if (points.size <= 1) return points
    val sorted = points.sortedWith(compareBy<Coords2d> { it.y }.thenBy { it.x })

    fun cross(o: Coords2d, a: Coords2d, b: Coords2d): Double =
        (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)

    val lower = ArrayList<Coords2d>()
    for (p in sorted) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower[lower.size - 1], p) <= 0) {
            lower.removeAt(lower.size - 1)
        }
        lower.add(p)
    }

    val upper = ArrayList<Coords2d>()
    for (p in sorted.asReversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper[upper.size - 1], p) <= 0) {
            upper.removeAt(upper.size - 1)
        }
        upper.add(p)
    }

    lower.removeAt(lower.size - 1)
    upper.removeAt(upper.size - 1)
    return lower + upper
}
