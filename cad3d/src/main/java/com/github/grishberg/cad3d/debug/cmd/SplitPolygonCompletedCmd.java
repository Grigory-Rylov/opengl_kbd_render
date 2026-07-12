
package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.openscad.utils.Color;
import java.util.ArrayList;
import java.util.List;

public class SplitPolygonCompletedCmd implements DebugCmd {

    private final SplitStartedCmd splitStartedCmd;
    private final List<V3d> front;
    private final List<V3d> back;

    public SplitPolygonCompletedCmd(
        List<V3d> front,
        List<V3d> back,
        SplitStartedCmd splitStartedCmd
    ) {
        this.splitStartedCmd = splitStartedCmd;
        this.front = new ArrayList<V3d>(front);
        this.back = new ArrayList<>(back);
    }

    @Override
    public String getDescription() {
        return "Split completed - front: " + front.size() + " - " + EndCmd.verticesToString(front) +
            ", back = " + back.size() + " - " + EndCmd.verticesToString(back);
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {
        if (splitStartedCmd != null) {
            splitStartedCmd.render(debugVisualizer);
        }
        for (V3d p : back) {
            debugVisualizer.drawDebugPoint(p, 2, Color.BLUE);
        }

        for (V3d p : front) {
            debugVisualizer.drawDebugPoint(p, 2, Color.GREEN);
        }
    }
} 
