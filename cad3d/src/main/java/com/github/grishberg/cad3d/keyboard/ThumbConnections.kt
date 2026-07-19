package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.javascad.models.Abstract3dModel

class ThumbConnections(private val cfg: KeyboardConfig, private val thumbKeyPlace: ThumbKeyPlace) {

    private val models = ArrayList<Abstract3dModel>()
    fun buildThumbPlaceConnections(): Abstract3dModel {
        models.clear()

        return when(cfg.thumbClusterSettings.type){
            ThumbClusterMode.SingleColumn3Buttons -> create3ThumbsConnection()
            ThumbClusterMode.SingleColumn4Buttons -> create3ThumbsConnection()
            ThumbClusterMode.TwoRows5Buttons -> createTwoRows5Buttons()
        }
    }

    private fun create3ThumbsConnection(): Abstract3dModel {
        addHull(
            thumbKeyPlace.placeR(placeHolderLeft()), thumbKeyPlace.placeM(placeHolderRight())
        )
        addHull(
            thumbKeyPlace.placeM(placeHolderLeft()), thumbKeyPlace.placeL(placeHolderRight())
        )
        return Utils.union(models)
    }

    private fun createTwoRows5Buttons(): Abstract3dModel {
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

    private fun addHull(vararg children: Abstract3dModel) {
        models.add(Utils.hull(*children))
    }
}
