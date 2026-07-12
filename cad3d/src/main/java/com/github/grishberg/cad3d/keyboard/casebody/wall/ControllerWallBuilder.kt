package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.openscad.basic.Radius
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.Hull
import com.github.grishberg.openscad.models.Sphere
import com.github.grishberg.openscad.tranzitions.Union
import com.github.grishberg.openscad.utils.Color

class ControllerWallBuilder(
    private val controllerHolderWall: ControllerHolderWall,
    private val keyPlace: KeyPlace,
    private val isSkeletonMode: Boolean,
    private val topEdgeOffsetZ: Double,
    private val cfg: WallsSettings,
    private val bottomEdgePatcher: WallBottomEdgePatcher = DefaultBottomEdgePatcher(
        cfg.borderThickness, cfg.bottomBorderHeight
    ),

    ) {

    fun createWall(): List<Abstract3dModel> {
        val models = mutableListOf<Abstract3dModel>()

        val left = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackLeft().move(-cfg.outerLeftOffset, 0.0, cfg.outerBorderZOffset)
        )

        val right = keyPlace.place(
            2, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val mid = keyPlace.place(
            1, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val midLeft = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val backControllerLeft = controllerHolderWall.getWallPoint(left.move)
        val backControllerRight = controllerHolderWall.getWallPoint(right.move)
        val backControllerMid = controllerHolderWall.getWallPoint(mid.move)
        val backControllerMidLeft = controllerHolderWall.getWallPoint(midLeft.move)


        models.addAll(backLeftCorner())

        if (isSkeletonMode) {
            skeletonWalls(
                models,
                backControllerLeft,
                backControllerRight,
                left,
                mid,
                backControllerMid,
                midLeft,
                backControllerMidLeft
            )
        } else {
            solidWalls(
                models,
                backControllerLeft,
                backControllerRight,
                left,
                mid,
                backControllerMid,
                midLeft,
                backControllerMidLeft
            )
        }

        //models.add(backWall(0))
        //models.add(backWall(1))

        return models
    }

    private fun skeletonWalls(
        models: MutableList<Abstract3dModel>,
        backControllerLeft: V3d,
        backControllerRight: V3d,
        left: Abstract3dModel,
        mid: Abstract3dModel?,
        backControllerMid: V3d,
        midLeft: Abstract3dModel?,
        backControllerMidLeft: V3d
    ) {
        //top edge
        val sphere = Sphere(Radius.fromRadius(cfg.borderThickness).value)
        models.add(
            Hull(
                sphere.move(backControllerLeft), sphere.move(backControllerRight)
            )
        )

        //bottom edge

        models.add(
            Hull(
                bottomCylinder(backControllerRight),
                bottomCylinder(backControllerLeft),
            )
        )

        //left vertical
        models.add(
            Hull(
                sphere.move(backControllerLeft),
                bottomCylinder(backControllerLeft),
            )
        )

        //left corner - bottom
        models.add(
            Hull(
                left,
                bottomEdgePatcher.leftPoint(left),
            )
        )

        //left corner - back top controller
        models.add(
            Hull(
                left,
                sphere.move(backControllerLeft),
            )
        )

        //mid - back top controller
        models.add(
            Hull(
                mid,
                sphere.move(backControllerMid),
            )
        )

        //mid left - back top controller
        models.add(
            Hull(
                midLeft,
                sphere.move(backControllerMidLeft),
            )
        )

        models.add(
            Hull(
                bottomCylinder(backControllerLeft),
                bottomEdgePatcher.leftPoint(left),
            )
        )

        // center left
        val centerLeft = keyPlace.place(
            2, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        models.add(
            Hull(
                centerLeft,
                bottomEdgePatcher.backPoint(centerLeft),
            )
        )
    }

    private fun solidWalls(
        models: MutableList<Abstract3dModel>,
        backControllerLeft: V3d,
        backControllerRight: V3d,
        left: Abstract3dModel,
        mid: Abstract3dModel,
        backControllerMid: V3d,
        midLeft: Abstract3dModel,
        backControllerMidLeft: V3d
    ) {
        //top edge
        models.add(
            Hull(
                topBorderObj(backControllerLeft), topBorderObj(backControllerRight)
            ).withColor(Color.BLUE)
        )

        //bottom edge

        models.add(
            Hull(
                topBorderObj(backControllerRight),
                topBorderObj(backControllerLeft),
                bottomCylinder(backControllerRight),
                bottomCylinder(backControllerLeft)
            ).withColor(Color.RED)
        )

        //left vertical
        models.add(
            Hull(
                topBorderObj(backControllerLeft),
                bottomCylinder(backControllerLeft),
            ).withColor(Color.ORANGE)
        )

        //left corner - bottom
        models.add(
            Hull(
                topBorderObj(left),
                bottomEdgePatcher.leftPoint(left),
            ).withColor(Color.BISQUE)
        )

        //left corner - back top controller

        val right = keyPlace.place(
            1, 0, KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        //mid - back top controller
        models.add(
            Hull(
                topBorderObj(mid),
                topBorderObj(backControllerRight),
                topBorderObj(backControllerMid),
            ).withColor(Color.BROWN)
        )
        models.add(
            Hull(
                topBorderObj(mid),
                topBorderObj(right),
                topBorderObj(backControllerRight),
            ).withColor(Color.GREEN)
        )

        // column 0 back

        val outEdgeCol0R = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val outEdgeCol0L = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        models.add(
            Hull(
                topBorderObj(mid),
                topBorderObj(outEdgeCol0R),
                topBorderObj(outEdgeCol0L),
            ).withColor(Color.ORANGE)
        )

        models.add(
            Hull(
                topBorderObj(mid),
                topBorderObj(outEdgeCol0L),
                topBorderObj(backControllerMid),
            ).withColor(Color.ALICE_BLUE)
        )

        models.add(
            Hull(
                topBorderObj(outEdgeCol0L),
                topBorderObj(backControllerMid),
                topBorderObj(backControllerMidLeft),
            ).withColor(Color.AQUA)
        )

        // center left
        val centerLeft = keyPlace.place(
            2, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        models.add(
            Hull(
                // centerLeft,
                bottomEdgePatcher.backPoint(centerLeft),
            )
        )
    }

    private fun backWall(column: Int): Abstract3dModel {
        val left = keyPlace.place(
            column, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = keyPlace.place(
            column, 0, KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val backControllerRight = controllerHolderWall.getWallPoint(right.move)
        val backControllerLeft = controllerHolderWall.getWallPoint(left.move)

        if (isSkeletonMode) {
            val objects = mutableListOf<Abstract3dModel>()

            val sphere = Sphere(Radius.fromRadius(cfg.borderThickness).value)
            objects.add(Utils.hull(left, sphere.move(backControllerLeft)))
            objects.add(Utils.hull(right, sphere.move(backControllerRight)))
            objects.add(
                Utils.hull(
                    sphere.move(backControllerLeft),
                    sphere.move(backControllerRight),
                )
            )

            val bottomZOffset = cfg.borderThickness
            objects.add(
                Utils.hull(
                    sphere.move(backControllerRight), bottomCylinder(backControllerRight)
                )
            )

            objects.add(
                Utils.hull(
                    sphere.move(backControllerLeft.projectionZ(bottomZOffset)),
                    bottomCylinder(backControllerRight)
                )
            )

            objects.add(
                Utils.hull(
                    left,
                    left.moveY(-2.0),
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.leftPoint(left.moveY(-2.0)),
                )
            )
            objects.add(
                Utils.hull(
                    right,
                    bottomEdgePatcher.backPoint(right),
                )
            )
            objects.add(
                Utils.hull(
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.backPoint(right),
                )
            )
            return Union(objects)
        }

        val wall = Utils.hull(
            left, right,
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.backPoint(right),
        )
        return Union(wall)
    }

    private fun backLeftCorner(): List<Abstract3dModel> {

        val back = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val left = keyPlace.place(
            0, 0, KeyPlaceholder.placeHolderBackLeft().move(-cfg.outerLeftOffset, 0.0, cfg.outerBorderZOffset)
        )

        val backControllerBack = controllerHolderWall.getWallPoint(back.move)
        val backControllerLeft = controllerHolderWall.getWallPoint(left.move)

        val border = Utils.hull(
            verticalCube(
                keyPlace.place(
                    0, 0, KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace.place(
                    0, 0, KeyPlaceholder.placeHolderBackLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ),
            topBorderObj(back),
            topBorderObj(left),
        ).withColor(Color.PINK)

        if (isSkeletonMode) {
            val objects = mutableListOf<Abstract3dModel>()
            objects.add(border)

            val sphere = Sphere(Radius.fromRadius(cfg.borderThickness).value)/*
            objects.add(Utils.hull(left, sphere.move(backControllerLeft)))
            objects.add(Utils.hull(back, sphere.move(backControllerBack)))
            objects.add(
                Utils.hull(
                    sphere.move(backControllerLeft),
                    sphere.move(backControllerBack),
                )
            )

            objects.add(
                Utils.hull(
                    sphere.move(backControllerLeft),
                    bottomBorderObject().move(backControllerLeft.projectionZ(borderThickness))
                )
            )
            objects.add(
                Utils.hull(
                    sphere.move(backControllerBack),
                    bottomBorderObject().move(backControllerBack.projectionZ(borderThickness))
                )
            )

            objects.add(
                Utils.hull(
                    sphere.move(backControllerLeft.projectionZ(borderThickness)),
                    bottomBorderObject().move(backControllerBack.projectionZ(borderThickness))
                )
            )

            objects.add(
                Utils.hull(
                    left,
                    left.moveY(-2.0),
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.leftPoint(left.moveY(-2.0)),
                )
            )
*/

            return objects
        }

        val part2 = Utils.hull(
            topBorderObj(left),
            topBorderObj(backControllerLeft),
            bottomCylinder(left.move),
        ).withColor(Color.BISQUE)

        val part3 = Utils.hull(
            topBorderObj(backControllerLeft),
            bottomCylinder(backControllerLeft),
            bottomCylinder(left.move),
        ).withColor(Color.GREEN)

        return listOf(
            border,

            Utils.hull(
                topBorderObj(left), topBorderObj(back),
                topBorderObj(backControllerLeft),

                ).withColor(Color.RED),

            Utils.hull(
                topBorderObj(back),
                topBorderObj(backControllerBack),
                topBorderObj(backControllerLeft),
            ).withColor(Color.BURLY_WOOD),
            part2, part3

        )
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return cylinder(cfg.borderThickness, cfg.borderHeight).moveZ(topEdgeOffsetZ).move(obj.move)
    }

    private fun bottomCylinder(point: V3d): Abstract3dModel {
        return cylinder(
            cfg.borderThickness,
            cfg.bottomBorderHeight
        ).move(point.projectionZ(cfg.bottomBorderHeight / 2))
    }

    private fun cylinder(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObj(obj: Abstract3dModel): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0).move(obj.move)
    }

    private fun topBorderObj(point: V3d): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0).move(point)
    }
}
