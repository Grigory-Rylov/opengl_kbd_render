package com.github.grishberg.cad3d;

import com.github.grishberg.javascad.StlValidator;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.vrl.Facet;

import java.util.*;

/**
 * Analyze our exported STL with epsilon-based matching to match formware's perspective.
 */
public class AnalyzeWithEpsilon {
    static final double EPS = 1e-4;

    public static void main(String[] args) throws Exception {
        List<Facet> facets = CompareStl.readStl("/tmp/matrix_right.stl");
        System.out.println("Loaded: " + facets.size() + " facets");

        // Count naked edges with epsilon matching
        Map<EpsilonEdge, Integer> edgeCount = new HashMap<>();
        for (Facet f : facets) {
            List<V3d> pts = f.getTriangle().getPoints();
            for (int i = 0; i < 3; i++) {
                EpsilonEdge e = new EpsilonEdge(pts.get(i), pts.get((i + 1) % 3));
                edgeCount.merge(e, 1, Integer::sum);
            }
        }

        int naked = 0, manifold = 0, nonManifold = 0;
        for (int c : edgeCount.values()) {
            if (c == 1) naked++;
            else if (c == 2) manifold++;
            else nonManifold++;
        }
        System.out.println("Epsilon-based edges: naked=" + naked + ", manifold=" + manifold + ", non-manifold=" + nonManifold);

        // Count duplicate faces (epsilon-based)
        Set<List<EpsilonVert>> seen = new HashSet<>();
        int duplicates = 0;
        for (Facet f : facets) {
            List<V3d> pts = f.getTriangle().getPoints();
            List<EpsilonVert> sorted = new ArrayList<>();
            for (V3d v : pts) sorted.add(new EpsilonVert(v));
            Collections.sort(sorted);
            if (!seen.add(new ArrayList<>(sorted))) {
                duplicates++;
            }
        }
        System.out.println("Duplicate faces (epsilon): " + duplicates);

        // Count degenerate (all 3 vertices within epsilon)
        int degenerate = 0;
        for (Facet f : facets) {
            List<V3d> pts = f.getTriangle().getPoints();
            if (pts.get(0).distance(pts.get(1)) < EPS &&
                pts.get(1).distance(pts.get(2)) < EPS &&
                pts.get(0).distance(pts.get(2)) < EPS) {
                degenerate++;
            }
        }
        System.out.println("Degenerate faces: " + degenerate);

        // Count inverted normals
        int inverted = 0;
        for (Facet f : facets) {
            V3d normal = f.getNormal();
            List<V3d> pts = f.getTriangle().getPoints();
            V3d computed = pts.get(1).subtract(pts.get(0)).cross(pts.get(2).subtract(pts.get(0)));
            computed = computed.scale(1.0 / computed.magnitude());
            double dot = normal.dot(computed);
            if (dot < -0.7) inverted++;
        }
        System.out.println("Inverted normals: " + inverted);

        // Count unique vertices with epsilon
        Set<EpsilonVert> uniqueVerts = new HashSet<>();
        for (Facet f : facets) {
            for (V3d v : f.getTriangle().getPoints()) {
                uniqueVerts.add(new EpsilonVert(v));
            }
        }
        System.out.println("Unique vertices (epsilon): " + uniqueVerts.size());
    }

    static class EpsilonVert implements Comparable<EpsilonVert> {
        final long x, y, z;
        EpsilonVert(V3d v) {
            x = Math.round(v.getX() / EPS);
            y = Math.round(v.getY() / EPS);
            z = Math.round(v.getZ() / EPS);
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof EpsilonVert)) return false;
            EpsilonVert e = (EpsilonVert) o;
            return x == e.x && y == e.y && z == e.z;
        }
        @Override public int hashCode() {
            int r = Long.hashCode(x); r = 31 * r + Long.hashCode(y); r = 31 * r + Long.hashCode(z);
            return r;
        }
        @Override public int compareTo(EpsilonVert o) {
            int c = Long.compare(x, o.x); if (c != 0) return c;
            c = Long.compare(y, o.y); if (c != 0) return c;
            return Long.compare(z, o.z);
        }
    }

    static class EpsilonEdge {
        final EpsilonVert a, b;
        EpsilonEdge(V3d p0, V3d p1) {
            EpsilonVert f0 = new EpsilonVert(p0), f1 = new EpsilonVert(p1);
            if (f0.compareTo(f1) <= 0) { a = f0; b = f1; }
            else { a = f1; b = f0; }
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof EpsilonEdge)) return false;
            EpsilonEdge e = (EpsilonEdge) o;
            return a.equals(e.a) && b.equals(e.b);
        }
        @Override public int hashCode() {
            return 31 * a.hashCode() + b.hashCode();
        }
    }
}
