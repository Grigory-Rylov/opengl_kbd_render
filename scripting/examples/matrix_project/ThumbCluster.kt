// ThumbCluster.kt — thumb cluster (отдельный блок кнопок с поворотом)

class ThumbClusterBuilder(
    private val thumbCfg: ThumbClusterConfig,
) {
    private val keyBuilder = KeyBuilder(thumbCfg.keyConfig)

    interface ThumbLayout {
        fun positions(): List<Triple<Int, Int, Double>>
    }

    class StandardLayout(private val cols: Int, private val rows: Int) : ThumbLayout {
        override fun positions(): List<Triple<Int, Int, Double>> {
            val result = mutableListOf<Triple<Int, Int, Double>>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    result.add(Triple(c, r, 0.0))
                }
            }
            return result
        }
    }

    class StaggeredLayout(private val cols: Int, private val rows: Int) : ThumbLayout {
        override fun positions(): List<Triple<Int, Int, Double>> {
            val result = mutableListOf<Triple<Int, Int, Double>>()
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val stagger = if (r % 2 == 1) 5.0 else 0.0
                    result.add(Triple(c, r, stagger))
                }
            }
            return result
        }
    }

    fun buildCluster(layout: ThumbLayout): Model {
        val positions = layout.positions()
        val keys = positions.map { (col, row, staggerX) ->
            val key = keyBuilder.buildFullKey()
            val spacing = thumbCfg.keyConfig.width + 1.0
            val x = col * spacing + staggerX
            val y = row * spacing
            key.at(x, y)
        }

        val cluster = keys.merge()

        return cluster
            .at(thumbCfg.offsetX, thumbCfg.offsetY)
            .rotatedY(thumbCfg.rotationY)
            .rotatedZ(thumbCfg.rotationZ)
    }

    fun buildStandardCluster(): Model {
        return buildCluster(StandardLayout(thumbCfg.columns, thumbCfg.rows))
    }
}
