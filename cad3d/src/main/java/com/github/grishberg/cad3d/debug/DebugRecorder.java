package com.github.grishberg.cad3d.debug;

import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.javascad.optimizator.PolygonValidator;
import com.github.grishberg.openscad.vrl.Facet;
import com.github.grishberg.openscad.vrl.Polygon;
import com.github.grishberg.openscad.vrl.VertexPosition;
import java.util.List;

/**
 * Интерфейс для записи отладочных команд
 */
public interface DebugRecorder {

    /**
     * Возвращает все записанные команды
     *
     * @return список команд
     */
    List<DebugCmd> getCommands();

    void clear();


    /**
     * Возвращает количество записанных команд
     *
     * @return количество команд
     */
    int getCommandCount();

    /**
     * Возвращает команду по индексу
     *
     * @param index индекс команды
     * @return команда или null если индекс неверный
     */
    DebugCmd getCommand(int index);

    void onSplitPolygonStarted(Polygon polygon, Polygon polygon1, VertexPosition polygonType);

    void onClassifyAndSplitVertex(V3d currentVertex, V3d nextVertex, VertexPosition position);

    void onEdgeSpanning(V3d currentVertex, V3d nextVertex, V3d v);

    void onEdgeCross(
        List<V3d> list,
        V3d currentVertex,
        V3d nextVertex,
        V3d cross,
        VertexPosition vp
    );

    void onAddVertexNextPoint(
        List<V3d> list,
        V3d lastVertex,
        V3d nextVertex,
        V3d prev,
        V3d curr,
        VertexPosition vp
    );

    void onSplitPolygonCompleted(List<V3d> front, List<V3d> back);

    void onEnd(List<Polygon> polygons);

    void onEnd(Polygon polygon);
    void onEnd(Polygon polygon, List<Polygon> allPolygons);

    void onEndFacets(List<Facet> facets);

    void onLogEvent(String message);

    void onLogEvent(String message, List<V3d> points);

    void onDebugNakedEdge(Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet);


    void onDebugNakedEdgeWithFacets(
        Polygon polygon, V3d nakedEdgeA, V3d nakedEdgeB, Facet facet,
        List<Facet> allFacets
    );


    void onDebugPolygonTriangulation(Polygon polygon, List<Facet> facets);

    void onGroupedEdge(PolygonValidator.LineKey key, List<PolygonValidator.PolygonEdge> value);

    void onDebugNakedEdgeWithNearbyFacets(Polygon polygon, V3d pointA, V3d pointB, Facet facet, List<PolygonValidator.PolygonEdge> nearby);

    void onDebugNakedEdgeWithNearbyFacets(Polygon polygon, V3d pointA, V3d pointB, Facet facet, List<PolygonValidator.PolygonEdge> nearby, List<Facet> facets);

}
