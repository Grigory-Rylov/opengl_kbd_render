package com.github.grishberg.cad3d.util;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolder;
import static com.github.grishberg.cad3d.keyboard.Utils.sphere;

import com.github.grishberg.cad3d.keyboard.Connections;
import com.github.grishberg.cad3d.keyboard.ControlPointsController;
import com.github.grishberg.cad3d.keyboard.KeyHolderBottomWalls;
import com.github.grishberg.cad3d.keyboard.KeyPlace;
import com.github.grishberg.cad3d.keyboard.KeyPlaceHoles;
import com.github.grishberg.cad3d.keyboard.KeySwitchHoles;
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace;
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.IModel;
import eu.printingin3d.javascad.models.surfaces.SmoothSurface3;
import eu.printingin3d.javascad.models.surfaces.bicubic.BicubicSurfaceSpline3;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.ColorFacetGenerationContext;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.VertexHolder;
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
    private int resolution = 0;     // Количество промежуточных точек между заданными точками


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
        } else {
            resolution = 4;
        }
        create3dModels();
    }

    private void rebuildCaseAndInvalidate() {
        create3dModels();
    }

    private void create3dModels() {
        executor.execute(() -> {
            buffers.clear();
            createSurfaces();

            createThumbKeyPlace();
            SwingUtilities.invokeLater(() -> {
                if (listener != null) {
                    listener.onReady(buffers);
                }
            });
        });

        // createConnections();

        //createKeycaps();

        // createPlaceholders();

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
    }

    private Abstract3dModel keyHoles() {
        return new KeySwitchHoles(cfg, keyPlace).build();
    }

    private Abstract3dModel keyPlaceHoles(double offset) {
        return new KeyPlaceHoles(cfg, keyPlace).build(offset);
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

    private void createConnections() {
        createAndAdd(new Connections(cfg, keyPlace).buildConnections(), DEFAULT_COLOR);
    }

    private void createSurfaces() {
        showControlPoints(pointsController.controlPoints);

        // Задаем параметры поверхности
        double thickness = 4;  // Толщина поверхности

        Abstract3dModel topSurface = new SmoothSurface3(
            BicubicSurfaceSpline3.bSplineSurface(pointsController.controlPoints, resolution),
            thickness
        );

        createAndAdd(
            topSurface
                .subtractModel(keyPlaceHoles(-4))
            , Color.ORANGE
        );

        if (matrix == null) {
            matrix = keyPlaceBottomWalls()
                .subtractModel(keyPlaceHoles(0))
                .subtractModel(keyHoles());
        }
        createAndAdd(matrix, new Color(0, 127, 0));

        //Abstract3dModel model = top.addModel(front).addModel(back).addModel(left).addModel(right);
        //String stl = StlExporter.exportToSTL(model.toCSG().getVerticesAndColorsAsFloatArray());
        //Log.d("<DBG>", "stl size = " + stl.length());
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
