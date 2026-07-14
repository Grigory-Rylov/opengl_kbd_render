package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon

private const val EPS = 1e-8
private const val EPS2 = EPS * EPS

data class Tri(
    val v0: V3d, val v1: V3d, val v2: V3d,
    var inside: Boolean = false
) {
    fun centroid(): V3d = V3d(
        (v0.x + v1.x + v2.x) / 3.0,
        (v0.y + v1.y + v2.y) / 3.0,
        (v0.z + v1.z + v2.z) / 3.0
    )

    fun bbox(): TriBBox {
        val minX = minOf(v0.x, v1.x, v2.x)
        val minY = minOf(v0.y, v1.y, v2.y)
        val minZ = minOf(v0.z, v1.z, v2.z)
        val maxX = maxOf(v0.x, v1.x, v2.x)
        val maxY = maxOf(v0.y, v1.y, v2.y)
        val maxZ = maxOf(v0.z, v1.z, v2.z)
        return TriBBox(minX, minY, minZ, maxX, maxY, maxZ)
    }
}

data class TriBBox(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double
)

object ManifoldEngine {

    enum class Op { UNION, DIFFERENCE, INTERSECTION }

    fun union(a: List<Polygon>, b: List<Polygon>): List<Polygon> = operate(a, b, Op.UNION)
    fun difference(a: List<Polygon>, b: List<Polygon>): List<Polygon> = operate(a, b, Op.DIFFERENCE)
    fun intersection(a: List<Polygon>, b: List<Polygon>): List<Polygon> = operate(a, b, Op.INTERSECTION)

    fun operateTris(a: List<Polygon>, b: List<Polygon>, op: Op): List<Tri> {
        if (a.isEmpty()) return polygonsToTris(b)
        if (b.isEmpty()) return polygonsToTris(a)

        var trisA = polygonsToTris(a)
        var trisB = polygonsToTris(b)

        if (trisA.isEmpty()) return polygonsToTris(b)
        if (trisB.isEmpty()) return polygonsToTris(a)

        val gridA = SpatialGrid(trisA)
        val gridB = SpatialGrid(trisB)

        trisA = insertIntersections(trisA, gridB)
        trisB = insertIntersections(trisB, gridA)

        markInside(trisA, gridB)
        markInside(trisB, gridA)

        val resultTris = collectTris(trisA, trisB, op)
        val weldedTris = weldVertices(resultTris)
        println("[Manifold] operateTris Result: ${weldedTris.size} tris")
        return weldedTris
    }

    private fun operate(a: List<Polygon>, b: List<Polygon>, op: Op): List<Polygon> {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a

        var trisA = polygonsToTris(a)
        var trisB = polygonsToTris(b)

        if (trisA.isEmpty()) return b
        if (trisB.isEmpty()) return a

        val gridA = SpatialGrid(trisA)
        val gridB = SpatialGrid(trisB)

        trisA = insertIntersections(trisA, gridB)
        trisB = insertIntersections(trisB, gridA)

        markInside(trisA, gridB)
        markInside(trisB, gridA)

        // Debug: count inside/outside
        val insideA = trisA.count { it.inside }
        val insideB = trisB.count { it.inside }
        println("[Manifold] Op=$op: trisA=${trisA.size} (inside=${insideA}), trisB=${trisB.size} (inside=${insideB})")

        val resultTris = collectTris(trisA, trisB, op)
        println("[Manifold] Result: ${resultTris.size} tris")
        val weldedTris = weldVertices(resultTris)
        println("[Manifold] After weld: ${weldedTris.size} tris")
        return trisToPolygons(weldedTris)
    }

    private fun polygonsToTris(polygons: List<Polygon>): List<Tri> {
        val tris = mutableListOf<Tri>()
        for (poly in polygons) {
            val pts = poly.vertices
            if (pts.size < 3) continue
            val v0 = pts[0]
            for (i in 1 until pts.size - 1) {
                tris.add(Tri(v0, pts[i], pts[i + 1]))
            }
        }
        return tris
    }

