package com.github.grishberg.javascad

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import java.util.*

/**
 * Валидатор STL мешей на основе алгоритма из OrcaSlicer.
 */
class StlValidator {

    data class ValidationResult(
        val totalTriangles: Int,
        val uniqueVertices: Int,
        val openEdges: Int,
        val degenerateTriangles: Int,
        val disconnectedComponents: Int,
        val isManifold: Boolean = openEdges == 0 && degenerateTriangles == 0
    ) {
        override fun toString(): String {
            return """StlValidator.ValidationResult(
                |  totalTriangles=$totalTriangles,
                |  uniqueVertices=$uniqueVertices, 
                |  openEdges=$openEdges,
                |  degenerateTriangles=$degenerateTriangles,
                |  disconnectedComponents=$disconnectedComponents,
                |  isManifold=$isManifold
            """.trimMargin()
        }
    }

    fun validate(polygons: List<Polygon>): ValidationResult {
        val triangles = polygons.mapNotNull { it.toTriangle() }
        
        if (triangles.isEmpty()) return ValidationResult(0, 0, 0, 0, 0)

        var degenerateCount = 0
        for (tri in triangles) {
            if (isDegenerate(tri)) degenerateCount++
        }

        val vertexIndex = buildVertexFaceIndex(triangles)
        val uniqueVertices = vertexIndex.groupedVertices.size
        val faceNeighbors = createFaceNeighborsIndex(vertexIndex, triangles)

        val openEdgesCount = countOpenEdges(triangles)

        val disconnectedComponents = countDisconnectedComponents(faceNeighbors, triangles.size)

        return ValidationResult(
            totalTriangles = triangles.size,
            uniqueVertices = uniqueVertices,
            openEdges = openEdgesCount,
            degenerateTriangles = degenerateCount,
            disconnectedComponents = disconnectedComponents
        )
    }

    private fun Polygon.toTriangle(): Triangle? {
        val vertices = getVertices()
        return if (vertices.size == 3) Triangle(vertices[0], vertices[1], vertices[2]) else null
    }

    private fun isDegenerate(tri: Triangle): Boolean {
        val ab = tri.v1.subtract(tri.v0)
        val ac = tri.v2.subtract(tri.v0)
        return ab.cross(ac).magnitude() < 1e-10
    }

