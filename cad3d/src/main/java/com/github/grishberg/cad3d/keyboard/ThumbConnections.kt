package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.javascad.models.Model

class ThumbConnections(private val cfg: KeyboardConfig, private val thumbKeyPlace: ThumbKeyPlace) {

    private val models = ArrayList<Model>()
    fun buildThumbPlaceConnections(): Model {
        models.clear()

        return when(cfg.thumbClusterSettings.type){
            ThumbClusterMode.SingleColumn3Buttons -> create3ThumbsConnection()
            ThumbClusterMode.SingleColumn4Buttons -> create3ThumbsConnection()
            ThumbClusterMode.TwoRows5Buttons -> createTwoRows5Buttons()
        }
    }

    private fun create3ThumbsConnection(): Model {
        addHull(
            thumbKeyPlace.placeR(placeHolderLeft()), thumbKeyPlace.placeM(placeHolderRight())
        )
        addHull(
            thumbKeyPlace.placeM(placeHolderLeft()), thumbKeyPlace.placeL(placeHolderRight())
        )
        return Utils.union(models)
    }

    private fun createTwoRows5Buttons(): Model {
        addHull(
            thumbKeyPlace.placeR(placeHolderLeft()), thumbKeyPlace.placeM(placeHolderRight())
        )
        addHull(
            thumbKeyPlace.placeM(placeHolderLeft()), thumbKeyPlace.placeL(placeHolderRight())
        )

        addHull(
            thumbKeyPlace.placeR2(placeHolderLeft()), thumbKeyPlace.placeL2(placeHolderRight())
        )
        return Utils.union(models)
    }

    private fun addHull(vararg children: Model) {
        models.add(Utils.hull(*children))
    }
}