    private fun insertIntersections(trisA: List<Tri>, grid: SpatialGrid): List<Tri> {
        val result = mutableListOf<Tri>()
        var totalHits = 0

        for (tri in trisA) {
            val edges = arrayOf(
                MeshEdge(tri.v0, tri.v1),
                MeshEdge(tri.v1, tri.v2),
                MeshEdge(tri.v2, tri.v0)
            )

            val hits = mutableMapOf<MeshEdge, MutableList<V3d>>()

            for (edge in edges) {
                val candidates = grid.querySegment(edge.a, edge.b)
                val edgeHits = mutableListOf<V3d>()

                for (candidate in candidates) {
                    val hit = segmentTriangleIntersect(edge.a, edge.b, candidate)
                    if (hit != null && !near(hit, edge.a) && !near(hit, edge.b)) {
                        var dup = false
                        for (h in edgeHits) {
                            if (dist2(h, hit) < EPS2) { dup = true; break }
                        }
                        if (!dup) { edgeHits.add(hit); totalHits++ }
                    }
                }

                hits[edge] = edgeHits
            }

            val hasHits = hits.values.any { it.isNotEmpty() }
            if (!hasHits) {
                result.add(tri)
            } else {
                val newTris = splitTri(tri, hits)
                result.addAll(newTris)
            }
        }

        println("[Manifold] insertIntersections: ${trisA.size} -> ${result.size} (${totalHits} hits)")
        return result
    }

    private fun segmentTriangleIntersect(p1: V3d, p2: V3d, tri: Tri): V3d? {
        val e1x = tri.v1.x - tri.v0.x
        val e1y = tri.v1.y - tri.v0.y
        val e1z = tri.v1.z - tri.v0.z
        val e2x = tri.v2.x - tri.v0.x
        val e2y = tri.v2.y - tri.v0.y
        val e2z = tri.v2.z - tri.v0.z

        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z

        val hx = dy * e2z - dz * e2y
        val hy = dz * e2x - dx * e2z
        val hz = dx * e2y - dy * e2x

        val a = e1x * hx + e1y * hy + e1z * hz
        if (a > -EPS && a < EPS) return null

        val f = 1.0 / a
        val sx = p1.x - tri.v0.x
        val sy = p1.y - tri.v0.y
        val sz = p1.z - tri.v0.z

        val u = f * (sx * hx + sy * hy + sz * hz)
        if (u < -EPS || u > 1.0 + EPS) return null

        val qx = sy * e1z - sz * e1y
        val qy = sz * e1x - sx * e1z
        val qz = sx * e1y - sy * e1x

        val v = f * (dx * qx + dy * qy + dz * qz)
        if (v < -EPS || u + v > 1.0 + EPS) return null

        val t = f * (e2x * qx + e2y * qy + e2z * qz)
        if (t < -EPS || t > 1.0 + EPS) return null

        return V3d(p1.x + t * dx, p1.y + t * dy, p1.z + t * dz)
    }

    private fun splitTri(tri: Tri, hits: Map<MeshEdge, List<V3d>>): List<Tri> {
        val v0 = tri.v0
        val v1 = tri.v1
        val v2 = tri.v2

        val hits01 = hits[MeshEdge(v0, v1)]?.sortedBy { dist2(it, v0) } ?: emptyList()
        val hits12 = hits[MeshEdge(v1, v2)]?.sortedBy { dist2(it, v1) } ?: emptyList()
        val hits20 = hits[MeshEdge(v2, v0)]?.sortedBy { dist2(it, v2) } ?: emptyList()

        if (hits01.isEmpty() && hits12.isEmpty() && hits20.isEmpty()) return listOf(tri)

        // Collect all unique boundary hit points
        val allHits = mutableListOf<Pair<V3d, Double>>()
        for (h in hits01) allHits.add(Pair(h, 0.0))
        for (h in hits12) allHits.add(Pair(h, 0.0))
        for (h in hits20) allHits.add(Pair(h, 0.0))

        // For each hit point, split the triangle into 2
        // Process one at a time recursively
        val result = mutableListOf<Tri>()
        var currentTris = listOf(tri)

        for ((hit, _) in allHits) {
            val nextTris = mutableListOf<Tri>()
            for (ct in currentTris) {
                // Find which edge the hit is on and split
                val split = splitTriByPoint(ct, hit)
                nextTris.addAll(split)
            }
            currentTris = nextTris
        }

        return currentTris
    }

    private fun splitTriByPoint(tri: Tri, hit: V3d): List<Tri> {
        // Find which edge the hit point lies on
        if (near(hit, tri.v0) || near(hit, tri.v1) || near(hit, tri.v2)) return listOf(tri)

        // Check edge v0-v1
        if (onSegment(hit, tri.v0, tri.v1)) {
            return listOf(Tri(tri.v0, hit, tri.v2), Tri(hit, tri.v1, tri.v2))
        }
        // Check edge v1-v2
        if (onSegment(hit, tri.v1, tri.v2)) {
            return listOf(Tri(tri.v1, hit, tri.v0), Tri(hit, tri.v2, tri.v0))
        }
        // Check edge v2-v0
        if (onSegment(hit, tri.v2, tri.v0)) {
            return listOf(Tri(tri.v2, hit, tri.v1), Tri(hit, tri.v0, tri.v1))
        }

        return listOf(tri)
    }

