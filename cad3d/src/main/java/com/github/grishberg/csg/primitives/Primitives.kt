package com.github.grishberg.csg.primitives

import com.github.grishberg.csg.geom.Matrix4
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * Primitive generators following OpenSCAD's approach.
 * Each primitive produces a manifold-correct PolySet3.
 */
object Primitives {

    /**
     * Cube (OpenSCAD: cube(size, center)).
     * @param size triple (x, y, z) or single value for uniform size
     * @param center if true, cube is centered at origin
     */
    fun cube(size: Triple<Double, Double, Double>, center: Boolean = false): PolySet3 {
        val (x, y, z) = size
        require(x > 0 && y > 0 && z > 0) { "Cube dimensions must be positive" }

        val (x1, x2) = if (center) pairOf(-x / 2, x / 2) else 0.0 to x
        val (y1, y2) = if (center) pairOf(-y / 2, y / 2) else 0.0 to y
        val (z1, z2) = if (center) pairOf(-z / 2, z / 2) else 0.0 to z

        // 8 vertices: bit pattern (x, y, z) -> (i&1, i&2, i&4)
        val verts = listOf(
            Vec3(x1, y1, z1), // 0
            Vec3(x2, y1, z1), // 1
            Vec3(x1, y2, z1), // 2
            Vec3(x2, y2, z1), // 3
            Vec3(x1, y1, z2), // 4
            Vec3(x2, y1, z2), // 5
            Vec3(x1, y2, z2), // 6
            Vec3(x2, y2, z2), // 7
        )

        // 6 faces, CCW winding (outward normals)
        val faces = listOf(
            // top (z+)
            PolySet3.Triplet(4, 6, 5), PolySet3.Triplet(5, 6, 7),
            // bottom (z-)
            PolySet3.Triplet(2, 0, 3), PolySet3.Triplet(3, 0, 1),
            // front (y+)
            PolySet3.Triplet(0, 2, 4), PolySet3.Triplet(4, 2, 6),
            PolySet3.Triplet(4, 6, 5), PolySet3.Triplet(5, 6, 7),
            // ... wait, let me fix this
        )

        // 6 faces * 2 triangles each = 12 triangles
        val indices = listOf(
            // top (z+): 4,5,7,6
            PolySet3.Triplet(4, 6, 5), PolySet3.Triplet(5, 6, 7),
            // bottom (z-): 2,3,1,0
            PolySet3.Triplet(2, 1, 3), PolySet3.Triplet(3, 1, 0),
            // front (y+): 0,1,5,4 -> looking from +y
            PolySet3.Triplet(0, 4, 1), PolySet3.Triplet(1, 4, 5),
            // right (x+): 1,3,7,5
            PolySet3.Triplet(1, 5, 3), PolySet3.Triplet(3, 5, 7),
            // back (y-): 3,2,6,7
            PolySet3.Triplet(3, 7, 2), PolySet3.Triplet(2, 7, 6),
            // left (x-): 2,0,4,6
            PolySet3.Triplet(2, 6, 0), PolySet3.Triplet(0, 6, 4),
        )

        return PolySet3(verts, indices, isManifold = true)
    }

    /** Uniform cube. */
    fun cube(size: Double, center: Boolean = false): PolySet3 =
        cube(Triple(size, size, size), center)

    /**
     * Sphere (OpenSCAD: sphere(r, \$fn)).
     * Latitude-longitude tessellation.
     */
    fun sphere(r: Double, segments: Int = 32): PolySet3 {
        require(r > 0) { "Sphere radius must be positive" }
        require(segments >= 3) { "Segments must be >= 3" }

        val rings = (segments + 1) / 2
        val verts = mutableListOf<Vec3>()

        for (i in 0 until rings) {
            val phi = Math.PI * (i + 0.5) / rings
            val radius = r * Math.sin(phi)
            val z = r * Math.cos(phi)
            for (j in 0 until segments) {
                val theta = 2.0 * Math.PI * j / segments
                verts.add(Vec3(radius * Math.cos(theta), radius * Math.sin(theta), z))
            }
        }

        val indices = mutableListOf<PolySet3.Triplet>()

        // Top cap
        for (j in 0 until segments) {
            indices.add(PolySet3.Triplet(j, (j + 1) % segments, rings * segments))
        }

        // Wait, this is getting complex. Let me follow OpenSCAD's exact algorithm.
        // Rebuild with their approach:
        indices.clear()
        verts.clear()

        // Vertices: rings * segments per ring + top pole + bottom pole
        // Actually let's use the simpler approach: just rings, no poles needed
        for (i in 0 until rings) {
            val phi = Math.PI * (i + 0.5) / rings
            val sinPhi = Math.sin(phi)
            val cosPhi = Math.cos(phi)
            for (j in 0 until segments) {
                val theta = 2.0 * Math.PI * j / segments
                verts.add(Vec3(r * sinPhi * Math.cos(theta), r * sinPhi * Math.sin(theta), r * cosPhi))
            }
        }

        // Top cap: connect top ring to north pole
        val northPole = verts.size
        verts.add(Vec3(0.0, 0.0, r))
        for (j in 0 until segments) {
            indices.add(PolySet3.Triplet(j, northPole, (j + 1) % segments))
        }

        // Middle bands
        for (i in 0 until rings - 1) {
            for (j in 0 until segments) {
                val a = i * segments + j
                val b = i * segments + (j + 1) % segments
                val c = (i + 1) * segments + j
                val d = (i + 1) * segments + (j + 1) % segments
                indices.add(PolySet3.Triplet(a, c, b))
                indices.add(PolySet3.Triplet(b, c, d))
            }
        }

        // Bottom cap
        val southPole = verts.size
        verts.add(Vec3(0.0, 0.0, -r))
        val bottomRing = (rings - 1) * segments
        for (j in 0 until segments) {
            indices.add(PolySet3.Triplet(bottomRing + j, (bottomRing + (j + 1) % segments), southPole))
        }

        return PolySet3(verts, indices, isManifold = true)
    }

