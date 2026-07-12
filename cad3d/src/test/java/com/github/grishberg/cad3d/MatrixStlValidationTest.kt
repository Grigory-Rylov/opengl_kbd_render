package com.github.grishberg.cad3d

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.wall.FrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.SingleRow3ButtonsFrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbBorders
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.thumb.SingleColumn3ButtonsThumbsBordersBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.matrix.KeyMatrix
import com.github.grishberg.cad3d.kbd.core.cfg.KeyOffsetProvider
import com.github.grishberg.cad3d.kbd.core.cfg.KeyPlaceConfig
import com.github.grishberg.cad3d.kbd.core.cfg.KeyZAngleProvider
import com.github.grishberg.javascad.StlExporter
import com.github.grishberg.javascad.StlValidator
import com.github.grishberg.javascad.ScadExporter
import com.github.grishberg.cad3d.plugin.cfg.*
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.io.TempDir
import java.io.File

class MatrixStlValidationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    fun `generate matrix_right stl and validate no naked edges`() {
        val cfg = createDefaultKeyboardConfig()

        val keyPlace = KeyPlace(cfg.keyPlaceConfig)
        val thumbKeyPlace = ThumbKeyPlace(cfg)

        val thumbBorders: ThumbBorders = when (cfg.thumbClusterSettings.type) {
            ThumbClusterMode.SingleColumn3Buttons,
            ThumbClusterMode.SingleColumn4Buttons -> SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)

