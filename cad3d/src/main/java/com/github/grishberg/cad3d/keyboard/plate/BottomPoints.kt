package com.github.grishberg.cad3d.keyboard.plate

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.casebody.CircleBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import eu.printingin3d.javascad.coords.V3d

class BottomPoints(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val wallsSettings: WallsSettings,
    private val bottomEdgePatcher: WallBottomEdgePatcher = CircleBottomEdgePatcher(
        thickness = 1.5,
        objectHeight = if (cfg.isSkeletonMode) 4.0 else 2.0,
        radiusX = 115.0,
        radiusY = 81.0,
        centerY = -35.0,
    )
) {

    private val backPoints = createBackPoints()
    private val frontPoints = createFrontPoints()
    private val leftPoints = createLeftPoints()
    private val rightPoints = createRightPoints()
    private val controllerPoints = createControllerPoints()
    private val thumbPoints = createThumbsPoints()

    val B0 = backPoints[0]
    val B1 = backPoints[1]
    val B2 = backPoints[2]
    val B3 = backPoints[3]
    val B4 = backPoints[4]
    val B5 = backPoints[5]
    val B6 = backPoints[6]
    val B7 = backPoints[7]
    val B8 = backPoints[8]

    val F0 = frontPoints[0]
    val F1 = frontPoints[1]
    val F2 = frontPoints[2]
    val F3 = frontPoints[3]

    val L0 = leftPoints[0]
    val L1 = leftPoints[1]
    val L2 = leftPoints[2]
    val L3 = leftPoints[3]
    val L4 = leftPoints[4]
    val L5 = leftPoints[5]
    val LB = leftPoints[6]

    val R0 = rightPoints[0]
    val R1 = rightPoints[1]
    val R2 = rightPoints[2]
    val R3 = rightPoints[3]
    val R4 = rightPoints[4]
    val R5 = rightPoints[5]

    val BCL = controllerPoints[0]
    val BCR = controllerPoints[1]
    val BCM = controllerPoints[2]
    val BCML = controllerPoints[3]

    val T0 = thumbPoints[0]
    val T1 = thumbPoints[1]
    val T2 = thumbPoints[2]
    val T3 = thumbPoints[3]
    val T4 = thumbPoints[4]
    val T5 = thumbPoints[5]
    val T6 = thumbPoints[6]
    val T7 = thumbPoints[7]
    val T8 = thumbPoints[8]
    val T9 = thumbPoints[9]

    val TR2L1: V3d? = thumbPoints.getOrNull(10)
    val TR2L2: V3d? = thumbPoints.getOrNull(11)
    val TR2R1: V3d? = thumbPoints.getOrNull(12)
    val TR2R2: V3d? = thumbPoints.getOrNull(13)
    val TR2L3: V3d? = thumbPoints.getOrNull(14)


    private fun createBackPoints(): List<V3d> {
        val points = mutableListOf<V3d>()
        for (i in 2 until 6) {
            val leftBack = keyPlace.place(
                i,
                0,
                KeyPlaceholder.placeHolderBackLeft()
                    .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )
            val rightBack = keyPlace.place(
                i,
                0,
                KeyPlaceholder.placeHolderBackRight()
                    .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )

            points.add(
                bottomEdgePatcher.backPoint(leftBack).move.projectionZ(0.0)
            )

            points.add(
                bottomEdgePatcher.backPoint(rightBack).move.projectionZ(0.0)
            )

        }

        val right =
            keyPlace.place(cfg.lastCol, 0, KeyPlaceholder.placeHolderBackRight().move(cfg.wallsSettings.outerRightOffset, 0.0, cfg.wallsSettings.outerBorderZOffset))

        points.add(
            bottomEdgePatcher.backPoint(right).move.projectionZ(0.0)
        )
        return points
    }

    private fun createFrontPoints(): List<V3d> {

        val points = mutableListOf<V3d>()

        for (i in 4 until 6) {

            val xRightOffset = if (i == 4) {
                -2.5
            } else {
                0.0
            }
            val xLeftOffset = if (i == 5) {
                -2.5
            } else {
                0.0
            }
            val leftFront = keyPlace.place(
                i,
                cfg.lastRow,
                KeyPlaceholder.placeHolderFrontLeft()
                    .move(xLeftOffset, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )
            val rightFront = keyPlace.place(
                i,
                cfg.lastRow,
                KeyPlaceholder.placeHolderFrontRight()
                    .move(xRightOffset, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )

            points.add(
                bottomEdgePatcher.frontPoint(leftFront).move.projectionZ(0.0)
            )
            points.add(
                bottomEdgePatcher.frontPoint(rightFront).move.projectionZ(0.0)
            )
        }

        return points
    }

    private fun createLeftPoints(): List<V3d> {

        val points = mutableListOf<V3d>()


        for (i in 0 until 3) {
            val leftBack = keyPlace.place(
                0,
                i,
                KeyPlaceholder.placeHolderBackLeft()
                    .move(-wallsSettings.outerHorizontalOffset, 0, wallsSettings.outerBorderZOffset)
            )
            val leftFront = keyPlace.place(
                0,
                i,
                KeyPlaceholder.placeHolderFrontLeft()
                    .move(-wallsSettings.outerHorizontalOffset, 0, wallsSettings.outerBorderZOffset)
            )

            points.add(
                bottomEdgePatcher.leftPoint(leftBack).move.projectionZ(0.0)
            )

            points.add(
                bottomEdgePatcher.leftPoint(leftFront).move.projectionZ(0.0)
            )
        }

        val left = keyPlace.place(
            0,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )

        points.add(
            left.move.projectionZ(0.0)
        )

        return points
    }

    private fun createRightPoints(): List<V3d> {
        val points = mutableListOf<V3d>()

        for (i in 0 until 3) {
            val rightBack = keyPlace.place(
                cfg.lastCol,
                i,
                KeyPlaceholder.placeHolderBackRight()
                    .move(wallsSettings.outerHorizontalOffset, 0, wallsSettings.outerBorderZOffset)
            )
            val rightFront = keyPlace.place(
                cfg.lastCol,
                i,
                KeyPlaceholder.placeHolderFrontRight()
                    .move(wallsSettings.outerHorizontalOffset, 0, wallsSettings.outerBorderZOffset)
            )

            points.add(
                bottomEdgePatcher.rightPoint(rightBack).move.projectionZ(0.0)
            )
            points.add(
                bottomEdgePatcher.rightPoint(rightFront).move.projectionZ(0.0)
            )
        }
        return points
    }

    private fun createControllerPoints(): List<V3d> {
        val result = mutableListOf<V3d>()
        val controllerHolderWall = ControllerHolderWall(cfg.wallsSettings, keyPlace)
        val left = keyPlace.place(
            0,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )

        val right = keyPlace.place(
            2,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val mid = keyPlace.place(
            1,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val midLeft = keyPlace.place(
            0,
            0,
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val backControllerLeft = controllerHolderWall.getWallPoint(left.move)
        val backControllerRight = controllerHolderWall.getWallPoint(right.move)
        val backControllerMid = controllerHolderWall.getWallPoint(mid.move)
        val backControllerMidLeft = controllerHolderWall.getWallPoint(midLeft.move)

        result.add(backControllerLeft.projectionZ(0.0))
        result.add(backControllerRight.projectionZ(0.0))
        result.add(backControllerMid.projectionZ(0.0))
        result.add(backControllerMidLeft.projectionZ(0.0))
        return result
    }

    private fun createThumbsPoints(): List<V3d> {
        val result = mutableListOf<V3d>()

        //backLeft
        val backLeft1 = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val backLeft2 = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderBackLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )

        //frontRight
        val frontRight1 = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val frontRight2 = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderFrontRight()
                .move(wallsSettings.outerRightOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        //frontLeft
        val frontLeft1 = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val frontLeft2 = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        result.add(backLeft1.move.projectionZ(0.0))
        result.add(backLeft2.move.projectionZ(0.0))

        result.add(frontRight1.move.projectionZ(0.0))
        result.add(frontRight2.move.projectionZ(0.0))

        result.add(frontLeft1.move.projectionZ(0.0))
        result.add(frontLeft2.move.projectionZ(0.0))

        val frontRightL = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val frontLeftM = thumbKeyPlace.placeM(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val frontRightM = thumbKeyPlace.placeM(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val frontLeftR = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        result.add(frontRightL.move.projectionZ(0.0))
        result.add(frontLeftM.move.projectionZ(0.0))

        result.add(frontRightM.move.projectionZ(0.0))
        result.add(frontLeftR.move.projectionZ(0.0))

        if (cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons) {
            val frontRight21 = thumbKeyPlace.placeR2(
                KeyPlaceholder.placeHolderFrontRight()
                    .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )
            val frontRight22 = thumbKeyPlace.placeR2(
                KeyPlaceholder.placeHolderFrontRight()
                    .move(wallsSettings.outerRightOffset, 0.0, wallsSettings.outerBorderZOffset)
            )
            val frontLeft3 = thumbKeyPlace.placeR2(
                KeyPlaceholder.placeHolderFrontLeft()
                    .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )

            val frontLeft21 = thumbKeyPlace.placeL2(
                KeyPlaceholder.placeHolderFrontLeft()
                    .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
            )

            val frontLeft22 = thumbKeyPlace.placeL2(
                KeyPlaceholder.placeHolderFrontLeft()
                    .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
            )

            result.add(frontRight21.move.projectionZ(0.0))
            result.add(frontRight22.move.projectionZ(0.0))

            result.add(frontLeft21.move.projectionZ(0.0))
            result.add(frontLeft22.move.projectionZ(0.0))

            result.add(frontLeft3.move.projectionZ(0.0))
        }

        return result
    }
}
