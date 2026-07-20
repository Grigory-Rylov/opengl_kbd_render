package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.javascad.models.Model

class SingleColumn3ButtonsThumbsBordersBuilder(
    private val thumbKeyPlace: ThumbKeyPlace,
) : ThumbBorders {

    override fun create(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        isWallMode: Boolean,
    ): List<Model> {
        val models = ArrayList<Model>()
        //corners
        //left back
        models.add(cornerWallBuilder.backLeft { obj -> thumbKeyPlace.placeL(obj) })
        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL(obj) })

        // right back
        models.addAll(cornerWallBuilder.backRight { obj -> thumbKeyPlace.placeR(obj) })
        // right front
        models.add(cornerWallBuilder.frontRight { obj -> thumbKeyPlace.placeR(obj) })

        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR(o) })

        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeR(o) })
        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeM(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeM(o) })
        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL(o) })

        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })
        models.add(wallsBuilder.rightWall { o -> thumbKeyPlace.placeR(o) })

        models.add(
            wallsBuilder.backMidWall(
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
                leftPlace = { o -> thumbKeyPlace.placeM(o) },
            )
        )

        if (!isWallMode) {
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeM(o) },
                    rightPlace = { o -> thumbKeyPlace.placeR(o) },
                )
            )
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeL(o) },
                    rightPlace = { o -> thumbKeyPlace.placeM(o) },
                )
            )

//            models.add(
//                wallsBuilder.backMidWall(
//                    leftPlace = { o -> thumbKeyPlace.placeL(o) },
//                    rightPlace = { o -> thumbKeyPlace.placeM(o) },
//                )
//            )
        }

        models.add(
            wallsBuilder.midEdge(
                midPlace = { o -> thumbKeyPlace.placeM(o) },
                leftPlace = { o -> thumbKeyPlace.placeL(o) },
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
            )
        )
        return models
    }
}
