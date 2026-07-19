package com.github.grishberg.cad3d.util

import com.github.grishberg.javascad.coords.Triangle3d
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.utils.Color
import com.github.grishberg.javascad.vrl.Facet
import com.github.grishberg.javascad.vrl.Polygon
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class JsonSerializationTest {
    
    @Test
    fun testV3dJsonSerialization() {
        val original = V3d(1.5, 2.7, 3.9)
        val json = original.toJson()
        val restored = V3d.fromJson(json)
        
        assertEquals(original.x, restored.x, 1e-10)
        assertEquals(original.y, restored.y, 1e-10)
        assertEquals(original.z, restored.z, 1e-10)
    }
    
    @Test
    fun testTriangle3dJsonSerialization() {
        val a = V3d(0.0, 0.0, 0.0)
        val b = V3d(1.0, 0.0, 0.0)
        val c = V3d(0.0, 1.0, 0.0)
        val original = Triangle3d(a, b, c)
        
        val json = original.toJson()
        val restored = Triangle3d.fromJson(json)
        
        val originalPoints = original.points
        val restoredPoints = restored.points
        
        assertEquals(originalPoints[0].x, restoredPoints[0].x, 1e-10)
        assertEquals(originalPoints[0].y, restoredPoints[0].y, 1e-10)
        assertEquals(originalPoints[0].z, restoredPoints[0].z, 1e-10)
        
        assertEquals(originalPoints[1].x, restoredPoints[1].x, 1e-10)
        assertEquals(originalPoints[1].y, restoredPoints[1].y, 1e-10)
        assertEquals(originalPoints[1].z, restoredPoints[1].z, 1e-10)
        
        assertEquals(originalPoints[2].x, restoredPoints[2].x, 1e-10)
        assertEquals(originalPoints[2].y, restoredPoints[2].y, 1e-10)
        assertEquals(originalPoints[2].z, restoredPoints[2].z, 1e-10)
    }
    
    @Test
    fun testFacetJsonSerialization() {
        val triangle = Triangle3d(
            V3d(0.0, 0.0, 0.0),
            V3d(1.0, 0.0, 0.0),
            V3d(0.0, 1.0, 0.0)
        )
        val normal = V3d(0.0, 0.0, 1.0)
        val original = Facet(triangle, normal, Color.RED)
        
        val json = original.toJson()
        val restored = Facet.fromJson(json)
        
        val originalPoints = original.triangle.points
        val restoredPoints = restored.triangle.points
        assertEquals(originalPoints[0].x, restoredPoints[0].x, 1e-10)
        assertEquals(original.normal.x, restored.normal.x, 1e-10)
        assertEquals(original.normal.y, restored.normal.y, 1e-10)
        assertEquals(original.normal.z, restored.normal.z, 1e-10)
    }
    
    @Test
    fun testPolygonJsonSerialization() {
        val vertices = listOf(
            V3d(0.0, 0.0, 0.0),
            V3d(1.0, 0.0, 0.0),
            V3d(1.0, 1.0, 0.0),
            V3d(0.0, 1.0, 0.0)
        )
        val color = Color.BLUE
        val original = Polygon.fromPolygons(vertices, color)
        
        val json = original.toJson()
        val restored = Polygon.fromJson(json, color)
        
        assertEquals(original.vertices.size, restored.vertices.size)
        
        for (i in original.vertices.indices) {
            val origVertex = original.vertices[i]
            val restoredVertex = restored.vertices[i]
            
            assertEquals(origVertex.x, restoredVertex.x, 1e-10)
            assertEquals(origVertex.y, restoredVertex.y, 1e-10)
            assertEquals(origVertex.z, restoredVertex.z, 1e-10)
        }
    }
} 