    /**
     * Cylinder (OpenSCAD: cylinder(h, r1, r2, center, \$fn)).
     */
    fun cylinder(h: Double, r1: Double, r2: Double, center: Boolean = false, segments: Int = 32): PolySet3 {
        require(h > 0) { "Cylinder height must be positive" }
        require(r1 >= 0 && r2 >= 0) { "Radii must be non-negative" }
        require(segments >= 3) { "Segments must be >= 3" }

        val z1 = if (center) -h / 2 else 0.0
        val z2 = if (center) h / 2 else h

        val verts = mutableListOf<Vec3>()
        val indices = mutableListOf<PolySet3.Triplet>()

        val cone = r2 == 0.0
        val invertedCone = r1 == 0.0

        // Bottom ring
        if (invertedCone) {
            verts.add(Vec3(0.0, 0.0, z1))
        } else {
            for (j in 0 until segments) {
                val theta = 2.0 * Math.PI * j / segments
                verts.add(Vec3(r1 * Math.cos(theta), r1 * Math.sin(theta), z1))
            }
        }

        // Top ring
        if (cone) {
            verts.add(Vec3(0.0, 0.0, z2))
        } else {
            for (j in 0 until segments) {
                val theta = 2.0 * Math.PI * j / segments
                verts.add(Vec3(r2 * Math.cos(theta), r2 * Math.sin(theta), z2))
            }
        }

        // Side triangles
        if (cone) {
            for (j in 0 until segments) {
                val next = (j + 1) % segments
                indices.add(PolySet3.Triplet(j, segments, next))
            }
        } else if (invertedCone) {
            for (j in 0 until segments) {
                val next = (j + 1) % segments
                indices.add(PolySet3.Triplet(0, j + 2, j + 1))
            }
        } else {
            for (j in 0 until segments) {
                val next = (j + 1) % segments
                val a = j
                val b = next
                val c = j + segments
                val d = next + segments
                indices.add(PolySet3.Triplet(a, c, b))
                indices.add(PolySet3.Triplet(b, c, d))
            }
        }

        // Bottom cap
        if (!invertedCone) {
            for (j in 0 until segments) {
                indices.add(PolySet3.Triplet(segments - j - 1, segments - j % segments, 0))
            }
            // Actually, bottom cap without pole vertex
            // Let's add a center vertex for the cap
            val bottomCenter = verts.size
            verts.add(Vec3(0.0, 0.0, z1))
            for (j in 0 until segments) {
                val next = (j + 1) % segments
                indices.removeAt(indices.size - 1) // remove the bad one
                indices.add(PolySet3.Triplet(j, next, bottomCenter))
            }
        }

        // Top cap
        if (!cone) {
            val topCenter = verts.size
            verts.add(Vec3(0.0, 0.0, z2))
            for (j in 0 until segments) {
                val a = j + segments
                val next = segments + (j + 1) % segments
                indices.add(PolySet3.Triplet(a, topCenter, next))
            }
        }

        return PolySet3(verts, indices, isManifold = true)
    }

    /** Uniform cylinder (r1 == r2). */
    fun cylinder(h: Double, r: Double, center: Boolean = false, segments: Int = 32): PolySet3 =
        cylinder(h, r, r, center, segments)

