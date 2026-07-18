package com.github.grishberg.scripting

import com.github.grishberg.javascad.StlImporter
import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.Empty3dModel
import eu.printingin3d.javascad.models.Prism
import eu.printingin3d.javascad.models.Sphere
import eu.printingin3d.javascad.models.StlModel
import eu.printingin3d.javascad.utils.Color

/**
 * Контекст для DSL-скриптов.
 * Предоставляет примитивы, координаты и CSG-операторы.
 */
class ScriptBindings {

    // Примитивы
    fun cube(size: Number): Abstract3dModel = Cube(size.toDouble())
    fun cube(x: Number, y: Number, z: Number): Abstract3dModel =
        Cube(x.toDouble(), y.toDouble(), z.toDouble())
    fun cylinder(length: Number, radius: Number, fn: Int? = null): Abstract3dModel =
        if (fn != null) Prism(length.toDouble(), radius.toDouble(), fn)
        else Cylinder(length.toDouble(), radius.toDouble())
    fun cylinder(length: Number, bottomR: Number, topR: Number, fn: Int? = null): Abstract3dModel =
        if (fn != null) Prism(length.toDouble(), bottomR.toDouble(), topR.toDouble(), fn)
        else Cylinder(length.toDouble(), bottomR.toDouble(), topR.toDouble())
    fun cylinderD(d: Number, h: Number, fn: Int? = null): Abstract3dModel =
        cylinder(h, d.toDouble() / 2.0, fn)
    fun cylinderR(r: Number, h: Number, fn: Int? = null): Abstract3dModel =
        cylinder(h, r, fn)
    fun sphere(radius: Number): Abstract3dModel = Sphere(radius.toDouble())
    fun prism(length: Number, radius: Number, sides: Int): Abstract3dModel =
        Prism(length.toDouble(), radius.toDouble(), sides)
    fun prism(length: Number, r1: Number, r2: Number, sides: Int): Abstract3dModel =
        Prism(length.toDouble(), r1.toDouble(), r2.toDouble(), sides)
    fun emptyModel(): Abstract3dModel = Empty3dModel()

    /**
     * Loads a binary STL file and returns it as an [Abstract3dModel].
     * @param path file path; if not absolute, resolved relative to the project root
     *             (user.dir and its parents).
     * @param color optional color name ("red", "green", ...) or hex like "#rrggbb".
     */
    fun importStl(path: String, color: String? = null): Abstract3dModel {
        val file = resolveFileLoop(path)
        if (!file.exists()) {
            throw IllegalArgumentException("STL file not found: ${file.absolutePath}")
        }
        val c = color?.let { parseColor(it) } ?: Color.GRAY
        val polygons = StlImporter().loadBinarySTL(file.absolutePath, c)
        return StlModel(polygons)
    }
    fun hull(vararg models: Abstract3dModel): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Hull(models.toList())
    fun hull(models: List<Abstract3dModel>): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Hull(models)
    fun union(vararg models: Abstract3dModel): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Union(models.toList())
    fun union(models: List<Abstract3dModel>): Abstract3dModel =
        eu.printingin3d.javascad.tranzitions.Union(models)

    // Координаты и углы
    fun v3(x: Number, y: Number, z: Number): V3d = V3d(x.toDouble(), y.toDouble(), z.toDouble())
    fun v3(x: Number, y: Number): V3d = V3d(x.toDouble(), y.toDouble(), 0.0)
    fun angles(x: Number = 0.0, y: Number = 0.0, z: Number = 0.0): Angles3d =
        Angles3d(x.toDouble(), y.toDouble(), z.toDouble())

    // CSG-операторы как infix-функции
    infix fun Abstract3dModel.union(other: Abstract3dModel): Abstract3dModel =
        this.addModel(other)
    infix fun Abstract3dModel.minus(other: Abstract3dModel): Abstract3dModel =
        this.subtractModel(other)

    // Цвета
    fun Abstract3dModel.color(r: Int, g: Int, b: Int): Abstract3dModel =
        this.withColor(Color(r, g, b))
    fun Abstract3dModel.color(name: String): Abstract3dModel =
        this.withColor(parseColor(name))

    private fun parseColor(name: String): Color = when (name.lowercase()) {
        "red" -> Color.RED
        "green" -> Color.GREEN
        "blue" -> Color.BLUE
        "gray", "grey" -> Color.GRAY
        "white" -> Color.WHITE
        "black" -> Color.BLACK
        "yellow" -> Color.YELLOW
        "cyan" -> Color.CYAN
        "magenta" -> Color.MAGENTA
        "orange" -> Color.ORANGE
        "pink" -> Color.PINK
        else -> if (name.startsWith("#") && name.length == 7) {
            val r = name.substring(1, 3).toInt(16)
            val g = name.substring(3, 5).toInt(16)
            val b = name.substring(5, 7).toInt(16)
            Color(r, g, b)
        } else Color.GRAY
    }

    private fun resolveFileLoop(path: String): java.io.File {
        val f = java.io.File(path)
        if (f.isAbsolute && f.exists()) return f
        var dir = java.io.File(System.getProperty("user.dir"))
        var result: java.io.File? = null
        var remaining = 8
        while (result == null && remaining-- > 0 && dir != null) {
            val candidate = java.io.File(dir, path)
            if (candidate.exists()) result = candidate
            else dir = dir.parentFile
        }
        return result ?: f
    }

    // Повторение
    fun repeat(count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        var result: Abstract3dModel = emptyModel()
        for (i in 0 until count) {
            result = result.addModel(block(i))
        }
        return result
    }

    fun Abstract3dModel.along(axis: Axis, step: Number, count: Int, block: (Int) -> Abstract3dModel): Abstract3dModel {
        val stepD = step.toDouble()
        var result: Abstract3dModel = this
        for (i in 0 until count) {
            val m = block(i)
            result = result.addModel(
                when (axis) {
                    Axis.X -> m.moveX(i * stepD)
                    Axis.Y -> m.moveY(i * stepD)
                    Axis.Z -> m.moveZ(i * stepD)
                }
            )
        }
        return result
    }

    enum class Axis { X, Y, Z }

    // Математика
    val PI: Double = Math.PI
    val DEG_TO_RAD: Double = Math.PI / 180.0
    fun deg(degrees: Number): Double = degrees.toDouble() * Math.PI / 180.0

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
        rowCurvature: Number = 2.0,
        tentingAngle: Number = 3.0,
        thumbType: com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode =
            com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode.SingleColumn3Buttons,
        thumbXOffset: Number = -10.0,
        thumbYOffset: Number = -50.0,
        thumbYRotation: Number = -30.0,
        thumbZRotation: Number = 10.0,
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
                xOffset = thumbXOffset.toDouble(),
                yOffset = thumbYOffset.toDouble(),
                zOffset = 37.0,
                rotateY = thumbYRotation.toDouble(),
                rotateZ = thumbZRotation.toDouble(),
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
                rowCurvature = rowCurvature.toDouble(),
                tentingAngle = tentingAngle.toDouble(),
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
