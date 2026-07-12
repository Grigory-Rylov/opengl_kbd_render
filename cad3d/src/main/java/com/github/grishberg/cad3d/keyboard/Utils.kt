package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.openscad.basic.Radius
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.Cube
import com.github.grishberg.openscad.models.Cylinder
import com.github.grishberg.openscad.models.Hull
import com.github.grishberg.openscad.models.Sphere
import com.github.grishberg.openscad.tranzitions.Union

object Utils {

    @JvmStatic
    fun union(models: List<Abstract3dModel>): Abstract3dModel {
        return Union(models)
    }

    @JvmStatic
    fun union(vararg models: Abstract3dModel): Abstract3dModel {
        return Union(*models)
    }

    @JvmStatic
    fun hull(vararg models: Abstract3dModel): Abstract3dModel {
        return Hull(*models)
    }

    @JvmStatic
    fun hull(models: List<Abstract3dModel>): Abstract3dModel {
        return Hull(models)
    }

    @JvmStatic
    fun cube(x: Number, y: Number, z: Number): Cube {
        return Cube(x.toDouble(), y.toDouble(), z.toDouble())
    }

    @JvmStatic
    fun cube(size: Double): Cube {
        return Cube(size)
    }

    @JvmStatic
    fun cylinder(radius: Number, height: Number): Cylinder {
        return Cylinder(height.toDouble(), Radius.fromRadius(radius.toDouble()).value)
    }

    @JvmStatic
    fun sphere(radius: Number): Sphere {
        return Sphere(Radius.fromRadius(radius.toDouble()).value)
    }

    @JvmStatic
    fun v3d(x: Double, y: Double, z: Double): V3d {
        return V3d(x, y, z)
    }

    @JvmStatic
    fun v3d(x: Number, y: Number, z: Number): V3d {
        return V3d(x.toDouble(), y.toDouble(), z.toDouble())
    }
}
