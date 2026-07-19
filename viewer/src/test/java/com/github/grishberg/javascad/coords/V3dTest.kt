package com.github.grishberg.javascad.coords

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import org.junit.jupiter.api.Test

class V3dTest {

    @Test
    fun testEquals() {
        val a = V3d(76.51101271115557, -51.79415715919706, 23.72429834360106)
        val b = V3d(76.5110128, -51.79415715019706, 23.724298340)

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testNonEquals() {
        val a = V3d(76.51101271115557, -51.79415715919706, 23.72429834360106)
        val b = V3d(76.511018, -51.7941571501976, 23.72429831)

        assertNotEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun testNonEquals2() {
        val a = V3d(76.51101271115557, -51.79415715919706, 23.72429834360106)
        val b = V3d(77.511018, -52.7941571501976, 25.72429831)

        assertNotEquals(a.hashCode(), b.hashCode())
    }
}
