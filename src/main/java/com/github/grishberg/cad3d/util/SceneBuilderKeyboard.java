package com.github.grishberg.cad3d.util;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolder;
import static com.github.grishberg.cad3d.keyboard.Utils.sphere;

import com.github.grishberg.cad3d.keyboard.Connections;
import com.github.grishberg.cad3d.keyboard.ControlPointsController;
import com.github.grishberg.cad3d.keyboard.KeyHolderBottomWalls;
import com.github.grishberg.cad3d.keyboard.KeyPlace;
import com.github.grishberg.cad3d.keyboard.KeyPlaceHoles;
import com.github.grishberg.cad3d.keyboard.KeySwitchHoles;
import com.github.grishberg.cad3d.keyboard.ThumbConnections;
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace;
import com.github.grishberg.cad3d.keyboard.Walls;
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import com.github.grishberg.cad3d.keyboard.wristrest.WristRest;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.models.IModel;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.utils.StlExporter;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.VertexHolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import javax.swing.*;

public class SceneBuilderKeyboard implements SceneBuilder {

    /**
     * How many bytes per float.
     */
    private final int mBytesPerFloat = 4;
    private int resolution = 15;     // Количество промежуточных точек между заданными точками


    private static final Color DEFAULT_COLOR = Color.GRAY;

    public final List<VertexHolder> buffers;

    private final KeyboardConfig cfg;
    private final KeyPlace keyPlace;
    private final ControlPointsController pointsController;
    private SceneBuilder.ReadyListener listener;
    private Executor executor = Executors.newSingleThreadExecutor();
    private volatile Abstract3dModel matrix;

    public SceneBuilderKeyboard(
        KeyboardConfig cfg,
        KeyPlace keyPlace,
        ControlPointsController pointsController
    ) {
        this.cfg = cfg;
        this.keyPlace = keyPlace;
        this.pointsController = pointsController;
        buffers = new ArrayList<>();
        pointsController.addListener((row, col) -> rebuildCaseAndInvalidate());
    }

    @Override
    public void setListener(ReadyListener listener) {
        this.listener = listener;
    }

    @Override
    public void requestBuffers() {
        if (resolution == 0) {
            resolution = 20;
        }
        create3dModels();
    }

    private void rebuildCaseAndInvalidate() {
        create3dModels();
    }