    private fun onSegment(p: V3d, a: V3d, b: V3d): Boolean {
        val ab = b.subtract(a)
        val ap = p.subtract(a)
        val abLen2 = ab.dot(ab)
        if (abLen2 < EPS2) return false
        val t = ap.dot(ab) / abLen2
        if (t < -EPS || t > 1.0 + EPS) return false
        val proj = a.add(ab.scale(t))
        return dist2(p, proj) < EPS2 * 100
    }

    private fun markInside(trisA: List<Tri>, gridB: SpatialGrid) {
        for (tri in trisA) {
            val c = tri.centroid()
            tri.inside = gridB.rayCastInsideAdaptive(c, tri)
        }
    }

    private fun collectTris(trisA: List<Tri>, trisB: List<Tri>, op: Op): List<Tri> {
        val result = mutableListOf<Tri>()

        // trisA.inside = true => этот треугольник A находится внутри объекта B
        for (tri in trisA) {
            val keep = when (op) {
                Op.UNION -> !tri.inside
                Op.DIFFERENCE -> !tri.inside
                Op.INTERSECTION -> tri.inside
            }
            if (keep && !isDegenerate(tri)) result.add(tri)
        }

        // trisB.inside = true => этот треугольник B находится внутри объекта A
        for (tri in trisB) {
            val keep = when (op) {
                Op.UNION -> !tri.inside
                Op.DIFFERENCE -> tri.inside
                Op.INTERSECTION -> tri.inside
            }
            if (keep && !isDegenerate(tri)) {
                // Для DIFFERENCE: нормаль B должна быть перевёрнута (внутренняя стенка выреза)
                // Для UNION и INTERSECTION: нормаль B остаётся как есть
                if (op == Op.DIFFERENCE) {
                    result.add(Tri(tri.v0, tri.v2, tri.v1))
                } else {
                    result.add(tri)
                }
            }
        }

        return result
    }

    private fun weldVertices(tris: List<Tri>): List<Tri> {
        // Find all unique vertices by rounding to tolerance
        val vertexMap = mutableMapOf<LongArray, V3d>()
        val tolerance = 1e-4

        fun roundKey(v: V3d): LongArray = longArrayOf(
            Math.round(v.x / tolerance).toLong(),
            Math.round(v.y / tolerance).toLong(),
            Math.round(v.z / tolerance).toLong()
        )

        fun getOrAddVertex(v: V3d): V3d {
            val key = roundKey(v)
            if (!vertexMap.containsKey(key)) {
                vertexMap[key] = v
            }
            return vertexMap[key]!!
        }

        // First pass: collect all vertices and build canonical mapping
        for (tri in tris) {
            getOrAddVertex(tri.v0)
            getOrAddVertex(tri.v1)
            getOrAddVertex(tri.v2)
        }

        // Second pass: replace vertices with canonical ones
        return tris.map { tri ->
            Tri(getOrAddVertex(tri.v0), getOrAddVertex(tri.v1), getOrAddVertex(tri.v2), tri.inside)
        }
    }

    private fun isDegenerate(tri: Tri): Boolean {
        val e1x = tri.v1.x - tri.v0.x
        val e1y = tri.v1.y - tri.v0.y
        val e1z = tri.v1.z - tri.v0.z
        val e2x = tri.v2.x - tri.v0.x
        val e2y = tri.v2.y - tri.v0.y
        val e2z = tri.v2.z - tri.v0.z
        val nx = e1y * e2z - e1z * e2y
        val ny = e1z * e2x - e1x * e2z
        val nz = e1x * e2y - e1y * e2x
        return nx * nx + ny * ny + nz * nz < EPS2
    }

    private fun trisToPolygons(tris: List<Tri>): List<Polygon> {
        val result = mutableListOf<Polygon>()
        for (tri in tris) {
            val a = tri.v0
            val b = tri.v1
            val c = tri.v2
            val n = b.subtract(a).cross(c.subtract(a)).unit()
            val d = n.dot(a)
            val pa = a.subtract(n.scale(a.dot(n) - d))
            val pb = b.subtract(n.scale(b.dot(n) - d))
            val pc = c.subtract(n.scale(c.dot(n) - d))
            result.add(Polygon.fromPolygons(listOf(pa, pb, pc), n, Color.white))
        }
        return result
    }