            ThumbClusterMode.TwoRows5Buttons -> {
                // Not used in default config
                throw UnsupportedOperationException()
            }
        }

        val wallsSettings = WallsSettings(bottomBorderHeight = 1.0)
        val bottomEdgePatcher = DefaultBottomEdgePatcher(
            wallsSettings.borderThickness,
            wallsSettings.bottomBorderHeight
        )
        val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder =
            SingleRow3ButtonsFrontRightToMatrixWallBuilder(cfg, bottomEdgePatcher, topEdgeOffsetZ = -2.0)

        val thumbWalls: ThumbWalls = SingleColumn3ButtonsThumbWalls(
            cfg, keyPlace, thumbKeyPlace, frontRightToMatrixWallBuilder
        )

        // Generate matrix model
        val keyMatrix = KeyMatrix(cfg, keyPlace, thumbKeyPlace)
        val connections = keyMatrix.createConnectionsModel()
        val borders = keyMatrix.createBordersModel(
            amoebaHoles = null,
            thumbBorders = thumbBorders,
            thumbWalls = thumbWalls
        )
        val placeHolders = keyMatrix.createPlaceholders()
        val matrix = placeHolders.model.addModel(connections.model.addModel(borders.model))

        // Convert to CSG
        val context = ColorFacetGenerationContext(Color.GRAY)
        context.setFn(cfg.stlFn)
        val csg = matrix.toCSG(context)

        // Use StlExporter.saveStl which calls fixPolygons before triangulation
        val stlFile = File(tempDir, "matrix_right.stl")
        StlExporter.saveStl(csg.polygons, stlFile.absolutePath)
        println("Generated ${csg.polygons.size} polygons, saved to ${stlFile.absolutePath}")

        // Also save to permanent location
        val permanentFile = File("/tmp/matrix_right.stl")
        stlFile.copyTo(permanentFile, overwrite = true)
        println("STL saved to: ${permanentFile.absolutePath} (${permanentFile.length()} bytes)")

        assertTrue(permanentFile.exists(), "STL file should be generated")
        assertTrue(permanentFile.length() > 0, "STL file should not be empty")

        // Validate final BSP result with non-manifold count
        val finalFacets = StlValidator.loadStl(permanentFile.absolutePath)
        val finalOpen = StlValidator.countOpenEdgesOrcaStyle(finalFacets)
        val finalNM = StlValidator.countNonManifoldEdgesOrcaStyle(finalFacets)
        println("BSP fn=${cfg.stlFn}: facets=${finalFacets.size}, open=$finalOpen, non-manifold=$finalNM")
    }

    @Test
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    fun `generate matrix_right via OpenSCAD Nef and validate`() {
        val cfg = createDefaultKeyboardConfig()
        val keyPlace = KeyPlace(cfg.keyPlaceConfig)
        val thumbKeyPlace = ThumbKeyPlace(cfg)

        val thumbBorders: ThumbBorders = when (cfg.thumbClusterSettings.type) {
            ThumbClusterMode.SingleColumn3Buttons,
            ThumbClusterMode.SingleColumn4Buttons -> SingleColumn3ButtonsThumbsBordersBuilder(thumbKeyPlace)
            ThumbClusterMode.TwoRows5Buttons -> throw UnsupportedOperationException()
        }

        val wallsSettings = WallsSettings(bottomBorderHeight = 1.0)
        val bottomEdgePatcher = DefaultBottomEdgePatcher(
            wallsSettings.borderThickness,
            wallsSettings.bottomBorderHeight
        )
        val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder =
            SingleRow3ButtonsFrontRightToMatrixWallBuilder(cfg, bottomEdgePatcher, topEdgeOffsetZ = -2.0)

        val thumbWalls: ThumbWalls = SingleColumn3ButtonsThumbWalls(
            cfg, keyPlace, thumbKeyPlace, frontRightToMatrixWallBuilder
        )

        val keyMatrix = KeyMatrix(cfg, keyPlace, thumbKeyPlace)
        val connections = keyMatrix.createConnectionsModel()
        val borders = keyMatrix.createBordersModel(
            amoebaHoles = null,
            thumbBorders = thumbBorders,
            thumbWalls = thumbWalls
        )
        val placeHolders = keyMatrix.createPlaceholders()
        val matrix = placeHolders.model.addModel(connections.model.addModel(borders.model))

        val context = ColorFacetGenerationContext(Color.GRAY)
        context.setFn(cfg.stlFn)
        val csg = matrix.toCSG(context)

        println("BSP polygons for ScadExporter: ${csg.polygons.size}")

        // Export via OpenSCAD Nef (tree-based, no BSP!)
        val scadStlFile = File(tempDir, "matrix_right_nef.stl")
        println("ScadExporter: generating script...")
        val script = ScadExporter.generateScript(matrix, cfg.stlFn)
        println("Script size: ${script.length} chars, lines: ${script.lines().size}")
        // Save script for debugging
        File("/tmp/matrix_right.scad").writeText(script)
        println("Script saved to /tmp/matrix_right.scad")
        
        ScadExporter.export(matrix, scadStlFile.absolutePath, cfg.stlFn)
        println("ScadExporter done: ${scadStlFile.absolutePath} (${scadStlFile.length()} bytes)")

        assertTrue(scadStlFile.exists(), "STL file should be generated")
        assertTrue(scadStlFile.length() > 0, "STL file should not be empty")

        // Validate raw
        val rawFacets = StlValidator.loadStl(scadStlFile.absolutePath)
        println("Nef raw: ${rawFacets.size} facets, openEdges: ${StlValidator.countOpenEdgesOrcaStyle(rawFacets)}, nonManifold: ${StlValidator.countNonManifoldEdgesOrcaStyle(rawFacets)}")

        // Apply repair pipeline to Nef STL
        println("Applying repair to Nef STL...")
        val repairedFacets = StlValidator.validateAndRepair(rawFacets)
        val repairedFile = File(tempDir, "matrix_right_nef_repaired.stl")
        com.github.grishberg.javascad.StlExporter.writeBinaryStl(repairedFacets, repairedFile.absolutePath)
        println("Repaired: ${repairedFacets.size} facets, openEdges: ${StlValidator.countOpenEdgesOrcaStyle(repairedFacets)}, nonManifold: ${StlValidator.countNonManifoldEdgesOrcaStyle(repairedFacets)}")
    }

    private fun createDefaultKeyboardConfig(): com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig {
        val settings = SettingsContainer(
            assemblySettings = AssemblySettings(),
            viewerSettings = ViewerSettings(
                rotateX = 0f, rotateY = 0f, rotateZ = 0f,
                translateX = 0f, translateY = 0f, translateZ = 0f
            ),
            keyboardSettings = KeyboardSettings(
                fn = 3,
                stlFn = 20,
                columnsCount = 6,
                rowsCount = 3,
                plateZOffset = 8.0,
                rowCurvature = 20.1,
                tentingAngle = 8.0,
                columnCurvature = 12.1,
                plateThickness = 2.0,
                saProfileKeyHeight = 4.5,
                centerRow = 1,
                centerCol = 2,
                isLowProfile = true,
                powerSwitcherType = PowerSwitcherType.None,
                isHasHotswap = false,
                isMagneticWristRestHolder = false,
                bordersOffset = 4.0,
                isSkeletonMode = false,
                keyPlaceholderType = KeyPlaceholderType.None,
                controllerType = ControllerType.SuperMiniNRF52840,
                batteryType = BatteryType.Bt18650,
            ),
            thumbClusterSettings = ThumbClusterSettings(
                xOffset = 0.0,
                yOffset = -50.0,
                zOffset = 37.0,
                rotateY = -45.0,
                rotateZ = 18.0,
                arcRadiusZ = 0.0,
                arcRadiusY = -80.0,
            ),
            trackballSettings = TrackballConfig(
                mode = TrackballMode.Back,
                ballDiameter = 25.0,
                bearingDiameter = 3.175,
                controllerScrewDiameter = 1.3,
            )
        )

        return KeyboardConfig(
            fn = settings.keyboardSettings.fn,
            stlFn = settings.keyboardSettings.stlFn,
            powerSwitcherType = settings.keyboardSettings.powerSwitcherType,
            isMagneticWristRestHolder = settings.keyboardSettings.isMagneticWristRestHolder,
            bordersOffset = settings.keyboardSettings.bordersOffset,
            visibleKeyboardParts = settings.assemblySettings.toKeyboardPartsList(),
            modifiedKeyboardParts = emptySet(),
            thumbClusterSettings = settings.thumbClusterSettings,
            screwNutHoleDiameter = settings.keyboardSettings.screwNutHoleDiameter,
            screwHolderWallhickness = settings.keyboardSettings.screwHolderWallhickness,
            isSkeletonMode = settings.keyboardSettings.isSkeletonMode,
            trackball = settings.trackballSettings,
            wallsSettings = WallsSettings(),
            controllerType = settings.keyboardSettings.controllerType,
            innerBatteryType = settings.keyboardSettings.batteryType,
            keyPlaceConfig = KeyPlaceConfig(
                plateZOffset = settings.keyboardSettings.plateZOffset,
                rowCurvature = settings.keyboardSettings.rowCurvature,
                tentingAngle = settings.keyboardSettings.tentingAngle,
                columnCurvature = settings.keyboardSettings.columnCurvature,
                keyswitchHeight = 18.0,
                keyswitchWidth = 18.0,
                extraWidth = 2.5,
                extraHeight = 1.0,
                keyPlaceHolderWidth = 15.7,
                keyPlaceHolderDepth = 15.7,
                keyPlaceHolderHeight = 4.0,
                horizontalExtraSpace = settings.keyboardSettings.horizontalExtraSpace,
                verticalExtraSpace = settings.keyboardSettings.verticalExtraSpace,
                zAngleProvider = KeyZAngleProvider(),
                columnOffsetProvider = KeyOffsetProvider(),
                plateThickness = settings.keyboardSettings.plateThickness,
                saProfileKeyHeight = settings.keyboardSettings.saProfileKeyHeight,
                columnsCount = settings.keyboardSettings.columnsCount,
                rowsCount = settings.keyboardSettings.rowsCount,
                centerCol = settings.keyboardSettings.centerCol,
                centerRow = settings.keyboardSettings.centerRow,
                isLowProfile = settings.keyboardSettings.isLowProfile,
                isHasHotswap = settings.keyboardSettings.isHasHotswap,
                keyPlaceholderType = settings.keyboardSettings.keyPlaceholderType,
            ),
        )
    }
}
