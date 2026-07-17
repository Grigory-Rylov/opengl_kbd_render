package com.github.grishberg.scripting

import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.Empty3dModel
import eu.printingin3d.javascad.models.Prism
import eu.printingin3d.javascad.models.Sphere
import eu.printingin3d.javascad.utils.Color

/**
 * Контекст для DSL-скриптов.
 * Предоставляет примитивы, координаты и CSG-операторы.
 */
class ScriptBindings {

    // Примитивы
    fun cube(size: Double): Abstract3dModel = Cube(size)
    fun cube(x: Double, y: Double, z: Double): Abstract3dModel = Cube(x, y, z)
    fun cylinder(length: Double, radius: Double): Abstract3dModel = Cylinder(length, radius)
    fun cylinder(length: Double, bottomR: Double, topR: Double): Abstract3dModel =
        Cylinder(length, bottomR, topR)
    fun sphere(radius: Double): Abstract3dModel = Sphere(radius)
    fun prism(length: Double, radius: Double, sides: Int): Abstract3dModel =
        Prism(length, radius, sides)
    fun prism(length: Double, r1: Double, r2: Double, sides: Int): Abstract3dModel =
        Prism(length, r1, r2, sides)
    fun emptyModel(): Abstract3dModel = Empty3dModel()
    fun hull(vararg models: Abstract3dModel): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Hull(models.toList())
    fun hull(models: List<Abstract3dModel>): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Hull(models)
    fun union(vararg models: Abstract3dModel): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Union(models.toList())
    fun union(models: List<Abstract3dModel>): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Union(models)