    private fun buildVertexFaceIndex(triangles: List<Triangle>): VertexFaceIndex {
        val groupedVertices = mutableMapOf<V3dKey, V3d>()
        
        for (tri in triangles) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                val key = v.toV3dKey()
                if (!groupedVertices.containsKey(key)) groupedVertices[key] = v
            }
        }

        val vertexToId = mutableMapOf<V3dKey, Int>()
        var nextId = 0
        for ((key) in groupedVertices) {
            vertexToId[key] = nextId++
        }

        val faceCounts = IntArray(nextId + 1)
        
        for (tri in triangles) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                val id = vertexToId[v.toV3dKey()]!!
                faceCounts[id]++
            }
        }

        var runningSum = 0
        for (i in faceCounts.indices) {
            val temp = faceCounts[i]
            faceCounts[i] = runningSum
            runningSum += temp
        }

        val vertexFacesAll = IntArray(runningSum)
        val currentPos = IntArray(nextId + 1)
        
        for ((faceIdx, tri) in triangles.withIndex()) {
            for (v in listOf(tri.v0, tri.v1, tri.v2)) {
                val id = vertexToId[v.toV3dKey()]!!
                val pos = faceCounts[id] + currentPos[id]
                if (pos < vertexFacesAll.size) {
                    vertexFacesAll[pos] = faceIdx
                    currentPos[id]++
                }
            }
        }

        return VertexFaceIndex(groupedVertices, vertexToId, faceCounts, vertexFacesAll.toList())
    }

    private fun createFaceNeighborsIndex(
        index: VertexFaceIndex, triangles: List<Triangle>
    ): Array<IntArray> {

        val numTriangles = triangles.size
        val neighbors = Array(numTriangles) { IntArray(3) { -1 } }

        for (faceIdx in 0 until numTriangles) {
            val tri = triangles[faceIdx]
            
            for (edgeIdx in 0 until 3) {
                if (neighbors[faceIdx][edgeIdx] != -1) continue
                
                val vertices = listOf(tri.v0, tri.v1, tri.v2)
                val vA = vertices[edgeIdx]
                val vB = vertices[(edgeIdx + 1) % 3]

                val aKey = vA.toV3dKey()
                val bKey = vB.toV3dKey()
                val groupId = index.vertexToId[aKey] ?: continue
                
                if (groupId >= index.faceStarts.size - 1) continue
                
                val startIdx = index.faceStarts[groupId]
                val endIdx = index.faceStarts[groupId + 1]

                for (i in startIdx until minOf(endIdx, index.vertexFacesAll.size)) {
                    val candidateFaceIdx = index.vertexFacesAll[i]
                    
                    if (candidateFaceIdx <= faceIdx) continue
                    
                    val candTri = triangles[candidateFaceIdx]
                    val candVertexList = listOf(candTri.v0, candTri.v1, candTri.v2)

                    val candidateEdgeIdx = findSharedEdge(candVertexList, aKey, bKey)

                    if (candidateEdgeIdx >= 0) {
                        if (neighbors[candidateFaceIdx][candidateEdgeIdx] != -1) continue
                        
                        neighbors[faceIdx][edgeIdx] = candidateFaceIdx
                        neighbors[candidateFaceIdx][candidateEdgeIdx] = faceIdx
                    }
                }
            }
        }

        return neighbors
    }

    private fun findSharedEdge(candidateVertices: List<V3d>, vAKey: V3dKey, vBKey: V3dKey): Int {
        for (edgeIdx in 0 until 3) {
            if ((candidateVertices[edgeIdx].toV3dKey() == vBKey && 
                 candidateVertices[(edgeIdx + 1) % 3].toV3dKey() == vAKey) ||
                (candidateVertices[edgeIdx].toV3dKey() == vAKey && 
                 candidateVertices[(edgeIdx + 1) % 3].toV3dKey() == vBKey)) return edgeIdx
        }
        return -1
    }

    private fun countOpenEdges(triangles: List<Triangle>): Int {
        val edgeFaceCounts = mutableMapOf<EdgeKey, Int>()
        for (tri in triangles) {
            for (edge in listOf(
                tri.v0 to tri.v1, tri.v1 to tri.v2, tri.v2 to tri.v0
            )) {
                val key = EdgeKey(edge.first.toV3dKey(), edge.second.toV3dKey())
                edgeFaceCounts[key] = (edgeFaceCounts[key] ?: 0) + 1
            }
        }
        return edgeFaceCounts.count { it.value < 2 }
    }

    private fun countDisconnectedComponents(neighbors: Array<IntArray>, totalTriangles: Int): Int {
        if (totalTriangles == 0) return 0
        
        val visited = BooleanArray(totalTriangles)
        var components = 0

        for (startFace in 0 until totalTriangles) {
            if (visited[startFace]) continue
            
            components++
            val queue: Queue<Int> = LinkedList()
            queue.add(startFace)
            visited[startFace] = true

            while (!queue.isEmpty()) {
                val faceIdx = queue.poll()!!
                
                for (neighbor in neighbors[faceIdx]) {
                    if (neighbor >= 0 && !visited[neighbor]) {
                        visited[neighbor] = true
                        queue.add(neighbor)
                    }
                }
            }
        }

        return components
    }

    data class Triangle(val v0: V3d, val v1: V3d, val v2: V3d)
    
    data class VertexFaceIndex(
        val groupedVertices: Map<V3dKey, V3d>,
        val vertexToId: Map<V3dKey, Int>,
        val faceStarts: IntArray,
        val vertexFacesAll: List<Int>
    )

    data class V3dKey(val x: Long, val y: Long, val z: Long) {
        constructor(v: V3d) : this(
            Math.round(v.x / 1e-6),
            Math.round(v.y / 1e-6), 
            Math.round(v.z / 1e-6)
        )
    }

    private data class EdgeKey(val key: Pair<V3dKey, V3dKey>) {
        constructor(a: V3dKey, b: V3dKey) : this(
            if (a.x < b.x || (a.x == b.x && (a.y < b.y || (a.y == b.y && a.z <= b.z)))) Pair(a, b) else Pair(b, a)
        )
    }

    private fun V3d.toV3dKey(): V3dKey = V3dKey(this)
}