    private fun near(a: V3d, b: V3d): Boolean {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return dx * dx + dy * dy + dz * dz < EPS2
    }

    private fun dist2(a: V3d, b: V3d): Double {
        val dx = a.x - b.x
        val dy = a.y - b.y
        val dz = a.z - b.z
        return dx * dx + dy * dy + dz * dz
    }
}

private class MeshEdge(val a: V3d, val b: V3d) {
    private val r0 = roundKey(a)
    private val r1 = roundKey(b)
    companion object {
        fun roundKey(v: V3d): LongArray = longArrayOf(
            Math.round(v.x * 1e4).toLong(),
            Math.round(v.y * 1e4).toLong(),
            Math.round(v.z * 1e4).toLong()
        )
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshEdge) return false
        val o = other
        return (r0.contentEquals(o.r0) && r1.contentEquals(o.r1)) ||
               (r0.contentEquals(o.r1) && r1.contentEquals(o.r0))
    }
    override fun hashCode(): Int = r0.contentHashCode() * 31 + r1.contentHashCode()
}

/**
 * Spatial hash grid for edge-triangle intersection queries and ray casting.
 * Triangles are inserted into all cells their bounding box covers.
 */
private class SpatialGrid(tris: List<Tri>) {
    private val cells = mutableMapOf<Long, MutableList<Tri>>()
    val cellSize: Double
    private val globalMinX: Double
    private val globalMaxX: Double
    private val globalMinY: Double
    private val globalMaxY: Double
    private val globalMinZ: Double
    private val globalMaxZ: Double

    init {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (tri in tris) {
            val bb = tri.bbox()
            if (bb.minX < minX) minX = bb.minX
            if (bb.maxX > maxX) maxX = bb.maxX
            if (bb.minY < minY) minY = bb.minY
            if (bb.maxY > maxY) maxY = bb.maxY
            if (bb.minZ < minZ) minZ = bb.minZ
            if (bb.maxZ > maxZ) maxZ = bb.maxZ
        }

        globalMinX = minX; globalMaxX = maxX
        globalMinY = minY; globalMaxY = maxY
        globalMinZ = minZ; globalMaxZ = maxZ

        val diag = Math.sqrt(
            (maxX - minX) * (maxX - minX) +
            (maxY - minY) * (maxY - minY) +
            (maxZ - minZ) * (maxZ - minZ)
        )
        cellSize = if (diag > 0) diag / 100.0 else 1.0

        for (tri in tris) {
            val bb = tri.bbox()
            val cx0 = Math.floor(bb.minX / cellSize).toLong()
            val cx1 = Math.floor(bb.maxX / cellSize).toLong()
            val cy0 = Math.floor(bb.minY / cellSize).toLong()
            val cy1 = Math.floor(bb.maxY / cellSize).toLong()
            val cz0 = Math.floor(bb.minZ / cellSize).toLong()
            val cz1 = Math.floor(bb.maxZ / cellSize).toLong()

            for (cx in cx0..cx1) {
                for (cy in cy0..cy1) {
                    for (cz in cz0..cz1) {
                        cells.computeIfAbsent(hashKey(cx, cy, cz)) { mutableListOf() }.add(tri)
                    }
                }
            }
        }
    }

