package com.github.grishberg.cad3d;

import eu.printingin3d.javascad.coords.Triangle3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.Facet;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compare raw vs repaired STL to understand the repair algorithm.
 */
public class CompareStl {
    public static void main(String[] args) throws Exception {
        List<Facet> raw = readStl("/tmp/matrix_right_raw.stl");
        List<Facet> repaired = readStl("/tmp/matrix_right_repaired.stl");

        System.out.println("Raw: " + raw.size() + " facets");
        System.out.println("Repaired: " + repaired.size() + " facets");
        System.out.println("Difference: " + (repaired.size() - raw.size()) + " new facets");

        // Count naked edges in repaired using FloatEdgeKey (matching our validator)
        Map<FloatEdgeKey, Integer> repairedEdgeCount = new HashMap<>();
        for (Facet f : repaired) {
            List<V3d> pts = f.getTriangle().getPoints();
            for (int i = 0; i < 3; i++) {
                FloatEdgeKey e = new FloatEdgeKey(pts.get(i), pts.get((i + 1) % 3));
                repairedEdgeCount.merge(e, 1, Integer::sum);
            }
        }
        int repairedNaked = 0;
        for (int c : repairedEdgeCount.values()) {
            if (c == 1) repairedNaked++;
        }
        System.out.println("Repaired naked edges (FloatEdgeKey): " + repairedNaked);

        // Count naked edges in repaired using V3d.epsilon equals
        Map<Edge, Integer> repairedEdgeCountV3d = new HashMap<>();
        for (Facet f : repaired) {
            List<V3d> pts = f.getTriangle().getPoints();
            for (int i = 0; i < 3; i++) {
                Edge e = new Edge(pts.get(i), pts.get((i + 1) % 3));
                repairedEdgeCountV3d.merge(e, 1, Integer::sum);
            }
        }
        int repairedNakedV3d = 0;
        for (int c : repairedEdgeCountV3d.values()) {
            if (c == 1) repairedNakedV3d++;
        }
        System.out.println("Repaired naked edges (V3d.epsilon): " + repairedNakedV3d);

        // Count unique vertices in repaired using different methods
        Set<FloatKey> repairedVertsFloat = new HashSet<>();
        Set<V3d> repairedVertsV3d = new HashSet<>();
        for (Facet f : repaired) {
            for (V3d v : f.getTriangle().getPoints()) {
                repairedVertsFloat.add(new FloatKey(v));
                repairedVertsV3d.add(v);
            }
        }
        System.out.println("Repaired unique vertices (FloatKey): " + repairedVertsFloat.size());
        System.out.println("Repaired unique vertices (V3d.equals): " + repairedVertsV3d.size());

        // Count unique vertices in raw
        Set<FloatKey> rawVertsFloat = new HashSet<>();
        Set<V3d> rawVertsV3d = new HashSet<>();
        for (Facet f : raw) {
            for (V3d v : f.getTriangle().getPoints()) {
                rawVertsFloat.add(new FloatKey(v));
                rawVertsV3d.add(v);
            }
        }
        System.out.println("Raw unique vertices (FloatKey): " + rawVertsFloat.size());
        System.out.println("Raw unique vertices (V3d.equals): " + rawVertsV3d.size());

        // Analyze what the tool did differently
        // Find facets in repaired that are NOT in raw (new facets added for hole filling)
        Set<FacetKey> rawFacetKeys = new HashSet<>();
        for (Facet f : raw) {
            List<V3d> pts = f.getTriangle().getPoints();
            rawFacetKeys.add(new FacetKey(pts.get(0), pts.get(1), pts.get(2)));
        }

        Set<FacetKey> repairedFacetKeys = new HashSet<>();
        for (Facet f : repaired) {
            List<V3d> pts = f.getTriangle().getPoints();
            repairedFacetKeys.add(new FacetKey(pts.get(0), pts.get(1), pts.get(2)));
        }

        Set<FacetKey> newFacets = new HashSet<>(repairedFacetKeys);
        newFacets.removeAll(rawFacetKeys);
        System.out.println("New facets in repaired: " + newFacets.size());

        // Analyze edge distribution
        Map<Integer, Long> repairedEdgeDist = repairedEdgeCount.values().stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        System.out.println("Edge distribution in repaired (FloatEdgeKey): " + repairedEdgeDist);

        Map<Integer, Long> rawEdgeDist = countRawEdges(raw).values().stream().collect(Collectors.groupingBy(v -> v, Collectors.counting()));
        System.out.println("Edge distribution in raw (FloatEdgeKey): " + rawEdgeDist);

        // Analyze naked edge lengths in raw
        System.out.println("\n=== Analyzing raw naked edge lengths (FloatEdgeKey) ===");
        List<Double> nakedEdgeLengths = new ArrayList<>();
        for (Map.Entry<FloatEdgeKey, Integer> entry : countRawEdges(raw).entrySet()) {
            if (entry.getValue() == 1) {
                nakedEdgeLengths.add(entry.getKey().length());
            }
        }
        nakedEdgeLengths.sort(Double::compareTo);
        System.out.println("Naked edges total: " + nakedEdgeLengths.size());
        if (!nakedEdgeLengths.isEmpty()) {
            System.out.println("Min: " + nakedEdgeLengths.get(0));
            System.out.println("Max: " + nakedEdgeLengths.get(nakedEdgeLengths.size() - 1));
            System.out.println("Median: " + nakedEdgeLengths.get(nakedEdgeLengths.size() / 2));

            // Count by size buckets
            double[] buckets = {0.001, 0.01, 0.05, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0, 50.0};
            for (double threshold : buckets) {
                long count = nakedEdgeLengths.stream().filter(l -> l < threshold).count();
                System.out.println("  < " + threshold + "mm: " + count);
            }
        }
    }

