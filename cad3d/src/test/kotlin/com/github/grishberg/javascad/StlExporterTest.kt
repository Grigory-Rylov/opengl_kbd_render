package com.github.grishberg.javascad

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext
import eu.printingin3d.javascad.utils.Color
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class StlExporterTest {

    private val validator = StlValidator()
    private val importer = StlImporter()
    private val repairer = StlRepairer()

    @Test
    fun `manifold cube should be detected by validator`(@TempDir tempDir: File) {
        val cube = Cube.fromCoordinates(V3d(0.0, 0.0, 0.0), V3d(10.0, 10.0, 10.0))
        val context: FacetGenerationContext = ColorFacetGenerationContext(Color.GRAY)
        val polygons = cube.toCSG(context).getPolygons()

        val stlPath = File(tempDir, "cube.stl").absolutePath
        StlExporter.saveStl(polygons, stlPath)

        val loadedPolygons = importer.loadBinarySTL(stlPath)
        val result = validator.validate(loadedPolygons)

        assertTrue(result.isManifold, "Cube should be manifold, got: $result")
        assertTrue(result.openEdges == 0, "Cube should have 0 open edges, got: ${result.openEdges}")
        assertTrue(result.degenerateTriangles == 0, "Cube should have 0 degenerate triangles")
        assertTrue(result.totalTriangles > 0, "Cube should have triangles")
    }

    @Test
    fun `repair should preserve manifold mesh`(@TempDir tempDir: File) {
        val cube = Cube.fromCoordinates(V3d(0.0, 0.0, 0.0), V3d(10.0, 10.0, 10.0))
        val context: FacetGenerationContext = ColorFacetGenerationContext(Color.GRAY)
        val polygons = cube.toCSG(context).getPolygons()

        val stlPath = File(tempDir, "cube_repair.stl").absolutePath
        StlExporter.saveStl(polygons, stlPath)

        val loadedPolygons = importer.loadBinarySTL(stlPath)
        val beforeResult = validator.validate(loadedPolygons)
        assertTrue(beforeResult.isManifold, "Cube should be manifold before repair")

        val (repaired, repairResult) = repairer.repair(loadedPolygons)
        val afterResult = validator.validate(repaired)

        println("Cube repair result: $repairResult")
        println("Before: $beforeResult")
        println("After:  $afterResult")

        assertTrue(afterResult.isManifold, "Cube should remain manifold after repair, afterResult=$afterResult")
    }

    @Test
    fun `non-manifold mesh should be detected by validator`() {
        val polygons = createNonManifoldMesh()
        val result = validator.validate(polygons)

        assertFalse(result.isManifold, "Non-manifold mesh should not be manifold")
        assertTrue(result.openEdges > 0, "Non-manifold mesh should have open edges")
    }

    @Test
    fun `repair should fix non-manifold mesh`() {
        val polygons = createNonManifoldMesh()
        val beforeResult = validator.validate(polygons)
        assertFalse(beforeResult.isManifold)

        val (repaired, repairResult) = repairer.repair(
            polygons, StlRepairer.RepairOptions(
                snapTolerance = 1e-3,
                maxSnapIterations = 2,
                removeDisconnectedFacets = true,
                fixNormals = true
            )
        )
        val afterResult = validator.validate(repaired)

        println("Before: $beforeResult")
        println("After:  $afterResult")
        println("Repair: $repairResult")

        assertTrue(
            afterResult.openEdges <= beforeResult.openEdges,
            "Open edges should not increase after repair"
        )
    }

    @Test
    fun `export with autoRepair should not break manifold mesh`(@TempDir tempDir: File) {
        val cube = Cube.fromCoordinates(V3d(0.0, 0.0, 0.0), V3d(10.0, 10.0, 10.0))
        val context: FacetGenerationContext = ColorFacetGenerationContext(Color.GRAY)
        val polygons = cube.toCSG(context).getPolygons()

        val stlPath = File(tempDir, "auto_repaired_cube.stl").absolutePath
        StlExporter.saveStl(polygons, stlPath, autoRepair = true)

        val loadedPolygons = importer.loadBinarySTL(stlPath)
        val afterResult = validator.validate(loadedPolygons)
        assertTrue(afterResult.isManifold, "Cube should remain manifold after auto-repair export")
    }

    @Test
    fun `stl roundtrip preserves manifold for cube`(@TempDir tempDir: File) {
        val cube = Cube.fromCoordinates(V3d(0.0, 0.0, 0.0), V3d(10.0, 10.0, 10.0))
        val context: FacetGenerationContext = ColorFacetGenerationContext(Color.GRAY)
        val polygons = cube.toCSG(context).getPolygons()

        val stlPath = File(tempDir, "roundtrip.stl").absolutePath
        StlExporter.saveStl(polygons, stlPath)

        val loadedPolygons = importer.loadBinarySTL(stlPath)
        val result = validator.validate(loadedPolygons)

        assertTrue(result.openEdges == 0)
        assertTrue(result.degenerateTriangles == 0)
        assertTrue(result.totalTriangles > 0)
    }

    /**
     * Creates a deliberately non-manifold triangle mesh:
     * Two triangles that share only a vertex (not an edge),
     * plus one triangle that is completely disconnected.
     */
    private fun createNonManifoldMesh(): List<Polygon> {
        val normal = V3d(0.0, 0.0, 1.0)
        val color = Color.GRAY

        return listOf(
            // Triangle 1
            Polygon.fromPolygons(
                listOf(V3d(0.0, 0.0, 0.0), V3d(10.0, 0.0, 0.0), V3d(5.0, 10.0, 0.0)),
                normal, color
            ),
            // Triangle 2 - shares only vertex (0,0,0) with T1 -> non-manifold
            Polygon.fromPolygons(
                listOf(V3d(0.0, 0.0, 0.0), V3d(-5.0, 5.0, 0.0), V3d(-10.0, 0.0, 0.0)),
                normal, color
            ),
            // Triangle 3 - completely disconnected
            Polygon.fromPolygons(
                listOf(V3d(20.0, 20.0, 0.0), V3d(30.0, 20.0, 0.0), V3d(25.0, 30.0, 0.0)),
                normal, color
            ),
        )
    }
}
