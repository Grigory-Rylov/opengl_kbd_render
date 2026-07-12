package com.github.grishberg.javascad.openscad

class GeometryEvaluator(
    private val autoRepair: Boolean = false
) {
    private val cache = mutableMapOf<Int, Geometry>()
    private var nextCacheId = 0

    fun evaluate(node: CsgNode): Geometry {
        return evaluateInternal(node)
    }

    private fun evaluateInternal(node: CsgNode): Geometry {
        val nodeId = node.hashCode()
        val cached = cache[nodeId]
        if (cached != null) return cached

        val result: Geometry = when (node) {
            is CsgNode.Empty -> PolySetGeometry.empty()

            is CsgNode.Leaf -> {
                if (node.polygons.isEmpty()) {
                    PolySetGeometry.empty()
                } else {
                    PolySetGeometry.fromPolygons(node.polygons)
                }
            }

            is CsgNode.Transform -> {
                val childGeom = evaluateInternal(node.child)
                childGeom.transform(node.matrix)
            }

            is CsgNode.Union -> {
                val children = node.children.map { evaluateInternal(it) }
                val nonEmpty = children.filter { !it.isEmpty() }
                when {
                    nonEmpty.isEmpty() -> PolySetGeometry.empty()
                    nonEmpty.size == 1 -> nonEmpty[0]
                    else -> BspBackend.applyOperator(
                        nonEmpty.filterIsInstance<PolySetGeometry>(),
                        OpenSCADOperator.UNION,
                        autoRepair
                    )
                }
            }

            is CsgNode.Difference -> {
                val left = evaluateInternal(node.left)
                val right = evaluateInternal(node.right)
                when {
                    left.isEmpty() -> PolySetGeometry.empty()
                    right.isEmpty() -> left
                    else -> BspBackend.applyOperator(
                        listOfNotNull(
                            left as? PolySetGeometry,
                            right as? PolySetGeometry
                        ),
                        OpenSCADOperator.DIFFERENCE,
                        autoRepair
                    )
                }
            }

            is CsgNode.Intersection -> {
                val left = evaluateInternal(node.left)
                val right = evaluateInternal(node.right)
                when {
                    left.isEmpty() || right.isEmpty() -> PolySetGeometry.empty()
                    else -> BspBackend.applyOperator(
                        listOfNotNull(
                            left as? PolySetGeometry,
                            right as? PolySetGeometry
                        ),
                        OpenSCADOperator.INTERSECTION,
                        autoRepair
                    )
                }
            }
        }

        cache[nodeId] = result
        return result
    }

    fun clearCache() {
        cache.clear()
    }
}
