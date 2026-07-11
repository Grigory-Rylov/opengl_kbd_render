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
        val normalsFlipped: Int
    ) {
        override fun toString(): String {
            return """StlRepairer.RepairResult(
                |  originalTriangles=$originalTriangles -> repairedTriangles=$repairedTriangles,
                |  edgesFixed=$edgesFixed,
                |  degenerateRemoved=$degenerateRemoved, 
                |  disconnectedRemoved=$disconnectedRemoved,
                |  normalsFlipped=$normalsFlipped
            """.trimMargin()
        }
    }

    data class RepairOptions(
        val snapTolerance: Double = 1e-4,
        val maxSnapIterations: Int = 2,
        val removeDisconnectedFacets: Boolean = true,
        val fixNormals: Boolean = true
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
        val degenerateRemoved = beforeDegenerate - triangles.size
        
        println("StlRepairer: Removed $degenerateRemoved degenerate triangles")

        // Шаг 3: Remove disconnected facets  
        var disconnectedRemoved = 0
        if (options.removeDisconnectedFacets && triangles.isNotEmpty()) {
            val connected = removeDisconnectedFacets(triangles)
            disconnectedRemoved = triangles.size - connected.triangles.size
            triangles = connected.triangles
            
            println("StlRepairer: Removed $disconnectedRemoved disconnected facets")
        }

        // Шаг 4-5: Fix normals  
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
            normalsFlipped = normalsFlipped
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
        
        for ((key, group) in vertexGroups) {
            if (group.size <= 1) continue
            
            val center = computeGroupCenter(group)
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
    
    private fun removeDisconnectedFacets(triangles: List<Triangle>): SnapResult {
        if (triangles.isEmpty()) return SnapResult(emptyList(), 0)

        val vertexIndex = buildVertexFaceIndexForRepair(triangles)
        
        // Удаляем грани без правильно соединенных ребер  
        var disconnectedCount = 0
        
        for ((faceIdx, tri) in triangles.withIndex()) {
            var hasConnectedEdge = false
            
            for (edgeIdx in 0 until 3) {
                val edgeVerts = tri.getEdgeVertices(edgeIdx)

                if (findNeighborFace(vertexIndex, triangles, faceIdx, edgeVerts.first, edgeVerts.second)) {
                    hasConnectedEdge = true
                    break
                }
            }
            
            if (!hasConnectedEdge) disconnectedCount++
        }

        val newTriangles = triangles.filterIndexed { faceIdx, tri -> 
            var hasConnectedEdge = false
            
            for (edgeIdx in 0 until 3) {
                val edgeVerts = tri.getEdgeVertices(edgeIdx)
                
                if (findNeighborFace(vertexIndex, triangles, faceIdx, edgeVerts.first, edgeVerts.second)) {
                    hasConnectedEdge = true
                    break
                }
            }
            
            hasConnectedEdge
        }

        return SnapResult(newTriangles, disconnectedCount)
    }

    private fun buildVertexFaceIndexForRepair(triangles: List<Triangle>): Map<V3dKey, MutableList<Int>> {
        val index = mutableMapOf<V3dKey, MutableList<Int>>()
        
        for ((faceIdx, tri) in triangles.withIndex()) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                val key = V3dKey(v, 1e-4)
                if (!index.containsKey(key)) index[key] = mutableListOf()
                index[key]?.add(faceIdx)
            }
        }

        return index
    }

    private fun findNeighborFace(
        vertexIndex: Map<V3dKey, MutableList<Int>>, 
        triangles: List<Triangle>,
        faceIdx: Int, vA: V3d, vB: V3d
    ): Boolean {
        val aKey = V3dKey(vA, 1e-4)
        val bKey = V3dKey(vB, 1e-4)

        val candidateFaces = vertexIndex[aKey] ?: return false
        
        for (candidateFaceIdx in candidateFaces) {
            if (candidateFaceIdx == faceIdx) continue
            
            if (hasOppositeEdge(triangles[candidateFaceIdx], aKey, bKey)) return true
        }

        return false
    }

    private fun hasOppositeEdge(candidateTri: Triangle, vAKey: V3dKey, vBKey: V3dKey): Boolean {
        val vertices = listOf(candidateTri.v0, candidateTri.v1, candidateTri.v2)

        for (edgeIdx in 0 until 3) {
            if (V3dKey(vertices[edgeIdx], 1e-4) == vBKey && 
                V3dKey(vertices[(edgeIdx + 1) % 3], 1e-4) == vAKey) return true
        }

        return false
    }

    // ==================== Шаг 4-5: Fix Normal Directions ====================
    
    private fun fixNormalDirections(triangles: List<Triangle>): NormalsFixResult {
        if (triangles.isEmpty()) return NormalsFixResult(emptyList(), 0)

        val trianglesMutable = triangles.toMutableList()
        var normalsFlippedCount = 0

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
                    val neighborIdx = findNeighborByEdge(trianglesMutable, faceIdx, edgeIdx)

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

    private fun findNeighborByEdge(
        triangles: List<Triangle>, faceIdx: Int, edgeIdx: Int
    ): Int {
        val tri = triangles[faceIdx]
        val edgeVerts = tri.getEdgeVertices(edgeIdx)
        
        for ((candidateFaceIdx, candTri) in triangles.withIndex()) {
            if (candidateFaceIdx == faceIdx) continue
            
            val vertices = listOf(candTri.v0, candTri.v1, candTri.v2)

            for (cEdge in 0 until 3) {
                if (V3dKey(vertices[cEdge], 1e-4) == V3dKey(edgeVerts.second, 1e-4) && 
                    V3dKey(vertices[(cEdge + 1) % 3], 1e-4) == V3dKey(edgeVerts.first, 1e-4)) {
                    return candidateFaceIdx
                }
            }
        }

        return -1
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
