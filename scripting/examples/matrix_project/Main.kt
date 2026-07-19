// Main.kt — точка входа: собирает полную модель matrix_right
// Запуск: ./gradlew :scripting:run --args="examples/matrix_project"

fun scriptMain(): Abstract3dModel {
    val matrixCfg = KeyboardConfig.matrix
    val thumbCfg = KeyboardConfig.thumb

    // 1. Строим основную матрицу клавиш
    val keyBuilder = KeyBuilder(matrixCfg.keyConfig)
    val matrixKeys = buildList {
        for (row in 0 until matrixCfg.rows) {
            for (col in 0 until matrixCfg.columns) {
                add(keyBuilder.buildKeyWithCurvature(row, col, matrixCfg))
            }
        }
    }.merge()

    // 2. Соединения между клавишами
    val connections = ConnectionBuilder(matrixCfg).buildAllConnections()

    // 3. Внешние бордеры
    val outerBorder = BorderBuilder(matrixCfg).buildOuterBorder()

    // 4. Крепёжные отверстия
    val screws = BorderBuilder(matrixCfg).buildCornerScrews()

    // 5. Thumb cluster (отдельный блок кнопок)
    val thumbCluster = ThumbClusterBuilder(thumbCfg).buildStandardCluster()

    // 6. Собираем всё в единую модель
    val mainMatrix = matrixKeys
        .addModel(connections)
        .addModel(outerBorder)
        .addModel(screws)

    return mainMatrix.addModel(thumbCluster)
}
