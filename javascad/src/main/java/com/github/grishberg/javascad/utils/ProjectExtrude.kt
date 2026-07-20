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
 * Algorithm:
 * 1. Export mesh vertices and faces
 * 2. Find silhouette edges (edges between face-up and face-down triangles)
 * 3. Project silhouette vertices to 2D (XY plane)
 * 4. Compute 2D convex hull
 * 5. Build a closed extruded mesh directly via manifold native API
 *
 * Usage in scripts:
 *   cube(50, 30, 20).projectAndExtrude(2.0)
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

    // Find silhouette edges and collect projected 2D points
    val projectedPoints = HashSet<String>()
    for ((edgeKeyVal, faces) in edgeFaces) {
        if (faces.size >= 2) {
            val hasUp = faces.any { faceNormalZ[it] > 1e-4 }
            val hasDown = faces.any { faceNormalZ[it] < -1e-4 }
            if (hasUp && hasDown) {
                val (v0, v1) = edgeVertices(edgeKeyVal)
                addProjectedPoint(projectedPoints, verts, v0)
                addProjectedPoint(projectedPoints, verts, v1)
            }
        }
    }

    val pointsList = projectedPoints.map { parseProjectedPoint(it) }.toList()

    // Fallback: convex hull of all vertices
    val hullPoints = if (pointsList.size >= 3) {
        convexHull2d(pointsList)
    } else {
        val allProjected = (0 until verts.size step 3).map { i ->
            Coords2d(verts[i].toDouble(), verts[i + 1].toDouble())
        }
        convexHull2d(allProjected)
    }

    if (hullPoints.size < 3) return Empty3dModel()

    // Build extruded mesh directly via manifold
    // Hull points are CCW (convexHull2d returns CCW).
    // Bottom face (Z=0): CCW -> inward normal = down (correct)
    // Top face (Z=thickness): CW -> inward normal = up (correct)
    // Side faces: bottom CCW -> top CW, outward normals point sideways (correct)

    val allVerts = ArrayList<Double>()
    val allTris = ArrayList<Long>()
    val bottomZ = 0.0
    val topZ = thickness
    val n = hullPoints.size

    fun addVert(v: V3d): Long {
        val idx = (allVerts.size / 3).toLong()
        allVerts.add(v.x)
        allVerts.add(v.y)
        allVerts.add(v.z)
        return idx
    }

    // Bottom vertices (same order as hull, CCW)
    val bottomVerts = hullPoints.map { addVert(V3d(it.x, it.y, bottomZ)) }
    val topVerts = hullPoints.map { addVert(V3d(it.x, it.y, topZ)) }

    // Bottom face: reversed winding for CCW when viewed from below (outside)
    for (i in n - 1 downTo 1) {
        allTris.add(bottomVerts[0])
        allTris.add(bottomVerts[i])
        allTris.add(bottomVerts[i - 1])
    }

    // Top face: CCW from above (outside)
    for (i in 1 until n - 1) {
        allTris.add(topVerts[0])
        allTris.add(topVerts[i])
        allTris.add(topVerts[i + 1])
    }

    // Side faces: bottom[i] -> bottom[(i+1)%n] -> top[(i+1)%n] -> top[i]
    // Triangle 1: bottomVerts[i], bottomVerts[(i+1)%n], topVerts[(i+1)%n]
    // Triangle 2: bottomVerts[i], topVerts[(i+1)%n], topVerts[i]
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

    // Import via manifold
    val resultMesh = mb.importMeshGL64(
        vertArray,
        triArray,
        (vertArray.size / 3).toLong(),
        triCount.toLong()
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

private fun addProjectedPoint(set: MutableSet<String>, verts: DoubleArray, vIdx: Int) {
    val i = vIdx * 3
    val x = (verts[i] * 1000000.0).toLong()
    val y = (verts[i + 1] * 1000000.0).toLong()
    set.add("$x,$y")
}

private fun parseProjectedPoint(s: String): Coords2d {
    val parts = s.split(",")
    return Coords2d(parts[0].toLong() / 1000000.0, parts[1].toLong() / 1000000.0)
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
