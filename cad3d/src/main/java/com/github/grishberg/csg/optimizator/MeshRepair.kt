package com.github.grishberg.csg.optimizator

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * Валидатор/репаратор для PolySet3.
 */

data class MeshStats(
    val totalTriangles: Int,
    val uniqueVertices: Int,
    val openEdges: Int,
    val nonManifoldEdges: Int,
    val degenerateTriangles: Int
)

/** Валидация меша. */
fun validateMesh(mesh: PolySet3): MeshStats {
    val N = mesh.indices.size
    if (N == 0) return MeshStats(0, 0, 0, 0, 0)

    data class EKey(val a: Int, val b: Int) {
        override fun hashCode() = 31 * a + b
        override fun equals(other: Any?) = other is EKey && a == other.a && b == other.b
    }

    val edgeCount = mutableMapOf<EKey, Int>()
    var degenerate = 0

    for (tri in mesh.indices) {
        val p0 = mesh.vertices[tri.a]
        val p1 = mesh.vertices[tri.b]
        val p2 = mesh.vertices[tri.c]
        if ((p1 - p0).cross(p2 - p0).squaredLength() < 1e-20) degenerate++

        val edges = listOf(
            if (tri.a < tri.b) EKey(tri.a, tri.b) else EKey(tri.b, tri.a),
            if (tri.b < tri.c) EKey(tri.b, tri.c) else EKey(tri.c, tri.b),
            if (tri.c < tri.a) EKey(tri.c, tri.a) else EKey(tri.a, tri.c)
        )
        for (key in edges) {
            edgeCount[key] = edgeCount.getOrDefault(key, 0) + 1
        }
    }

    var openEdges = 0
    var nonManifold = 0
    for (count in edgeCount.values) {
        if (count == 1) openEdges++
        else if (count > 2) nonManifold++
    }

    val stats = MeshStats(N, mesh.vertices.size, openEdges, nonManifold, degenerate)
    println("MeshStats: tris=${stats.totalTriangles}, verts=${stats.uniqueVertices}, " +
        "open=${stats.openEdges}, multi=${stats.nonManifoldEdges}, degenerate=${stats.degenerateTriangles}")
    return stats
}

// ---- Repair pipeline ----

fun repairMesh(mesh: PolySet3): PolySet3 {
    val startTime = System.currentTimeMillis()

    var repaired = removeDegenerate(mesh)
    println("  repair: degenerate removal -> ${repaired.indices.size} tris")

    // Iterative: weld with increasing tolerance + fill holes
    var prevOpen = validateMesh(repaired).openEdges
    var tolerance = 1e-6
    for (pass in 1..6) {
        if (prevOpen == 0) break

        // Weld with current tolerance
        repaired = weldVertices(repaired, tolerance)
        val afterWeld = validateMesh(repaired).openEdges

        if (afterWeld < prevOpen) {
            prevOpen = afterWeld
            tolerance = minOf(tolerance * 2.5, 0.5)
            continue
        }

        // Fill holes
        repaired = fillBoundaryHoles(repaired)
        val afterFill = validateMesh(repaired).openEdges
        println("  repair: pass$pass fill -> ${afterFill} open, ${repaired.indices.size} tris")

        if (afterFill >= prevOpen) break
        prevOpen = afterFill
    }

    println("  repair: done in ${System.currentTimeMillis() - startTime} ms")
    return repaired
}

private fun removeDegenerate(mesh: PolySet3): PolySet3 {
    val valid = mesh.indices.filter { tri ->
        val p0 = mesh.vertices[tri.a]
        val p1 = mesh.vertices[tri.b]
        val p2 = mesh.vertices[tri.c]
        (p1 - p0).cross(p2 - p0).squaredLength() > 1e-20
    }
    return PolySet3(mesh.vertices, valid)
}

