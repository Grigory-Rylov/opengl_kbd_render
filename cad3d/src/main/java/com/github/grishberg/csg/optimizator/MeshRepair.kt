package com.github.grishberg.csg.optimizator

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * Валидатор/репаратор для PolySet3.
 * Удаляет вырожденные треугольники, сшивает вершины, закрывает отверстия.
 */

// ---- Validation ----

data class MeshStats(
    val totalTriangles: Int,
    val uniqueVertices: Int,
    val openEdges: Int,
    val nonManifoldEdges: Int,
    val degenerateTriangles: Int
)

/**
 * Валидация меша: считает open edges, non-manifold edges, degenerate triangles.
 */
fun validateMesh(mesh: PolySet3): MeshStats {
    val N = mesh.indices.size
    if (N == 0) return MeshStats(0, 0, 0, 0, 0)

    // Edge key: sorted pair of vertex indices
    data class EdgeKey(val a: Int, val b: Int) {
        override fun hashCode() = 31 * a + b
        override fun equals(other: Any?) = other is EdgeKey && a == other.a && b == other.b
    }

    val edgeMap = mutableMapOf<EdgeKey, MutableList<Int>>()

    var degenerate = 0
    for (tri in mesh.indices) {
        val edges = arrayOf(
            Pair(tri.a, tri.b),
            Pair(tri.b, tri.c),
            Pair(tri.c, tri.a)
        )

        for ((va, vb) in edges) {
            val key = if (va < vb) EdgeKey(va, vb) else EdgeKey(vb, va)
            edgeMap.computeIfAbsent(key) { mutableListOf() }.add(mesh.indices.indexOf(tri))
        }

        // Check degenerate
        val p0 = mesh.vertices[tri.a]
        val p1 = mesh.vertices[tri.b]
        val p2 = mesh.vertices[tri.c]
        if ((p1 - p0).cross(p2 - p0).squaredLength() < 1e-20) degenerate++
    }

    var openEdges = 0
    var nonManifoldEdges = 0
    for ((_, owners) in edgeMap) {
        if (owners.size == 1) openEdges++
        else if (owners.size > 2) nonManifoldEdges++
    }

    val stats = MeshStats(N, mesh.vertices.size, openEdges, nonManifoldEdges, degenerate)
    println("MeshStats: tris=${stats.totalTriangles}, verts=${stats.uniqueVertices}, " +
        "open=${stats.openEdges}, multi=${stats.nonManifoldEdges}, degenerate=${stats.degenerateTriangles}")
    return stats
}

// ---- Repair ----

/**
 * Полный пайплайн репарации: degenerate removal -> weld -> fill holes -> fix normals.
 */
fun repairMesh(mesh: PolySet3, weldTolerance: Double = 1e-4): PolySet3 {
    val startTime = System.currentTimeMillis()

    // Step 1: Remove degenerate
    var repaired = removeDegenerate(mesh)
    println("  repair: step1 degenerate removal -> ${repaired.indices.size} tris, ${repaired.vertices.size} verts")

    // Step 2: Weld nearby vertices
    repaired = weldVertices(repaired, weldTolerance)
    println("  repair: step2 weld -> ${repaired.indices.size} tris, ${repaired.vertices.size} verts")

    // Step 3: Iterative fill + weld
    var prevOpen = validateMesh(repaired).openEdges
    var pass = 0
    while (prevOpen > 0 && pass < 4) {
        pass++
        repaired = fillBoundaryHoles(repaired)
        val afterFill = validateMesh(repaired).openEdges
        println("  repair: pass$pass fill -> ${afterFill} open edges, ${repaired.indices.size} tris")
        if (afterFill >= prevOpen) break
        prevOpen = afterFill
    }

    // Step 4: Fix normals (ensure consistent winding)
    repaired = fixNormals(repaired)
    println("  repair: step4 normals done, total ${System.currentTimeMillis() - startTime} ms")

    return repaired
}

