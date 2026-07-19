package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import java.util.ArrayList;
import java.util.List;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.VertexPosition;

public class AddVertexNextPoint implements DebugCmd {
    private final V3d lastVertex;
    private final V3d newVertex;
    private final V3d prev;
	private final V3d curr;
    private final VertexPosition vp;
    private final SplitStartedCmd splitStartedCmd;
    private final List<V3d> newPolygonVertex;

    public AddVertexNextPoint(List<V3d> list,
                              V3d lastVertex, V3d newVertex, V3d prev, V3d curr,
                              VertexPosition vp, SplitStartedCmd splitStartedCmd) {
        this.lastVertex = lastVertex;
        this.newVertex = newVertex;
        this.prev = prev;
		this.curr = curr;
        this.vp = vp;
        this.splitStartedCmd = splitStartedCmd;
        this.newPolygonVertex = new ArrayList<>(list);
    }

    @Override
    public String getDescription() {
        return "Add vertex next poin check - Last: (" + lastVertex +
            "), New: (" + newVertex +
            "), Polygon: Prev: (" + prev + ") , Curr : (" + curr + ") , vert pos = " + vp;
    }

	@Override
    public void render(DebugVisualizer debugVisualizer) {
        if (splitStartedCmd != null) {
            splitStartedCmd.render(debugVisualizer);
        }
        // Рисуем текущую вершину оранжевым цветом
        debugVisualizer.drawDebugPoint(lastVertex, DbgConfig.POINT_THICKNESS_1, Color.ORANGE);

        // Рисуем следующую вершину фиолетовым цветом
        debugVisualizer.drawDebugPoint(newVertex, DbgConfig.POINT_THICKNESS_2, Color.MAGENTA);

        // Рисуем spanning вершину голубым цветом
        debugVisualizer.drawDebugPoint(prev, DbgConfig.POINT_THICKNESS_1, Color.CYAN);
        debugVisualizer.drawDebugPoint(curr, DbgConfig.POINT_THICKNESS_1, Color.YELLOW);

		

        // Рисуем линию между текущей и следующей вершиной оранжевым цветом
        debugVisualizer.drawDebugLine(lastVertex, newVertex, DbgConfig.LINE_THICKNESS_1, Color.GREEN);
        debugVisualizer.drawDebugLine(prev, curr, DbgConfig.LINE_THICKNESS_1, Color.RED);

        for (V3d p : newPolygonVertex) {
            debugVisualizer.drawDebugPoint(p, DbgConfig.POINT_THICKNESS_2, Color.WHITE);
		}
    }
} 
