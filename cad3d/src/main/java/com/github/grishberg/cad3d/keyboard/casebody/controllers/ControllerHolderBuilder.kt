package com.github.grishberg.cad3d.keyboard.casebody.controllers

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.casebody.controllers.battery.Battery
import com.github.grishberg.cad3d.keyboard.casebody.controllers.battery.NoBattery
import com.github.grishberg.cad3d.keyboard.casebody.controllers.switcher.Switcher
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.screws.ScrewWallPlaces
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModel
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.enums.Side
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder
import eu.printingin3d.javascad.models.Hull
import eu.printingin3d.javascad.tranzitions.Union
import eu.printingin3d.javascad.utils.Color

/**
 * Creates controller holder.
 */
class ControllerHolderBuilder(
    private val cfg: KeyboardConfig,
    private val controller: Controller,
    private val controllerPlace: ControllerPlace,
    private val switcherPlace: SwitcherPlace,
    private val controllerHolderDimensions: ControllerHolderDimensions,
    private val screwWallPlaces: ScrewWallPlaces,
    private val switcher: Switcher,
    private val battery: Battery,
) {

    private val verticalPanelWidth = 1.5
    private val wall = 1.0

    fun create(showPreview: Boolean): ModelHolder {
        var models = createControllerModel()
        var model: Abstract3dModel = Union(models)

        val vertexHolders = mutableListOf<VertexHolder>()
        models.forEach {
            vertexHolders.add(fromModel(it, 20))
        }

        if (controller.isWireless && battery != NoBattery) {
            val batteryHolder = placeBatteryHolder(battery.create())
            model = model.addModel(batteryHolder)
            vertexHolders.add(
                fromModel(
                    model, Color.YELLOW, 20
                )
            )

            if (showPreview) {
                vertexHolders.add(
fromModel(
                        placeBatteryHolder(battery.createBatteryPreview()), Color.RED, 20
                    )
                )
            }
        }
        return ModelHolder(model, vertexHolders)
    }

    private fun placeBatteryHolder(o: Abstract3dModel): Abstract3dModel {
        return controllerPlace.place(o.rotate(Angles3d.zOnly(25.0))).move(7.0, -battery.depth + 20.7, -4.0)
    }

    private fun createControllerModel(): List<Abstract3dModel> {
        val base = createBase().withColor(Color.BISQUE)
        val height = 10.0
        val screwOffset = cfg.screwHolderWallhickness + cfg.screwNutHoleDiameter / 2
        val panelWidth = controllerHolderDimensions.distanceBetweenControllerHolderMountX - 2 * screwOffset - 2.0
        val verticalWall = Cube(panelWidth, verticalPanelWidth, height).align(Side.BACK_IN_CENTER, Side.TOP_OUT, base)

        val holder = createHolder().withColor(Color.LIGHT_BLUE)

        val controllerBody = controller.createBody(controllerPlace)

        return listOf(
            base,
            createControllerHolderCylinders().align(Side.CENTER, controllerBody).move(0, -0.2, 1.3)
                .withColor(Color.RED),
            holder.align(Side.CENTER, controllerBody).align(Side.BOTTOM_IN, base).align(Side.FRONT_OUT, verticalWall)
                .subtractModel(usbHole()),
            verticalWall.subtractModel(usbHole()).subtractModel(switcherPlace.place(switcher.createSwitcherHole()))
                .withColor(Color.YELLOW)
        )
    }

    private fun createControllerHolderCylinders(): Abstract3dModel {
        val diam = 1.5
        val length = 2.0
        val cylinder = Cylinder(length, Radius.fromDiameter(diam)).rotate(Angles3d.yOnly(90.0))
        val offset = 7.0
        val depth = controller.depth / 2.0 + 0.3
        return cylinder.move(-offset, depth, 0.0).addModel(cylinder.move(offset, depth, 0.0))
            .addModel(cylinder.move(offset, -depth, 0.0)).addModel(cylinder.move(-offset, -depth, 0.0))
    }

    private fun usbHole(): Abstract3dModel {
        val diameter = 3.5
        val width = 10.0

        val cylinder = Cylinder(5.0, Radius.fromDiameter(diameter)).rotate(Angles3d.xOnly(90.0))
        val usb = Hull(
            cylinder.moveX(-width / 2 + diameter / 2), cylinder.moveX(width / 2 - diameter / 2)
        ).moveZ(diameter / 2 + 1.4)
        return controllerPlace.place(controller.placeUsbPort(usb))
    }

    private fun createHolder(): Abstract3dModel {
        val holeWidth = controller.width + 0.2
        val holeDepth = controller.depth + 0.7
        val holeHeight = 6.0
        val height = controller.height + 7
        val body = Cube(controller.width + 2 * wall, controller.depth + wall, height)
            .subtractModel(Cube(holeWidth, holeDepth, holeHeight).move(0.0, wall / 2 + 0.1, 4.3))
            .subtractModel(Cube(30.0, 20.0, 5.0).moveZ(2.5)) // поперечная дырка для вынимания
            .subtractModel(
                Cube(
                    controller.width - 2 * wall,
                    controller.depth - 2 * wall,
                    20.0
                ).moveY(wall / 2)
            ) // сквозная дырка в держателе
        return body
    }

    private fun createBase(): Abstract3dModel {
        val cylinder = Cube(7.0, 6.0, cfg.controllerPlateHeight)
        val hole = Cylinder(cfg.controllerPlateHeight + 5, Radius.fromDiameter(cfg.screwBoltDiameter + 0.5))
        val horizontal = Hull(
            screwWallPlaces.placeControllerScrews(
                cylinder, ScrewWallPlaces.HeightMode.ControllerHolder, ScrewWallPlaces.ControllerMode.Back
            )
        )

        val vertical = Hull(
            screwWallPlaces.placeControllerScrews(
                cylinder, ScrewWallPlaces.HeightMode.ControllerHolder, ScrewWallPlaces.ControllerMode.Side
            )
        )

        val holes = screwWallPlaces.placeControllerScrews(
            hole, ScrewWallPlaces.HeightMode.ControllerHolder, ScrewWallPlaces.ControllerMode.All
        )

        return Union(horizontal, vertical).moveZ(cfg.controllerPlateHeight / 2).subtractModel(holes)
    }
}