    static Map<FloatEdgeKey, Integer> countRawEdges(List<Facet> facets) {
        Map<FloatEdgeKey, Integer> edgeCount = new HashMap<>();
        for (Facet f : facets) {
            List<V3d> pts = f.getTriangle().getPoints();
            for (int i = 0; i < 3; i++) {
                FloatEdgeKey e = new FloatEdgeKey(pts.get(i), pts.get((i + 1) % 3));
                edgeCount.merge(e, 1, Integer::sum);
            }
        }
        return edgeCount;
    }

    static List<Facet> readStl(String path) throws Exception {
        List<Facet> facets = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] header = new byte[80];
            fis.read(header);
            int count = readInt(fis);
            for (int i = 0; i < count; i++) {
                V3d normal = new V3d(readFloat(fis), readFloat(fis), readFloat(fis));
                V3d v0 = new V3d(readFloat(fis), readFloat(fis), readFloat(fis));
                V3d v1 = new V3d(readFloat(fis), readFloat(fis), readFloat(fis));
                V3d v2 = new V3d(readFloat(fis), readFloat(fis), readFloat(fis));
                fis.skip(2);
                facets.add(new Facet(new Triangle3d(v0, v1, v2), normal, null));
            }
        }
        return facets;
    }

    static float readFloat(FileInputStream fis) throws Exception {
        byte[] b = new byte[4];
        fis.read(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    static int readInt(FileInputStream fis) throws Exception {
        byte[] b = new byte[4];
        fis.read(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private static final class FloatKey {
        final int x, y, z;
        FloatKey(V3d v) {
            this.x = Float.floatToRawIntBits((float) v.getX());
            this.y = Float.floatToRawIntBits((float) v.getY());
            this.z = Float.floatToRawIntBits((float) v.getZ());
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FloatKey)) return false;
            FloatKey other = (FloatKey) o;
            return x == other.x && y == other.y && z == other.z;
        }
        @Override public int hashCode() {
            int r = x; r = 31 * r + y; r = 31 * r + z;
            return r;
        }

        double distance(FloatKey other) {
            double dx = Float.intBitsToFloat(this.x) - Float.intBitsToFloat(other.x);
            double dy = Float.intBitsToFloat(this.y) - Float.intBitsToFloat(other.y);
            double dz = Float.intBitsToFloat(this.z) - Float.intBitsToFloat(other.z);
            return Math.sqrt(dx*dx + dy*dy + dz*dz);
        }
    }

    private static final class FloatEdgeKey {
        final FloatKey a, b;
        FloatEdgeKey(V3d p0, V3d p1) {
            FloatKey f0 = new FloatKey(p0);
            FloatKey f1 = new FloatKey(p1);
            if (f0.hashCode() <= f1.hashCode()) { a = f0; b = f1; }
            else { a = f1; b = f0; }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FloatEdgeKey)) return false;
            FloatEdgeKey other = (FloatEdgeKey) o;
            return a.equals(other.a) && b.equals(other.b);
        }
        @Override public int hashCode() {
            return 31 * a.hashCode() + b.hashCode();
        }

        double length() {
            return a.distance(b);
        }
    }

    private static final class Edge {
        final V3d a, b;
        Edge(V3d a, V3d b) {
            if (compare(a, b) > 0) { this.a = b; this.b = a; }
            else { this.a = a; this.b = b; }
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge)) return false;
            Edge other = (Edge) o;
            return a.equals(other.a) && b.equals(other.b);
        }
        @Override public int hashCode() {
            return 31 * a.hashCode() + b.hashCode();
        }
    }

    private static int compare(V3d a, V3d b) {
        int c = Double.compare(a.getX(), b.getX());
        if (c != 0) return c;
        c = Double.compare(a.getY(), b.getY());
        if (c != 0) return c;
        return Double.compare(a.getZ(), b.getZ());
    }

    private static final class FacetKey {
        final V3d a, b, c;
        FacetKey(V3d p0, V3d p1, V3d p2) {
            V3d[] sorted = new V3d[]{p0, p1, p2};
            Arrays.sort(sorted, (u, v) -> compare(u, v));
            this.a = sorted[0]; this.b = sorted[1]; this.c = sorted[2];
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FacetKey)) return false;
            FacetKey other = (FacetKey) o;
            return a.equals(other.a) && b.equals(other.b) && c.equals(other.c);
        }
        @Override public int hashCode() {
            int r = a.hashCode(); r = 31 * r + b.hashCode(); r = 31 * r + c.hashCode();
            return r;
        }
    }
}
