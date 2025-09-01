package eu.printingin3d.javascad.utils

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.plugin.cfg.AssemblySettings
import com.github.grishberg.cad3d.keyboard.cfg.KeyOffsetProvider
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.keyboard.cfg.KeyZAngleProvider
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.PowerSwitcherType
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterSettings
import com.github.grishberg.cad3d.plugin.cfg.TrackballConfig
import com.github.grishberg.cad3d.plugin.cfg.TrackballMode
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.matrix.KeyMatrix
import eu.printingin3d.javascad.coords.Triangulator
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.optimizator.PolygonValidatorMultithreading
import eu.printingin3d.javascad.vrl.Polygon
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class NonManifoldFinderTest {

    private val cfg = createConfig()
    private val keyPlace = KeyPlace(cfg)
    private val thumbKeyPlace = ThumbKeyPlace(cfg)

    @Test
    fun findNonManifoldInMatrix() {
        val keyMatrix = KeyMatrix(cfg, keyPlace, thumbKeyPlace)
        val connections = keyMatrix.createConnectionsModel()
        val amoebaHoles = null
        val borders = keyMatrix.createBordersModel(amoebaHoles)
        val placeHolders = keyMatrix.createPlaceholders()
        val matrix = connections.model.addModel(borders.model).addModel(placeHolders.model)

        val fixPolygons: List<Polygon> = PolygonValidatorMultithreading().fixPolygons(matrix.toCSG().polygons)

        val trianglePolygons = mutableListOf<Polygon>()
        for (p in fixPolygons) {
            val triangles = Triangulator.triangulate(p.getVertices(), p.getNormal())
            for (t in triangles) {
                val rounded = ArrayList<V3d?>()
                for (trianglePoint in t.getPoints()) {
                    rounded.add(trianglePoint.roundedToEpsilon())
                }
                trianglePolygons.add(Polygon.fromPolygons(rounded, p.color))
            }
        }

        val nonManifoldEdges = NonManifoldFinder.findNonManifoldEdges(trianglePolygons)


        StlExporter.saveStl(trianglePolygons, "non_manifold_test_1.stl")

        assertEquals(0, nonManifoldEdges.size)
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
    )

}
