package com.github.grishberg.javascad.coords;

import static com.github.grishberg.javascad.vrl.Const.EPSILON;

import java.util.ArrayList;
import java.util.List;

public class EarClippingMod {

    /**
     * Триангуляция выпуклого полигона с возможными коллинеарными точками.
     * Использует модифицированный алгоритм Ear Clipping.
     *
     * @param vertices Список вершин полигона (в порядке обхода).
     * Предполагается, что полигон выпуклый.
     * @return Список треугольников (Triangle3d), представляющих триангуляцию полигона.
     * @throws IllegalArgumentException если количество вершин меньше 3.
     */
    public static List<Triangle3d> triangulate(List<V3d> vertices, V3d normal) {
        int n = vertices.size();
        if (n < 3) {
            throw new IllegalArgumentException(
                "Полигон должен иметь не менее 3 вершин. Получено: " + n);
        }

        List<Triangle3d> triangles = new ArrayList<>();

        // Для треугольника просто возвращаем его
        if (n == 3) {
            V3d a = vertices.get(0);
            V3d b = vertices.get(1);
            V3d c = vertices.get(2);
            // Даже если он вырожден, добавляем
            triangles.add(new Triangle3d(a, b, c, normal));
            return triangles;
        }

        // Работаем с копией списка индексов вершин, чтобы не модифицировать оригинал
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }

        int iterations = 0;
        final int MAX_ITERATIONS = n * n; // Предотвращение зацикливания

        // Основной цикл откусывания ушей
        while (indices.size() > 3 && iterations < MAX_ITERATIONS) {
            boolean earFound = false;
            iterations++;

            int size = indices.size();
            for (int i = 0; i < size; i++) {
                int prevIdx = (i - 1 + size) % size;
                int currIdx = i;
                int nextIdx = (i + 1) % size;

                int prevVertexIdx = indices.get(prevIdx);
                int currVertexIdx = indices.get(currIdx);
                int nextVertexIdx = indices.get(nextIdx);

                V3d a = vertices.get(prevVertexIdx);
                V3d b = vertices.get(currVertexIdx);
                V3d c = vertices.get(nextVertexIdx);

                // Проверяем, является ли вершина b подходящей для откусывания
                if (isEar(a, b, c, vertices, indices, EPSILON)) {
                    // Добавляем треугольник
                    triangles.add(new Triangle3d(a, b, c, normal));
                    // Удаляем вершину "уха" (среднюю)
                    indices.remove(currIdx);
                    earFound = true;
                    break; // Начинаем новый цикл поиска
                }
                // Если точка коллинеарна или вогнута, она пропускается (earFound остается false)
            }

            // Если за полный проход не удалось найти "ухо", полигон может быть проблемным
            if (!earFound) {
                // В выпуклом полигоне это может означать, что остались только коллинеарные точки
                // или возникла численная нестабильность.
                // Пробуем удалить коллинеарные точки напрямую.
                boolean collinearRemoved = false;
                size = indices.size();
                for (int i = 0; i < size; i++) {
                    int prevIdx = (i - 1 + size) % size;
                    int nextIdx = (i + 1) % size;

                    V3d a = vertices.get(indices.get(prevIdx));
                    V3d b = vertices.get(indices.get(i));
                    V3d c = vertices.get(indices.get(nextIdx));

                    if (isDegenerate(a, b, c, EPSILON)) {
                        indices.remove(i);
                        collinearRemoved = true;
                        break;
                    }
                }

                // Если даже коллинеарные точки не найдены, прерываем цикл
                if (!collinearRemoved) {
                    System.err.println(
                        "Предупреждение: Не удалось завершить триангуляцию. Оставшиеся вершины: " +
                            indices.size());
                    break;
                }
            }
        }

        // Обработка зацикливания
        if (iterations >= MAX_ITERATIONS) {
            System.err.println("Ошибка: Превышено максимальное количество итераций в триангуляции" +
                ".");
            // Возвращаем то, что есть
            return triangles;
        }

