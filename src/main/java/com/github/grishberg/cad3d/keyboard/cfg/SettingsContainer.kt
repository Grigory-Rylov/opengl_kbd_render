package com.github.grishberg.cad3d.keyboard.cfg

import kotlinx.serialization.Serializable

@Serializable
data class ViewerSettings(
    val rotateX: Float,
    val rotateY: Float,
    val rotateZ: Float,
    val translateX: Float,
    val translateY: Float,
    val translateZ: Float,
)

@Serializable
data class AssemblySettings(
    val settingsShowCaps: Boolean = true,
    val settingsShowCase: Boolean = true,
    val settingsShowMatrix: Boolean = true,
    val settingsShowPlate: Boolean = true,
    val settingsShowWristRest: Boolean = false,
    val settingsTrackball: Boolean = false,
    val showController: Boolean = true,
    val showTrackballSensor: Boolean = false,
    val showTrackbalSensorCap: Boolean = false,
    val showTrackbalBall: Boolean = false,
    val showControllerHolder: Boolean = false,
    val showAmoeba: Boolean = false,
)

@Serializable
data class ThumbClusterSettings(
    val xOffset: Double = -10.0,
    val yOffset: Double = -50.0,
    val zOffset: Double = 40.0,
    val rotateY: Double = -30.0,
    val rotateZ: Double = 10.0,
    val arcRadiusZ: Double = 0.0,
    val arcRadiusY: Double = 0.0,
    val spaceBetweenKey:Double = 5.3,
    val type: ThumbClusterMode = ThumbClusterMode.SingleColumn3Buttons,
)

@Serializable
data class KeyboardSettings(
    val fn: Int,
    val stlFn: Int,
    val plateZOffset: Double,
    val rowCurvature: Double,
    val tentingAngle: Double,
    val columnCurvature: Double,
    val plateThickness: Double,
    val saProfileKeyHeight: Double,
    val columnsCount: Int,
    val rowsCount: Int,
    val centerRow: Int,
    val centerCol: Int,
    val isLowProfile: Boolean,
    val powerSwitcherType: PowerSwitcherType,
    val isHasHotswap: Boolean,
    val isMagneticWristRestHolder: Boolean,
    val bordersOffset: Double,
    val screwNutHoleDiameter: Double = 4.0,
    val screwHolderWallhickness: Double = 1.6,
    val isSkeletonMode: Boolean,
    val keyPlaceholderType: KeyPlaceholderType,
)

@Serializable
data class SettingsContainer(
    val assemblySettings: AssemblySettings,
    val viewerSettings: ViewerSettings,
    val keyboardSettings: KeyboardSettings,
    val thumbClusterSettings: ThumbClusterSettings,
    val trackballSettings: TrackballConfig,
) {

    fun getKeyboardConfig(): KeyboardConfig = KeyboardConfig(
        fn = keyboardSettings.fn,
        stlFn = keyboardSettings.stlFn,
        plateZOffset = keyboardSettings.plateZOffset,
        rowCurvature = keyboardSettings.rowCurvature,
        tentingAngle = keyboardSettings.tentingAngle,
        columnCurvature = keyboardSettings.columnCurvature,
        keyswitchHeight = 14.2,
        keyswitchWidth = 14.2,
        extraWidth = 2.5,
        extraHeight = 1.0,
        keyPlaceHolderWidth = 15.7,
        keyPlaceHolderDepth = 15.7,
        keyPlaceHolderHeight = 4.0,
        zAngleProvider = KeyZAngleProvider(),
        columnOffsetProvider = KeyOffsetProvider(),
        plateThickness = keyboardSettings.plateThickness,
        saProfileKeyHeight = keyboardSettings.saProfileKeyHeight,
        columnsCount = keyboardSettings.columnsCount,
        rowsCount = keyboardSettings.rowsCount,
        centerCol = keyboardSettings.centerCol,
        centerRow = keyboardSettings.centerRow,
        isLowProfile = keyboardSettings.isLowProfile,
        powerSwitcherType = keyboardSettings.powerSwitcherType,
        isHasHotswap = keyboardSettings.isHasHotswap,
        isMagneticWristRestHolder = keyboardSettings.isMagneticWristRestHolder,
        bordersOffset = keyboardSettings.bordersOffset,
        assemblySettings = assemblySettings,
        thumbClusterSettings = thumbClusterSettings,
        screwNutHoleDiameter = keyboardSettings.screwNutHoleDiameter,
        screwHolderWallhickness = keyboardSettings.screwHolderWallhickness,
        isSkeletonMode = keyboardSettings.isSkeletonMode,
        trackball = trackballSettings,
        wallsSettings = WallsSettings()
    )
}
