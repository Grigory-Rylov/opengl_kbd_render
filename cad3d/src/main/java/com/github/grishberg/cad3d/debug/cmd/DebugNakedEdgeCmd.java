package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import com.github.grishberg.javascad.Triangulator;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.optimizator.PolygonValidator;
import com.github.grishberg.javascad.vrl.Facet;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugNakedEdgeCmd implements DebugCmd {

    private final Color[] colors = {
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.MAGENTA
    };

    private final Polygon polygon;
    private final V3d nakedEdgeA;
    private final V3d nakedEdgeB;
    private final Facet facet;
    private final List<Facet> allFacets;
    private final Map<Polygon, List<Facet>> nearbyMap;

    public DebugNakedEdgeCmd(Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet) {
        this.polygon = polygon;
        this.nakedEdgeA = nakedEdgeA;
        this.nakedEdgeB = nakedEdgeB;
        this.facet = facet;
        this.nearbyMap = new HashMap<>();
        allFacets = new ArrayList<>();
    }

    private DebugNakedEdgeCmd(
        Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet,
        List<Facet> allFacets, List<PolygonValidator.PolygonEdge> nearby
    ) {
        this.polygon = polygon;
        this.nakedEdgeA = nakedEdgeA;
        this.nakedEdgeB = nakedEdgeB;
        this.facet = facet;
        this.allFacets = allFacets;
        this.nearbyMap = new HashMap<>();
        if (nearby != null) {
            for (PolygonValidator.PolygonEdge p : nearby) {
                this.nearbyMap.put(p.polygon, Triangulator.triangulate(p.polygon));
            }
        }
    }

    public static DebugNakedEdgeCmd withAllFacets(
        Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet,
        List<Facet> allFacets
    ) {
        return new DebugNakedEdgeCmd(
            polygon,
            nakedEdgeA,
            nakedEdgeB,
            facet,
            allFacets,
            new ArrayList<>()
        );
    }

    public static DebugNakedEdgeCmd withAllNearbyFacets(
        Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet,
        List<PolygonValidator.PolygonEdge> nearby, List<Facet> allFacets
    ) {
        return new DebugNakedEdgeCmd(polygon, nakedEdgeA, nakedEdgeB, facet, allFacets, nearby);
    }

    @Override
    public String getDescription() {
        String verticesInfo = polygon != null ? EndCmd.verticesToString(polygon.getVertices()) :
            EndCmd.verticesToString(facet.getTriangle().getPoints());
        return "DebugNakedEdgeCmd polygon = " + verticesInfo + ", A = " + nakedEdgeA + ", B = " +
            nakedEdgeB + ", " +
            "nearby = " + nearbyMap.size();
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {
        for (Facet f : allFacets) {
            debugVisualizer.drawDebugFacet(
                f, DbgConfig.LINE_THICKNESS,
                DbgConfig.POINT_THICKNESS, Color.lightGray, Color.WHITE
            );
        }

        int colorIndex = 0;
        for (Map.Entry<Polygon, List<Facet>> p : nearbyMap.entrySet()) {
            Color polygonColor = colors[colorIndex % colors.length];
            for (Facet facet : p.getValue()) {
                debugVisualizer.drawDebugFacet(
                    facet,
                    DbgConfig.LINE_THICKNESS,
                    DbgConfig.POINT_THICKNESS,
                    polygonColor,
                    Color.WHITE
                );
            }
            colorIndex++;
        }

        if (polygon != null) {
            debugVisualizer.drawDebugPolygon(
                polygon, DbgConfig.LINE_THICKNESS,
                DbgConfig.POINT_THICKNESS, Color.YELLOW, Color.BLUE
            );
        }
        debugVisualizer.drawDebugPoint(nakedEdgeA, DbgConfig.POINT_THICKNESS_1, Color.GREEN);
        debugVisualizer.drawDebugPoint(nakedEdgeB, DbgConfig.POINT_THICKNESS_1, Color.RED);
        debugVisualizer.drawDebugFacet(
            facet,
            DbgConfig.LINE_THICKNESS,
            DbgConfig.POINT_THICKNESS,
            Color.ORANGE,
            Color.ORANGE
        );

        debugVisualizer.drawDebugLine(
            nakedEdgeA, nakedEdgeB,
            DbgConfig.LINE_THICKNESS_2, Color.RED
        );

    }
}