    // Координаты и углы
    fun v3(x: Double, y: Double, z: Double): V3d = V3d(x, y, z)
    fun v3(x: Double, y: Double): V3d = V3d(x, y, 0.0)
    fun angles(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Angles3d =
        Angles3d(x, y, z)

    // CSG-операторы как infix-функции
    infix fun Abstract3dModel.union(other: Abstract3dModel): Abstract3dModel =
        this.addModel(other)
    infix fun Abstract3dModel.minus(other: Abstract3dModel): Abstract3dModel =
        this.subtractModel(other)

    // Цвета
    fun Abstract3dModel.color(r: Int, g: Int, b: Int): Abstract3dModel =
        this.withColor(Color(r, g, b))
    fun Abstract3dModel.color(name: String): Abstract3dModel =
        when (name.lowercase()) {
            "red" -> this.withColor(Color.RED)
            "green" -> this.withColor(Color.GREEN)
            "blue" -> this.withColor(Color.BLUE)
            "gray" -> this.withColor(Color.GRAY)
            "white" -> this.withColor(Color.WHITE)
            "black" -> this.withColor(Color.BLACK)
            else -> this.withColor(Color.GRAY)
        }

    // Повторение
    fun repeat(count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        var result: Abstract3dModel = emptyModel()
        for (i in 0 until count) {
            result = result.addModel(block(i))
        }
        return result
    }

    fun Abstract3dModel.along(axis: Axis, step: Double, count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        var result: Abstract3dModel = this
        for (i in 0 until count) {
            val m = block(i)
            result = result.addModel(
                when (axis) {
                    Axis.X -> m.moveX(i * step)
                    Axis.Y -> m.moveY(i * step)
                    Axis.Z -> m.moveZ(i * step)
                }
            )
        }
        return result
    }

    enum class Axis { X, Y, Z }

    // Математика
    val PI: Double = Math.PI
    val DEG_TO_RAD: Double = Math.PI / 180.0
    fun deg(degrees: Double): Double = degrees * Math.PI / 180.0

    /**
     * Генерация модели matrix_right через cad3d.
     * @param columns количество колонок (default 6)
     * @param rows количество рядов (default 3)
     * @param centerCol центральный столбец для кривизны (default 2)
     * @param centerRow центральный ряд для кривизны (default 1)
     * @param rowCurvature кривизна рядов (default 2.0)
     * @param tentingAngle угол наклона (default 3.0)
     * @param thumbType тип thumb-кластера
     * @param thumbXOffset смещение thumb по X (default -10.0)
     * @param thumbYOffset смещение thumb по Y (default -50.0)
     * @param thumbYRotation поворот thumb по Y (default -30.0)
     * @param thumbZRotation поворот thumb по Z (default 10.0)
     * @param onStep лямбда для отладки: получает название шага
     */
    fun buildMatrixRight(
        columns: Int = 6,
        rows: Int = 3,
        centerCol: Int = 2,
        centerRow: Int = 1,
        rowCurvature: Double = 2.0,
        tentingAngle: Double = 3.0,
        thumbType: com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode =
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn3Buttons,
        thumbXOffset: Double = -10.0,
        thumbYOffset: Double = -50.0,
        thumbYRotation: Double = -30.0,
        thumbZRotation: Double = 10.0,
        onStep: ((String) -> Unit)? = null,
    ): Abstract3dModel {
        val step = onStep ?: { _ -> }

        val cfg = com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig(
            fn = 20,
            stlFn = 60,
            powerSwitcherType = com.github.grishberg.cad3d.plugin.cfg.PowerSwitcherType.None,
            isMagneticWristRestHolder = false,
            bordersOffset = 0.0,
            visibleKeyboardParts = setOf(com.github.grishberg.cad3d.plugin.cfg.KeyboardPart.KeyMatrix),
            modifiedKeyboardParts = emptySet(),
            thumbClusterSettings = com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings(
                xOffset = thumbXOffset,
                yOffset = thumbYOffset,
                zOffset = 37.0,
                rotateY = thumbYRotation,
                rotateZ = thumbZRotation,
                arcRadiusZ = 0.0,
                arcRadiusY = 0.0,
                spaceBetweenKey = 6.5,
                type = thumbType,
            ),
            screwNutHoleDiameter = 4.0,
            screwHolderWallhickness = 1.6,
            isSkeletonMode = false,
            trackball = com.github.grishberg.cad3d.plugin.cfg.TrackballConfig(
                mode = com.github.grishberg.cad3d.plugin.cfg.TrackballMode.None,
                ballDiameter = 39.0,
                bearingDiameter = 10.0,
                controllerScrewDiameter = 3.0,
            ),
            wallsSettings = com.github.grishberg.cad3d.keyboard.cfg.WallsSettings(),
            controllerType = com.github.grishberg.cad3d.plugin.cfg.ControllerType.SuperMiniNRF52840,
            innerBatteryType = com.github.grishberg.cad3d.plugin.cfg.BatteryType.None,
            keyPlaceConfig = com.github.grishberg.cad3d.kbd.core.cfg.KeyPlaceConfig(
                plateZOffset = 0.0,
                rowCurvature = rowCurvature,
                tentingAngle = tentingAngle,
                columnCurvature = 0.0,
                keyswitchHeight = 18.0,
                keyswitchWidth = 18.0,
                extraWidth = 2.5,
                extraHeight = 1.0,
                keyPlaceHolderWidth = 15.7,
                keyPlaceHolderDepth = 15.7,
                keyPlaceHolderHeight = 4.0,
                horizontalExtraSpace = 1.0,
                verticalExtraSpace = 1.0,
                zAngleProvider = com.github.grishberg.cad3d.kbd.core.cfg.KeyZAngleProvider(),
                columnOffsetProvider = com.github.grishberg.cad3d.kbd.core.cfg.KeyOffsetProvider(),
                plateThickness = 1.6,
                saProfileKeyHeight = 17.5,
                columnsCount = columns,
                rowsCount = rows,
                centerCol = centerCol,
                centerRow = centerRow,
                isLowProfile = false,
                isHasHotswap = false,
                keyPlaceholderType = com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType.None,
            ),
        )

        step("KeyPlace")
        val keyPlace = com.github.grishberg.cad3d.keyboard.KeyPlace(cfg.keyPlaceConfig)
        step("ThumbKeyPlace")
        val thumbKeyPlace = com.github.grishberg.cad3d.keyboard.ThumbKeyPlace(cfg)

        step("ThumbBorders")
        val thumbBorders = when (cfg.thumbClusterSettings.type) {
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn3Buttons,
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn4Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.TwoRows5Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.thumb.TwoRows5ButtonsMatrixThumbsBordersBuilder(thumbKeyPlace)
        }

        step("BottomEdgePatcher")
        val topEdgeOffsetZ = -2.0
        val bottomEdgePatcher =
            com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher(
                com.github.grishberg.cad3d.keyboard.cfg.WallsSettings().borderThickness,
                com.github.grishberg.cad3d.keyboard.cfg.WallsSettings().bottomBorderHeight,
            )

        step("FrontRightBuilder")
        val thumbPoints = if (cfg.thumbClusterSettings.type == com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.TwoRows5Buttons) {
            com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbPoints(cfg, keyPlace, thumbKeyPlace)
        } else null

        val frontRightBuilder = when (cfg.thumbClusterSettings.type) {
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn3Buttons,
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn4Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.wall.SingleRow3ButtonsFrontRightToMatrixWallBuilder(
                    cfg, bottomEdgePatcher, topEdgeOffsetZ,
                )
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.TwoRows5Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.wall.TwoRowsButtonsFrontRightToMatrixWallBuilder(
                    cfg, bottomEdgePatcher, topEdgeOffsetZ, thumbPoints!!,
                )
        }

        step("ThumbWalls")
        val thumbWalls = when (cfg.thumbClusterSettings.type) {
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn3Buttons,
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn4Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbWalls(
                    cfg, keyPlace, thumbKeyPlace, frontRightBuilder,
                )
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.TwoRows5Buttons ->
                com.github.grishberg.cad3d.keyboard.casebody.thumb.TwoRows5ButtonsThumbWalls(
                    cfg, keyPlace, thumbKeyPlace, thumbPoints!!, frontRightBuilder,
                )
        }

        step("KeyMatrix")
        val keyMatrix = com.github.grishberg.cad3d.keyboard.matrix.KeyMatrix(cfg, keyPlace, thumbKeyPlace)

        step("Connections")
        val connections = keyMatrix.createConnectionsModel()

        step("Borders")
        val borders = keyMatrix.createBordersModel(null, thumbBorders, thumbWalls)

        step("Placeholders")
        val placeholders = keyMatrix.createPlaceholders()

        step("Union")
        return placeholders.model.addModel(connections.model.addModel(borders.model))
    }
}
