package com.github.grishberg.javascad.utils;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


// Класс для поиска Non-manifold рёбер
public class NonManifoldFinder {

    // Главный метод для поиска проблемных рёбер
    public static List<Edge> findNonManifoldEdges(List<Polygon> polygons) {
        // 1. Собираем все рёбра и храним список полигонов, которым они принадлежат
        Map<Edge, List<Polygon>> edgePolygonsMap = new HashMap<>();

        for (Polygon polygon : polygons) {
            List<V3d> vertices = polygon.getVertices();
            Edge[] edges = new Edge[vertices.size()];
            for (int i = 0; i < vertices.size(); i++) {
                int start = i;
                int end = (i + 1) % vertices.size();
                edges[i] = new Edge(vertices.get(start), vertices.get(end));
            }

            // Добавляем полигон в список для каждого ребра
            for (Edge edge : edges) {
                edgePolygonsMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(polygon);
            }
        }

        System.out.println("Check edges...");
        // 2. Ищем проблемные рёбра
        List<Edge> nonManifoldEdges = new ArrayList<>();

        for (Map.Entry<Edge, List<Polygon>> entry : edgePolygonsMap.entrySet()) {
            Edge edge = entry.getKey();
            List<Polygon> polygonList = entry.getValue();
            int usageCount = polygonList.size();

            if (usageCount > 2) {
                // Ребро используется более чем 2 гранями - это ошибка!
                StringBuilder detailMessage = new StringBuilder();
                detailMessage.append("Non-manifold edge (used by >2 faces): ")
                    .append(edge)
                    .append("\n");
                detailMessage.append("  Used by ").append(usageCount).append(" polygons:\n");

                for (int i = 0; i < polygonList.size(); i++) {
                    detailMessage.append("    Polygon ")
                        .append(i)
                        .append(": ")
                        .append(polygonList.get(i).toString())
                        .append("\n");
                }

                System.err.println(detailMessage);
                nonManifoldEdges.add(edge);
            } else if (usageCount == 1) {
                // Ребро используется только одной гранью - "висящее" ребро
                StringBuilder floatMessage = new StringBuilder();
                floatMessage.append("Floating edge (used by 1 face): ").append(edge).append("\n");
                floatMessage.append("  Used by polygon: ").append(polygonList.get(0).toString());

                System.out.println(floatMessage);
                nonManifoldEdges.add(edge);
            }
        }

        return nonManifoldEdges;
    }

    // Класс для представления ребра
    public static class Edge {

        public V3d p1, p2;

        public Edge(V3d p1, V3d p2) {
            // Всегда храним точки в одинаковом порядке для сравнения
            if (p1.hashCode() < p2.hashCode()) {
                this.p1 = p1;
                this.p2 = p2;
            } else {
                this.p1 = p2;
                this.p2 = p1;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Edge edge = (Edge) o;
            return Objects.equals(p1, edge.p1) && Objects.equals(p2, edge.p2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(p1, p2);
        }

        @Override
        public String toString() {
            return "Edge " + p1.toJson() + " - " + p2.toJson();
        }
    }
}
