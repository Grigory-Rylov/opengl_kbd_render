// Key.kt — модель одной клавиши: бордеры + placeholder

class KeyBuilder(private val config: KeyConfig) {

    fun buildWall(): Abstract3dModel {
        val outer = bindings.cube(config.width, config.height, config.wallHeight)
        val inner = bindings.cube(config.innerWidth, config.innerHeight, config.wallHeight)
        return outer.subtractModel(inner.moveZ(0.0))
    }

    fun buildPlaceholder(): Abstract3dModel {
        return bindings.cube(
            config.placeholderWidth,
            config.placeholderHeight,
            config.placeholderDepth
        ).moveZ(config.placeholderDepth / 2.0)
    }

    fun buildFullKey(): Abstract3dModel {
        val wall = buildWall()
        val placeholder = buildPlaceholder()
        return wall.addModel(placeholder)
    }

    fun buildKeyWithCurvature(row: Int, col: Int, matrixCfg: MatrixConfig): Abstract3dModel {
        val key = buildFullKey()
        val x = col * matrixCfg.spacingX
        val y = row * matrixCfg.spacingY
        val zCurve = matrixCfg.rowCurvature * row
        return key.at(x, y, zCurve)
    }
}
