package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.V3d;
import java.util.*;
import java.util.stream.Collectors;

public class VoronoiSurface {
    private final S12x3 surface;
    private final List<V3d> voronoiSites;
    private final double edgeWidth;
    private final double neighborRadius;

    public VoronoiSurface(V3d[][] controlPoints, List<V3d> sites, double edgeWidth) {
        this.surface = S12x3.create(controlPoints);
        this.voronoiSites = sites;
        this.edgeWidth = edgeWidth;
        this.neighborRadius = edgeWidth * 2.0;
    }

    public List<List<V3d>> calculateVoronoiEdges(int resolution) {
        List<V3d> surfacePoints = surface.buildSurface(resolution);
        Map<V3d, V3d> pointToSite = mapPointsToSites(surfacePoints);
        List<V3d> edgePoints = findEdgePoints(surfacePoints, pointToSite);
        return groupEdgePoints(edgePoints);
    }

    private Map<V3d, V3d> mapPointsToSites(List<V3d> points) {
        return points.stream().collect(Collectors.toMap(
            p -> p,
            this::findNearestSite,
            (a, b) -> a
        ));
    }

    private V3d findNearestSite(V3d point) {
        return voronoiSites.stream()
            .min(Comparator.comparingDouble(s -> distance(s, point)))
            .orElseThrow();
    }

    private double distance(V3d a, V3d b) {
        return Math.sqrt(distanceSq(a, b));
    }

    private static double distanceSq(V3d a, V3d b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        double dz = a.getZ() - b.getZ();
        return dx*dx + dy*dy + dz*dz;
    }

    private List<V3d> findEdgePoints(List<V3d> points, Map<V3d, V3d> pointToSite) {
        List<V3d> edges = new ArrayList<>();
        double threshold = edgeWidth * edgeWidth;

        for (V3d p : points) {
            V3d site = pointToSite.get(p);
            double distToSite = distanceSq(p, site);
            double minDist = findMinDistanceToOtherSites(p, site);

            if (Math.abs(Math.sqrt(distToSite) - Math.sqrt(minDist)) <= edgeWidth) {
                edges.add(p);
            }
        }
        return edges;
    }

    private double findMinDistanceToOtherSites(V3d point, V3d currentSite) {
        return voronoiSites.stream()
            .filter(s -> !s.equals(currentSite))
            .mapToDouble(s -> distanceSq(point, s))
            .min()
            .orElse(Double.MAX_VALUE);
    }

    private List<List<V3d>> groupEdgePoints(List<V3d> edgePoints) {
        KdTree tree = new KdTree(edgePoints);
        Map<V3d, Boolean> visited = new HashMap<>();
        List<List<V3d>> contours = new ArrayList<>();

        for (V3d p : edgePoints) {
            if (!visited.containsKey(p)) {
                List<V3d> contour = new ArrayList<>();
                followContour(p, tree, visited, contour);
                if (contour.size() > 2) {
                    contours.add(closeContour(contour));
                }
            }
        }
        return contours;
    }

    private void followContour(V3d start, KdTree tree,
                               Map<V3d, Boolean> visited, List<V3d> contour) {
        V3d current = start;
        do {
            contour.add(current);
            visited.put(current, true);
            current = findNextUnvisitedNeighbor(current, tree, visited);
        } while (current != null && !current.equals(start));
    }

    private V3d findNextUnvisitedNeighbor(V3d point, KdTree tree, Map<V3d, Boolean> visited) {
        return tree.nearestNeighbors(point, neighborRadius).stream()
            .filter(p -> !visited.containsKey(p))
            .min(Comparator.comparingDouble(p -> distance(point, p)))
            .orElse(null);
    }

    private List<V3d> closeContour(List<V3d> contour) {
        if (!contour.get(0).equals(contour.get(contour.size()-1))) {
            contour.add(contour.get(0));
        }
        return contour;
    }


    // Вспомогательный класс для пространственного индексирования
    private static class KdTree {
        private final List<V3d> points;
        private final Map<V3d, Integer> index;

        public KdTree(List<V3d> points) {
            this.points = new ArrayList<>(points);
            this.index = new HashMap<>();
            for (int i = 0; i < points.size(); i++) {
                index.put(points.get(i), i);
            }
        }

        public List<V3d> nearestNeighbors(V3d query, double radius) {
            double radiusSq = radius * radius;
            return points.stream()
                .filter(p -> distanceSq(p, query) <= radiusSq)
                .collect(Collectors.toList());
        }

    }
}