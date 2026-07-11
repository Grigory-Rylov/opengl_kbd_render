package com.github.grishberg.javascad

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.utils.Color
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue

class MatrixRightStlTest {

    @Test
    fun `generate matrix_right stl for vk upload`(@TempDir tempDir: File) {
        val cube = Cube.fromCoordinates(V3d(0.0, 0.0, 0.0), V3d(10.0, 10.0, 10.0))
        val context: FacetGenerationContext = ColorFacetGenerationContext(Color.GRAY)
        val polygons = cube.toCSG(context).getPolygons()

        val stlDir = File("stl")
        stlDir.mkdirs()
        val stlPath = File(stlDir, "matrix_right.stl").absolutePath

        StlExporter.saveStl(polygons, stlPath, autoRepair = true)

        val result = File(stlPath)
        assertTrue(result.exists(), "STL file should exist")
        assertTrue(result.length() > 0, "STL file should not be empty")
        println("Generated: ${result.absolutePath} (${result.length()} bytes)")
    }
}