        // Обрабатываем оставшийся финальный полигон (должен быть треугольником)
        if (indices.size() == 3) {
            int idx1 = indices.get(0);
            int idx2 = indices.get(1);
            int idx3 = indices.get(2);
            V3d a = vertices.get(idx1);
            V3d b = vertices.get(idx2);
            V3d c = vertices.get(idx3);

            triangles.add(new Triangle3d(a, b, c, normal));
        }

        return triangles;
    }

    /**
     * Проверяет, является ли вершина "ухом" (ear).
     * Вершина b является "ухом", если:
     * 1. Угол при b выпуклый (или близок к выпуклому).
     * 2. Треугольник abc не содержит других вершин полигона.
     * 3. Треугольник abc не вырожден.
     *
     * @param a Предыдущая вершина
     * @param b Текущая вершина (проверяемое "ухо")
     * @param c Следующая вершина
     * @param allVertices Все вершины исходного полигона
     * @param currentIndices Индексы вершин, оставшихся в текущем полигоне
     * @param epsilon Точность для вычислений
     * @return true, если вершина b является допустимым "ухом"
     */
    private static boolean isEar(
        V3d a,
        V3d b,
        V3d c,
        List<V3d> allVertices,
        List<Integer> currentIndices,
        double epsilon
    ) {
        // 1. Проверка на вырожденность
        if (isDegenerate(a, b, c, epsilon)) {
            return false; // Коллинеарные точки не образуют "уха"
        }

        // 2. Проверка выпуклости угла
        if (!isConvexAngle(a, b, c, epsilon)) {
            return false; // Вогнутый угол не может быть "ухом"
        }

        // 3. Проверка, не содержит ли треугольник abc других вершин полигона
        for (int i = 0; i < currentIndices.size(); i++) {
            int vertexIndex = currentIndices.get(i);
            V3d testPoint = allVertices.get(vertexIndex);

            // Пропускаем сами точки a, b, c
            // Сравнение по ссылке может не сработать, сравниваем по индексам
            int indexInAll = allVertices.indexOf(testPoint);
            if (indexInAll == allVertices.indexOf(a) ||
                indexInAll == allVertices.indexOf(b) ||
                indexInAll == allVertices.indexOf(c)) {
                continue;
            }

            // Проверяем, находится ли testPoint внутри или на границе треугольника abc
            if (isPointInOrOnTriangle(testPoint, a, b, c, epsilon)) {
                return false; // Найдена точка внутри или на границе, значит это не "ухо"
            }
        }

        return true; // "Ухо" найдено
    }

    /**
     * Проверяет, является ли треугольник, образованный тремя точками, вырожденным.
     *
     * @param a Первая точка
     * @param b Вторая точка
     * @param c Третья точка
     * @param epsilon Порог для определения вырожденности
     * @return true, если точки коллинеарны (треугольник вырожден)
     */
    private static boolean isDegenerate(V3d a, V3d b, V3d c, double epsilon) {
        // Вычисляем два вектора треугольника
        V3d ab = new V3d(b.x - a.x, b.y - a.y, b.z - a.z);
        V3d ac = new V3d(c.x - a.x, c.y - a.y, c.z - a.z);

        // Вычисляем векторное произведение
        V3d cross = new V3d(
            ab.y * ac.z - ab.z * ac.y,
            ab.z * ac.x - ab.x * ac.z,
            ab.x * ac.y - ab.y * ac.x
        );

        // Квадрат длины векторного произведения (пропорционален удвоенной площади треугольника)
        double areaSquared = cross.x * cross.x + cross.y * cross.y + cross.z * cross.z;

        return areaSquared < epsilon * epsilon;
    }

    /**
     * Проверяет, является ли угол при вершине b выпуклым.
     * Для выпуклого полигона все внутренние углы должны быть выпуклыми.
     *
     * @param a Предыдущая вершина
     * @param b Текущая вершина
     * @param c Следующая вершина
     * @param epsilon Точность для вычислений
     * @return true, если угол выпуклый
     */
    private static boolean isConvexAngle(V3d a, V3d b, V3d c, double epsilon) {
        // Векторы
        V3d ba = new V3d(a.x - b.x, a.y - b.y, a.z - b.z);
        V3d bc = new V3d(c.x - b.x, c.y - b.y, c.z - b.z);

        // Векторное произведение
        V3d cross = new V3d(
            ba.y * bc.z - ba.z * bc.y,
            ba.z * bc.x - ba.x * bc.z,
            ba.x * bc.y - ba.y * bc.x
        );

        // Для выпуклого полигона, ориентированного против часовой стрелки,
        // векторное произведение должно быть положительно направлено.
        // Но так как мы не знаем ориентацию, проверим, что угол не вогнутый.
        // Простой способ: проверим, что длина векторного произведения не отрицательна.
        // Однако, для более точной проверки, нужно знать нормаль полигона.

        // Упрощенная проверка: угол считается выпуклым, если он "не слишком вогнутый".
        // В выпуклом полигоне все углы выпуклые, так что мы можем просто проверить,
        // что площадь треугольника положительна (что уже проверяется в isDegenerate).
        // Но для Ear Clipping важно, чтобы "ухо" было направлено "наружу".

        // Альтернатива: предположим, что полигон ориентирован против часовой стрелки.
        // Тогда векторное произведение BA x BC должно быть положительно направлено
        // относительно некоторой нормали (например, Z-оси для 2D полигона в XY-плоскости).
        // Для общего случая в 3D это сложно.

        // Для выпуклого полигона можно использовать более простой подход:
        // если треугольник не вырожден, и полигон выпуклый, то угол выпуклый.
        // Но алгоритм Ear Clipping должен работать и для не-выпуклых.

        // Вернемся к стандартному определению: угол выпуклый, если он <= 180 градусов.
        // Это эквивалентно тому, что скалярное произведение нормалей соседних ребер
        // и векторное произведение имеют определенное соотношение.

        // Проще всего: если треугольник не вырожден, и мы в выпуклом полигоне,
        // то угол можно считать подходящим. Но для Ear Clipping нужна более точная логика.

        // Реализуем проверку через ориентацию: если полигон выпуклый и ориентирован
        // против часовой стрелки, то векторное произведение BA x BC должно быть
        // положительно направлено относительно нормали.

        // Для упрощения, если треугольник не вырожден, будем считать угол подходящим.
        // Это работает для выпуклых полигонов.
        return !isDegenerate(a, b, c, epsilon);

        // Более точная (но сложная) проверка требует знания нормали всего полигона.
    }

    /**
     * Проверяет, находится ли точка внутри или на границе треугольника в 3D пространстве.
     * Предполагается, что точка лежит в той же плоскости, что и треугольник.
     *
     * @param p Точка для проверки
     * @param a Вершина треугольника
     * @param b Вершина треугольника
     * @param c Вершина треугольника
     * @param epsilon Точность для вычислений
     * @return true, если точка находится внутри или на границе треугольника
     */
    private static boolean isPointInOrOnTriangle(V3d p, V3d a, V3d b, V3d c, double epsilon) {
        // Векторы треугольника
        V3d v0 = new V3d(c.x - a.x, c.y - a.y, c.z - a.z); // AC
        V3d v1 = new V3d(b.x - a.x, b.y - a.y, b.z - a.z); // AB
        V3d v2 = new V3d(p.x - a.x, p.y - a.y, p.z - a.z); // AP

        // Скалярные произведения
        double dot00 = v0.x * v0.x + v0.y * v0.y + v0.z * v0.z;
        double dot01 = v0.x * v1.x + v0.y * v1.y + v0.z * v1.z;
        double dot02 = v0.x * v2.x + v0.y * v2.y + v0.z * v2.z;
        double dot11 = v1.x * v1.x + v1.y * v1.y + v1.z * v1.z;
        double dot12 = v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;

        // Вычисляем барицентрические координаты
        double denom = dot00 * dot11 - dot01 * dot01;
        if (Math.abs(denom) < epsilon * epsilon) {
            // Треугольник вырожден, точка не может быть "внутри" в обычном смысле
            return false;
        }

        double invDenom = 1.0 / denom;
        double u = (dot11 * dot02 - dot01 * dot12) * invDenom;
        double v = (dot00 * dot12 - dot01 * dot02) * invDenom;

        // Точка находится внутри или на границе, если:
        // u >= 0, v >= 0 и u + v <= 1
        return (u >= -epsilon) && (v >= -epsilon) && (u + v <= 1 + epsilon);
    }
}
