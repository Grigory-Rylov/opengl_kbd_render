package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.openscad.utils.Color;

public class DbgConfig {

    public static Color[] COLORS = {
        new Color(255, 175, 175), // pink (ваш пример)
        new Color(255, 0, 0),     // red
        new Color(0, 255, 0),     // green
        new Color(0, 0, 255),     // blue
        new Color(255, 255, 0),   // yellow
        new Color(255, 0, 255),   // magenta
        new Color(0, 255, 255),   // cyan
        new Color(255, 165, 0),   // orange
        new Color(128, 0, 128),   // purple
        new Color(255, 192, 203), // pink (light)
        new Color(165, 42, 42),   // brown
        new Color(0, 128, 0),     // green (dark)
        new Color(0, 0, 128),     // navy
        new Color(255, 215, 0),   // gold
        new Color(75, 0, 130),    // indigo
        new Color(255, 105, 180), // hot pink
        new Color(50, 205, 50),   // lime green
        new Color(139, 69, 19),   // saddle brown
        new Color(128, 128, 0),   // olive
        new Color(0, 191, 255)    // deep sky blue
    };

    private static double SCALE = 1.0;

    public static double LINE_THICKNESS = 0.1 * SCALE;
    public static double LINE_THICKNESS_1 = 0.2 * SCALE;
    public static double LINE_THICKNESS_2 = 0.3 * SCALE;

    public static double POINT_THICKNESS = 0.5 * SCALE;
    public static double POINT_THICKNESS_1 = 0.6 * SCALE;
    public static double POINT_THICKNESS_2 = 0.7 * SCALE;
}
