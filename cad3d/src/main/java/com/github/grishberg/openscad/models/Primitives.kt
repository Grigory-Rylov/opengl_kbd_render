package com.github.grishberg.openscad.models

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.primitives.Primitives
import com.github.grishberg.csg.model.Model as CsgModel

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
    override fun cloneModel(): Hull = Hull()
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
    override fun cloneModel(): Minkowski = Minkowski()
}
