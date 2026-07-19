package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.VertexPosition;

public class ClassifyAndSplitVertexCmd implements DebugCmd {

    private final V3d currentVertex;
    private final V3d nextVertex;
    private final VertexPosition position;
    private final SplitStartedCmd splitStartedCmd;

    public ClassifyAndSplitVertexCmd(
        V3d currentVertex, V3d nextVertex,
        VertexPosition position,
        SplitStartedCmd splitStartedCmd
    ) {
        this.currentVertex = currentVertex;
        this.nextVertex = nextVertex;
        this.position = position;
        this.splitStartedCmd = splitStartedCmd;
    }

    @Override
    public String getDescription() {
        return "Classify and split vertex - Current: " +
            currentVertex +
            ", Next: " +
            nextVertex + ", position = " + position;
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {
        if (splitStartedCmd != null) {
            splitStartedCmd.render(debugVisualizer);
        }

        // Рисуем текущую вершину желтым цветом
        debugVisualizer.drawDebugPoint(currentVertex, DbgConfig.POINT_THICKNESS_1, Color.YELLOW);

        // Рисуем следующую вершину зеленым цветом
        debugVisualizer.drawDebugPoint(nextVertex, DbgConfig.POINT_THICKNESS_1, Color.GREEN);

        // Рисуем линию между вершинами желтым цветом
        debugVisualizer.drawDebugLine(currentVertex, nextVertex, DbgConfig.LINE_THICKNESS_1, Color.YELLOW);
    }
} 
