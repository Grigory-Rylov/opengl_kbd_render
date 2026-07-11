package com.github.grishberg.javascad

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import java.util.LinkedList
import java.util.Queue
import kotlin.math.max
import kotlin.math.pow

/**
 * Исправитель STL мешей на основе admesh pipeline из OrcaSlicer.
 */
class StlRepairer {

    data class RepairResult(
        val originalTriangles: Int,
        val repairedTriangles: Int,
        val edgesFixed: Int,
        val degenerateRemoved: Int,
        val disconnectedRemoved: Int,
        val normalsFlipped: Int,
        val holesFilled: Int = 0
    ) {
        override fun toString(): String {
            return """StlRepairer.RepairResult(
                |  originalTriangles=$originalTriangles -> repairedTriangles=$repairedTriangles,
                |  edgesFixed=$edgesFixed,
                |  degenerateRemoved=$degenerateRemoved, 
                |  disconnectedRemoved=$disconnectedRemoved,
                |  normalsFlipped=$normalsFlipped,
                |  holesFilled=$holesFilled
            """.trimMargin()
        }
    }

    data class RepairOptions(
        val snapTolerance: Double = 1e-4,
        val maxSnapIterations: Int = 2,
        val removeDisconnectedFacets: Boolean = true,
        val fixNormals: Boolean = true,
        val fillHoles: Boolean = true
    )

    fun repair(polygons: List<Polygon>, options: RepairOptions = RepairOptions()): Pair<List<Polygon>, RepairResult> {
        val startTime = System.currentTimeMillis()
        
        var triangles = polygons.mapNotNull { it.toTriangle() }
        if (triangles.isEmpty()) return Pair(emptyList(), RepairResult(0, 0, 0, 0, 0, 0))

        val originalCount = triangles.size
        
        // Шаг 1: Edge snapping  
        var edgesFixed = 0
        for (iteration in 0 until options.maxSnapIterations) {
            val tolerance = options.snapTolerance * 10.0.pow(iteration.toDouble())
            val snapped = snapEdges(triangles, tolerance)
            if (snapped.edgesFixed == 0) break
            triangles = snapped.triangles
            edgesFixed += snapped.edgesFixed
            
            println("StlRepairer: Edge snapping iteration $iteration fixed ${snapped.edgesFixed} edges")
        }

        // Шаг 2: Remove degenerate triangles  
        val beforeDegenerate = triangles.size
        triangles = triangles.filter { !isDegenerate(it) }
        var degenerateRemoved = beforeDegenerate - triangles.size
        
        println("StlRepairer: Removed $degenerateRemoved degenerate triangles")

        // Шаг 3: Remove disconnected facets  
        var disconnectedRemoved = 0
        if (options.removeDisconnectedFacets && triangles.isNotEmpty()) {
            val connected = removeDisconnectedFacets(triangles)
            disconnectedRemoved = triangles.size - connected.triangles.size
            triangles = connected.triangles
            
            println("StlRepairer: Removed $disconnectedRemoved disconnected facets")
        }

        // Шаг 4: Fill holes (boundary triangulation)
        var holesFilled = 0
        if (options.fillHoles && triangles.isNotEmpty()) {
            val (filledTris, holesFilledCount) = fillHoles(triangles)
            holesFilled = holesFilledCount
            triangles = filledTris
            println("StlRepairer: Filled $holesFilled holes")
        }

        // Шаг 4b: Remove degenerate triangles created by hole filling
        val beforeDegenerate2 = triangles.size
        triangles = triangles.filter { !isDegenerate(it) }
        val degenerateRemoved2 = beforeDegenerate2 - triangles.size
        if (degenerateRemoved2 > 0) {
            degenerateRemoved += degenerateRemoved2
            println("StlRepairer: Removed $degenerateRemoved2 degenerate triangles from fill")
        }

        // Шаг 5-6: Fix normals  
        var normalsFlipped = 0
        if (options.fixNormals && triangles.isNotEmpty()) {
            val fixed = fixNormalDirections(triangles)
            triangles = fixed.triangles
            normalsFlipped = fixed.normalsFlipped
            
            println("StlRepairer: Fixed $normalsFlipped normal directions")
        }

        // Преобразуем обратно в полигоны  
        val resultPolygons = triangles.map { 
            Polygon.fromPolygons(listOf(it.v0, it.v1, it.v2), Color.GRAY) 
        }

        println("StlRepairer: Repair completed in ${System.currentTimeMillis() - startTime}ms")
        
        return Pair(resultPolygons, RepairResult(
            originalTriangles = originalCount,
            repairedTriangles = resultPolygons.size,
            edgesFixed = edgesFixed,
            degenerateRemoved = degenerateRemoved,
            disconnectedRemoved = disconnectedRemoved,
            normalsFlipped = normalsFlipped,
            holesFilled = holesFilled
        ))
    }