/** Weld nearby vertices within tolerance using spatial hashing + union-find. */
private fun weldVertices(mesh: PolySet3, tolerance: Double): PolySet3 {
    val V = mesh.vertices.size
    if (V < 2) return mesh

    if (tolerance <= 0) return mesh

    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY
    for (v in mesh.vertices) {
        if (v.x < minX) minX = v.x
        if (v.y < minY) minY = v.y
        if (v.z < minZ) minZ = v.z
    }

    data class CellKey(val x: Int, val y: Int, val z: Int)

    fun gridCell(v: Vec3): CellKey {
        return CellKey(
            ((v.x - minX) / tolerance).toInt(),
            ((v.y - minY) / tolerance).toInt(),
            ((v.z - minZ) / tolerance).toInt()
        )
    }

    val cellMap = mutableMapOf<CellKey, MutableList<Int>>()
    for ((i, v) in mesh.vertices.withIndex()) {
        val cell = gridCell(v)
        cellMap.computeIfAbsent(cell) { mutableListOf() }.add(i)
    }

    val parent = IntArray(V) { it }
    val centroid = mesh.vertices.toMutableList()
    val count = IntArray(V) { 1 }

    fun find(i: Int): Int {
        var r = i
        while (parent[r] != r) { parent[r] = parent[parent[r]]; r = parent[r] }
        return r
    }

    fun union(i: Int, j: Int) {
        var ri = find(i); var rj = find(j)
        if (ri == rj) return
        if (count[ri] < count[rj]) {
            parent[ri] = rj
            centroid[rj] = (centroid[ri] + centroid[rj]) / 2.0
            count[rj] += count[ri]
        } else {
            parent[rj] = ri
            centroid[ri] = (centroid[ri] + centroid[rj]) / 2.0
            count[ri] += count[rj]
        }
    }

    var weldCount = 0
    val cellKeys = cellMap.keys.toTypedArray()
    for (cell in cellKeys) {
        val indices = cellMap[cell] ?: continue
        for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
            if (dx == 0 && dy == 0 && dz == 0) continue
            val nIndices = cellMap[CellKey(cell.x + dx, cell.y + dy, cell.z + dz)] ?: continue
            for (vi in indices) {
                val va = mesh.vertices[vi]
                for (vj in nIndices) {
                    val vb = mesh.vertices[vj]
                    val d = (va - vb).length()
                    if (d > 0 && d <= tolerance) {
                        if (find(vi) != find(vj)) {
                            union(vi, vj)
                            weldCount++
                        }
                    }
                }
            }
        }
    }

    // Remap
    val rootToIdx = mutableMapOf<Int, Int>()
    val newVertices = mutableListOf<Vec3>()
    fun getNewIdx(i: Int): Int {
        val r = find(i)
        if (r !in rootToIdx) {
            rootToIdx[r] = newVertices.size
            newVertices.add(centroid[r])
        }
        return rootToIdx[r]!!
    }

    val newTris = mutableListOf<PolySet3.Triplet>()
    for (tri in mesh.indices) {
        val i0 = getNewIdx(tri.a)
        val i1 = getNewIdx(tri.b)
        val i2 = getNewIdx(tri.c)
        if (i0 != i1 && i1 != i2 && i0 != i2) {
            newTris.add(PolySet3.Triplet(i0, i1, i2))
        }
    }

    if (weldCount > 0) {
        println("  Weld: tol=${tolerance}, merged=${weldCount}, tris=${mesh.indices.size}->${newTris.size}, verts=${mesh.vertices.size}->${newVertices.size}")
    }
    return PolySet3(newVertices, newTris)
}

/** Fill boundary holes using coordinate-based edge matching. */
private fun fillBoundaryHoles(mesh: PolySet3): PolySet3 {
    val N = mesh.indices.size
    if (N == 0) return mesh

    // Use vertex INDEX-based edges (after weld, shared vertices share the same index)
    data class EKey(val a: Int, val b: Int) {
        override fun hashCode() = 31 * a + b
        override fun equals(other: Any?) = other is EKey && a == other.a && b == other.b
    }

    val edgeCount = mutableMapOf<EKey, Int>()
    for (tri in mesh.indices) {
        val edges = listOf(
            if (tri.a < tri.b) EKey(tri.a, tri.b) else EKey(tri.b, tri.a),
            if (tri.b < tri.c) EKey(tri.b, tri.c) else EKey(tri.c, tri.b),
            if (tri.c < tri.a) EKey(tri.c, tri.a) else EKey(tri.a, tri.c)
        )
        for (key in edges) {
            edgeCount[key] = edgeCount.getOrDefault(key, 0) + 1
        }
    }

    // Find boundary edges (exactly 1 owner)
    val boundaryEdges = edgeCount.filter { it.value == 1 }.keys
    if (boundaryEdges.isEmpty()) return mesh

    // Build adjacency: vertex index -> list of neighbor vertex indices
    val adj = mutableMapOf<Int, MutableList<Int>>()
    for (key in boundaryEdges) {
        adj.computeIfAbsent(key.a) { mutableListOf() }.add(key.b)
        adj.computeIfAbsent(key.b) { mutableListOf() }.add(key.a)
    }

    // Walk boundary cycles and fan-triangulate
    val visited = mutableSetOf<Int>()
    val newTris = mutableListOf<PolySet3.Triplet>()

    for (start in adj.keys) {
        if (start in visited) continue

        val cycle = mutableListOf(start)
        var current = start
        var prev: Int? = null

        for (step in 0..1000) {
            visited.add(current)
            val neighbors = adj[current] ?: break

            val next = neighbors.firstOrNull { it != prev && it !in visited }
                ?: neighbors.firstOrNull { it == start && cycle.size >= 3 }

            if (next == null) break
            if (next == start) break
            cycle.add(next)
            prev = current
            current = next
        }

        // Fan-triangulate
        if (cycle.size >= 3) {
            val idx0 = cycle[0]
            for (i in 1 until cycle.size - 1) {
                val v0 = mesh.vertices[idx0]
                val v1 = mesh.vertices[cycle[i]]
                val v2 = mesh.vertices[cycle[i + 1]]
                if ((v1 - v0).cross(v2 - v0).squaredLength() > 1e-20) {
                    newTris.add(PolySet3.Triplet(idx0, cycle[i], cycle[i + 1]))
                }
            }
        }
    }

    if (newTris.isNotEmpty()) {
        println("  FillHoles: +${newTris.size} triangles, ${(newTris.size + N)} total")
    }
    return PolySet3(mesh.vertices, mesh.indices + newTris)
}
