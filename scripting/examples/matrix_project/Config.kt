// Config.kt — параметры клавиатурной матрицы
// Доступно: bindings, V3d, Abstract3dModel

data class KeyConfig(
    val width: Double = 18.0,
    val height: Double = 18.0,
    val wallThickness: Double = 1.0,
    val wallHeight: Double = 6.0,
    val placeholderWidth: Double = 15.7,
    val placeholderHeight: Double = 15.7,
    val placeholderDepth: Double = 4.0,
) {
    val innerWidth = width - wallThickness * 2
    val innerHeight = height - wallThickness * 2
}

data class MatrixConfig(
    val columns: Int = 6,
    val rows: Int = 3,
    val spacingX: Double = 19.0,
    val spacingY: Double = 19.0,
    val rowCurvature: Double = 2.0,
    val tentingAngle: Double = 3.0,
    val keyConfig: KeyConfig = KeyConfig(),
)

data class ThumbClusterConfig(
    val columns: Int = 3,
    val rows: Int = 2,
    val offsetX: Double = 65.0,
    val offsetY: Double = -45.0,
    val rotationY: Double = -45.0,
    val rotationZ: Double = 18.0,
    val keyConfig: KeyConfig = KeyConfig(width = 16.0, height = 16.0),
)

object KeyboardConfig {
    val matrix = MatrixConfig()
    val thumb = ThumbClusterConfig()
}
