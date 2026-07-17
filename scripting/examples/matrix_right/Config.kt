package com.github.grishberg.scripting.matrix

// Параметры клавиш и матрицы
object KeyCfg {
    val keyswitchWidth = 18.0
    val keyswitchHeight = 18.0
    val plateThickness = 2.0
    val saProfileKeyHeight = 4.5
    val horizontalExtraSpace = 1.0
    val verticalExtraSpace = 1.0
    val extraHeight = 1.0
    val extraWidth = 2.5
    val keyPlaceHolderWidth = 15.7
    val keyPlaceHolderDepth = 15.7
    val keyPlaceHolderHeight = 4.0
    val plateZOffset = 8.0

    // Кривизна
    val rowCurvature = 20.1
    val columnCurvature = 12.1
    val tentingAngle = 8.0
    val columnsCount = 6
    val rowsCount = 3
    val centerRow = 1
    val centerCol = 2
    val lastCol = columnsCount - 1
    val lastRow = rowsCount - 1

    // Вычисляемые
    val capTopHeight = plateThickness + saProfileKeyHeight
    val mountWidth = keyswitchWidth + horizontalExtraSpace
    val mountHeight = keyswitchHeight + verticalExtraSpace

    val toRad = Math.PI / 180.0

    // Радиусы кривизны
    val columnRadius = ((mountWidth + extraHeight) / 2.0) / Math.sin(toRad * columnCurvature / 2.0) + capTopHeight
    val rowRadius = ((mountHeight + extraWidth) / 2.0) / Math.sin(toRad * rowCurvature / 2.0) + capTopHeight
}

// Z-углы по колонкам (KeyZAngleProvider)
val zAngles = doubleArrayOf(4.0, 2.0, 0.0, -7.0, -13.0, -15.0)

// Смещения по колонкам (KeyOffsetProvider)
val keyOffsets = arrayOf(
    Offset(-6.0, -7.8, 3.0),
    Offset(-2.0, -5.8, 3.0),
    Offset(1.5, 2.82, -3.5),
    Offset(6.0, -2.0, 0.0),
    Offset(9.5, -15.0, 5.64),
    Offset(14.0, -20.0, 5.64),
)

data class Offset(val x: Double, val y: Double, val z: Double)

// Thumb параметры
object ThumbCfg {
    val xOffset = -10.0
    val yOffset = -50.0
    val zOffset = 37.0
    val rotateY = -30.0
    val rotateZ = 10.0
    val spaceBetweenKey = 6.5
    val arcRadiusZ = 0.0
    val arcRadiusY = 0.0
}

// Placeholder константы
object PH {
    val CORNER_OFFSET = 9.25
    val EDGE_HEIGHT = 1.2
    val OUTER_W = 20.0
    val OUTER_H = 20.0
    val KEY_HOLE_OUTER_W = 14.7
    val KEY_HOLE_INNER_W = 14.0
    val KEY_HOLE_H = 14.0
    val TOP_THICKNESS = 4.0
    val KEY_PLACE_TOP_THICKNESS = 3.0
    val BASE_TOP_OFFSET = 2.0
    val VERTICAL_WALL_HEIGHT = 3.2
    val VERTICAL_TOP_OFFSET = BASE_TOP_OFFSET - VERTICAL_WALL_HEIGHT / 2 + 2.6 / 2
    val delta = (TOP_THICKNESS - KEY_PLACE_TOP_THICKNESS) / 2.0
}
