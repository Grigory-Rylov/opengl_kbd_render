package eu.printingin3d.javascad.models2d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AreaUnion {
   private List<Area2d> areas = new ArrayList<>();

   /**
    * Добавляет новую область, объединяя ее с пересекающимися областями в списке.
    * @param newArea Новая область для добавления.
    */
   public void addArea(Area2d newArea) {
      List<Area2d> intersectingAreas = new ArrayList<>();

      // Ищем все области, пересекающиеся с новой
      Iterator<Area2d> iterator = areas.iterator();
      while (iterator.hasNext()) {
         Area2d existingArea = iterator.next();
         if (areasIntersect(existingArea, newArea)) {
            intersectingAreas.add(existingArea);
            iterator.remove(); // Удаляем из основного списка
         }
      }

      // Объединяем все пересекающиеся области
      Area2d mergedArea = newArea;
      for (Area2d area : intersectingAreas) {
         mergedArea = mergedArea.union(area);
      }

      areas.add(mergedArea);
   }

   /**
    * Проверяет, пересекаются ли две области.
    */
   private boolean areasIntersect(Area2d a, Area2d b) {
      // Используем встроенную проверку из Area2d
      return !a.isDistinct(b);
   }

   /**
    * Возвращает текущий список объединенных областей.
    */
   public List<Area2d> getAreas() {
      return new ArrayList<>(areas);
   }
}
