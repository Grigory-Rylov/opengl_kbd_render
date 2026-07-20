// Key.kt — модель одной клавиши: бордеры + placeholder

class KeyBuilder(private val config: KeyConfig) {

    fun buildWall(): Model {
        val outer = bindings.cube(config.width, config.height, config.wallHeight)
        val inner = bindings.cube(config.innerWidth, config.innerHeight, config.wallHeight)
        return outer.subtractModel(inner.moveZ(0.0))
    }

    fun buildPlaceholder(): Model {
        return bindings.cube(
            config.placeholderWidth,
            config.placeholderHeight,
            config.placeholderDepth
        ).moveZ(config.placeholderDepth / 2.0)
    }

    fun buildFullKey(): Model {
        val wall = buildWall()
        val placeholder = buildPlaceholder()
        return wall.addModel(placeholder)
    }

    fun buildKeyWithCurvature(row: Int, col: Int, matrixCfg: MatrixConfig): Model {
        val key = buildFullKey()
        val x = col * matrixCfg.spacingX
        val y = row * matrixCfg.spacingY
        val zCurve = matrixCfg.rowCurvature * row
        return key.at(x, y, zCurve)
    }
}
