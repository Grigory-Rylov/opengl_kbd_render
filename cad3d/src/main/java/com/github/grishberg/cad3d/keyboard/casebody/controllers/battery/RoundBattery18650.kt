package com.github.grishberg.cad3d.keyboard.casebody.controllers.battery

import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.Angles3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.models.Cylinder

class RoundBattery18650 : Battery {

    private val innerWidth = 18.7
    private val innerDepth = 65.7
    private val innerHeight = 18.7
    private val wallWidth = 1.5
    private val contactsHolderDepth = 4

    override val width: Double = innerWidth + wallWidth * 2
    override val depth: Double = innerDepth + wallWidth * 2 + contactsHolderDepth * 2 + 1
    override val height: Double = innerHeight + wallWidth

    override fun create(): Model {
        return Cylinder(depth, Radius.fromDiameter(height)).rotate(Angles3d.xOnly(90.0)).addModel(
                Cube(width, depth, height / 2).moveZ(-height / 4)
            ).subtractModel(
                Cylinder(depth - wallWidth * 2, Radius.fromDiameter(innerHeight)).rotate(Angles3d.xOnly(90.0))
            ).subtractModel(
                Cube(width - 2 * wallWidth, depth - wallWidth * 2, height / 2).moveZ(-height / 4)
            ).subtractModel(Cube(width + 5, depth - 25.0, 20.0).moveZ(height / 2))
            .subtractModel(Cube(width + 5, 15.0, 20.0).move(0.0, depth / 2, height / 2))
            .subtractModel(Cube(width + 5, 15.0, 20.0).move(0.0, -depth / 2, height / 2))
            .addModel(createContactsHolder())
            .moveZ(height / 2)

    }

    private fun createContactsHolder(): Model {
        val holder = Cube(width, 1.0, height / 2)
            .subtractModel(
                Cube(width - 8.0, 2.0, height/2 + 1)
            ).moveZ(-height / 4)

        return holder.moveY(depth/2 - 3.0)
            .addModel(holder.moveY(-depth/2 + 3.0))
    }

    override fun createBatteryPreview(): Model {
        return Cylinder(innerDepth, Radius.fromDiameter(innerHeight)).rotate(Angles3d.xOnly(90.0))
            .moveZ(innerHeight / 2)
    }
}