    fun querySegment(p1: V3d, p2: V3d): List<Tri> {
        val result = mutableSetOf<Tri>()
        val dist = Math.sqrt(
            (p2.x - p1.x) * (p2.x - p1.x) +
            (p2.y - p1.y) * (p2.y - p1.y) +
            (p2.z - p1.z) * (p2.z - p1.z)
        )
        val steps = maxOf(2, Math.ceil(dist / cellSize).toInt())

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val x = p1.x + t * (p2.x - p1.x)
            val y = p1.y + t * (p2.y - p1.y)
            val z = p1.z + t * (p2.z - p1.z)
            val key = hashKey(
                Math.floor(x / cellSize).toLong(),
                Math.floor(y / cellSize).toLong(),
                Math.floor(z / cellSize).toLong()
            )
            cells[key]?.let { result.addAll(it) }
        }
        return result.toList()
    }

    fun rayCastInsideAdaptive(pt: V3d, sourceTri: Tri): Boolean {
        val ex = sourceTri.v1.x - sourceTri.v0.x
        val ey = sourceTri.v1.y - sourceTri.v0.y
        val ez = sourceTri.v1.z - sourceTri.v0.z
        val fx = sourceTri.v2.x - sourceTri.v0.x
        val fy = sourceTri.v2.y - sourceTri.v0.y
        val fz = sourceTri.v2.z - sourceTri.v0.z
        val nx = ey * fz - ez * fy
        val ny = ez * fx - ex * fz
        val nz = ex * fy - ey * fx

        val absNx = Math.abs(nx)
        val absNy = Math.abs(ny)
        val absNz = Math.abs(nz)

        return if (absNx >= absNy && absNx >= absNz) {
            rayCastAxis(pt, sourceTri, 0)
        } else if (absNy >= absNz) {
            rayCastAxis(pt, sourceTri, 1)
        } else {
            rayCastAxis(pt, sourceTri, 2)
        }
    }

    private fun rayCastAxis(pt: V3d, sourceTri: Tri, axis: Int): Boolean {
        var crossings = 0
        val allTris = cells.values.flatten().toSet()

        for (tri in allTris) {
            if (tri === sourceTri) continue

            val e1x = tri.v1.x - tri.v0.x
            val e1y = tri.v1.y - tri.v0.y
            val e1z = tri.v1.z - tri.v0.z
            val e2x = tri.v2.x - tri.v0.x
            val e2y = tri.v2.y - tri.v0.y
            val e2z = tri.v2.z - tri.v0.z

            var proj: Double
            var triMin: Double
            var triMax: Double
            var ptVal: Double

            when (axis) {
                0 -> {
                    proj = e1y * e2z - e1z * e2y
                    triMin = minOf(tri.v0.x, tri.v1.x, tri.v2.x)
                    triMax = maxOf(tri.v0.x, tri.v1.x, tri.v2.x)
                    ptVal = pt.x
                }
                1 -> {
                    proj = e1z * e2x - e1x * e2z
                    triMin = minOf(tri.v0.y, tri.v1.y, tri.v2.y)
                    triMax = maxOf(tri.v0.y, tri.v1.y, tri.v2.y)
                    ptVal = pt.y
                }
                else -> {
                    proj = e1x * e2y - e1y * e2x
                    triMin = minOf(tri.v0.z, tri.v1.z, tri.v2.z)
                    triMax = maxOf(tri.v0.z, tri.v1.z, tri.v2.z)
                    ptVal = pt.z
                }
            }

            if (Math.abs(proj) < 1e-10) continue

            // Только треугольники ПЕРЕД точкой в направлении луча
            if (proj > 0 && triMin <= ptVal) continue
            if (proj < 0 && triMax >= ptVal) continue

            if (triRayCrossAxis(pt, tri, axis, proj > 0)) crossings++
        }
        return crossings % 2 == 1
    }

    private fun triRayCrossAxis(pt: V3d, tri: Tri, axis: Int, flip: Boolean): Boolean {
        val a0 = if (flip) tri.v1 else tri.v2
        val a1 = if (flip) tri.v2 else tri.v1
        val c = tri.v0

        var ac0 = 0.0
        var ac1 = 0.0
        var bc0 = 0.0
        var bc1 = 0.0
        var cc0 = 0.0
        var cc1 = 0.0

        when (axis) {
            0 -> {
                ac0 = pt.y - c.y
                ac1 = pt.z - c.z
                bc0 = a0.y - c.y
                bc1 = a0.z - c.z
                cc0 = a1.y - c.y
                cc1 = a1.z - c.z
            }
            1 -> {
                ac0 = pt.x - c.x
                ac1 = pt.z - c.z
                bc0 = a0.x - c.x
                bc1 = a0.z - c.z
                cc0 = a1.x - c.x
                cc1 = a1.z - c.z
            }
            else -> {
                ac0 = pt.x - c.x
                ac1 = pt.y - c.y
                bc0 = a0.x - c.x
                bc1 = a0.y - c.y
                cc0 = a1.x - c.x
                cc1 = a1.y - c.y
            }
        }

        val denom = bc0 * cc1 - bc1 * cc0
        if (Math.abs(denom) < 1e-10) return false

        val alpha = (bc1 * ac0 - bc0 * ac1) / denom
        val beta = (cc0 * ac1 - cc1 * ac0) / denom

        return alpha >= -EPS && beta >= -EPS && alpha + beta <= 1.0 + EPS
    }

    private fun hashKey(x: Long, y: Long, z: Long): Long {
        return x * 73856093L xor y * 19349663L xor z * 83492791L
    }
}
