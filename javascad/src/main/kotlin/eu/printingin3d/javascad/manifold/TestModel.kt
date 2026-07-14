package eu.printingin3d.javascad.manifold

import java.io.File

object TestModel {

    @JvmStatic
    fun main(args: Array<String>) {
        val mb = Manifold3dEngine.bindings()

        // Sphere R=10 at origin, Cube 10x10x10 at origin
        // Hull them
        val sphere = mb.sphere(10.0, 64)
        val cube = mb.cube(10.0, 10.0, 10.0, true)
        val hull = mb.batchHull(longArrayOf(sphere, cube))
        println("Hull: ${mb.numTri(hull)} tris")

        // Cylinder R=3.5, H=30 at origin (manifold3d cylinder is along Z axis)
        // 3rd param = taper ratio? Let me try 1.0 for straight cylinder
        val cyl = mb.cylinder(3.5, 1.0, 30.0, 64, 64)
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
