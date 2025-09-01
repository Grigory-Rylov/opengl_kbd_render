package com.github.grishberg.cad3d

import com.github.grishberg.cad3d.keyboard.KeyCaps
import com.github.grishberg.cad3d.keyboard.KeyHolderBottomWalls
import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceHoles
import com.github.grishberg.cad3d.keyboard.KeySwitchHoles
import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.amoeba.Amoeba
import com.github.grishberg.cad3d.keyboard.casebody.Walls
import com.github.grishberg.cad3d.keyboard.casebody.controllers.Controller
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerFactory
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerHolderBuilder
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerHolderDimensions
import com.github.grishberg.cad3d.keyboard.casebody.controllers.ControllerPlace
import com.github.grishberg.cad3d.keyboard.casebody.controllers.SuperMiniNRF52840
import com.github.grishberg.cad3d.keyboard.casebody.controllers.battery.BatteryFactory
import com.github.grishberg.cad3d.keyboard.casebody.controllers.switcher.SwitcherFactory
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.TrackballMode
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.matrix.KeyMatrix
import com.github.grishberg.cad3d.keyboard.plate.Plate
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewKeyMatrixPlace
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.cad3d.keyboard.screws.ScrewsMatrixHolder
import com.github.grishberg.cad3d.keyboard.wristrest.WristRest
import com.github.grishberg.cad3d.plugin.ResultListener
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.trackball.Trackball
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.StlExporter
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.tranzitions.Union
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeyboardBuilder(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val mainThreadDispatcher: CoroutineDispatcher,
) {

    private var resolution = 15 // Количество промежуточных точек между заданными точками
    private val cache = ConcurrentHashMap<KeyboardPart, List<VertexHolder>>()
    private val currentResults = mutableListOf<VertexHolder>()

    fun rebuildModels(cfg: KeyboardConfig, listener: ResultListener) {
        if (resolution == 0) {
            resolution = 20
        }
        create3dModels(cfg, listener)
    }

    private fun create3dModels(cfg: KeyboardConfig, listener: ResultListener) {
        currentResults.clear()

        val visibleModels = cfg.visibleKeyboardParts
        println("--------------- create3dModels: ${this} visibleModels = $visibleModels, modifiedKeyboardParts = ${cfg.modifiedKeyboardParts}")

        // remove modified parts from cache
        cfg.modifiedKeyboardParts.forEach {
            cache.remove(it)
        }

        val keyPlace = KeyPlace(cfg)
        val thumbKeyPlace = ThumbKeyPlace(cfg)

        val controllerPlace = ControllerPlace(keyPlace)
        val controllerFactory = ControllerFactory(cfg, controllerPlace)
        val controller = controllerFactory.createController()
        val controllerHolderDimensions = ControllerHolderDimensions()

        val wallsSettings = WallsSettings(bottomBorderHeight = 4.0)
        val controllerHolderWall = ControllerHolderWall(wallsSettings, keyPlace)
        val screwWallPlaces = ScrewWallPlaces(
            cfg, wallsSettings, keyPlace, thumbKeyPlace, controllerHolderWall, controllerHolderDimensions
        )

        val topEdgeOffsetZ = -2.0
        val walls = Walls(
            cfg, wallsSettings, keyPlace, thumbKeyPlace, topEdgeOffsetZ = topEdgeOffsetZ,
        )

        val wallsForPlate = Walls(
            cfg,
            wallsSettings.copy(
                borderThickness = cfg.plateThickness, borderHeight = cfg.plateThickness
            ),
            keyPlace, thumbKeyPlace, topEdgeOffsetZ = topEdgeOffsetZ,
        )

        val resultsChannel = Channel<List<VertexHolder>>()
        coroutineScope.launch {
            createIfNeeded(resultsChannel, KeyboardPart.KeyMatrix, visibleModels) {
                createMatrix(cfg, keyPlace, thumbKeyPlace)
            }

            createIfNeeded(resultsChannel, KeyboardPart.Case, visibleModels) {
                createCase(cfg, keyPlace, thumbKeyPlace, screwWallPlaces, walls)
            }
            createIfNeeded(resultsChannel, KeyboardPart.KeyCaps, visibleModels) {
                createKeyCaps(cfg, keyPlace, thumbKeyPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.WristRest, visibleModels) {
                createWristRest(cfg)
            }
            createIfNeeded(resultsChannel, KeyboardPart.TrackBallHolder, visibleModels) {
                createTrackball(cfg, keyPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.TrackBall, visibleModels) {
                createTrackBall(cfg, keyPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.TrackBallSensor, visibleModels) {
                createTrackballSensor(cfg, keyPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.TrackBallSensorCap, visibleModels) {
                createTrackballSensorCap(cfg, keyPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.Controller, visibleModels) {
                createController(cfg, controllerPlace)
            }
            createIfNeeded(resultsChannel, KeyboardPart.ControllerHolder, visibleModels) {
                createControllerHolder(
                    cfg, controllerPlace, controller, controllerHolderDimensions, screwWallPlaces
                )
            }
            createIfNeeded(resultsChannel, KeyboardPart.Plate, visibleModels) {
                createPlate(cfg, wallsForPlate)
            }
            createIfNeeded(resultsChannel, KeyboardPart.Amoeba, visibleModels) {
                createAmoeba(cfg, keyPlace, thumbKeyPlace)
            }

            val expectedCount = visibleModels.size
            launch(mainThreadDispatcher) {
                var receivedCount = 0

                while (receivedCount < expectedCount && isActive) {
                    try {
                        currentResults.addAll(resultsChannel.receive())
                        receivedCount++
                    } catch (e: ClosedReceiveChannelException) {
                        println("Channel was closed ${this@KeyboardBuilder}" + e.message)
                        break
                    }
                }
                println("--------------- create3dModels: $this onReady : ${currentResults.size}")

                listener.onReady(currentResults)
                resultsChannel.close()
            }
        }
    }

    private fun CoroutineScope.createIfNeeded(
        resultsChannel: Channel<List<VertexHolder>>,
        keyboardPart: KeyboardPart,
        visibleModels: Set<KeyboardPart>,
        producer: () -> List<VertexHolder>
    ) {
        if (!visibleModels.contains(keyboardPart)) {
            return
        }
        val cachedPart = cache.get(keyboardPart)

        launch {
            if (cachedPart == null) {
                val vertexHolders = producer.invoke()
                cache[keyboardPart] = vertexHolders
                resultsChannel.send(vertexHolders)
            } else {
                resultsChannel.send(cachedPart)
            }
        }
    }

    private fun createMatrix(
        cfg: KeyboardConfig, keyPlace: KeyPlace, thumbKeyPlace: ThumbKeyPlace
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()

        val startTime = System.currentTimeMillis()

        val keyMatrix = KeyMatrix(cfg, keyPlace, thumbKeyPlace)
        val connections = keyMatrix.createConnectionsModel()
        val amoebaHoles = amoebaHoles(
            cfg, keyPlace, thumbKeyPlace
        ).takeIf { cfg.keyPlaceholderType == KeyPlaceholderType.AmoebaSu120 }
        val borders = keyMatrix.createBordersModel(amoebaHoles)
        val placeHolders = keyMatrix.createPlaceholders()

        result.addAll(connections.vertexHolders)
        result.addAll(borders.vertexHolders)
        result.addAll(placeHolders.vertexHolders)

        val matrix = connections.model.addModel(borders.model).addModel(placeHolders.model)

        saveModel(cfg, "matrix.stl", matrix)

        val delta = System.currentTimeMillis() - startTime
        println("createMatrix : $delta")
        return result
    }

    private fun createCase(
        cfg: KeyboardConfig,
        keyPlace: KeyPlace,
        thumbKeyPlace: ThumbKeyPlace,
        screwWallPlaces: ScrewWallPlaces,
        walls: Walls,
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()

        val startTime = System.currentTimeMillis()

        var tbHolder: Abstract3dModel? = null
        val caseWalls = createCaseModel(cfg, keyPlace, thumbKeyPlace, screwWallPlaces, walls)
        if (cfg.trackball.mode != TrackballMode.None) {
            val trackBallHolder = Trackball(cfg, keyPlace).createTrackballHolder()
            tbHolder = trackBallHolder.model
            result.addAll(trackBallHolder.vertexHolders)

        }
        result.addAll(caseWalls.vertexHolders)
        val resultCase = if (tbHolder != null) caseWalls.model.addModel(tbHolder) else caseWalls.model
        saveModel(cfg, "case.stl", resultCase)
        val delta = System.currentTimeMillis() - startTime
        println("createCase : $delta")
        return result
    }

    private fun createKeyCaps(
        cfg: KeyboardConfig, keyPlace: KeyPlace, thumbKeyPlace: ThumbKeyPlace
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()
        val keyCap = KeyCaps(cfg)
        result.addAll(
            createThumbKeyPlaceModel(
                cfg, keyCap.create().model, thumbKeyPlace, Color.BLUE
            ).vertexHolders
        )
        result.addAll(createKeycapsModel(cfg, keyCap.create().model, keyPlace, Color.PINK).vertexHolders)
        val delta = System.currentTimeMillis() - startTime
        println("createKeyCaps : $delta")
        return result
    }

    private fun amoebaHoles(cfg: KeyboardConfig, keyPlace: KeyPlace, thumbKeyPlace: ThumbKeyPlace): Abstract3dModel {
        val models = mutableListOf<Abstract3dModel>()
        val amoeba = Amoeba(cfg)
        val hole = amoeba.createHoles(height = 7.0, diameter = 0.7).addModel(amoeba.createSimple())

        models.add(createThumbKeyPlaceModel(cfg, hole, thumbKeyPlace, Color.BLUE).model)
        models.add(createKeycapsModel(cfg, hole, keyPlace, Color.PINK).model)
        return Union(models)
    }

    private fun createWristRest(cfg: KeyboardConfig): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()
        val wristRest = createWristRestModel(cfg)
        result.addAll(wristRest.vertexHolders)
        val delta = System.currentTimeMillis() - startTime
        println("createWristRest : $delta")
        return result
    }

    private fun createTrackball(
        cfg: KeyboardConfig, keyPlace: KeyPlace,
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()
        val trackball = Trackball(cfg, keyPlace)
        val trackBallModelHolder = trackball.create()
        result.addAll(trackBallModelHolder.vertexHolders)
        saveModel(cfg, "trackball.stl", trackBallModelHolder.model)

        val delta = System.currentTimeMillis() - startTime
        println("createTrackball : $delta")
        return result
    }

    private fun createTrackBall(cfg: KeyboardConfig, keyPlace: KeyPlace): List<VertexHolder> {
        val trackball = Trackball(cfg, keyPlace)
        return listOf(trackball.createTrackBall())
    }

    private fun createTrackballSensorCap(cfg: KeyboardConfig, keyPlace: KeyPlace): List<VertexHolder> {
        val trackball = Trackball(cfg, keyPlace)
        val sensorCap = trackball.createSensorCap()
        saveModel(cfg, "trackballCap.stl", sensorCap.model)
        return sensorCap.vertexHolders
    }

    private fun createTrackballSensor(cfg: KeyboardConfig, keyPlace: KeyPlace): List<VertexHolder> {
        val trackball = Trackball(cfg, keyPlace)
        return trackball.createTrackballSensor().vertexHolders
    }

    private fun createController(
        cfg: KeyboardConfig, controllerPlace: ControllerPlace,
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()
        val trackBall = SuperMiniNRF52840(cfg, controllerPlace).create()
        result.addAll(trackBall.vertexHolders)
        val delta = System.currentTimeMillis() - startTime
        println("createController : $delta")
        return result
    }

    private fun createControllerHolder(
        cfg: KeyboardConfig, controllerPlace: ControllerPlace, controller: Controller,
        controllerHolderDimensions: ControllerHolderDimensions, screwWallPlaces: ScrewWallPlaces,
    ): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()

        val switcherFactory = SwitcherFactory(cfg)
        val batteryFactory = BatteryFactory(cfg)

        val controllerHolder = ControllerHolderBuilder(
            cfg,
            controller,
            controllerPlace,
            controllerHolderDimensions,
            screwWallPlaces,
            switcherFactory.createSwitcher(),
            batteryFactory.create()
        ).create(showPreview = true)
        result.addAll(controllerHolder.vertexHolders)
        saveModel(cfg, "controller_holder.stl", controllerHolder.model)

        val delta = System.currentTimeMillis() - startTime
        println("createControllerHolder : $delta")
        return result
    }

    private fun createPlate(cfg: KeyboardConfig, walls: Walls): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()

        val plate = Plate(cfg, walls).create()

        result.addAll(plate.vertexHolders)
        //saveModel("controller_holder.stl", controllerHolder.model)

        val delta = System.currentTimeMillis() - startTime
        println("createPlate : $delta")
        return result
    }

    private fun createAmoeba(
        cfg: KeyboardConfig, keyPlace: KeyPlace, thumbKeyPlace: ThumbKeyPlace
    ): List<VertexHolder> {
        val settings = cfg.visibleKeyboardParts
        val result = mutableListOf<VertexHolder>()
        val startTime = System.currentTimeMillis()

        val amoeba = Amoeba(cfg).create()

        result.addAll(createThumbKeyPlaceModel(cfg, amoeba, thumbKeyPlace, Color.GREEN).vertexHolders)
        result.addAll(createKeycapsModel(cfg, amoeba, keyPlace, Color.green).vertexHolders)

        val delta = System.currentTimeMillis() - startTime
        println("createAmoeba : $delta")
        return result
    }

    private fun saveModel(cfg: KeyboardConfig, name: String, model: Abstract3dModel, needCheck: Boolean = false) {
        val outDir = File("stl")
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        Thread {
            try {
                val context: FacetGenerationContext = ColorFacetGenerationContext(DEFAULT_COLOR)
                context.setFn(cfg.stlFn)
                println("Start stl exporting")
                StlExporter.saveStl(
                    model.toCSG(context).polygons, File(outDir, name).absolutePath
                )
                println("End stl exporting")
            } catch (e: IOException) {
                println("Error while stl exporting" + e.message)
                throw RuntimeException(e)
            }
        }.start()
    }

    private fun createThumbKeyPlaceModel(
        cfg: KeyboardConfig, model: Abstract3dModel, thumbKeyPlace: ThumbKeyPlace, color: Color
    ): ModelHolder {
        val placedModel = thumbKeyPlace.thumbPlace(model)
        return ModelHolder(placedModel, createVertexHolder(cfg, placedModel, color))
    }

    private fun keyHoles(cfg: KeyboardConfig, keyPlace: KeyPlace): Abstract3dModel {
        return KeySwitchHoles(cfg, keyPlace).build()
    }

    private fun keyPlaceHoles(cfg: KeyboardConfig, keyPlace: KeyPlace, offset: Double): Abstract3dModel {
        return KeyPlaceHoles(cfg, keyPlace).build(offset)
    }

    private fun wristRestMount(): Abstract3dModel {
        // left back
        return Cylinder(42.0, 6.0).move(-56.0, -88.0, -2.0).addModel( // left front
            Cylinder(56.0, 6.0).move(-53.0, -142.0, -4.0)
        ).addModel( // right back
            Cylinder(48.0, 6.0).move(60.0, -85.0, -6.0)
        ).addModel( // right front
            Cylinder(62.0, 6.0).move(53.0, -140.0, -6.0)
        )
    }

    private fun keyPlaceBottomWalls(cfg: KeyboardConfig, keyPlace: KeyPlace): Abstract3dModel {
        return KeyHolderBottomWalls(cfg, keyPlace).build()
    }

    private fun createKeycapsModel(
        cfg: KeyboardConfig, model: Abstract3dModel, keyPlace: KeyPlace, color: Color
    ): ModelHolder {
        val models = mutableListOf<Abstract3dModel>()
        for (column in 0 until cfg.columnsCount) {
            for (row in 0 until cfg.rowsCount) {
                models.add(keyPlace.place(column, row, model))
            }
        }
        val result = Union(models)
        return ModelHolder(result, fromModel(result, color, 20))
    }

    private fun createCaseModel(
        cfg: KeyboardConfig,
        keyPlace: KeyPlace,
        thumbKeyPlace: ThumbKeyPlace,
        screwWallPlaces: ScrewWallPlaces,
        walls: Walls,
    ): ModelHolder {
        val holeVerticalExtra = -3.0

        val borderHeight = 5.0
        val borderVerticalOffset = 5.0

        val wallsSettings = WallsSettings(
            verticalOffset = borderVerticalOffset,
            borderHeight = borderHeight,
            bottomBorderHeight = if (cfg.isSkeletonMode) 4.0 else 2.0
        )

        val holeBorderHeight = 2.0
        val holeBorders = Walls(
            cfg,
            wallsSettings,
            keyPlace,
            thumbKeyPlace,
            topEdgeOffsetZ = holeVerticalExtra / 2,
            isPlateMode = false,
        ).createBorders(
            borderThickness = holeBorderHeight, borderHeight = borderHeight + holeVerticalExtra
        ).moveZ(holeBorderHeight - 1.7)

        val screwBase = ScrewBase(cfg)
        val matrixWallScrewHolder = ScrewsMatrixHolder(cfg, screwBase).create()
        val matrixWallNutHole = ScrewsMatrixHolder(cfg, screwBase).createNutHole()
        val screwKeyMatrixPlace = ScrewKeyMatrixPlace(cfg, keyPlace, thumbKeyPlace)
        val screwMatrixHoldersHoles = screwKeyMatrixPlace.place(matrixWallNutHole)
        val screwMatrixHolders = screwKeyMatrixPlace.place(matrixWallScrewHolder).subtractModel(holeBorders)
            .subtractModel(screwMatrixHoldersHoles)

        //
        val wallScrews = placeWallScrews(screwBase.screwHolder(), screwWallPlaces)

        val bottomEdgeHeight = if (cfg.isSkeletonMode) 4.0 else 2.0

        val wallsModel = walls.createWalls(
            bottomBorderHeight = bottomEdgeHeight
        ).subtractModel(holeBorders).subtractModel(Cube(300.0, 300.0, 50.0).move(0.0, 0.0, -25.0))

        return ModelHolder(
            wallsModel.addModel(screwMatrixHolders).addModel(wallScrews).subtractModel(screwMatrixHoldersHoles),
            createVertexHolder(cfg, wallsModel.subtractModel(screwMatrixHoldersHoles), Color.gray),
            createVertexHolder(cfg, wallScrews, Color.yellow),
            //createVertexHolder(holeBorders, Color.PINK),
            createVertexHolder(
                cfg, screwMatrixHolders.subtractModel(Cube(300.0, 300.0, 50.0).move(0.0, 0.0, -25.0)), Color.CYAN
            )

        )
    }

    private fun placeWallScrews(
        screwHolder: Abstract3dModel,
        screwWallPlaces: ScrewWallPlaces,
    ): Abstract3dModel {

        val holderHeight = 2.2
        val screwWallHolderBack = Cube(6.0, 4.0, holderHeight).move(0.0, 4.0, holderHeight / 2)
        val screwWallHolderSide = Cube(6.0, 6.0, holderHeight).move(-5.0, 0.0, holderHeight / 2)
        val controllerScrewsHolderBack = screwWallPlaces.placeControllerScrews(
            screwWallHolderBack,
            heightMode = ScrewWallPlaces.HeightMode.Walls,
            mode = ScrewWallPlaces.ControllerMode.Back
        )

        val controllerScrewsHolderSide = screwWallPlaces.placeControllerScrews(
            screwWallHolderSide,
            heightMode = ScrewWallPlaces.HeightMode.Walls,
            mode = ScrewWallPlaces.ControllerMode.Side
        )

        return screwWallPlaces.place(screwHolder, heightMode = ScrewWallPlaces.HeightMode.Walls)
            .addModel(controllerScrewsHolderBack).addModel(controllerScrewsHolderSide)
    }

    private fun createWristRestModel(cfg: KeyboardConfig): ModelHolder {
        val wristRest = WristRest.build().subtractModel(Cube(300.0, 300.0, 50.0).move(0.0, 0.0, -25.0))

        val vertexHolders = mutableListOf<VertexHolder>()
        vertexHolders.add(
            createVertexHolder(
                cfg, wristRest // .subtractModel(keyPlaceHoles(-4))
                , Color.ORANGE
            )
        )
        //vertexHolders.addAll(createControlPoints(pointsController.controlPoints))

        return ModelHolder(wristRest, vertexHolders)
    }

    private fun createControlPoints(cfg: KeyboardConfig, points: Array<Array<V3d>>): List<VertexHolder> {
        val result = mutableListOf<VertexHolder>()
        val colors = arrayOf(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.ORANGE)
        for (rowIndex in points.indices) {
            for (colIndex in points[rowIndex].indices) {
                result.add(
                    createVertexHolder(
                        cfg, Utils.sphere(2.0).move(points[rowIndex][colIndex]), colors[colIndex % colors.size]
                    )
                )
            }
        }
        return result
    }

    private fun createVertexHolder(cfg: KeyboardConfig, model: IModel) {
        createVertexHolder(cfg, model, DEFAULT_COLOR)
    }

    private fun createVertexHolder(cfg: KeyboardConfig, model: IModel, color: Color): VertexHolder {
        return fromModel(model, color, cfg.fn)
    }

    companion object {

        private val DEFAULT_COLOR = Color.GRAY
    }
}
