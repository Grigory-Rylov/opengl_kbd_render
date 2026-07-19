package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.cfg.KeyOffsetProvider
import com.github.grishberg.cad3d.keyboard.cfg.KeyZAngleProvider
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.plugin.cfg.AssemblySettings
import com.github.grishberg.cad3d.plugin.cfg.BatteryType
import com.github.grishberg.cad3d.plugin.cfg.ControllerType
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.plugin.cfg.PowerSwitcherType
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import com.github.grishberg.cad3d.plugin.cfg.TrackballConfig
import com.github.grishberg.cad3d.plugin.cfg.TrackballMode
import com.github.grishberg.javascad.coords.V3d
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

internal class KeyPlaceTest {

    private val underTest: KeyPlace = KeyPlace(createConfig())

    @Test
    fun test() {
        val p = underTest.calculateCoordinates(0, 0)
        val p2 = underTest.calculateCoordinates(0, 0, V3d(0.0, 0.0, 0.0))
        assertEquals(p, p2)
    }

    private fun createConfig(): KeyboardConfig = KeyboardConfig(
        fn = 10,
        stlFn = 10,
        plateZOffset = 12.0,
        rowCurvature = 20.1,
        columnCurvature = 12.1,
        tentingAngle = 16.0,
        plateThickness = 3.0,
        saProfileKeyHeight = 2.5,
        columnsCount = 3,
        rowsCount = 3,
        centerRow = 1,
        centerCol = 2,
        isLowProfile = true,
        powerSwitcherType = PowerSwitcherType.None,
        isHasHotswap = false,
        isMagneticWristRestHolder = false,
        bordersOffset = 4.0,
        screwNutHoleDiameter = 2.9,
        screwHolderWallhickness = 1.6,
        isSkeletonMode = true,
        keyPlaceholderType = KeyPlaceholderType.AmoebaSu120,
        horizontalExtraSpace = 1.0,
        verticalExtraSpace = 0.0,
        keyswitchHeight = 18.0,
        keyswitchWidth = 18.0,
        extraWidth = 2.5,
        extraHeight = 1.0,
        keyPlaceHolderWidth = 15.7,
        keyPlaceHolderDepth = 15.7,
        keyPlaceHolderHeight = 4.0,
        zAngleProvider = KeyZAngleProvider(),
        columnOffsetProvider = KeyOffsetProvider(),
        visibleKeyboardParts = AssemblySettings().toKeyboardPartsList(),
        modifiedKeyboardParts = emptySet(),
        thumbClusterSettings = ThumbClusterSettings(
            xOffset = 0.0,
            yOffset = -50.0,
            zOffset = 37.0,
            rotateY = -40.0,
            rotateZ = 18.0,
            arcRadiusZ = -80.0,
            arcRadiusY = 0.0,
            spaceBetweenKey = 6.5,
            type = ThumbClusterMode.SingleColumn3Buttons,
        ),
        trackball = TrackballConfig(
            mode = TrackballMode.Back,
            ballDiameter = 34.4,
            bearingDiameter = 3.175,
        ),
        wallsSettings = WallsSettings(),
        controllerType = ControllerType.Rp2040Pink,
        innerBatteryType = BatteryType.None,
    )
}
