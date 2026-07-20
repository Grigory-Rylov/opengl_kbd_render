package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.V3d

class MeshEdge(val a: V3d, val b: V3d) {
    private val r0 = roundKey(a)
    private val r1 = roundKey(b)
    companion object {
        fun roundKey(v: V3d): LongArray = longArrayOf(
            Math.round(v.x * 1e4).toLong(),
            Math.round(v.y * 1e4).toLong(),
            Math.round(v.z * 1e4).toLong()
        )
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MeshEdge) return false
        val o = other
        return (r0.contentEquals(o.r0) && r1.contentEquals(o.r1)) ||
                (r0.contentEquals(o.r1) && r1.contentEquals(o.r0))
    }
    override fun hashCode(): Int = r0.contentHashCode() * 31 + r1.contentHashCode()
}