    // ==================== Внутренние типы ====================
    
    data class Triangle(val v0: V3d, val v1: V3d, val v2: V3d) {
        fun getEdgeVertices(edgeIdx: Int): Pair<V3d, V3d> {
            val vertices = listOf(v0, v1, v2)
            return Pair(vertices[edgeIdx], vertices[(edgeIdx + 1) % 3])
        }

        fun computeNormal(): V3d {
            val cross = v1.subtract(v0).cross(v2.subtract(v0))
            val mag = cross.magnitude()
            if (mag < 1e-10) return cross
            return cross.scale(1.0 / mag)
        }
    }

    private data class SnapResult(val triangles: List<Triangle>, val edgesFixed: Int)
    
    private data class NormalsFixResult(val triangles: List<Triangle>, val normalsFlipped: Int)

    /** Ключ для хеширования вершин с учетом допуску */  
    private data class V3dKey(val x: Long, val y: Long, val z: Long) {
        constructor(v: V3d, tolerance: Double = 1e-4) : this(
            Math.round(v.x / max(tolerance, 1e-10)),
            Math.round(v.y / max(tolerance, 1e-10)), 
            Math.round(v.z / max(tolerance, 1e-10))
        )
    }

    private fun Polygon.toTriangle(): Triangle? {
        val vertices = getVertices()
        return if (vertices.size == 3) Triangle(vertices[0], vertices[1], vertices[2]) else null
    }

    // ==================== Шаг 1: Edge Snapping ====================
    
