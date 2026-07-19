package com.github.grishberg.javascad.manifold

import java.io.File

object TestModel {

    @JvmStatic
    fun main(args: Array<String>) {
        val mb = Manifold3dEngine.bindings()

        // Hull: сфера R=10 в origin + куб 10x10x10 смещён вправо на 20
        val sphere = mb.sphere(10.0, 64)
        val cube = mb.translate(mb.cube(10.0, 10.0, 10.0, true), 20.0, 0.0, 0.0)
        val hull = mb.batchHull(longArrayOf(sphere, cube))
        println("Hull: ${mb.numTri(hull)} tris")

        // Cylinder: height=30, radiusLow=3.5, radiusHigh=3.5, segments=64, center=1
        val cyl = mb.cylinder(30.0, 3.5, 3.5, 64, 1)
        println("Cylinder: ${mb.numTri(cyl)} tris")

        // Difference: hull - cylinder
        val result = mb.difference(hull, cyl)
        println("Result: ${mb.numTri(result)} tris")
        val resultPolys = Manifold3dEngine.manifoldToPolygonsExport(result)
        println("Result polys: ${resultPolys.size}")
        Manifold3dEngine.writeStl(resultPolys, File("test_part.stl"))
        println("Exported test_part.stl (${File("test_part.stl").length()} bytes)")
    }
}
