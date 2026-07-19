package com.github.grishberg.javascad.vrl;

import com.github.grishberg.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

class PointsSorter {

    /**
     * Сортирует точки полигона по часовой стрелке относительно нормали полигона.
     *
     * @param points Список точек полигона (V3d)
     * @return Новый список точек, отсортированных по часовой стрелке
     */
    public static List<V3d> sortPointsClockwise(List<V3d> points, V3d normal) {
        if (points == null || points.size() < 3) {
            // Если точек меньше 3, возвращаем копию оригинального списка
            return points == null ? new ArrayList<>() : new ArrayList<>(points);
        }

        // 1. Вычисляем центр тяжести (centroid) точек
        V3d centroid = calculateCentroid(points);

        // 3. Сортируем точки с помощью компаратора
        List<V3d> sortedPoints = new ArrayList<>(points);

        sortedPoints.sort((p1, p2) -> {
            // Создаем векторы от центроида к каждой точке
            V3d v1 = new V3d(p1.x - centroid.x, p1.y - centroid.y, p1.z - centroid.z);
            V3d v2 = new V3d(p2.x - centroid.x, p2.y - centroid.y, p2.z - centroid.z);

            // Вычисляем векторное произведение v1 x v2
            V3d cross = new V3d(
                v1.y * v2.z - v1.z * v2.y,
                v1.z * v2.x - v1.x * v2.z,
                v1.x * v2.y - v1.y * v2.x
            );

            // Скалярное произведение векторного произведения и нормали
            double dot = cross.x * normal.x + cross.y * normal.y + cross.z * normal.z;

            // Если dot > 0, то p1 идет перед p2 (по часовой стрелке)
            // Если dot < 0, то p2 идет перед p1
            if (dot > 0) return 1;  // Меняем местами для часовой стрелки
            if (dot < 0) return -1;
            return 0;
        });

        return sortedPoints;
    }

    /**
     * Вычисляет центр тяжести (среднее арифметическое) точек.
     */
    private static V3d calculateCentroid(List<V3d> points) {
        double sumX = 0, sumY = 0, sumZ = 0;
        int n = points.size();

        for (V3d point : points) {
            sumX += point.x;
            sumY += point.y;
            sumZ += point.z;
        }

        return new V3d(sumX / n, sumY / n, sumZ / n);
    }

    // Альтернативная версия - сортировка против часовой стрелки
    /**
     * Сортирует точки полигона против часовой стрелки относительно нормали полигона.
     *
     * @param points Список точек полигона (V3d)
     * @return Новый список точек, отсортированных против часовой стрелки
     */
    public static List<V3d> sortPointsCounterClockwise(List<V3d> points, V3d normal) {
        if (points == null || points.size() < 3) {
            return points == null ? new ArrayList<>() : new ArrayList<>(points);
        }

        V3d centroid = calculateCentroid(points);

        List<V3d> sortedPoints = new ArrayList<>(points);

        sortedPoints.sort((p1, p2) -> {
            V3d v1 = new V3d(p1.x - centroid.x, p1.y - centroid.y, p1.z - centroid.z);
            V3d v2 = new V3d(p2.x - centroid.x, p2.y - centroid.y, p2.z - centroid.z);

            V3d cross = new V3d(
                v1.y * v2.z - v1.z * v2.y,
                v1.z * v2.x - v1.x * v2.z,
                v1.x * v2.y - v1.y * v2.x
            );

            double dot = cross.x * normal.x + cross.y * normal.y + cross.z * normal.z;

            // Для против часовой стрелки меняем знак сравнения
            if (dot > 0) return -1; // Оставляем порядок для против часовой
            if (dot < 0) return 1;
            return 0;
        });

        return sortedPoints;
    }
}
