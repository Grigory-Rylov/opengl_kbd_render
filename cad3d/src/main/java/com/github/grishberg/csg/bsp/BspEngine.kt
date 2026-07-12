package com.github.grishberg.csg.bsp

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * BSP (Binary Space Partitioning) CSG engine — pure Kotlin.
 *
 * Classic BSP algorithm:
 * 1. Build BSP tree from solid's polygons using plane splitting
 * 2. Classify polygons of one tree against another tree
 * 3. Combine inside/outside polygons based on operation type
 * 4. Shared vertices in final mesh eliminate open edges
 */

const val EPSILON = 1e-6

// ---- Polygon ----

class Polygon(
    var v0: Vec3,
    var v1: Vec3,
    var v2: Vec3
) {
    val normal: Vec3
        get() {
            val e1 = v1 - v0
            val e2 = v2 - v0
            val n = e1.cross(e2)
            val len = n.length()
            return if (len > 1e-15) n / len else Vec3.ZERO
        }

    fun clone(): Polygon = Polygon(v0, v1, v2)
    fun flip(): Polygon = Polygon(v2, v1, v0)
}

// ---- Plane ----

enum class Side { FRONT, BACK, COPLANAR }

class Plane(
    var nx: Double,
    var ny: Double,
    var nz: Double,
    var d: Double
) {
    companion object {
        fun fromPolygon(p: Polygon): Plane {
            val n = p.normal
            return Plane(n.x, n.y, n.z, p.v0.x * n.x + p.v1.y * n.y + p.v2.z * n.z)
        }
    }

    fun classify(v: Vec3): Side {
        val t = nx * v.x + ny * v.y + nz * v.z - d
        return if (t < -EPSILON) Side.BACK
        else if (t > EPSILON) Side.FRONT
        else Side.COPLANAR
    }

    fun flip() {
        nx = -nx
        ny = -ny
        nz = -nz
        d = -d
    }

    /** Split a polygon against this plane. Returns list of new polygons. */
    fun splitPolygon(p: Polygon): List<Polygon> {
        val s0 = classify(p.v0)
        val s1 = classify(p.v1)
        val s2 = classify(p.v2)

        val allSides = setOf(s0, s1, s2)

        // Entirely on one side or coplanar
        if (allSides.size == 1) return listOf(p)

        // Need to split: collect vertices by side
        val front = mutableListOf<Vec3>()
        val back = mutableListOf<Vec3>()

        val verts = listOf(p.v0, p.v1, p.v2)
        val sides = listOf(s0, s1, s2)

        for (i in 0..2) {
            val v = verts[i]
            val s = sides[i]
            val next = (i + 1) % 3
            val sn = sides[next]
            val vn = verts[next]

            if (s != Side.BACK) front.add(v)
            if (s != Side.FRONT) back.add(v)

            // Edge crosses plane
            if (s != sn && s != Side.COPLANAR && sn != Side.COPLANAR) {
                val t = (d - nx * v.x - ny * v.y - nz * v.z) /
                    (nx * (vn.x - v.x) + ny * (vn.y - v.y) + nz * (vn.z - v.z))
                if (t.isFinite() && t in 0.0..1.0) {
                    val sp = Vec3(
                        v.x + t * (vn.x - v.x),
                        v.y + t * (vn.y - v.y),
                        v.z + t * (vn.z - v.z)
                    )
                    front.add(sp)
                    back.add(sp)
                }
            }
        }

        val result = mutableListOf<Polygon>()
        if (front.size >= 3) result.add(Polygon(front[0], front[1], front[2]))
        if (back.size >= 3) result.add(Polygon(back[0], back[1], back[2]))
        return result
    }
}

// ---- BSP Node ----

class Node(
    var polygons: MutableList<Polygon> = mutableListOf(),
    var plane: Plane? = null,
    var front: Node? = null,
    var back: Node? = null
) {

    fun clone(): Node = Node(
        polygons.map { it.clone() }.toMutableList(),
        plane?.let { Plane(it.nx, it.ny, it.nz, it.d) },
        front?.clone(),
        back?.clone()
    )

    /** Build BSP tree recursively. */
    fun build() {
        if (polygons.isEmpty()) return

        // Pick splitting plane from first polygon
        plane = Plane.fromPolygon(polygons[0])

        val frontPolys = mutableListOf<Polygon>()
        val backPolys = mutableListOf<Polygon>()

        for (p in polygons) {
            if (p === polygons[0]) {
                // Skip source polygon — it defines the plane
                continue
            }

            val split = plane!!.splitPolygon(p)
            for (sp in split) {
                when (plane!!.classify(sp.v0)) {
                    Side.FRONT -> frontPolys.add(sp)
                    Side.BACK -> backPolys.add(sp)
                    Side.COPLANAR -> {
                        // Check if front or back relative to plane
                        val test = plane!!.classify(sp.v0)
                        if (test == Side.FRONT) frontPolys.add(sp)
                        else backPolys.add(sp)
                    }
                }
            }
        }

        if (frontPolys.isNotEmpty()) {
            front = Node(frontPolys, null, null, null)
            front!!.build()
        }

        if (backPolys.isNotEmpty()) {
            back = Node(backPolys, null, null, null)
            back!!.build()
        }
    }

    /** Collect all polygons in subtree. */
    fun allPolys(): List<Polygon> {
        val result = mutableListOf<Polygon>()
        result.addAll(polygons)
        front?.allPolys()?.let { result.addAll(it) }
        back?.allPolys()?.let { result.addAll(it) }
        return result
    }

    /**
     * Classify this node's polygons against the subject tree.
     * @param isSolid whether the subject tree is solid (true) or void (false)
     */
    fun classify(subject: Node, isSolid: Boolean): List<Polygon> {
        return if (plane == null) {
            // Leaf node
            if (isSolid) polygons else emptyList()
        } else {
            val inFront = plane!!.classifyNode(subject, Side.FRONT, Side.BACK)
            val inBack = plane!!.classifyNode(subject, Side.BACK, Side.FRONT)

            val frontInside = front?.let { inFront.classify(it, !isSolid) } ?: emptyList()
            val backInside = back?.let { inBack.classify(it, !isSolid) } ?: emptyList()

            frontInside + backInside
        }
    }

    /** Flip all polygon windings (invert inside/outside). */
    fun flip() {
        polygons = polygons.map { it.flip() }.toMutableList()
        front?.flip()
        back?.flip()
        plane?.flip()
    }
}

