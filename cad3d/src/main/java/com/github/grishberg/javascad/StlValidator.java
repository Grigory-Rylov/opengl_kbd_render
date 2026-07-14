package com.github.grishberg.javascad;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.printingin3d.javascad.coords.Triangle3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.Const;
import eu.printingin3d.javascad.vrl.Facet;
import eu.printingin3d.javascad.vrl.Polygon;

public class StlValidator {

    private static final int PRECISION = 6;

    public static List<Facet> validateAndRepair(List<Facet> facets) {
        List<ProcessedFacet> processed = preprocessFacets(facets);

        // Новая проверка
        int initialNakedCount = validateNakedEdges(facets);

        // Если есть naked edges, попробуем их исправить итеративно
        if (initialNakedCount > 0) {
            int currentNakedCount = initialNakedCount;
            int iterations = 0;
            int maxIterations = 5; // Ограничиваем количество итераций

            while (currentNakedCount > 0 && iterations < maxIterations) {
                processed = fixNakedEdges(processed);

                // Проверяем результат
                List<Facet> rebuilt = rebuildFacets(processed);
                int newNakedCount = validateNakedEdges(rebuilt);

                System.out.println("Итерация " + (iterations + 1) + ": naked edges=" + newNakedCount);

                // Если улучшения нет, прекращаем
                if (newNakedCount >= currentNakedCount) {
                    break;
                }

                currentNakedCount = newNakedCount;
                iterations++;

                // Обновляем processed для следующей итерации
                processed = preprocessFacets(rebuilt);
            }

            System.out.println("Naked edges: до исправления=" + initialNakedCount +
                ", после исправления=" + currentNakedCount +
                " (итераций: " + iterations + ")");
        }

        // Проверка порядка точек граней относительно нормали
        validateFacePointOrder(rebuildFacets(processed));

        // Основные шаги
        //processed = fixNormals(processed);
        //processed = removeDegenerate(processed);
        //processed = fixNonManifoldEdges(processed);
        //processed = removeSelfIntersecting(processed);

        return rebuildFacets(processed);
    }

    public static List<ProcessedFacet> fixNakedEdges(List<ProcessedFacet> facets) {
        Map<Edge, List<ProcessedFacet>> nakedEdges = findNakedEdges(facets);
        List<ProcessedFacet> repaired = new ArrayList<>(facets);
        double scale = calculateModelScale(facets);
        double searchRadius = scale * 0.2; // Увеличиваем радиус поиска до 20%

        System.out.println("Исправляем " + nakedEdges.size() + " naked edges");

        for (Map.Entry<Edge, List<ProcessedFacet>> entry : nakedEdges.entrySet()) {
            Edge edge = entry.getKey();
            V3d v1 = edge.a;
            V3d v2 = edge.b;

            // Ищем несколько ближайших вершин для закрытия дыры
            List<V3d> candidateVertices = findClosestVertices(v1, v2, facets, searchRadius, 3);

            boolean addedTriangle = false;
            for (V3d v3 : candidateVertices) {
                // Создаем новый треугольник
                ProcessedFacet newFacet = createProcessedFacet(
                    v1, v2, v3,
                    computeFaceNormal(Arrays.asList(v1, v2, v3)),
                    entry.getValue().get(0).original,
                    scale
                );

                if (newFacet != null) {
                    repaired.add(newFacet);
                    System.out.println("Добавлен треугольник для закрытия дыры: "
                        + v1 + " - " + v2 + " - " + v3);
                    addedTriangle = true;
                    break; // Используем только первую подходящую вершину
                }
            }

            // Если не нашли подходящую вершину, создаем новую
            if (!addedTriangle) {
                V3d edgeCenter = V3d.midPoint(v1, v2);
                V3d facetNormal = entry.getValue().get(0).normal;

                // Создаем новую вершину на небольшом расстоянии от ребра
                V3d newVertex = edgeCenter.add(facetNormal.scale(scale * 0.01));

                ProcessedFacet newFacet = createProcessedFacet(
                    v1, v2, newVertex,
                    computeFaceNormal(Arrays.asList(v1, v2, newVertex)),
                    entry.getValue().get(0).original,
                    scale
                );

                if (newFacet != null) {
                    repaired.add(newFacet);
                    System.out.println("Создан новый треугольник с новой вершиной: "
                        + v1 + " - " + v2 + " - " + newVertex);
                }
            }
        }

        return repaired;
    }


