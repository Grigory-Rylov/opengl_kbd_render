package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.V3d

/**
 * Factory for generating primitive meshes using the Half-edge data structure.
 */
object PrimitiveFactory {

    fun cube(w: Double, h: Double, d: Double): Mesh {
        val builder = MeshBuilder()
        val hw = w / 2.0
        val hh = h / 2.0
        val hd = d / 2.0

        // Front face
        builder.addFace(listOf(
            V3d(-hw, -hh, hd), V3d(hw, -hh, hd), V3d(hw, hh, hd), V3d(-hw, hh, hd)
        ))
        // Back face
        builder.addFace(listOf(
            V3d(hw, -hh, -hd), V3d(-hw, -hh, -hd), V3d(-hw, hh, -hd), V3d(hw, hh, -hd)
        ))
        // Left face
        builder.addFace(listOf(
            V3d(-hw, -hh, -hd), V3d(-hw, -hh, hd), V3d(-hw, hh, hd), V3d(-hw, hh, -hd)
        ))
        // Right face
        builder.addFace(listOf(
            V3d(hw, -hh, hd), V3d(hw, -hh, -hd), V3d(hw, hh, -hd), V3d(hw, hh, hd)
        ))
        // Bottom face
        builder.addFace(listOf(
            V3d(-hw, -hh, -hd), V3d(hw, -hh, -hd), V3d(hw, -hh, hd), V3d(-hw, -hh, hd)
        ))
        // Top face
        builder.addFace(listOf(
            V3d(-hw, hh, hd), V3d(hw, hh, hd), V3d(hw, hh, -hd), V3d(-hw, hh, -hd)
        ))

        return builder.build()
    }

    fun sphere(radius: Double, slices: Int = 32, stacks: Int = 16): Mesh {
        val builder = MeshBuilder()

        for (i in 0 until stacks) {
            val theta1 = Math.PI * (i / stacks.toDouble())
            val theta2 = Math.PI * ((i + 1) / stacks.toDouble())

            val r1 = radius * Math.sin(theta1)
            val y1 = radius * Math.cos(theta1)
            val r2 = radius * Math.sin(theta2)
            val y2 = radius * Math.cos(theta2)

            val ring1 = mutableListOf<V3d>()
            val ring2 = mutableListOf<V3d>()

            for (j in 0 until slices) {
                val phi = 2.0 * Math.PI * (j / slices.toDouble())
                ring1.add(V3d(r1 * Math.cos(phi), y1, r1 * Math.sin(phi)))
                ring2.add(V3d(r2 * Math.cos(phi), y2, r2 * Math.sin(phi)))
            }

            if (r1 > 1e-10 && r2 > 1e-10) {
                for (j in 0 until slices) {
                    val nextJ = (j + 1) % slices
                    builder.addFace(listOf(
                        ring1[j], ring1[nextJ], ring2[nextJ], ring2[j]
                    ))
                }
            }
        }

        return builder.build()
    }

    fun cylinder(height: Double, radiusLow: Double, radiusHigh: Double, segments: Int = 32): Mesh {
        val builder = MeshBuilder()
        val halfH = height / 2.0

        val bottomRing = mutableListOf<V3d>()
        val topRing = mutableListOf<V3d>()

        for (i in 0 until segments) {
            val theta = 2.0 * Math.PI * (i / segments.toDouble())
            bottomRing.add(V3d(radiusLow * Math.cos(theta), -halfH, radiusLow * Math.sin(theta)))
            topRing.add(V3d(radiusHigh * Math.cos(theta), halfH, radiusHigh * Math.sin(theta)))
        }

        // Side faces
        for (i in 0 until segments) {
            val nextI = (i + 1) % segments
            builder.addFace(listOf(
                bottomRing[i], bottomRing[nextI], topRing[nextI], topRing[i]
            ))
        }

        // Bottom cap
        builder.addFace(bottomRing)

        // Top cap
        builder.addFace(topRing)

        return builder.build()
    }

    fun prism(height: Double, r1: Double, r2: Double, sides: Int): Mesh {
        return cylinder(height, r1, r2, sides)
    }
}
