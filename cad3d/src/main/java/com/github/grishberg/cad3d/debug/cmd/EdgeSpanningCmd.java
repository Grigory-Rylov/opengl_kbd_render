package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;

public class EdgeSpanningCmd implements DebugCmd {
    private final V3d currentVertex;
    private final V3d newVertex;
    private final V3d spanningVertex;
    private final SplitStartedCmd splitStartedCmd;

    public EdgeSpanningCmd(V3d currentVertex, V3d newVertex, V3d spanningVertex, SplitStartedCmd splitStartedCmd) {
        this.currentVertex = currentVertex;
        this.newVertex = newVertex;
        this.spanningVertex = spanningVertex;
        this.splitStartedCmd = splitStartedCmd;
    }

    @Override
    public String getDescription() {
        return "Edge spanning - Current: (" + currentVertex +
            "), New: (" + newVertex +
            "), Spanning: (" + spanningVertex + ")";
    }

        @Override
    public void render(DebugVisualizer debugVisualizer) {
        if (splitStartedCmd != null) {
            splitStartedCmd.render(debugVisualizer);
        }
        // Рисуем текущую вершину оранжевым цветом
        debugVisualizer.drawDebugPoint(currentVertex, DbgConfig.POINT_THICKNESS_1, Color.ORANGE);

        // Рисуем следующую вершину фиолетовым цветом
        debugVisualizer.drawDebugPoint(newVertex, DbgConfig.POINT_THICKNESS_1, Color.MAGENTA);

        // Рисуем spanning вершину голубым цветом
        debugVisualizer.drawDebugPoint(spanningVertex, DbgConfig.POINT_THICKNESS_2, Color.CYAN);

        // Рисуем линию между текущей и следующей вершиной оранжевым цветом
        debugVisualizer.drawDebugLine(currentVertex, newVertex, DbgConfig.LINE_THICKNESS_1, Color.ORANGE);

        // Рисуем линии от spanning вершины к остальным вершинам голубым цветом
        debugVisualizer.drawDebugLine(spanningVertex, currentVertex, DbgConfig.LINE_THICKNESS_1, Color.CYAN);
        debugVisualizer.drawDebugLine(spanningVertex, newVertex, DbgConfig.LINE_THICKNESS_1, Color.CYAN);
    }
} 