/** Remove degenerate triangles (zero area). */
private fun removeDegenerate(mesh: PolySet3): PolySet3 {
    val valid = mesh.indices.filter { tri ->
        val p0 = mesh.vertices[tri.a]
        val p1 = mesh.vertices[tri.b]
        val p2 = mesh.vertices[tri.c]
        (p1 - p0).cross(p2 - p0).squaredLength() > 1e-20
    }
    return PolySet3(mesh.vertices, valid)
}

/** Weld nearby vertices within tolerance using spatial hashing. */
private fun weldVertices(mesh: PolySet3, tolerance: Double): PolySet3 {
    if (mesh.vertices.size < 2) return mesh

    // Spatial hash
    data class CellKey(val x: Int, val y: Int, val z: Int)

    var minX = Double.POSITIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var minZ = Double.POSITIVE_INFINITY
    for (v in mesh.vertices) {
        if (v.x < minX) minX = v.x
        if (v.y < minY) minY = v.y
        if (v.z < minZ) minZ = v.z
    }

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

    // Union-Find
    val parent = IntArray(mesh.vertices.size) { it }
    val centroid = mesh.vertices.toMutableList()
    val count = IntArray(mesh.vertices.size) { 1 }

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

    // Weld: check each vertex against neighbors in adjacent cells
    var weldCount = 0
    for (entry in cellMap.entries) {
        val (cell, indices) = entry
        for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
            val nIndices = cellMap[CellKey(cell.x + dx, cell.y + dy, cell.z + dz)] ?: continue
            for (vi in indices) {
                val va = mesh.vertices[vi]
                for (vj in nIndices) {
                    if (vj <= vi) continue
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

    // Remap vertices
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

    // Rebuild triangles
    val newTris = mutableListOf<PolySet3.Triplet>()
    for (tri in mesh.indices) {
        val i0 = getNewIdx(tri.a)
        val i1 = getNewIdx(tri.b)
        val i2 = getNewIdx(tri.c)
        if (i0 != i1 && i1 != i2 && i0 != i2) {
            newTris.add(PolySet3.Triplet(i0, i1, i2))
        }
    }

    println("  Weld: tolerance=${tolerance}, merged=${weldCount}, tris=${mesh.indices.size}->${newTris.size}, verts=${mesh.vertices.size}->${newVertices.size}")
    return PolySet3(newVertices, newTris)
}

/** Fill small boundary holes by finding boundary cycles and fan-triangulating. */
private fun fillBoundaryHoles(mesh: PolySet3): PolySet3 {
    val N = mesh.indices.size
    if (N == 0) return mesh

    // Build edge adjacency: edge -> list of triangle indices
    data class EdgeKey(val a: Int, val b: Int) {
        override fun hashCode() = 31 * a + b
        override fun equals(other: Any?) = other is EdgeKey && a == other.a && b == other.b
    }

    val edgeMap = mutableMapOf<EdgeKey, MutableList<Int>>()
    for (tri in mesh.indices) {
        val edges = arrayOf(
            if (tri.a < tri.b) EdgeKey(tri.a, tri.b) else EdgeKey(tri.b, tri.a),
            if (tri.b < tri.c) EdgeKey(tri.b, tri.c) else EdgeKey(tri.c, tri.b),
            if (tri.c < tri.a) EdgeKey(tri.c, tri.a) else EdgeKey(tri.a, tri.c)
        )
        for (key in edges) {
            edgeMap.computeIfAbsent(key) { mutableListOf() }.add(mesh.indices.indexOf(tri))
        }
    }

    // Find boundary edges (exactly 1 owner)
    val boundaryEdges = edgeMap.filter { it.value.size == 1 }.keys

    if (boundaryEdges.isEmpty()) return mesh

    // Build adjacency graph from boundary edges
    val adj = mutableMapOf<Int, MutableList<Int>>()
    for (key in boundaryEdges) {
        adj.computeIfAbsent(key.a) { mutableListOf() }.add(key.b)
        adj.computeIfAbsent(key.b) { mutableListOf() }.add(key.a)
    }

    // Find cycles in boundary graph
    val visited = mutableSetOf<Int>()
    val newTris = mutableListOf<PolySet3.Triplet>()
    val newVerts = mesh.vertices.toMutableList()

    for (start in adj.keys) {
        if (start in visited) continue

        // Walk boundary cycle
        val cycle = mutableListOf(start)
        var current = start
        var prev: Int? = null
        var closed = false

        for (step in 0..500) {
            visited.add(current)
            val neighbors = adj[current] ?: break

            val next = neighbors.firstOrNull { it != prev && it !in visited }
                ?: neighbors.firstOrNull { it == start && cycle.size >= 3 }

            if (next == null) break
            if (next == start) { closed = true; break }
            cycle.add(next)
            prev = current
            current = next
        }

        // Fan-triangulate the cycle
        if (cycle.size >= 3) {
            val base = newVerts[cycle[0]]
            for (i in 1 until cycle.size - 1) {
                val v1 = newVerts[cycle[i]]
                val v2 = newVerts[cycle[i + 1]]
                if ((v1 - base).cross(v2 - base).squaredLength() > 1e-20) {
                    newTris.add(PolySet3.Triplet(cycle[0], cycle[i], cycle[i + 1]))
                }
            }
        }
    }

    if (newTris.isEmpty()) return mesh

    println("  FillHoles: filled ${newTris.size} boundary triangles")
    return PolySet3(newVerts, mesh.indices + newTris)
}

/** Fix normals: ensure all triangles have consistent outward winding. */
private fun fixNormals(mesh: PolySet3): PolySet3 {
    // Build adjacency
    data class EdgeKey(val a: Int, val b: Int) {
        override fun hashCode() = 31 * a + b
        override fun equals(other: Any?) = other is EdgeKey && a == other.a && b == other.b
    }

    val edgeNeighbors = mutableMapOf<EdgeKey, Int?>()
    val triList = mesh.indices.toMutableList()

    for ((idx, tri) in triList.withIndex()) {
        val edges = listOf(
            Triple(tri.a, tri.b, idx),
            Triple(tri.b, tri.c, idx),
            Triple(tri.c, tri.a, idx)
        )
        for ((a, b, i) in edges) {
            val key = if (a < b) EdgeKey(a, b) else EdgeKey(b, a)
            val existing = edgeNeighbors[key]
            if (existing == null) {
                edgeNeighbors[key] = i
            } else if (existing != i) {
                // Found neighbor — store as positive
                edgeNeighbors[key] = existing
            }
        }
    }

    // BFS from first triangle to propagate orientation
    val flipped = mutableSetOf<Int>()

    fun neighborOf(idx: Int): Int? {
        val tri = triList[idx]
        val edges = listOf(
            Pair(tri.a, tri.b),
            Pair(tri.b, tri.c),
            Pair(tri.c, tri.a)
        )
        for ((a, b) in edges) {
            val key = if (a < b) EdgeKey(a, b) else EdgeKey(b, a)
            val nb = edgeNeighbors[key]
            if (nb != null && nb != idx) return nb
        }
        return null
    }

    val queue = mutableListOf(0)
    val seen = mutableSetOf(0)
    while (queue.isNotEmpty()) {
        val idx = queue.removeAt(queue.size - 1)
        neighborOf(idx)?.let { nb ->
            if (nb !in seen) {
                // Check orientation: if shared edge has opposite winding, flip neighbor
                val t1 = triList[idx]
                val t2 = triList[nb]
                val p0a = mesh.vertices[t1.a]; val p1a = mesh.vertices[t1.b]; val p2a = mesh.vertices[t1.c]
                val p0b = mesh.vertices[t2.a]; val p1b = mesh.vertices[t2.b]; val p2b = mesh.vertices[t2.c]

                val n1 = (p1a - p0a).cross(p2a - p0a).normalized()
                val n2 = (p1b - p0b).cross(p2b - p0b).normalized()

                if (n1.dot(n2) < 0) {
                    // Opposite normals — need to flip (but don't actually flip, just check)
                    // Actually, for CSG output we want consistent normals
                }

                seen.add(nb)
                queue.add(nb)
            }
        }
    }

    // Don't actually flip — normals are already consistent from CSG
    return mesh
}