    private static V3d findClosestVertex(V3d v1, V3d v2, List<ProcessedFacet> facets, double searchRadius) {
        V3d edgeCenter = V3d.midPoint(v1, v2);
        double minDistance = Double.MAX_VALUE;
        V3d closest = null;

        for (ProcessedFacet facet : facets) {
            for (V3d candidate : facet.vertices) {
                // Исключаем вершины самого ребра
                if (candidate.equals(v1) || candidate.equals(v2)) continue;

                double dist = edgeCenter.distance(candidate);
                if (dist < minDistance && dist < searchRadius) {
                    minDistance = dist;
                    closest = candidate;
                }
            }
        }

        return closest;
    }

    private static List<V3d> findClosestVertices(V3d v1, V3d v2, List<ProcessedFacet> facets, double searchRadius, int maxCount) {
        V3d edgeCenter = V3d.midPoint(v1, v2);
        List<VertexDistance> candidates = new ArrayList<>();

        for (ProcessedFacet facet : facets) {
            for (V3d candidate : facet.vertices) {
                // Исключаем вершины самого ребра
                if (candidate.equals(v1) || candidate.equals(v2)) continue;

                double dist = edgeCenter.distance(candidate);
                if (dist < searchRadius) {
                    candidates.add(new VertexDistance(candidate, dist));
                }
            }
        }

        // Сортируем по расстоянию и возвращаем ближайшие
        return candidates.stream()
            .sorted((a, b) -> Double.compare(a.distance, b.distance))
            .limit(maxCount)
            .map(vd -> vd.vertex)
            .collect(Collectors.toList());
    }

    private static class VertexDistance {
        final V3d vertex;
        final double distance;

        VertexDistance(V3d vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }

    public static int validateNakedEdges(List<Facet> facets) {
        List<ProcessedFacet> processed = preprocessFacets(facets);
        Map<Edge, List<ProcessedFacet>> nakedEdges = findNakedEdges(processed);

        if (!nakedEdges.isEmpty()) {
            System.err.println("Найдено Naked edges: " + nakedEdges.size());
            nakedEdges.forEach((edge, facetsList) -> {
                System.err.println("Ребро: " + edge.a + " -> " + edge.b);
                System.err.println("Принадлежит треугольнику: " + facetsList.get(0).vertices);
            });
        } else {
            System.out.println("Naked edges не обнаружены.");
        }
        return nakedEdges.size();
    }

    /**
     * Проверяет порядок точек граней относительно нормали.
     * В STL формате вершины треугольника должны быть ориентированы против часовой стрелки
     * при взгляде снаружи объекта, чтобы нормаль указывала наружу.
     *
     * @param facets список граней для проверки
     * @return количество граней с неправильным порядком точек
     */
    public static int validateFacePointOrder(List<Facet> facets) {
        int invalidCount = 0;
        double angleThreshold = Math.cos(Math.toRadians(90)); // 90 градусов

        for (Facet facet : facets) {
            V3d storedNormal = facet.getNormal();
            List<V3d> vertices = facet.getTriangle().getPoints();

            // Вычисляем нормаль из порядка точек (правило правой руки)
            V3d computedNormal = computeFaceNormal(vertices);

            if (computedNormal == null) {
                System.err.println("Не удалось вычислить нормаль для вырожденной грани: " + vertices);
                // Для вырожденных треугольников не считаем это ошибкой порядка точек
                continue;
            }

            // Проверяем соответствие направления нормали порядку точек
            double dotProduct = storedNormal.dot(computedNormal);

            if (dotProduct < angleThreshold) {
                invalidCount++;
                System.err.println("Неправильный порядок точек грани:");
                System.err.println("  Вершины: " + vertices);
                System.err.println("  Хранимая нормаль: " + storedNormal);
                System.err.println("  Вычисленная нормаль: " + computedNormal);
                System.err.println("  Косинус угла: " + dotProduct);

                // Предлагаем исправление
                if (dotProduct < -angleThreshold) {
                    System.err.println("  Рекомендация: изменить порядок вершин на обратный");
                } else {
                    System.err.println("  Рекомендация: проверить правильность нормали или геометрию грани");
                }
            }
        }

        if (invalidCount == 0) {
            System.out.println("Порядок точек всех граней соответствует нормалям.");
        } else {
            System.err.println("Найдено граней с неправильным порядком точек: " + invalidCount + " из " + facets.size());
        }

        return invalidCount;
    }

