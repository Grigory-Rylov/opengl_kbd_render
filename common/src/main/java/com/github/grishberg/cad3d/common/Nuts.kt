package com.github.grishberg.cad3d.common

import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.manifold.Manifold3dEngine
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.StlModel
import com.github.grishberg.javascad.vrl.FacetGenerationContext

class Nuts {
    private val map = mapOf<MetricType, NutsParams>(
        MetricType.M2 to NutsParams(1.6, 4.0, 4.38),
        MetricType.M2 to NutsParams(2.4, 5.5, 6.08),
        MetricType.M2 to NutsParams(3.2, 7.0, 7.74),
        MetricType.M2 to NutsParams(4.0, 8.0, 8.87),
    )

    fun createNutHole(m: MetricType, height: Number): Abstract3dModel {
        val params = map[m]!!
        val context = FacetGenerationContext.DEFAULT
        context.setFn(6)
        val mesh = Cylinder(height.toDouble(), Radius.fromDiameter(params.e)).toNativeMesh(context)
        val polygons = Manifold3dEngine.manifoldToPolygonsExport(mesh)
        return StlModel(polygons)
    }

    private data class NutsParams(val m: Double, val s: Double, val e: Double)
}
