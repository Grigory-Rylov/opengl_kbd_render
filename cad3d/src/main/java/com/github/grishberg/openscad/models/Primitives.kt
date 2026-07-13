package com.github.grishberg.openscad.models

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.primitives.Primitives
import com.github.grishberg.csg.model.Model as CsgModel
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.utils.Color
import com.github.grishberg.openscad.vrl.CSG
import com.github.grishberg.openscad.vrl.ColorFacetGenerationContext
import com.github.grishberg.openscad.vrl.FacetGenerationContext
import com.github.grishberg.openscad.vrl.Polygon

class Cube(
    val sizeX: Double, val sizeY: Double, val sizeZ: Double
) : Abstract3dModel(CsgModel(Primitives.cube(Triple(sizeX, sizeY, sizeZ), center = true))) {
    constructor(size: Double) : this(size, size, size)
    constructor(dims: com.github.grishberg.openscad.utils.Dims3d) : this(dims.x, dims.y, dims.z)
    override fun cloneModel(): Cube = Cube(sizeX, sizeY, sizeZ)
}

class Cylinder(
    private val _height: Double,
    private val _radius1: Double,
    private val _radius2: Double,
    private val _segments: Int
) : Abstract3dModel(CsgModel(Primitives.cylinder(_height, _radius1, _radius2, center = true, _segments))) {
    constructor(height: Double, r: Double) : this(height, r, r, 32)
    constructor(height: Double, r1: Double, r2: Double) : this(height, r1, r2, 32)
    constructor(height: Double, r: Double, segments: Int) : this(height, r, r, segments)
    override fun cloneModel(): Cylinder = Cylinder(_height, _radius1, _radius2, _segments)
}

class Sphere(
    private val _radius: Double,
    private val _segments: Int
) : Abstract3dModel(CsgModel(Primitives.sphere(_radius, _segments))) {
    constructor(r: Double) : this(r, 32)
    override fun cloneModel(): Sphere = Sphere(_radius, _segments)
}

class Hull @JvmOverloads constructor(vararg models: IModel?) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(list: List<Abstract3dModel>) : this(*list.toTypedArray())
    private val children: List<IModel> = models.filterNotNull()

    override fun toCSG(context: FacetGenerationContext): CSG {
        if (children.isEmpty()) return super.toCSG(context)
        if (children.size == 1) return children[0].toCSG(context)

        val points = HashSet<V3d>()
        for (child in children) {
            for (polygon in child.toCSG(context).polygons) {
                points.addAll(polygon.vertices)
            }
        }

        return generateHull(context, points.toList())
    }

    companion object {
        private fun generateHull(context: FacetGenerationContext, points: List<V3d>): CSG {
            val uniquePoints = HashSet<V3d>(points)
            if (uniquePoints.size < 4) {
                return CSG()
            }

            val hull = com.github.quickhull3d.QuickHull3D(uniquePoints.toList())
            val vertices = hull.getVertices()
            val faces = hull.getFaces()
            val color = (context as? ColorFacetGenerationContext)?.defaultColor ?: Color.GRAY

            val hullPolygons = mutableListOf<Polygon>()

            for (face in faces) {
                if (face.size == 3) {
                    val v0 = vertices[face[0]].toCoords3d()
                    val v1 = vertices[face[1]].toCoords3d()
                    val v2 = vertices[face[2]].toCoords3d()
                    hullPolygons.add(Polygon.fromPolygons(listOf(v0, v1, v2), color))
                } else {
                    val p0 = vertices[face[0]].toCoords3d()
                    val p1 = vertices[face[1]].toCoords3d()
                    val p2 = vertices[face[2]].toCoords3d()
                    val faceNormal = (p1 - p0).cross(p2 - p0).unit()
                    val faceDist = faceNormal.dot(p0)

                    val sp0 = snapToPlane(p0, faceNormal, faceDist)
                    for (i in 1 until face.size - 1) {
                        val vi = snapToPlane(vertices[face[i]].toCoords3d(), faceNormal, faceDist)
                        val vi1 = snapToPlane(vertices[face[i + 1]].toCoords3d(), faceNormal, faceDist)
                        hullPolygons.add(Polygon(listOf(sp0, vi, vi1), faceNormal, color))
                    }
                }
            }

            return CSG().apply { polygons.addAll(hullPolygons) }
        }

        private fun snapToPlane(point: V3d, normal: V3d, dist: Double): V3d {
            val deviation = normal.dot(point) - dist
            if (Math.abs(deviation) > 1e-10) {
                return point + normal.mul(deviation)
            }
            return point
        }
    }

    override fun cloneModel(): Hull = Hull(*children.toTypedArray())
}

class StlModel(
    val path: String,
    val scale: Double
) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(path: String) : this(path, 1.0)
    override fun toCSG(context: com.github.grishberg.openscad.vrl.FacetGenerationContext): com.github.grishberg.openscad.vrl.CSG {
        val result = com.github.grishberg.openscad.vrl.CSG()
        try {
            java.io.File(path).inputStream().use { fis ->
                val dis = java.io.DataInputStream(fis)
                val header = ByteArray(80)
                dis.read(header)
                val numFacets = dis.readInt()
                for (i in 0 until numFacets) {
                    val nx = dis.readFloat().toDouble()
                    val ny = dis.readFloat().toDouble()
                    val nz = dis.readFloat().toDouble()
                    val normal = com.github.grishberg.openscad.coords.V3d(nx, ny, nz)
                    val verts = mutableListOf<com.github.grishberg.openscad.coords.V3d>()
                    for (j in 0..2) {
                        val vx = dis.readFloat().toDouble()
                        val vy = dis.readFloat().toDouble()
                        val vz = dis.readFloat().toDouble()
                        verts.add(com.github.grishberg.openscad.coords.V3d(vx * scale, vy * scale, vz * scale))
                    }
                    result.polygons.add(com.github.grishberg.openscad.vrl.Polygon(verts, normal, color))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
    override fun cloneModel(): StlModel = StlModel(path, scale)
}

class Minkowski(vararg models: IModel) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    constructor(list: List<IModel>) : this(*list.toTypedArray())
    private val children: List<IModel> = models.filterNotNull()

    override fun toCSG(context: FacetGenerationContext): CSG {
        if (children.isEmpty()) return super.toCSG(context)
        if (children.size == 1) return children[0].toCSG(context)
        var combined = children[0].toCSG(context)
        for (i in 1 until children.size) {
            val otherCsg = children[i].toCSG(context)
            combined = combined.union(otherCsg)
        }
        return combined
    }

    override fun cloneModel(): Minkowski = Minkowski(*children.toTypedArray())
}
