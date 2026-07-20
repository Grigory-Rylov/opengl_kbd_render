// Border.kt — внешние бордеры вокруг матрицы

sealed class BorderSide {
    object Top : BorderSide()
    object Bottom : BorderSide()
    object Left : BorderSide()
    object Right : BorderSide()
}

data class BorderSegment(
    val side: BorderSide,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val thickness: Double = 1.5,
)

class BorderBuilder(private val cfg: MatrixConfig) {

    private val totalWidth = (cfg.columns - 1) * cfg.spacingX + cfg.keyConfig.width
    private val totalHeight = (cfg.rows - 1) * cfg.spacingY + cfg.keyConfig.height

    fun buildOuterBorder(): Model {
        val segments = listOf<BorderSegment>(
            // Top
            BorderSegment(
                BorderSide.Top,
                totalWidth / 2.0,
                -cfg.spacingY / 2.0,
                totalWidth + cfg.spacingX,
                cfg.keyConfig.wallThickness,
            ),
            // Bottom
            BorderSegment(
                BorderSide.Bottom,
                totalWidth / 2.0,
                totalHeight + cfg.spacingY / 2.0,
                totalWidth + cfg.spacingX,
                cfg.keyConfig.wallThickness,
            ),
            // Left
            BorderSegment(
                BorderSide.Left,
                -cfg.spacingX / 2.0,
                totalHeight / 2.0,
                cfg.keyConfig.wallThickness,
                totalHeight + cfg.spacingY,
            ),
            // Right
            BorderSegment(
                BorderSide.Right,
                totalWidth + cfg.spacingX / 2.0,
                totalHeight / 2.0,
                cfg.keyConfig.wallThickness,
                totalHeight + cfg.spacingY,
            ),
        )

        return segments.map { seg ->
            val z = if (seg.side == BorderSide.Top) cfg.rowCurvature * 0 else cfg.rowCurvature * cfg.rows / 2.0
            bindings.cube(seg.width, seg.height, cfg.keyConfig.wallHeight)
                .at(seg.x, seg.y, z)
        }.merge()
    }

    fun buildCornerScrews(): Model {
        val screwRadius = 2.0
        val screwLength = cfg.keyConfig.wallHeight + 1.0

        val corners = listOf(
            V3d(-cfg.spacingX / 2.0, -cfg.spacingY / 2.0, 0.0),
            V3d(totalWidth + cfg.spacingX / 2.0, -cfg.spacingY / 2.0, 0.0),
            V3d(-cfg.spacingX / 2.0, totalHeight + cfg.spacingY / 2.0, 0.0),
            V3d(totalWidth + cfg.spacingX / 2.0, totalHeight + cfg.spacingY / 2.0, 0.0),
        )

        return corners.map { v ->
            bindings.cylinder(screwLength, screwRadius).at(v.x, v.y)
        }.merge()
    }
}
