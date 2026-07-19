package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.javascad.models.Abstract3dModel

class TwoRows5ButtonsMatrixThumbsBordersBuilder(
    private val thumbKeyPlace: ThumbKeyPlace,
): ThumbBorders {

    override fun create(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        isWallMode: Boolean,
    ): List<Abstract3dModel> {
        val models = ArrayList<Abstract3dModel>()
        //corners
        //left back
        models.add(cornerWallBuilder.backLeft { obj -> thumbKeyPlace.placeL(obj) })
        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL2(obj) })

        // right back
        models.addAll(cornerWallBuilder.backRight { obj -> thumbKeyPlace.placeR(obj) })
        // right front
        models.add(cornerWallBuilder.frontRight { obj -> thumbKeyPlace.placeR(obj) })
        models.add(cornerWallBuilder.frontRight { obj -> thumbKeyPlace.placeR2(obj) })

        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR2(o) })
        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeR(o) })
        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeM(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL2(o) })

        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })
        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL2(o) })
        models.add(wallsBuilder.leftMidWall(
            leftPlace = { o -> thumbKeyPlace.placeL(o) },
            rightPlace = { o -> thumbKeyPlace.placeL2(o) },
            ))
        models.add(wallsBuilder.rightWall { o -> thumbKeyPlace.placeR(o) })

        models.add(hull(
            thumbKeyPlace.placeL(KeyPlaceholder.placeHolderFront()),
            thumbKeyPlace.placeL2(KeyPlaceholder.placeHolderBack()),
        ))

        models.add(hull(
            thumbKeyPlace.placeM(KeyPlaceholder.placeHolderFront()),
            thumbKeyPlace.placeR2(KeyPlaceholder.placeHolderBack()),
        ))

        models.add(hull(
            thumbKeyPlace.placeL(KeyPlaceholder.placeHolderFrontRight()),
            thumbKeyPlace.placeL2(KeyPlaceholder.placeHolderBackRight()),

            thumbKeyPlace.placeM(KeyPlaceholder.placeHolderFrontLeft()),
            thumbKeyPlace.placeR2(KeyPlaceholder.placeHolderBackLeft()),
        ))

        models.add(hull(
            thumbKeyPlace.placeR(KeyPlaceholder.placeHolderFrontRight()),
            thumbKeyPlace.placeR2(KeyPlaceholder.placeHolderBackRight()),
            thumbKeyPlace.placeR2(KeyPlaceholder.placeHolderFrontRight()),
        ))


        models.add(
            wallsBuilder.backMidWall(
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
                leftPlace = { o -> thumbKeyPlace.placeM(o) },
            )
        )

        models.add(wallsBuilder.rightDiagonal(
            backKeyPlace = { o -> thumbKeyPlace.placeR(o) },
            frontKeyPlace = { o -> thumbKeyPlace.placeR2(o) },

        ))

        if (!isWallMode) {
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeL2(o) },
                    rightPlace = { o -> thumbKeyPlace.placeR2(o) },
                )
            )

            models.add(
                wallsBuilder.backMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeL(o) },
                    rightPlace = { o -> thumbKeyPlace.placeM(o) },
                )
            )
        }

        return models
    }
}