    private static Map<Edge, List<ProcessedFacet>> findNakedEdges(List<ProcessedFacet> facets) {
        double scale = calculateModelScale(facets);
        double tolerance = scale * 1e-6; // Адаптивная толерантность

        Map<Edge, List<ProcessedFacet>> edgeMap = new HashMap<>();
        List<EdgeFacetPair> allEdges = new ArrayList<>();

        // Собираем все рёбра и связанные с ними треугольники
        for (ProcessedFacet facet : facets) {
            for (Edge edge : getEdges(facet.vertices)) {
                allEdges.add(new EdgeFacetPair(edge, facet));
            }
        }

        // Группируем рёбра с учетом толерантности
        for (EdgeFacetPair pair : allEdges) {
            Edge edge = pair.edge;
            ProcessedFacet facet = pair.facet;

            // Ищем существующее совпадающее ребро в карте
            Edge matchingKey = null;
            for (Edge existingEdge : edgeMap.keySet()) {
                if (edge.equals(existingEdge) || edge.nearlyEquals(existingEdge, tolerance)) {
                    matchingKey = existingEdge;
                    break;
                }
            }

            if (matchingKey != null) {
                edgeMap.get(matchingKey).add(facet);
            } else {
                edgeMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(facet);
            }
        }

        // Фильтруем рёбра, принадлежащие только одному треугольнику
        return edgeMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() == 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static class EdgeFacetPair {
        final Edge edge;
        final ProcessedFacet facet;

        EdgeFacetPair(Edge edge, ProcessedFacet facet) {
            this.edge = edge;
            this.facet = facet;
        }
    }

    private static List<ProcessedFacet> removeSelfIntersecting(List<ProcessedFacet> facets) {
        return facets.stream()
            .filter(f -> !hasSelfIntersections(f, facets))
            .collect(Collectors.toList());
    }

    private static boolean hasSelfIntersections(ProcessedFacet facet, List<ProcessedFacet> allFacets) {
        // Проверка пересечения текущего треугольника с другими
        for (ProcessedFacet other : allFacets) {
            if (facet != other && trianglesIntersect(facet.vertices, other.vertices)) {
                return true;
            }
        }
        return false;
    }

    private static boolean trianglesIntersect(List<V3d> a, List<V3d> b) {
        // Реализация алгоритма проверки пересечения треугольников
        // (например, с использованием SAT или алгоритма Мёллер-Трумбор)
        return false;
    }

    ////////////

    // Обработка и хеширование данных
    private static List<ProcessedFacet> preprocessFacets(List<Facet> facets) {
        List<ProcessedFacet> processed = facets.stream()
            .map(f -> new ProcessedFacet(
                f,
                f.getNormal(),
                f.getTriangle().getPoints()
            ))
            .collect(Collectors.toList());

        // Вычисляем масштаб модели один раз
        double scale = calculateModelScale(processed);

        // Обрабатываем с учетом масштаба
        return processed.stream()
            .map(f -> {
                V3d normal = roundVector(f.normal, scale);
                List<V3d> vertices = f.vertices.stream()
                    .map(v -> roundVector(v, scale))
                    .collect(Collectors.toList());
                return new ProcessedFacet(f.original, normal, vertices);
            })
            .collect(Collectors.toList());
    }

    // Удаление вырожденных треугольников
    private static List<ProcessedFacet> removeDegenerate(List<ProcessedFacet> facets) {
        double scale = calculateModelScale(facets); // Вычисляем масштаб

        List<ProcessedFacet> valid = new ArrayList<>();
        for (ProcessedFacet f : facets) {
            if (isDegenerate(f.vertices, scale)) {
                System.out.println("Удален вырожденный треугольник: " + f.vertices);
                continue;
            }
            valid.add(f);
        }
        return valid;
    }

    // Проверка на вырожденность с учетом масштаба
    private static boolean isDegenerate(List<V3d> vertices, double scale) {
        double epsilon = scale * Const.EPSILON; // Адаптивный порог
        V3d edge1 = vertices.get(1).subtract(vertices.get(0));
        V3d edge2 = vertices.get(2).subtract(vertices.get(0));
        return edge1.cross(edge2).magnitude() < epsilon;
    }

    // Коррекция нормалей
    private static List<ProcessedFacet> fixNormals(List<ProcessedFacet> facets) {
        Map<Edge, List<ProcessedFacet>> edgeMap = new HashMap<>();

        // Построение карты рёбер
        for (ProcessedFacet facet : facets) {
            for (Edge edge : getEdges(facet.vertices)) {
                edgeMap.computeIfAbsent(edge, k -> new ArrayList<>()).add(facet);
            }
        }

        // Коррекция направления нормалей
        for (ProcessedFacet facet : facets) {
            V3d computedNormal = computeFaceNormal(facet.vertices);
            if (facet.normal.dot(computedNormal) < 0) {
                facet.normal = computedNormal.inverse();
            }
        }

        return facets;
    }

    // Исправление non-manifold edges
    private static List<ProcessedFacet> fixNonManifoldEdges(List<ProcessedFacet> facets) {
        double scale = calculateModelScale(facets); // Вычисляем масштаб

        return facets.stream()
            .flatMap(f -> {
                List<ProcessedFacet> repaired = splitAndRepairFacet(f, scale);
                return repaired.isEmpty() ? Stream.of(f) : repaired.stream();
            })
            .collect(Collectors.toList());
    }

    private static List<ProcessedFacet> splitAndRepairFacet(ProcessedFacet facet, double scale) {
        List<V3d> vertices = facet.vertices;

        // Вычисление середин рёбер
        V3d mid0 = V3d.midPoint(vertices.get(0), vertices.get(1));
        V3d mid1 = V3d.midPoint(vertices.get(1), vertices.get(2));
        V3d mid2 = V3d.midPoint(vertices.get(2), vertices.get(0));

        // Создание новых треугольников
        List<ProcessedFacet> newFacets = Arrays.asList(
            createProcessedFacet(vertices.get(0), mid0, mid2, facet.normal, facet.original, scale),
            createProcessedFacet(mid0, vertices.get(1), mid1, facet.normal, facet.original, scale),
            createProcessedFacet(mid2, mid1, vertices.get(2), facet.normal, facet.original, scale),
            createProcessedFacet(mid0, mid1, mid2, facet.normal, facet.original, scale)
        );

        // Фильтрация null (вырожденные треугольники)
        return newFacets.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    private static ProcessedFacet createProcessedFacet(V3d v1, V3d v2, V3d v3, V3d normal, Facet original, double scale) {
        // Округление вершин с учетом масштаба
        V3d roundedV1 = roundVector(v1, scale);
        V3d roundedV2 = roundVector(v2, scale);
        V3d roundedV3 = roundVector(v3, scale);

        List<V3d> vertices = Arrays.asList(roundedV1, roundedV2, roundedV3);

        // Проверка на вырожденность с адаптивным epsilon
        if (isDegenerate(vertices, scale)) {
            System.out.println("Удален вырожденный треугольник: " + vertices);
            return null;
        }

        // Вычисление нормали для нового треугольника
        V3d computedNormal = computeFaceNormal(vertices);
        if (computedNormal == null) {
            return null; // Не удалось вычислить нормаль
        }

        // Коррекция направления нормали (совместимость с исходной)
        if (computedNormal.dot(normal) < 0) {
            computedNormal = computedNormal.inverse();
        }

        // Создание обработанного фасета
        return new ProcessedFacet(
            original,
            roundVector(computedNormal, scale), // Округленная нормаль
            vertices
        );
    }

    // Вспомогательные классы и методы
    private static class ProcessedFacet {
        final Facet original;
        V3d normal;
        final List<V3d> vertices;

        // Конструктор для начального создания
        ProcessedFacet(Facet original, V3d normal, List<V3d> vertices) {
            this.original = original;
            this.normal = normal;
            this.vertices = vertices;
        }

        // Конструктор для обновленных данных
        ProcessedFacet(Facet original, V3d normal, List<V3d> vertices, double scale) {
            this.original = original;
            this.normal = roundVector(normal, scale);
            this.vertices = vertices.stream()
                .map(v -> roundVector(v, scale))
                .collect(Collectors.toList());
        }
    }

    private static class Edge {

        final V3d a, b;

        Edge(V3d a, V3d b) {
            if (compare(a, b) > 0) {
                this.a = b;
                this.b = a;
            } else {
                this.a = a;
                this.b = b;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            Edge other = (Edge) o;
            return a.equals(other.a) && b.equals(other.b);
        }

        @Override
        public int hashCode() {
            return a.hashCode() ^ b.hashCode();
        }

        // Толерантное сравнение рёбер для поиска "почти совпадающих"
        public boolean nearlyEquals(Edge other, double tolerance) {
            return (a.distance(other.a) < tolerance && b.distance(other.b) < tolerance) ||
                (a.distance(other.b) < tolerance && b.distance(other.a) < tolerance);
        }
    }

    private static List<Edge> getEdges(List<V3d> vertices) {
        return Arrays.asList(
            new Edge(vertices.get(0), vertices.get(1)),
            new Edge(vertices.get(1), vertices.get(2)),
            new Edge(vertices.get(2), vertices.get(0))
        );
    }

    private static V3d roundVector(V3d v, double scale) {
        int dynamicPrecision = Math.max(3, (int) Math.log10(1 / scale) + 2);

        return new V3d(
            Math.round(v.getX() * Math.pow(10, dynamicPrecision)) / Math.pow(10, dynamicPrecision),
            Math.round(v.getY() * Math.pow(10, dynamicPrecision)) / Math.pow(10, dynamicPrecision),
            Math.round(v.getZ() * Math.pow(10, dynamicPrecision)) / Math.pow(10, dynamicPrecision)
        );
    }

    private static double calculateModelScale(List<ProcessedFacet> processedFacets) {
        if (processedFacets.isEmpty()) return 1.0;

        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        // Находим границы модели
        for (ProcessedFacet facet : processedFacets) {
            for (V3d v : facet.vertices) {
                minX = Math.min(minX, v.getX());
                maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY());
                maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ());
                maxZ = Math.max(maxZ, v.getZ());
            }
        }

        // Вычисляем диагональ ограничивающего куба
        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;

        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    private static V3d computeFaceNormal(List<V3d> vertices) {
        try {
            V3d edge1 = vertices.get(1).subtract(vertices.get(0));
            V3d edge2 = vertices.get(2).subtract(vertices.get(0));
            return edge1.cross(edge2).unit();
        } catch (ArithmeticException e) {
            System.err.println("Ошибка вычисления нормали: нулевой вектор");
            return null;
        }
    }

    private static int compare(V3d a, V3d b) {
        int cmp = Double.compare(a.getX(), b.getX());
        if (cmp != 0) {
            return cmp;
        }
        cmp = Double.compare(a.getY(), b.getY());
        return cmp != 0 ? cmp : Double.compare(a.getZ(), b.getZ());
    }

    private static List<Facet> rebuildFacets(List<ProcessedFacet> processed) {
        return processed.stream().map(p ->
            new Facet(
                new Triangle3d(p.vertices.get(0), p.vertices.get(1), p.vertices.get(2)),
                p.normal,
                p.original.getColor()
            )).collect(Collectors.toList());
    }

    /**
     * Информация о висящей грани (naked edge)
     */
    public static class NakedEdgeInfo {
        private final V3d pointA;
        private final V3d pointB;
        private final Facet facet;
        private final String description;

        public NakedEdgeInfo(V3d pointA, V3d pointB, Facet facet, String description) {
            this.pointA = pointA;
            this.pointB = pointB;
            this.facet = facet;
            this.description = description;
        }

        public V3d getPointA() { return pointA; }
        public V3d getPointB() { return pointB; }
        public Facet getFacet() { return facet; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            return String.format("NakedEdge[%s -> %s] in facet %s: %s",
                pointA, pointB, facet.getTriangle().getPoints(), description);
        }
    }

    /**
     * Детальный анализ naked edges с возвратом структурированной информации
     */
    public static List<NakedEdgeInfo> analyzeNakedEdges(List<Facet> facets) {
        List<ProcessedFacet> processed = preprocessFacets(facets);
        Map<Edge, List<ProcessedFacet>> nakedEdges = findNakedEdges(processed);
        List<NakedEdgeInfo> result = new ArrayList<>();

        for (Map.Entry<Edge, List<ProcessedFacet>> entry : nakedEdges.entrySet()) {
            Edge edge = entry.getKey();
            ProcessedFacet facet = entry.getValue().get(0);

            String description = String.format("Ребро принадлежит только одному треугольнику из %d вершин",
                facet.vertices.size());

            NakedEdgeInfo info = new NakedEdgeInfo(
                edge.a,
                edge.b,
                facet.original,
                description
            );

            result.add(info);
        }

        return result;
    }

    /**
     * Конвертирует полигоны в facets для анализа
     */
    public static List<Facet> convertPolygonsToFacets(List<Polygon> polygons) {
        List<Facet> facets = new ArrayList<>();
        for (Polygon polygon : polygons) {
            try {
                List<Triangle3d> triangles = Triangulator.triangulate(polygon.getVertices(), polygon.getNormal());
                for (Triangle3d triangle : triangles) {
                    facets.add(new Facet(triangle, polygon.getNormal(), polygon.getColor()));
                }
            } catch (Exception e) {
                System.out.println("StlValidator: ошибка триангуляции: " + e.getMessage());
            }
        }
        return facets;
    }

    /**
     * Подсчитывает количество naked edges
     */
    public static int countNakedEdges(List<Facet> facets) {
        List<ProcessedFacet> processed = preprocessFacets(facets);
        Map<Edge, List<ProcessedFacet>> nakedEdges = findNakedEdges(processed);
        return nakedEdges.size();
    }

    /**
     * Быстрый подсчёт non-manifold edges (exact match, без толерантности).
     * Так как рёбра уже округлены в preprocessFacets, достаточно exact equality.
     */
    public static int countNonManifoldEdges(List<Facet> facets) {
        List<ProcessedFacet> processed = preprocessFacets(facets);
        Map<Edge, Integer> edgeCounts = new HashMap<>();

        for (ProcessedFacet facet : processed) {
            for (Edge edge : getEdges(facet.vertices)) {
                edgeCounts.merge(edge, 1, Integer::sum);
            }
        }

        int nakedCount = 0;
        int nonManifoldCount = 0;
        for (int count : edgeCounts.values()) {
            if (count == 1) nakedCount++;
            else if (count > 2) nonManifoldCount++;
        }

        int total = nakedCount + nonManifoldCount;
        System.out.printf("Non-manifold edges: naked=%d, multi-face=%d, total=%d%n",
            nakedCount, nonManifoldCount, total);
        return total;
    }
}