    private void create3dModels() {
        executor.execute(() -> {
            buffers.clear();
            createKeyMatrix();

            createThumbKeyPlace();

            Abstract3dModel connections = createConnections();

            createBorders();

            createKeycaps();

            createPlaceholders();

            SwingUtilities.invokeLater(() -> {
                if (listener != null) {
                    listener.onReady(buffers);
                }
            });

            new Thread(() -> {
                try {
                    FacetGenerationContext context = new ColorFacetGenerationContext(DEFAULT_COLOR);
                    context.setFn(20);
                    StlExporter.saveStringToFile(
                        connections.toCSG(context).getVerticesAndColorsAsFloatArray(),
                        "connections.stl"
                    );
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        });


        //   createAndAdd(keyPlaceHoles(), new Color(127, 5, 60), 15);

        //createAndAdd(cornerModel(), Color.ORANGE, 20);

    }

    private void createThumbKeyPlace() {
        Abstract3dModel keycap =
            new Cube(
                cfg.getKeyswitchWidth(),
                cfg.getKeyswitchHeight(),
                cfg.getSaProfileKeyHeight()
            ).move(0, 0, 10);

        createAndAdd(ThumbKeyPlace.thumbPlace(keycap), Color.BLUE);
        createAndAdd(ThumbKeyPlace.thumbPlace(placeHolder()), Color.ORANGE);
    }

    private Abstract3dModel keyHoles() {
        return new KeySwitchHoles(cfg, keyPlace).build();
    }

    private Abstract3dModel keyPlaceHoles(double offset) {
        return new KeyPlaceHoles(cfg, keyPlace).build(offset);
    }

    private Abstract3dModel wristRestMount() {
        // left back
        return new Cylinder(42, 6).move(-56, -88, -2)
            .addModel(
                // left front
                new Cylinder(56, 6).move(-53, -142, -4)
            )
            .addModel(
                // right back
                new Cylinder(48, 6).move(60, -85, -6)
            ).addModel(
                // right front
                new Cylinder(62, 6).move(53, -140, -6)
            );
    }

    private Abstract3dModel keyPlaceBottomWalls() {
        return new KeyHolderBottomWalls(cfg, keyPlace).build();
    }

    private void createPlaceholders() {
        for (int column = 0; column < cfg.getColumnsCount(); column++) {
            for (int row = 0; row < cfg.getRowsCount(); row++) {
                createAndAdd(keyPlace.place(column, row, placeHolder()), new Color(30, 127, 40));
            }
        }
    }

    private void createKeycaps() {
        for (int column = 0; column < cfg.getColumnsCount(); column++) {
            for (int row = 0; row < cfg.getRowsCount(); row++) {
                Abstract3dModel obj =
                    new Cube(
                        cfg.getKeyswitchWidth(),
                        cfg.getKeyswitchHeight(),
                        cfg.getSaProfileKeyHeight()
                    ).move(0, 0, 10);

                createAndAdd(keyPlace.place(column, row, obj), Color.PINK);
            }
        }
    }

    private Abstract3dModel createConnections() {
        Abstract3dModel connections = new Connections(cfg, keyPlace).buildConnections();
        createAndAdd(connections, DEFAULT_COLOR);
        createAndAdd(new ThumbConnections(cfg, keyPlace).buildThumbPlaceConnections(), DEFAULT_COLOR);
        return connections;
    }

    private Abstract3dModel createBorders() {
        Abstract3dModel borders = new Walls(cfg, keyPlace).borders();
        createAndAdd(borders, Color.lightGray);
        return borders;
    }

    private void createKeyMatrix() {


        //        if (matrix == null) {
        //            matrix = keyPlaceBottomWalls()
        //                .subtractModel(keyPlaceHoles(0))
        //                .subtractModel(keyHoles());
        //        }
        //        createAndAdd(matrix, new Color(0, 127, 0));
        //


        //System.out.println(stl);
        //Log.d("<DBG>", "stl size = " + stl.length());
    }

    private void createWristRest() {
        showControlPoints(pointsController.controlPoints);

        // Задаем параметры поверхности
        double thickness = 4;  // Толщина поверхности
/*
        Abstract3dModel topSurface = new SmoothSurface3(
            BicubicSurfaceSpline3.bSplineSurface(pointsController.controlPoints, resolution),
            thickness
        );
*/

        Abstract3dModel wristRest = WristRest.build()
            .subtractModel(new Cube(300, 300, 50).move(0, 0, -25));

        createAndAdd(
            wristRest
            // .subtractModel(keyPlaceHoles(-4))
            , Color.ORANGE
            , 30
        );

        new Thread(() -> {
            try {
                FacetGenerationContext context = new ColorFacetGenerationContext(DEFAULT_COLOR);
                context.setFn(20);
                StlExporter.saveStringToFile(
                    wristRest.toCSG(context).getVerticesAndColorsAsFloatArray(),
                    "out.stl"
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    private void showControlPoints(V3d[][] points) {
        Color[] colors = {Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA};

        for (int rowIndex = 0; rowIndex < points.length; rowIndex++) {
            for (int colIndex = 0; colIndex < points[rowIndex].length; colIndex++) {
                createAndAdd(
                    sphere(3).move(points[rowIndex][colIndex]),
                    colors[rowIndex % colors.length]
                );
            }
        }
    }

    private void createAndAdd(IModel model) {
        createAndAdd(model, DEFAULT_COLOR);
    }

    private VertexHolder createAndAdd(IModel model, Color color) {
        return createAndAdd(model, color, 6);
    }

    private VertexHolder createAndAdd(IModel model, Color color, int fn) {
        FacetGenerationContext context = new ColorFacetGenerationContext(color);
        context.setFn(fn);
        CSG csg = model.toCSG(context);
        VertexHolder vertex = csg.getVerticesAndColorsAsFloatArray();
        buffers.add(vertex);
        return vertex;
    }
}