    private fun snapEdges(triangles: List<Triangle>, tolerance: Double): SnapResult {
        if (triangles.isEmpty()) return SnapResult(emptyList(), 0)

        val vertexGroups = mutableMapOf<V3dKey, MutableList<V3d>>()
        
        for (tri in triangles) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                val key = V3dKey(v, tolerance)
                if (!vertexGroups.containsKey(key)) {
                    vertexGroups[key] = mutableListOf(v)
                } else {
                    val groupCenter = computeGroupCenter(vertexGroups[key]!!)
                    if (distanceSquared(groupCenter, v) < max(tolerance * tolerance, 1e-20)) {
                        vertexGroups[key]?.add(v)
                    } else {
                        val newKey = V3dKey(
                            Math.round(v.x / max(tolerance, 1e-10)),
                            Math.round(v.y / max(tolerance, 1e-10)), 
                            Math.round(v.z / max(tolerance, 1e-10))
                        )
                        vertexGroups[newKey] = mutableListOf(v)
                    }
                }
            }
        }

        val snappedVertices = mutableMapOf<V3d, V3d>()
        var edgesFixedCount = 0
        
        for ((_, group) in vertexGroups) {
            if (group.size <= 1) continue
            
            val center = computeGroupCenter(group)
            if (group.all { distanceSquared(it, center) < 1e-20 }) continue
            
            edgesFixedCount += group.size - 1
            for (v in group) snappedVertices[v] = center
        }

        if (edgesFixedCount == 0) return SnapResult(triangles, 0)

        val newTriangles = triangles.map { tri ->
            Triangle(
                snappedVertices[tri.v0] ?: tri.v0,
                snappedVertices[tri.v1] ?: tri.v1, 
                snappedVertices[tri.v2] ?: tri.v2
            )
        }

        return SnapResult(newTriangles, edgesFixedCount)
    }

    // ==================== Шаг 2: Remove Degenerate Triangles ====================
    
    private fun isDegenerate(tri: Triangle): Boolean {
        val ab = tri.v1.subtract(tri.v0)
        val ac = tri.v2.subtract(tri.v0)
        return ab.cross(ac).magnitude() < 1e-10
    }

    // ==================== Шаг 3: Remove Disconnected Facets ====================
    
    private data class CanonicalEdgeKey(val a: V3dKey, val b: V3dKey)

    private fun canonicalEdgeKey(v0: V3d, v1: V3d, tolerance: Double): CanonicalEdgeKey {
        val k0 = V3dKey(v0, tolerance)
        val k1 = V3dKey(v1, tolerance)
        return if (k0.x < k1.x || (k0.x == k1.x && (k0.y < k1.y || (k0.y == k1.y && k0.z <= k1.z))))
            CanonicalEdgeKey(k0, k1) else CanonicalEdgeKey(k1, k0)
    }

    private fun buildCanonicalEdgeIndex(triangles: List<Triangle>, tolerance: Double): Map<CanonicalEdgeKey, MutableList<Int>> {
        val index = mutableMapOf<CanonicalEdgeKey, MutableList<Int>>()
        for ((faceIdx, tri) in triangles.withIndex()) {
            for (edgeIdx in 0 until 3) {
                val e = tri.getEdgeVertices(edgeIdx)
                val key = canonicalEdgeKey(e.first, e.second, tolerance)
                index.getOrPut(key) { mutableListOf() }.add(faceIdx)
            }
        }
        return index
    }

    private fun removeDisconnectedFacets(triangles: List<Triangle>): SnapResult {
        if (triangles.isEmpty()) return SnapResult(emptyList(), 0)

        val edgeIndex = buildCanonicalEdgeIndex(triangles, 1e-4)

        val newTriangles = triangles.filterIndexed { faceIdx, _ ->
            (0 until 3).any { edgeIdx ->
                val e = triangles[faceIdx].getEdgeVertices(edgeIdx)
                val key = canonicalEdgeKey(e.first, e.second, 1e-4)
                val faces = edgeIndex[key]
                faces != null && faces.any { it != faceIdx }
            }
        }

        val disconnectedCount = triangles.size - newTriangles.size
        return SnapResult(newTriangles, disconnectedCount)
    }

    // ==================== Шаг 4-5: Fix Normal Directions ====================
    
    private fun fixNormalDirections(triangles: List<Triangle>): NormalsFixResult {
        if (triangles.isEmpty()) return NormalsFixResult(emptyList(), 0)

        val trianglesMutable = triangles.toMutableList()
        var normalsFlippedCount = 0

        val edgeIndex = buildCanonicalEdgeIndex(trianglesMutable, 1e-4)

        // 4. Flood-fill to make normals consistent between neighbors
        val visited = BooleanArray(trianglesMutable.size)
        
        for (startFace in 0 until trianglesMutable.size) {
            if (visited[startFace]) continue
            
            val queue: Queue<Int> = LinkedList()
            queue.add(startFace)
            visited[startFace] = true

            while (!queue.isEmpty()) {
                val faceIdx = queue.poll()!!
                
                for (edgeIdx in 0 until 3) {
                    val tri = trianglesMutable[faceIdx]
                    val e = tri.getEdgeVertices(edgeIdx)
                    val key = canonicalEdgeKey(e.first, e.second, 1e-4)
                    val faces = edgeIndex[key]
                    val neighborIdx = faces?.firstOrNull { it != faceIdx } ?: -1

                    if (neighborIdx >= 0 && !visited[neighborIdx]) {
                        visited[neighborIdx] = true
                        
                        val tri1 = trianglesMutable[faceIdx]
                        var tri2 = trianglesMutable[neighborIdx]
                        
                        val n1 = tri1.computeNormal()
                        val n2 = tri2.computeNormal()

                        if (n1.magnitude() > 1e-10 && n2.magnitude() > 1e-10) {
                            if (n1.dot(n2) < -0.5) {
                                trianglesMutable[neighborIdx] = Triangle(tri2.v2, tri2.v1, tri2.v0)
                                normalsFlippedCount++
                            }
                        }

                        queue.add(neighborIdx)
                    }
                }
            }
        }

        // 5. Use signed volume to determine overall orientation and flip if needed
        val signedVolume = computeSignedVolume(trianglesMutable)
        if (signedVolume < 0.0) {
            for (i in trianglesMutable.indices) {
                val tri = trianglesMutable[i]
                trianglesMutable[i] = Triangle(tri.v2, tri.v1, tri.v0)
            }
            normalsFlippedCount += trianglesMutable.size
        }

        return NormalsFixResult(trianglesMutable.toList(), normalsFlippedCount)
    }
    
    private fun computeSignedVolume(triangles: List<Triangle>): Double {
        var volume = 0.0
        for (tri in triangles) {
            val v0 = tri.v0
            val v1 = tri.v1
            val v2 = tri.v2
            // Signed volume of tetrahedron (v0, v1, v2, origin)
            volume += v0.x * (v1.y * v2.z - v2.y * v1.z)
            volume -= v0.y * (v1.x * v2.z - v2.x * v1.z)
            volume += v0.z * (v1.x * v2.y - v2.x * v1.y)
        }
        return volume / 6.0
    }



    // ==================== Шаг 4: Fill Holes (Boundary Triangulation) ====================

    private fun fillHoles(triangles: List<Triangle>): Pair<List<Triangle>, Int> {
        if (triangles.isEmpty()) return Pair(emptyList(), 0)

        val edgeFaces = mutableMapOf<CanonicalEdgeKey, MutableList<Int>>()
        for ((faceIdx, tri) in triangles.withIndex()) {
            for (edgeIdx in 0 until 3) {
                val e = tri.getEdgeVertices(edgeIdx)
                val key = canonicalEdgeKey(e.first, e.second, 1e-4)
                edgeFaces.getOrPut(key) { mutableListOf() }.add(faceIdx)
            }
        }

        val boundaryEdges = mutableListOf<Pair<Int, Int>>()
        for ((faceIdx, tri) in triangles.withIndex()) {
            for (edgeIdx in 0 until 3) {
                val e = tri.getEdgeVertices(edgeIdx)
                val key = canonicalEdgeKey(e.first, e.second, 1e-4)
                if (edgeFaces[key]?.size == 1) {
                    boundaryEdges.add(faceIdx to edgeIdx)
                }
            }
        }

        if (boundaryEdges.isEmpty()) return Pair(triangles, 0)

        val vertexOutEdges = mutableMapOf<V3dKey, MutableList<Pair<V3d, V3d>>>()
        for ((faceIdx, edgeIdx) in boundaryEdges) {
            val tri = triangles[faceIdx]
            val e = tri.getEdgeVertices(edgeIdx)
            val key = V3dKey(e.first, 1e-4)
            vertexOutEdges.getOrPut(key) { mutableListOf() }.add(e.first to e.second)
        }

        val visited = mutableSetOf<Pair<V3dKey, V3dKey>>()
        val cycles = mutableListOf<List<V3d>>()

        for ((faceIdx, edgeIdx) in boundaryEdges) {
            val tri = triangles[faceIdx]
            val e = tri.getEdgeVertices(edgeIdx)
            val keyA = V3dKey(e.first, 1e-4)
            val keyB = V3dKey(e.second, 1e-4)

            if (visited.contains(keyA to keyB)) continue

            val cycle = mutableListOf(e.first, e.second)
            visited.add(keyA to keyB)

            var currentEnd = e.second
            var maxSteps = boundaryEdges.size

            while (maxSteps-- > 0) {
                val curKey = V3dKey(currentEnd, 1e-4)
                val outgoing = vertexOutEdges[curKey] ?: break

                var found = false
                for (outEdge in outgoing) {
                    val outKey = V3dKey(outEdge.second, 1e-4)
                    if (!visited.contains(curKey to outKey)) {
                        visited.add(curKey to outKey)
                        currentEnd = outEdge.second
                        cycle.add(currentEnd)
                        found = true
                        break
                    }
                }

                if (!found) break
                if (V3dKey(currentEnd, 1e-4) == keyA) break
            }

            if (cycle.size >= 4 && V3dKey(cycle.last(), 1e-4) == V3dKey(cycle.first(), 1e-4)) {
                cycle.removeAt(cycle.lastIndex)
            }

            if (cycle.size >= 3) {
                cycles.add(cycle.toList())
            }
        }

        val resultTriangles = triangles.toMutableList()
        for (cycle in cycles) {
            val newTris = triangulatePolygon(cycle)
            resultTriangles.addAll(newTris)
        }

        return Pair(resultTriangles, cycles.size)
    }

    private fun triangulatePolygon(vertices: List<V3d>): List<Triangle> {
        if (vertices.size < 3) return emptyList()
        if (vertices.size == 3) return listOf(Triangle(vertices[0], vertices[1], vertices[2]))

        var nx = 0.0; var ny = 0.0; var nz = 0.0
        for (i in vertices.indices) {
            val p1 = vertices[i]
            val p2 = vertices[(i + 1) % vertices.size]
            nx += (p1.y - p2.y) * (p1.z + p2.z)
            ny += (p1.z - p2.z) * (p1.x + p2.x)
            nz += (p1.x - p2.x) * (p1.y + p2.y)
        }
        val normal = V3d(nx, ny, nz)
        val mag = normal.magnitude()
        if (mag < 1e-10) {
            // Degenerate polygon - fall back to fan triangulation
            return fanTriangulate(vertices)
        }
        val n = normal.scale(1.0 / mag)

        val ref = if (Math.abs(n.x) < 0.9) V3d(1.0, 0.0, 0.0) else V3d(0.0, 1.0, 0.0)
        var u = n.cross(ref)
        val uMag = u.magnitude()
        if (uMag < 1e-10) return fanTriangulate(vertices)
        u = u.scale(1.0 / uMag)
        val v = u.cross(n)

        class Point2D(val x: Double, val y: Double)

        fun cross2D(a: Point2D, b: Point2D, c: Point2D): Double =
            (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

        fun pointInTriangle(a: Point2D, b: Point2D, c: Point2D, p: Point2D): Boolean {
            val d1 = cross2D(a, b, p)
            val d2 = cross2D(b, c, p)
            val d3 = cross2D(c, a, p)
            val hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0)
            val hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0)
            return !(hasNeg && hasPos)
        }

        val origin = vertices[0]
        val pts = vertices.map { vert ->
            val d = vert.subtract(origin)
            Point2D(d.dot(u), d.dot(v))
        }

        val indices = mutableListOf<Int>()
        for (i in pts.indices) indices.add(i)

        val result = mutableListOf<Triangle>()

        while (indices.size > 3) {
            var earFound = false
            for (i in 0 until indices.size) {
                val prev = indices[(i - 1 + indices.size) % indices.size]
                val curr = indices[i]
                val next = indices[(i + 1) % indices.size]

                val cross = cross2D(pts[prev], pts[curr], pts[next])
                if (cross <= 0) continue

                var isEar = true
                for (j in indices) {
                    if (j == prev || j == curr || j == next) continue
                    if (pointInTriangle(pts[prev], pts[curr], pts[next], pts[j])) {
                        isEar = false
                        break
                    }
                }

                if (isEar) {
                    result.add(Triangle(vertices[prev], vertices[curr], vertices[next]))
                    indices.removeAt(i)
                    earFound = true
                    break
                }
            }

            if (!earFound) {
                return fanTriangulate(vertices)
            }
        }

        if (indices.size == 3) {
            result.add(Triangle(
                vertices[indices[0]], vertices[indices[1]], vertices[indices[2]]
            ))
        }

        return result
    }

    private fun fanTriangulate(vertices: List<V3d>): List<Triangle> {
        val cx = vertices.sumOf { it.x } / vertices.size
        val cy = vertices.sumOf { it.y } / vertices.size
        val cz = vertices.sumOf { it.z } / vertices.size
        val center = V3d(cx, cy, cz)
        val result = mutableListOf<Triangle>()
        for (i in vertices.indices) {
            val j = (i + 1) % vertices.size
            result.add(Triangle(vertices[i], vertices[j], center))
        }
        return result
    }

    // ==================== Утилиты ====================
    
    private fun computeGroupCenter(group: List<V3d>): V3d {
        val n = group.size.toDouble()
        return V3d(
            group.sumOf { it.x } / n,
            group.sumOf { it.y } / n, 
            group.sumOf { it.z } / n
        )
    }

    private fun distanceSquared(a: V3d, b: V3d): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val dz = b.z - a.z
        return dx * dx + dy * dy + dz * dz
    }

    private fun normalizeVec(v: V3d): V3d {
        val mag = v.magnitude()
        if (mag < 1e-10) return v
        return v.scale(1.0 / mag)
    }
}
