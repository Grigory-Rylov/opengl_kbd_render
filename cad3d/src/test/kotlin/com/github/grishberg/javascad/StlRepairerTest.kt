package com.github.grishberg.javascad

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StlRepairerTest {

    private val validator = StlValidator()
    private val repairer = StlRepairer()

    @Test
    fun `fillHoles should close a bowl mesh`() {
        val allFaces = createCubeFaces()
        val bowl = allFaces.filter { poly ->
            poly.vertices.any { it.z > 9.9 }
        }

        val before = validator.validate(bowl)
        println("Bowl before repair: $before")
        assertTrue(before.openEdges > 0, "Bowl should have open edges before repair")
        assertFalse(before.isManifold, "Bowl should not be manifold before repair")

        val (repaired, result) = repairer.repair(bowl)
        val after = validator.validate(repaired)
        println("Bowl after repair: $after")
        println("Repair result: $result")

        assertTrue(after.isManifold, "Bowl should be manifold after repair: $after")
        assertTrue(after.openEdges == 0, "Bowl should have 0 open edges after repair: $after")
    }

    @Test
    fun `fillHoles should close cube with one missing face`() {
        val allFaces = createCubeFaces()
        val bowl = allFaces.take(10)

        val before = validator.validate(bowl)
        println("Cube-1 before repair: $before")
        assertTrue(before.openEdges > 0)

        val (repaired, result) = repairer.repair(bowl)
        val after = validator.validate(repaired)
        println("Cube-1 after repair: $after")
        println("Repair result: $result")

        assertTrue(after.isManifold, "Should be manifold: $after")
        assertTrue(after.openEdges == 0, "Should have 0 open edges: $after")
    }

    private fun createCubeFaces(): List<Polygon> {
        val v = listOf(
            V3d(0.0, 0.0, 0.0), V3d(10.0, 0.0, 0.0),
            V3d(10.0, 10.0, 0.0), V3d(0.0, 10.0, 0.0),
            V3d(0.0, 0.0, 10.0), V3d(10.0, 0.0, 10.0),
            V3d(10.0, 10.0, 10.0), V3d(0.0, 10.0, 10.0),
        )
        val color = Color.GRAY

        return listOf(
            // bottom (z=0) - facing down, normal is (0,0,-1) but fromPolygons auto-computes
            Polygon.fromPolygons(listOf(v[0], v[1], v[2]), color),
            Polygon.fromPolygons(listOf(v[0], v[2], v[3]), color),
            // top (z=10)
            Polygon.fromPolygons(listOf(v[4], v[6], v[5]), color),
            Polygon.fromPolygons(listOf(v[4], v[7], v[6]), color),
            // front (y=0)
            Polygon.fromPolygons(listOf(v[0], v[4], v[5]), color),
            Polygon.fromPolygons(listOf(v[0], v[5], v[1]), color),
            // back (y=10)
            Polygon.fromPolygons(listOf(v[3], v[2], v[6]), color),
            Polygon.fromPolygons(listOf(v[3], v[6], v[7]), color),
            // left (x=0)
            Polygon.fromPolygons(listOf(v[0], v[3], v[7]), color),
            Polygon.fromPolygons(listOf(v[0], v[7], v[4]), color),
            // right (x=10)
            Polygon.fromPolygons(listOf(v[1], v[5], v[6]), color),
            Polygon.fromPolygons(listOf(v[1], v[6], v[2]), color),
        )
    }
}
