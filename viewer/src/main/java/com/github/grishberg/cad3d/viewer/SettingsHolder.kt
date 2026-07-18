package com.github.grishberg.cad3d.viewer

import com.github.grishberg.cad3d.plugin.cfg.TrackballConfig
import com.github.grishberg.cad3d.plugin.cfg.AssemblySettings
import com.github.grishberg.cad3d.plugin.cfg.BatteryType
import com.github.grishberg.cad3d.plugin.cfg.ControllerType
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.plugin.cfg.KeyboardSettings
import com.github.grishberg.cad3d.plugin.cfg.PowerSwitcherType
import com.github.grishberg.cad3d.plugin.cfg.SettingsContainer
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import com.github.grishberg.cad3d.plugin.cfg.TrackballMode
import com.github.grishberg.cad3d.plugin.cfg.ViewerSettings
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SettingsHolder(
    val filePath: String,
) {
    var rotateX: Float = -55.0f
    var rotateY: Float = 0.0f
    var rotateZ: Float = 0.0f
    var translateX: Float = 0.0f
    var translateY: Float = 0.0f
    var translateZ: Float = -300.0f

    var showScriptPanel: Boolean = false

    var settings: SettingsContainer = createDefaultSettings()
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pendingSaveJob: Job? = null
    private val saveDebounceMillis: Long = 1000

    var settingsShowCaps: Boolean
        get() = settings.assemblySettings.settingsShowCaps
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsShowCaps = value))
        }

    var settingsShowCase: Boolean
        get() = settings.assemblySettings.settingsShowCase
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsShowCase = value))
        }

    var settingsShowMatrix: Boolean
        get() = settings.assemblySettings.settingsShowMatrix
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsShowMatrix = value))
        }

    var settingsShowPlate: Boolean
        get() = settings.assemblySettings.settingsShowPlate
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsShowPlate = value))
        }

    var settingsShowWristRest: Boolean
        get() = settings.assemblySettings.settingsShowWristRest
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsShowWristRest = value))
        }

    var settingsTrackball: Boolean
        get() = settings.assemblySettings.settingsTrackball
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(settingsTrackball = value))
        }

    var showTrackballSensor: Boolean
        get() = settings.assemblySettings.showTrackballSensor
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showTrackballSensor = value))
        }

    var showTrackballSensorCap: Boolean
        get() = settings.assemblySettings.showTrackbalSensorCap
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showTrackbalSensorCap = value))
        }

    var showControllerHolder: Boolean
        get() = settings.assemblySettings.showControllerHolder
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showControllerHolder = value))
        }
    var showController: Boolean
        get() = settings.assemblySettings.showController
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showController = value))
        }

    var showAmoeba: Boolean
        get() = settings.assemblySettings.showAmoeba
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showAmoeba = value))
        }

    var showTrackballCase: Boolean
        get() = settings.assemblySettings.showTrackballCase
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showTrackballCase = value))
        }

    var showTrackballCasePlate: Boolean
        get() = settings.assemblySettings.showTrackballCasePlate
        set(value) {
            settings = settings.copy(assemblySettings = settings.assemblySettings.copy(showTrackballCasePlate = value))
        }

    private fun createDefaultSettings() = SettingsContainer(
        assemblySettings = AssemblySettings(),
        viewerSettings = createViewerSettings(),
        keyboardSettings = KeyboardSettings(
            fn = 20,
            stlFn = 60,
            columnsCount = 6,
            rowsCount = 3,
            plateZOffset = 8.0,
            rowCurvature = 20.1,
            tentingAngle = 8.0,
            columnCurvature = 12.1,
            plateThickness = 2.0,
            saProfileKeyHeight = LOW_PROFILE_KEYCAP_HEIGHT,
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

    private fun createViewerSettings(): ViewerSettings {
        return ViewerSettings(
            rotateX = rotateX,
            rotateY = rotateY,
            rotateZ = rotateZ,
            translateX = translateX,
            translateY = translateY,
            translateZ = translateZ,

            )
    }

    fun updateSettings(
        keyboardSettings: KeyboardSettings,
    ) {
        settings = settings.copy(keyboardSettings = keyboardSettings)
        scheduleSaveDebounced()
    }

    fun updateSettings(
        thumbClusterSettings: ThumbClusterSettings
    ) {
        settings = settings.copy(thumbClusterSettings = thumbClusterSettings)
        scheduleSaveDebounced()
    }

    fun updateSettings(tbConfig: TrackballConfig) {
        settings = settings.copy(trackballSettings = tbConfig)
        scheduleSaveDebounced()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun saveSettings() {
        settings = settings.copy(
            viewerSettings = ViewerSettings(
                rotateX, rotateY, rotateZ, translateX, translateY, translateZ
            )
        )
        val file = File(filePath)
        try {
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(settings)
            file.writeText(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleSaveDebounced() {
        pendingSaveJob?.cancel()
        pendingSaveJob = coroutineScope.launch {
            delay(saveDebounceMillis)
            saveSettings()
        }
    }

    private val scriptPanelStateFile: File
        get() = File(filePath).parentFile?.resolve("script_panel_state.json")
            ?: File("script_panel_state.json")

    fun loadScriptPanelState() {
        try {
            val f = scriptPanelStateFile
            if (f.exists()) {
                val obj = json.decodeFromString<ScriptPanelState>(f.readText())
                showScriptPanel = obj.showScriptPanel
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveScriptPanelState() {
        try {
            val f = scriptPanelStateFile
            f.parentFile?.mkdirs()
            f.writeText(json.encodeToString(ScriptPanelState(showScriptPanel)))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadSettings() {
        val file = File(filePath)
        try {
            if (!file.exists()) {
                return
            }
            settings = json.decodeFromString<SettingsContainer>(file.readText())
            rotateX = settings.viewerSettings.rotateX
            rotateZ = settings.viewerSettings.rotateZ
            rotateZ = settings.viewerSettings.rotateZ
            translateX = settings.viewerSettings.translateX
            translateY = settings.viewerSettings.translateY
            translateZ = settings.viewerSettings.translateZ
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {

        private const val LOW_PROFILE_KEYCAP_HEIGHT = 4.5
        private const val STANDART_KEYCAP_HEIGHT = 12.7
    }
}

@kotlinx.serialization.Serializable
private data class ScriptPanelState(val showScriptPanel: Boolean)