/** Classify polygons relative to a specific side of this plane. */
private fun Plane.classifyNode(node: Node, keep: Side, discard: Side): Node {
    return if (node.polygons.isEmpty() && node.front == null && node.back == null) {
        Node()
    } else if (node.plane == null) {
        // Leaf
        val kept = node.polygons.filter { classify(it.v0) != discard }
        Node(kept.toMutableList())
    } else {
        val kept = node.polygons.filter { classify(it.v0) != discard }
        val firstFront = node.front?.polygons?.firstOrNull()
        val f = if (firstFront != null && classify(firstFront.v0) != discard) {
            classifyNode(node.front!!, keep, discard)
        } else null
        val firstBack = node.back?.polygons?.firstOrNull()
        val b = if (firstBack != null && classify(firstBack.v0) != discard) {
            classifyNode(node.back!!, keep, discard)
        } else null
        Node(kept.toMutableList(), node.plane, f, b)
    }
}

// ---- CSG Operations ----

enum class CsgOp { UNION, DIFFERENCE, INTERSECTION }

/**
 * Perform CSG boolean operation on two meshes.
 */
fun csgOperation(a: PolySet3, b: PolySet3, op: CsgOp): PolySet3 {
    if (a.isEmpty()) return if (op == CsgOp.DIFFERENCE) PolySet3.EMPTY else b
    if (b.isEmpty()) return if (op == CsgOp.INTERSECTION) PolySet3.EMPTY else a

    val polysA = meshToPolygons(a)
    val polysB = meshToPolygons(b)

    val nodeA = Node(polysA.toMutableList(), null, null, null)
    nodeA.build()
    val nodeB = Node(polysB.toMutableList(), null, null, null)
    nodeB.build()

    // Classify A against B
    val aInB = nodeB.classify(nodeA.clone(), false)

    // Classify B against A
    val bInA = nodeA.classify(nodeB.clone(), false)

    // Classify B against inverted A
    val nodeAFlip = nodeA.clone()
    nodeAFlip.flip()
    val bInNotA = nodeAFlip.classify(nodeB.clone(), false)

    val result: List<Polygon> = when (op) {
        CsgOp.UNION -> aInB + bInNotA
        CsgOp.INTERSECTION -> bInA
        CsgOp.DIFFERENCE -> {
            val flipped = bInA.map { it.flip() }
            aInB + flipped
        }
    }

    return meshFromPolygons(result)
}

/** Convert PolySet3 to polygon list. */
private fun meshToPolygons(mesh: PolySet3): MutableList<Polygon> {
    return mesh.indices.map { tri ->
        Polygon(
            mesh.vertices[tri.a],
            mesh.vertices[tri.b],
            mesh.vertices[tri.c]
        )
    }.toMutableList()
}

/** Convert polygon list back to PolySet3 with SHARED vertices. */
fun meshFromPolygons(polys: List<Polygon>): PolySet3 {
    data class VKey(val x: Double, val y: Double, val z: Double)

    val vertexMap = mutableMapOf<VKey, Int>()
    val vertices = mutableListOf<Vec3>()

    fun getIdx(v: Vec3): Int {
        val key = VKey(v.x, v.y, v.z)
        if (key !in vertexMap) {
            vertexMap[key] = vertices.size
            vertices.add(v)
        }
        return vertexMap[key]!!
    }

    val tris = mutableListOf<PolySet3.Triplet>()
    for (p in polys) {
        val i0 = getIdx(p.v0)
        val i1 = getIdx(p.v1)
        val i2 = getIdx(p.v2)
        if (i0 != i1 && i1 != i2 && i0 != i2) {
            tris.add(PolySet3.Triplet(i0, i1, i2))
        }
    }

    return PolySet3(vertices, tris)
}
