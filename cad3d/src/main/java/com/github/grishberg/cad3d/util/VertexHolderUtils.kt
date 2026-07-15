package com.github.grishberg.cad3d.util

import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.javascad.Triangulator
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon

fun fromModel(model: IModel, color: Color, fn: Int): VertexHolder {
    val context: FacetGenerationContext = ColorFacetGenerationContext(color)
    context.setFn(fn)
    val csg = model.toCSG(context)
    return getVerticesAndColorsAsFloatArray(csg.toFacets())
}

fun fromModel(model: Abstract3dModel, fn: Int): VertexHolder {
    val context: FacetGenerationContext = ColorFacetGenerationContext(model.color)
    context.setFn(fn)
    val csg = model.toCSG(context)
    return getVerticesAndColorsAsFloatArray(csg.toFacets())
}

fun fromModelNative(model: Abstract3dModel, fn: Int): VertexHolder {
    val context: FacetGenerationContext = ColorFacetGenerationContext(model.color)
    context.setFn(fn)
    val mesh = model.toNativeMesh(context)
    return try {
        val nv = Manifold3dEngine.toVertexHolder(mesh, model.color)
        VertexHolder(nv.vertex, nv.normals, nv.verticesCount)
    } finally {
        Manifold3dEngine.delete(mesh)
    }
}

fun fromModelNative(model: Abstract3dModel, color: Color): VertexHolder {
    val mesh = model.toNativeMesh()
    return try {
        val nv = Manifold3dEngine.toVertexHolder(mesh, color)
        VertexHolder(nv.vertex, nv.normals, nv.verticesCount)
    } finally {
        Manifold3dEngine.delete(mesh)
    }
}

fun fromModelNative(model: IModel, color: Color, fn: Int): VertexHolder {
    val context: FacetGenerationContext = ColorFacetGenerationContext(color)
    context.setFn(fn)
    val mesh = model.toNativeMesh(context)
    return try {
        val nv = Manifold3dEngine.toVertexHolder(mesh, color)
        VertexHolder(nv.vertex, nv.normals, nv.verticesCount)
    } finally {
        Manifold3dEngine.delete(mesh)
    }
}

fun fromPolygons(polygons: List<Polygon>, color: Color): VertexHolder {
    val facets: MutableList<Facet> = ArrayList<Facet>()
    for (p in polygons) {
        val triangle3ds = Triangulator.triangulate(p.getVertices(), p.getNormal())
        for (t in triangle3ds) {
            facets.add(Facet(t, p.getNormal(), color))
        }
    }

    return getVerticesAndColorsAsFloatArray(facets)
}

private fun getVerticesAndColorsAsFloatArray(facets: List<Facet>): VertexHolder {
    var verticesCount = 0
    verticesCount = facets.size * 3

    val verticesArray = FloatArray(verticesCount * 7)
    val normalsArray = FloatArray(verticesCount * 3)

    var normalArrayIndex = 0
    var vertexArrayIndex = 0
    for (facet in facets) {
        val facetColor = facet.getColor()
        val normal = facet.getNormal()
        val triangle3d = facet.getTriangle()
        for (vertex in triangle3d.getPoints()) {
            // X, Y, Z,
            // R, G, B, A
            verticesArray[vertexArrayIndex++] = vertex.getX().toFloat()
            verticesArray[vertexArrayIndex++] = vertex.getY().toFloat()
            verticesArray[vertexArrayIndex++] = vertex.getZ().toFloat()
            verticesArray[vertexArrayIndex++] = facetColor.getRed() / 255f
            verticesArray[vertexArrayIndex++] = facetColor.getGreen() / 255f
            verticesArray[vertexArrayIndex++] = facetColor.getBlue() / 255f
            verticesArray[vertexArrayIndex++] = facetColor.getAlpha() / 255f

            normalsArray[normalArrayIndex++] = normal.getX().toFloat()
            normalsArray[normalArrayIndex++] = normal.getY().toFloat()
            normalsArray[normalArrayIndex++] = normal.getZ().toFloat()
        }
    }

    return VertexHolder(verticesArray, normalsArray, verticesCount)
}
