package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottom;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottomLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottomRight;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTop;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTopLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTopRight;
import static com.github.grishberg.cad3d.keyboard.Utils.hull;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.models.Abstract3dModel;
import java.util.ArrayList;

public class Walls {

    private final KeyboardConfig cfg;
    private final KeyPlace keyPlace;
    private final ArrayList<Abstract3dModel> models = new ArrayList<>();

    public Walls(KeyboardConfig cfg, KeyPlace keyPlace) {
        this.cfg = cfg;
        this.keyPlace = keyPlace;
    }


    public Abstract3dModel borders() {
        models.clear();

        //columns
        double bordersOffset = cfg.getBordersOffset();
        double firstColunsBordersOffset = 4;
        double horizontalOffset = 8;
        double borderZOffset = -2;
        thumbBorders(horizontalOffset, borderZOffset);

        matrixBorders(bordersOffset, firstColunsBordersOffset, horizontalOffset, borderZOffset);


        return union(models);
    }

    private void matrixBorders(
        double bordersOffset,
        double firstColunsBordersOffset,
        double horizontalOffset,
        double borderZOffset
    ) {
        for (int column = 0; column < cfg.getColumnsCount(); column++) {
            bordersOffset = cfg.getBordersOffset();

            // back columns
            addHull(
                keyPlace.place(column, 0, placeHolderTop()),
                keyPlace.place(column, 0, placeHolderTop().move(0, bordersOffset, borderZOffset))
            );

            if (column < 2) {
                bordersOffset = firstColunsBordersOffset;
            }
            //fornt columns
            addHull(
                keyPlace.place(column, cfg.getLastRow(), placeHolderBottom()),
                keyPlace.place(
                    column,
                    cfg.getLastRow(),
                    placeHolderBottom().move(0, -bordersOffset, borderZOffset)
                )
            );
        }

        for (int column = 0; column < cfg.getColumnsCount() - 1; column++) {
            bordersOffset = cfg.getBordersOffset();
            // back diagonals
            addHull(
                keyPlace.place(column, 0, placeHolderTopRight()),
                keyPlace.place(column + 1, 0, placeHolderTopLeft()),
                keyPlace.place(
                    column,
                    0,
                    placeHolderTopRight().move(0, bordersOffset, borderZOffset)
                ),
                keyPlace.place(
                    column + 1,
                    0,
                    placeHolderTopLeft().move(0, bordersOffset, borderZOffset)
                )
            );

            if (column < 2) {
                bordersOffset = firstColunsBordersOffset;
            }
            // front diagonals
            addHull(
                keyPlace.place(column, cfg.getLastRow(), placeHolderBottomRight()),
                keyPlace.place(column + 1, cfg.getLastRow(), placeHolderBottomLeft()),
                keyPlace.place(
                    column,
                    cfg.getLastRow(),
                    placeHolderBottomRight().move(0, -bordersOffset, borderZOffset)
                ),
                keyPlace.place(
                    column + 1,
                    cfg.getLastRow(),
                    placeHolderBottomLeft().move(0, -bordersOffset, borderZOffset)
                )
            );

            bordersOffset = cfg.getBordersOffset();
            for (int row = 0; row < cfg.getRowsCount(); row++) {
                addHull(
                    keyPlace.place(0, row, placeHolderLeft())
                    ,
                    keyPlace.place(
                        0,
                        row,
                        placeHolderLeft().move(-horizontalOffset, 0, borderZOffset)
                    ));

                addHull(
                    keyPlace.place(cfg.getLastCol(), row, placeHolderRight())
                    ,
                    keyPlace.place(
                        cfg.getLastCol(),
                        row,
                        placeHolderRight().move(horizontalOffset, 0, borderZOffset)
                    ));
            }

        }

        for (int row = 0; row < cfg.getRowsCount() - 1; row++) {
            addHull(
                keyPlace.place(0, row, placeHolderBottomLeft()),
                keyPlace.place(0, row + 1, placeHolderTopLeft()),

                keyPlace.place(
                    0,
                    row,
                    placeHolderBottomLeft().move(-horizontalOffset, 0, borderZOffset)
                ),
                keyPlace.place(
                    0,
                    row + 1,
                    placeHolderTopLeft().move(-horizontalOffset, 0, borderZOffset)
                )
            );

            addHull(
                keyPlace.place(cfg.getLastCol(), row, placeHolderBottomRight()),
                keyPlace.place(cfg.getLastCol(), row + 1, placeHolderTopRight()),

                keyPlace.place(
                    cfg.getLastCol(),
                    row,
                    placeHolderBottomRight().move(horizontalOffset, 0, borderZOffset)
                ),
                keyPlace.place(
                    cfg.getLastCol(),
                    row + 1,
                    placeHolderTopRight().move(horizontalOffset, 0, borderZOffset)
                )
            );
        }

        // corners
        //left back
        addHull(
            keyPlace.place(0, 0, placeHolderTopLeft().move(0, bordersOffset, borderZOffset)),
            keyPlace.place(0, 0, placeHolderTopLeft().move(-horizontalOffset, 0, borderZOffset)),
            keyPlace.place(0, 0, placeHolderTopLeft())
        );

        //left front
        addHull(
            keyPlace.place(
                0,
                cfg.getLastRow(),
                placeHolderBottomLeft().move(0, -firstColunsBordersOffset, borderZOffset)
            ),
            keyPlace.place(
                0,
                cfg.getLastRow(),
                placeHolderBottomLeft().move(-horizontalOffset, 0, borderZOffset)
            ),
            keyPlace.place(0, cfg.getLastRow(), placeHolderBottomLeft())
        );

        // right back
        addHull(
            keyPlace.place(
                cfg.getLastCol(),
                0,
                placeHolderTopRight().move(0, bordersOffset, borderZOffset)
            ),
            keyPlace.place(
                cfg.getLastCol(),
                0,
                placeHolderTopRight().move(horizontalOffset, 0, borderZOffset)
            ),
            keyPlace.place(cfg.getLastCol(), 0, placeHolderTopRight())
        );

        // right front
        addHull(
            keyPlace.place(
                cfg.getLastCol(),
                cfg.getLastRow(),
                placeHolderBottomRight().move(0, -bordersOffset, borderZOffset)
            ),
            keyPlace.place(
                cfg.getLastCol(),
                cfg.getLastRow(),
                placeHolderBottomRight().move(horizontalOffset, 0, borderZOffset)
            ),
            keyPlace.place(cfg.getLastCol(), cfg.getLastRow(), placeHolderBottomRight())
        );
    }

