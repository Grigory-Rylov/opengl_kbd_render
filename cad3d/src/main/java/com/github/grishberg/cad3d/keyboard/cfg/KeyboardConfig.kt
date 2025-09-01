package com.github.grishberg.cad3d.keyboard.cfg

import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.plugin.Config
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.plugin.cfg.PowerSwitcherType
import com.github.grishberg.cad3d.plugin.cfg.SettingsContainer
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import com.github.grishberg.cad3d.plugin.cfg.TrackballConfig

data class KeyboardConfig(
    val fn: Int,
    val stlFn: Int,
    val plateZOffset: Double,
    val rowCurvature: Double,
    val tentingAngle: Double,
    val columnCurvature: Double,
    val keyswitchHeight: Double,
    val keyswitchWidth: Double,
    val extraWidth: Double,
    val extraHeight: Double,
    val plateThickness: Double,
    val saProfileKeyHeight: Double,
    val centerCol: Int,
    val rowsCount: Int,
    val columnsCount: Int,
    val centerRow: Int,
    val keyPlaceHolderWidth: Double,
    val keyPlaceHolderDepth: Double,
    val keyPlaceHolderHeight: Double,
    val isLowProfile: Boolean,
    val zAngleProvider: KeyZAngleProvider, //Не нужно сериализовывать
    val columnOffsetProvider: KeyOffsetProvider, //Не нужно сериализовывать
    val powerSwitcherType: PowerSwitcherType,
    val isHasHotswap: Boolean,
    val isMagneticWristRestHolder: Boolean,
    val bordersOffset: Double,
    val visibleKeyboardParts: Set<KeyboardPart>,
    val modifiedKeyboardParts: Set<KeyboardPart>,
    val thumbClusterSettings: ThumbClusterSettings,
    val screwNutHoleDiameter: Double,
    val screwHolderWallhickness: Double,
    val screwBoltDiameter: Double = 3.0,
    val isSkeletonMode: Boolean,
    val trackball: TrackballConfig,
    val wallsSettings: WallsSettings,
    val controllerPlateHeight: Double = 1.5,
    val keyPlaceholderType: KeyPlaceholderType,
    val horizontalExtraSpace: Double,
    val verticalExtraSpace: Double,
): Config {

    val lastCol: Int
        get() = columnsCount - 1
    val lastRow: Int
        get() = rowsCount - 1

    companion object {
        fun SettingsContainer.getKeyboardConfig(modifiedKeyboardParts: Set<KeyboardPart>): KeyboardConfig = KeyboardConfig(
            fn = keyboardSettings.fn,
            stlFn = keyboardSettings.stlFn,
            plateZOffset = keyboardSettings.plateZOffset,
            rowCurvature = keyboardSettings.rowCurvature,
            tentingAngle = keyboardSettings.tentingAngle,
            columnCurvature = keyboardSettings.columnCurvature,
            keyswitchHeight = 18.0,
            keyswitchWidth = 18.0,
            extraWidth = 2.5,
            extraHeight = 1.0,
            keyPlaceHolderWidth = 15.7,
            keyPlaceHolderDepth = 15.7,
            keyPlaceHolderHeight = 4.0,
            horizontalExtraSpace = keyboardSettings.horizontalExtraSpace,
            verticalExtraSpace = keyboardSettings.verticalExtraSpace,
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
            visibleKeyboardParts = assemblySettings.toKeyboardPartsList(),
            modifiedKeyboardParts = modifiedKeyboardParts,
            thumbClusterSettings = thumbClusterSettings,
            screwNutHoleDiameter = keyboardSettings.screwNutHoleDiameter,
            screwHolderWallhickness = keyboardSettings.screwHolderWallhickness,
            isSkeletonMode = keyboardSettings.isSkeletonMode,
            trackball = trackballSettings,
            wallsSettings = WallsSettings(),
            keyPlaceholderType = keyboardSettings.keyPlaceholderType,
        )
    }
}

