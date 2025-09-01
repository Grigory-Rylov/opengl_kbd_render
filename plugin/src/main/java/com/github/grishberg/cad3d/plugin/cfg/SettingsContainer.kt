package com.github.grishberg.cad3d.plugin.cfg

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
) {

    fun toKeyboardPartsList(): Set<KeyboardPart> {
        val result = mutableSetOf<KeyboardPart>()
        if (settingsShowCaps) {
            result.add(KeyboardPart.KeyCaps)
        }
        if (settingsShowCase) {
            result.add(KeyboardPart.Case)
        }
        if (settingsShowMatrix) {
            result.add(KeyboardPart.KeyMatrix)
        }
        if (settingsTrackball) {
            result.add(KeyboardPart.TrackBall)
        }
        if (showAmoeba) {
            result.add(KeyboardPart.Amoeba)
        }
        return result.toSet()
    }
}

@Serializable
data class ThumbClusterSettings(
    val xOffset: Double = -10.0,
    val yOffset: Double = -50.0,
    val zOffset: Double = 37.0,
    val rotateY: Double = -30.0,
    val rotateZ: Double = 10.0,
    val arcRadiusZ: Double = 0.0,
    val arcRadiusY: Double = 0.0,
    val spaceBetweenKey: Double = 6.5,
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
    val horizontalExtraSpace: Double = 1.0,
    val verticalExtraSpace: Double = 1.0,
)

@Serializable
data class SettingsContainer(
    val assemblySettings: AssemblySettings,
    val viewerSettings: ViewerSettings,
    val keyboardSettings: KeyboardSettings,
    val thumbClusterSettings: ThumbClusterSettings,
    val trackballSettings: TrackballConfig,
)
