package com.github.grishberg.cad3d.cli

import com.github.grishberg.cad3d.KeyboardBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig.Companion.getKeyboardConfig
import com.github.grishberg.cad3d.plugin.StlExportListener
import com.github.grishberg.cad3d.plugin.cfg.*
import kotlinx.coroutines.*
import java.io.File

class CliRunner {

    fun generate(part: String) {
        val settings = getDefaultSettings(part)
        val cfg = settings.getKeyboardConfig(emptySet())

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val builder = KeyboardBuilder(scope, Dispatchers.Default)

        val done = CompletableDeferred<Unit>()
        val plannedFiles = mutableListOf<String>()

        val listener = object : StlExportListener {
            override fun onExportPlan(fileNames: List<String>) {
                plannedFiles.addAll(fileNames)
                println("[CLI] Export plan: ${fileNames.joinToString(", ")}")
            }
            override fun onExportStart(fileName: String) {
                println("[CLI] Starting: $fileName")
            }
            override fun onExportFinish(fileName: String, success: Boolean, errorMessage: String?) {
                val status = if (success) "OK" else "FAILED: $errorMessage"
                println("[CLI] Done: $fileName — $status")
            }
            override fun onAllFinished() {
                done.complete(Unit)
            }
        }

        builder.exportStl(cfg, listener)

        runBlocking {
            try {
                done.await()
            } catch (_: CancellationException) {
            }
        }

        waitForExportFiles(plannedFiles)
        println("[CLI] Generation complete. Files in: stl/")
        scope.cancel()
    }

    private fun waitForExportFiles(expectedFiles: List<String>, timeoutSec: Long = 120) {
        val stlDir = File("stl")
        val deadline = System.currentTimeMillis() + timeoutSec * 1000

        for (expected in expectedFiles) {
            val target = File(stlDir, expected)
            while (!target.exists() && System.currentTimeMillis() < deadline) {
                Thread.sleep(200)
            }
            if (target.exists()) {
                println("[CLI] Verified file: ${target.absolutePath} (${target.length()} bytes)")
            } else {
                println("[CLI] WARNING: file not found: ${target.absolutePath}")
            }
        }
    }

    private fun getDefaultSettings(targetPart: String): SettingsContainer {
        val asm = when (targetPart.lowercase()) {
            "matrix_right", "matrix" -> AssemblySettings(
                settingsShowMatrix = true,
                settingsShowCaps = false,
                settingsShowCase = false,
                settingsShowPlate = false,
                settingsShowWristRest = false,
                settingsTrackball = false,
                showController = false,
                showTrackballSensor = false,
                showTrackbalSensorCap = false,
                showTrackballBall = false,
                showTrackballCase = false,
                showTrackballCasePlate = false,
                showControllerHolder = false,
                showAmoeba = false,
            )
            else -> AssemblySettings(
                settingsShowMatrix = true,
                settingsShowCaps = true,
                settingsShowCase = true,
                settingsShowPlate = true,
                settingsShowWristRest = false,
                settingsTrackball = false,
                showController = true,
                showTrackballSensor = false,
                showTrackbalSensorCap = false,
                showTrackballBall = false,
                showTrackballCase = false,
                showTrackballCasePlate = false,
                showControllerHolder = false,
                showAmoeba = false,
            )
        }

        return SettingsContainer(
            assemblySettings = asm,
            viewerSettings = ViewerSettings(0f, 0f, 0f, 0f, 0f, 0f),
            keyboardSettings = KeyboardSettings(
                fn = 20,
                stlFn = 60,
                plateZOffset = 8.0,
                rowCurvature = 20.1,
                tentingAngle = 8.0,
                columnCurvature = 12.1,
                plateThickness = 2.0,
                saProfileKeyHeight = 4.5,
                columnsCount = 6,
                rowsCount = 3,
                centerRow = 1,
                centerCol = 2,
                isLowProfile = true,
                powerSwitcherType = PowerSwitcherType.None,
                isHasHotswap = false,
                isMagneticWristRestHolder = false,
                bordersOffset = 4.0,
                screwNutHoleDiameter = 4.0,
                screwHolderWallhickness = 1.6,
                isSkeletonMode = false,
                keyPlaceholderType = KeyPlaceholderType.None,
                horizontalExtraSpace = 1.0,
                verticalExtraSpace = 1.0,
                controllerType = ControllerType.SuperMiniNRF52840,
                batteryType = BatteryType.Bt18650,
            ),
            thumbClusterSettings = ThumbClusterSettings(
                xOffset = -10.0,
                yOffset = -50.0,
                zOffset = 37.0,
                rotateY = -30.0,
                rotateZ = 10.0,
                arcRadiusZ = 0.0,
                arcRadiusY = 0.0,
                spaceBetweenKey = 6.5,
                type = ThumbClusterMode.SingleColumn3Buttons,
            ),
            trackballSettings = TrackballConfig(
                mode = TrackballMode.Back,
                ballDiameter = 25.0,
                bearingDiameter = 3.175,
                controllerScrewDiameter = 1.3,
            )
        )
    }
}

fun main(args: Array<String>) {
    val part = if (args.isNotEmpty()) args[0] else "matrix_right"

    println("[CLI] Generating part: $part")
    println("[CLI] Using default configuration")

    val runner = CliRunner()
    runner.generate(part)
}
