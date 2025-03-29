package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords2d.Coords2d;
import eu.printingin3d.javascad.models2d.Area2d;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EdgeMapper {
   // Класс для представления ребра с учетом неупорядоченности точек
   private static class Edge {
      private final Coords2d p1;
      private final Coords2d p2;

      public Edge(Coords2d a, Coords2d b) {
         // Сортируем точки, чтобы ребро (A-B) и (B-A) были одинаковыми
         if (a.hashCode() < b.hashCode()) {
            this.p1 = a;
            this.p2 = b;
         } else {
            this.p1 = b;
            this.p2 = a;
         }
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;
         Edge edge = (Edge) o;
         return p1.equals(edge.p1) && p2.equals(edge.p2);
      }

      @Override
      public int hashCode() {
         return p1.hashCode() + 31 * p2.hashCode();
      }
   }

   public Map<Edge, List<Area2d>> createEdgeMap(List<Area2d> areas) {
      Map<Edge, List<Area2d>> edgeMap = new HashMap<>();

      for (Area2d area : areas) {
         List<Coords2d> points = area.getPoints();

         // Перебираем все ребра в области
         for (int i = 0; i < points.size(); i++) {
            Coords2d current = points.get(i);
            Coords2d next = points.get((i + 1) % points.size());

            Edge edge = new Edge(current, next);

            // Добавляем область в список для этого ребра
            edgeMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(area);
         }
      }

      return edgeMap;
   }
}
