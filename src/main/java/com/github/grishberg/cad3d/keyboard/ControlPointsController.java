package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.Utils.v3d;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

public class ControlPointsController {

    // top
    public V3d[][] controlPoints;

    private List<OnChangeListener> listeners = new ArrayList<>();

    public ControlPointsController(KeyboardConfig cfg, KeyPlace keyPlace) {

        double topOffset = 10;
        double offset = 12;

//        V3d[][] points = {
//            {
//                v3d(-70, 50, 0),
//                v3d(-40, 50, 0),
//                v3d(-16, 50, 0),
//                v3d(0, 50, 0),
//                v3d(20, 55, 0),
//                v3d(40, 55, 0),
//                v3d(66, 55, 0)
//            },
//            {
//                v3d(-70, 45, 0),
//                v3d(-40, 50, 20),
//                v3d(-16, 50, 20),
//                v3d(0, 50, 20),
//                v3d(20, 55, 20),
//                v3d(40, 55, 20),
//                v3d(66, 45, 0)
//            },
//            {  // row 1
//                v3d(-70, 20, 0),
//                v3d(-60, 20, 20),
//                keyPlace.coords(0, 0,
//                    -offset, offset, topOffset
//                ),
//                keyPlace.coords(cfg.getCenterCol(), 0,
//                    0, offset, topOffset
//                ),
//                keyPlace.coords(cfg.getLastCol(), 0,
//                    offset, offset, topOffset
//                ),
//                v3d(70, 15, 15),
//                v3d(76, 25, 0)
//            },
//            { // row 2
//                v3d(-70, 0, 0),
//                v3d(-60, 0, 20),
//                keyPlace.coords(0, cfg.getCenterRow(),
//                    -offset, 0, topOffset
//                ),
//                keyPlace.coords(cfg.getCenterCol(), cfg.getCenterRow(),
//                    0, 0, topOffset
//                ),
//                keyPlace.coords(cfg.getLastCol(), cfg.getCenterRow(),
//                    offset, 0, topOffset
//                ),
//                v3d(70, 0, 20),
//                v3d(76, 0, 0),
//            },
//            { // row 3
//                v3d(-70, -40, 0),
//                v3d(-60, -30, 20),
//                keyPlace.coords(0, cfg.getLastRow(), -offset, -offset, topOffset),
//                keyPlace.coords(cfg.getCenterCol(), cfg.getLastRow(), 0, -offset, topOffset),
//                keyPlace.coords(cfg.getLastCol(), cfg.getLastRow(), offset, -offset, topOffset),
//                v3d(66, -50, 20),
//                v3d(70, -50, 0),
//            },
//            {
//                v3d(-70, -60, 0),
//                v3d(-60, -60, 20),
//                v3d(-26, -60, 20),
//                v3d(0, -60, 20),
//                v3d(20, -65, 20),
//                v3d(40, -65, 20),
//                v3d(60, -65, 0)
//            },
//            {
//                v3d(-70, -80, 0),
//                v3d(-60, -80, 0),
//                v3d(-26, -80, 0),
//                v3d(0, -80, 0),
//                v3d(20, -85, 0),
//                v3d(40, -85, 0),
//                v3d(60, -85, 0)
//            }
//        };

        V3d[][] points = {
            {
                v3d(-60, -80, 5),
                v3d(-40, -80, 30),
                v3d(-10, -90,  15),
                v3d( 16, -60,  15),
                v3d(60, -80, 15),
                v3d(60, -80,  10),
            },
            {
                v3d(-65, -100, 5),
                v3d(-40, -100, 30),
                v3d(-16, -100,  10),
                v3d(16, -100,  10),
                v3d(60, -100, 20),
                v3d(60, -100, 10),
            },
            {
                v3d(-60,-150,  5),
                v3d(-40, -150, 30),
                v3d(-16,-130,  40),
                v3d(16,-130,  40),
                v3d(60, -150, 20),
                v3d(60,-150,  30),
            }
        };
        controlPoints = points;

    }

    public int getColumnsCount() {
        return controlPoints[0].length;
    }

    public int getRowsCount() {
        return controlPoints.length;
    }

    public V3d getPoint(int row, int col) {
        return controlPoints[row][col];
    }

    public void notifyChanged(int row, int col, double x, double y, double z) {
        controlPoints[row][col] = v3d(x, y, z);

        for (OnChangeListener listener: listeners){
            listener.onPointChanged(row, col);
        }
    }

    public void addListener(OnChangeListener listener) {
        listeners.add(listener);
    }

    public interface OnChangeListener{
        void onPointChanged(int row, int col);
    }
}