    private void thumbBorders(double horizontalOffset, double borderZOffset) {
        double bordersOffset;
        bordersOffset = 4;

        //corners
        //left front
        addHull(
            ThumbKeyPlace.place3(placeHolderBottomLeft().move(0, -bordersOffset, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderBottomLeft().move(-horizontalOffset, 0, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderBottomLeft())
        );

        //left back
        addHull(
            ThumbKeyPlace.place3(placeHolderTopLeft().move(0, bordersOffset, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderTopLeft().move(-horizontalOffset, 0, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderTopLeft())
        );

        //right front
        addHull(
            ThumbKeyPlace.place1(placeHolderBottomRight().move(0, -bordersOffset, borderZOffset)),
            ThumbKeyPlace.place1(placeHolderBottomRight().move(bordersOffset, 0, borderZOffset)),
            ThumbKeyPlace.place1(placeHolderBottomRight())
        );

        //right back
        addHull(
            ThumbKeyPlace.place1(placeHolderTopRight().move(0, bordersOffset, borderZOffset)),
            ThumbKeyPlace.place1(placeHolderTopRight().move(bordersOffset, 0, borderZOffset)),
            ThumbKeyPlace.place1(placeHolderTopRight())
        );

        // thumb borders;
        addHull(
            ThumbKeyPlace.place1(placeHolderTop()),
            ThumbKeyPlace.place1(placeHolderTop().move(0, bordersOffset, borderZOffset))
        );
        addHull(
            ThumbKeyPlace.place2(placeHolderTop()),
            ThumbKeyPlace.place2(placeHolderTop().move(0, bordersOffset, borderZOffset))
        );
        addHull(
            ThumbKeyPlace.place3(placeHolderTop()),
            ThumbKeyPlace.place3(placeHolderTop().move(0, bordersOffset, borderZOffset))
        );

        addHull(
            ThumbKeyPlace.place1(placeHolderBottom()),
            ThumbKeyPlace.place1(placeHolderBottom().move(0, -bordersOffset, borderZOffset))
        );
        addHull(
            ThumbKeyPlace.place2(placeHolderBottom()),
            ThumbKeyPlace.place2(placeHolderBottom().move(0, -bordersOffset, borderZOffset))
        );
        addHull(
            ThumbKeyPlace.place3(placeHolderBottom()),
            ThumbKeyPlace.place3(placeHolderBottom().move(0, -bordersOffset, borderZOffset))
        );

        // right
        addHull(
            ThumbKeyPlace.place1(placeHolderRight()),
            ThumbKeyPlace.place1(placeHolderRight().move(bordersOffset, 0, borderZOffset))
        );

        // left
        addHull(
            ThumbKeyPlace.place3(placeHolderLeft()),
            ThumbKeyPlace.place3(placeHolderLeft().move(-horizontalOffset, 0, borderZOffset))
        );

        //middle back
        addHull(
            ThumbKeyPlace.place1(placeHolderBottomLeft()),
            ThumbKeyPlace.place1(placeHolderBottomLeft().move(0, -bordersOffset, borderZOffset)),
            ThumbKeyPlace.place2(placeHolderBottomRight()),
            ThumbKeyPlace.place2(placeHolderBottomRight().move(0, -bordersOffset, borderZOffset))
        );

        addHull(
            ThumbKeyPlace.place2(placeHolderBottomLeft()),
            ThumbKeyPlace.place2(placeHolderBottomLeft().move(0, -bordersOffset, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderBottomRight()),
            ThumbKeyPlace.place3(placeHolderBottomRight().move(0, -bordersOffset, borderZOffset))
        );

        // middle front
        addHull(
            ThumbKeyPlace.place1(placeHolderTopLeft()),
            ThumbKeyPlace.place1(placeHolderTopLeft().move(0, bordersOffset, borderZOffset)),
            ThumbKeyPlace.place2(placeHolderTopRight()),
            ThumbKeyPlace.place2(placeHolderTopRight().move(0, bordersOffset, borderZOffset))
        );

        addHull(
            ThumbKeyPlace.place2(placeHolderTopLeft()),
            ThumbKeyPlace.place2(placeHolderTopLeft().move(0, bordersOffset, borderZOffset)),
            ThumbKeyPlace.place3(placeHolderTopRight()),
            ThumbKeyPlace.place3(placeHolderTopRight().move(0, bordersOffset, borderZOffset))
        );


    }

    private void addHull(Abstract3dModel... children) {
        models.add(hull(children));
    }
}