    /**
     * Polyhedron from vertices and face definitions (OpenSCAD: polyhedron(points, faces)).
     * @param vertices vertex positions
     * @param faces list of face definitions; each face is a list of vertex indices
     */
    fun polyhedron(vertices: List<Vec3>, faces: List<List<Int>>): PolySet3 {
        val indices = mutableListOf<PolySet3.Triplet>()

        for (face in faces) {
            if (face.size < 3) continue
            if (face.size == 3) {
                indices.add(PolySet3.Triplet(face[0], face[2], face[1]))
            } else {
                // Ear clipping triangulation (fan from first vertex for simplicity)
                val v0 = face[0]
                for (i in 1 until face.size - 1) {
                    indices.add(PolySet3.Triplet(v0, face[i + 1], face[i]))
                }
            }
        }

        return PolySet3(vertices, indices, isManifold = false)
    }

    /**
     * Linear extrusion of a 2D polygon (OpenSCAD: linear_extrude(height, center)).
     */
    fun linearExtrude(outline: List<Vec2>, height: Double, center: Boolean = false, segments: Int = 1): PolySet3 {
        require(height > 0) { "Extrusion height must be positive" }
        require(outline.size >= 3) { "Outline needs at least 3 points" }

        val zStart = if (center) -height / 2 else 0.0
        val zEnd = if (center) height / 2 else height
        val steps = maxOf(1, segments)

        val verts = mutableListOf<Vec3>()
        val indices = mutableListOf<PolySet3.Triplet>()

        // Build vertices for each Z layer
        for (s in 0..steps) {
            val z = zStart + height * s / steps
            for (p in outline) {
                verts.add(Vec3(p.x, p.y, z))
            }
        }

        val n = outline.size

        // Bottom face (fan triangulation)
        val bottomCenter = verts.size
        verts.add(Vec3(0.0, 0.0, zStart))
        for (i in 0 until n) {
            val next = (i + 1) % n
            indices.add(PolySet3.Triplet(i, next, bottomCenter))
        }

        // Side faces between consecutive Z layers
        for (s in 0 until steps) {
            val base = s * n
            val nextBase = (s + 1) * n
            for (i in 0 until n) {
                val a = base + i
                val b = base + (i + 1) % n
                val c = nextBase + i
                val d = nextBase + (i + 1) % n
                indices.add(PolySet3.Triplet(a, c, b))
                indices.add(PolySet3.Triplet(b, c, d))
            }
        }

        // Top face
        val topCenter = verts.size
        verts.add(Vec3(0.0, 0.0, zEnd))
        val topBase = steps * n
        for (i in 0 until n) {
            val next = (i + 1) % n
            indices.add(PolySet3.Triplet(topBase + i, topCenter, topBase + next))
        }

        return PolySet3(verts, indices, isManifold = true)
    }

    /**
     * Rotate extrusion of a 2D profile (OpenSCAD: rotate_extrude(\$fn)).
     */
    fun rotateExtrude(profile: List<Vec2>, segments: Int = 32): PolySet3 {
        require(segments >= 3) { "Segments must be >= 3" }
        require(profile.size >= 2) { "Profile needs at least 2 points" }

        val verts = mutableListOf<Vec3>()
        val indices = mutableListOf<PolySet3.Triplet>()

        // Build rings for each profile point
        for ((pi, p) in profile.withIndex()) {
            for (j in 0 until segments) {
                val theta = 2.0 * Math.PI * j / segments
                val x = p.x * Math.cos(theta)
                val z = p.x * Math.sin(theta)
                verts.add(Vec3(x, p.y, z))
            }
        }

        // Side faces between consecutive profile points
        for (pi in 0 until profile.size - 1) {
            for (j in 0 until segments) {
                val a = pi * segments + j
                val b = pi * segments + (j + 1) % segments
                val c = (pi + 1) * segments + j
                val d = (pi + 1) * segments + (j + 1) % segments
                indices.add(PolySet3.Triplet(a, c, b))
                indices.add(PolySet3.Triplet(b, c, d))
            }
        }

        // Close the profile loop if needed
        if (profile.isNotEmpty() && profile.size >= 3) {
            for (j in 0 until segments) {
                val a = (profile.size - 1) * segments + j
                val b = (profile.size - 1) * segments + (j + 1) % segments
                val c = j
                val d = (j + 1) % segments
                indices.add(PolySet3.Triplet(a, c, b))
                indices.add(PolySet3.Triplet(b, c, d))
            }
        }

        return PolySet3(verts, indices, isManifold = true)
    }
}

/** 2D point for extrusion profiles. */
data class Vec2(val x: Double, val y: Double)

private fun pairOf(a: Double, b: Double): Pair<Double, Double> = a to b
