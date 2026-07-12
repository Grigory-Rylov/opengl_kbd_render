package com.github.grishberg.javascad;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.printingin3d.javascad.coords.Triangle3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.Const;
import eu.printingin3d.javascad.vrl.Facet;
import eu.printingin3d.javascad.vrl.Polygon;

public class StlValidator {

    private static final int PRECISION = 6;

    /** Load binary STL into Facet list */
    public static List<Facet> loadStl(String path) throws IOException {
        byte[] data = Files.readAllBytes(java.nio.file.Paths.get(path));
        return loadStl(data);
    }

    public static List<Facet> loadStl(byte[] data) {
        List<Facet> facets = new ArrayList<>();
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buf.position(80);
        if (buf.remaining() < 4) return facets;
        int count = buf.getInt();
        for (int i = 0; i < count && buf.remaining() >= 50; i++) {
            float nx = buf.getFloat(), ny = buf.getFloat(), nz = buf.getFloat();
            float v0x = buf.getFloat(), v0y = buf.getFloat(), v0z = buf.getFloat();
            float v1x = buf.getFloat(), v1y = buf.getFloat(), v1z = buf.getFloat();
            float v2x = buf.getFloat(), v2y = buf.getFloat(), v2z = buf.getFloat();
            buf.position(buf.position() + 2);
            V3d n = new V3d(nx, ny, nz);
            Triangle3d t = new Triangle3d(new V3d(v0x,v0y,v0z), new V3d(v1x,v1y,v1z), new V3d(v2x,v2y,v2z));
            facets.add(new Facet(t, n, new eu.printingin3d.javascad.utils.Color(128,128,128)));
        }
        return facets;
    }

    /**
     * Count ALL non-manifold edges: open edges (1 neighbor) + internal non-manifold (3+ neighbors).
     * This matches what Orca Slicer reports as "non-manifold edges".
     */
    /**
     * Count non-manifold issues: half-edges on edges with != 2 neighbors +
     * manifold edges with conflicting normals (adjacent facets nearly antiparallel).
     * Matches Orca Slicer's "non-manifold edges" count more closely than raw open-edge count.
     */
    public static int countNonManifoldEdgesOrcaStyle(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return 0;

        float[][][] verts = new float[N][3][3];
        float[][] normals = new float[N][3];
        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
            }
            // Compute normal from vertices
            float ex1 = verts[i][1][0] - verts[i][0][0], ey1 = verts[i][1][1] - verts[i][0][1], ez1 = verts[i][1][2] - verts[i][0][2];
            float ex2 = verts[i][2][0] - verts[i][0][0], ey2 = verts[i][2][1] - verts[i][0][1], ez2 = verts[i][2][2] - verts[i][0][2];
            float nx = ey1 * ez2 - ez1 * ey2;
            float ny = ez1 * ex2 - ex1 * ez2;
            float nz = ex1 * ey2 - ey1 * ex2;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 0) { nx /= len; ny /= len; nz /= len; }
            normals[i] = new float[]{nx, ny, nz};
        }

        // Exact edge matching: edge -> list of facet indices
        Map<OrcaEdgeKey, List<Integer>> edgeMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                float[] a = verts[i][j];
                float[] b = verts[i][(j + 1) % 3];
                OrcaEdgeKey key = exactEdgeKey(a, b);
                edgeMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }

        // Count non-manifold half-edges (all half-edges of edges with != 2 neighbors)
        int nonManifoldHE = 0;
        int openEdges = 0, manifoldEdges = 0, multiNeighborEdges = 0;
        for (List<Integer> neighbors : edgeMap.values()) {
            int c = neighbors.size();
            if (c == 1) { openEdges++; nonManifoldHE += c; }
            else if (c == 2) manifoldEdges++;
            else { multiNeighborEdges++; nonManifoldHE += c; }
        }

        // Count manifold edges with conflicting normals (dot < -0.99)
        int conflictingNormals = 0;
        for (Map.Entry<OrcaEdgeKey, List<Integer>> entry : edgeMap.entrySet()) {
            List<Integer> neighbors = entry.getValue();
            if (neighbors.size() != 2) continue;
            float[] n1 = normals[neighbors.get(0)];
            float[] n2 = normals[neighbors.get(1)];
            float dot = n1[0] * n2[0] + n1[1] * n2[1] + n1[2] * n2[2];
            if (dot < -0.99f) conflictingNormals++;
        }

        int total = nonManifoldHE + conflictingNormals;
        System.out.println("Non-manifold: open=" + openEdges + ", multi=" + multiNeighborEdges +
            ", nonManifoldHE=" + nonManifoldHE + ", conflictingNormals=" + conflictingNormals +
            ", total=" + total + ", unique_edges=" + edgeMap.size());
        return total;
    }

    public static List<Facet> validateAndRepair(List<Facet> facets) {
        int initialCount = facets.size();
        int initialOpen = countOpenEdgesOrcaStyle(facets);

        // Step 1: Remove degenerate facets
        List<Facet> nonDeg = removeDegenerateFloat32(facets);
        int removed = initialCount - nonDeg.size();
        if (removed > 0) System.out.println("Removed " + removed + " degenerate facets");

        // Step 2: Rebuild with shared vertices (exact float32 matching)
        List<Facet> shared = rebuildWithSharedVertices(nonDeg);
        int sharedOpen = countOpenEdgesOrcaStyle(shared);
        System.out.println("Shared vertex rebuild: " + initialOpen + " -> " + sharedOpen + " open edges, facets=" + shared.size());

        // Step 3: Iterative repair: weld → fill → weld → fill until convergence
        List<Facet> repaired = shared;
        int prevOpen = countOpenEdgesOrcaStyle(repaired);
        float weldTol = 0.001f;
        int pass = 0;
        while (prevOpen > 0 && pass < 4) {
            pass++;
            // Weld
            int beforeWeld = prevOpen;
            repaired = snapOpenEdgesAndRebuild(repaired, weldTol);
            prevOpen = countOpenEdgesOrcaStyle(repaired);
            System.out.println("Weld p" + pass + " tol=" + weldTol + "mm: " + beforeWeld + " -> " + prevOpen + " open edges, facets=" + repaired.size());

            // Fill holes
            if (prevOpen > 0) {
                List<Facet> filled = fillSmallHoles(repaired);
                int afterFill = countOpenEdgesOrcaStyle(filled);
                System.out.println("FillHoles p" + pass + ": " + prevOpen + " -> " + afterFill + " open edges, facets=" + filled.size());
                repaired = filled;
                prevOpen = afterFill;
            }

            // Early exit: if weld didn't reduce open edges, stop (fill can't close disconnected boundaries)
            if (prevOpen >= beforeWeld) break;

            // Increase tolerance for next pass
            weldTol = Math.min(weldTol * 2.5f, 0.01f);
        }

        // Step 4: Fix normals
        repaired = fixNormalsFacet(repaired);

        // Step 5: Validate winding
        validateFacePointOrder(repaired);

        int finalOpen = countOpenEdgesOrcaStyle(repaired);
        System.out.println("Final: " + initialOpen + " -> " + finalOpen + " open edges, facets=" + repaired.size());
        return repaired;
    }

    /**
     * Aggressive vertex welding: cluster ALL nearby vertices within tolerance,
     * merge to centroid, rebuild facets. This eliminates floating-point gaps
     * between vertices that should be shared.
     */
    private static List<Facet> weldVerticesAggressive(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return new ArrayList<>();

        float[][][] verts = new float[N][3][3];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
                minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY()); maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
            }
        }

        double boundingDiameter = Math.sqrt(
            (maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY) + (maxZ - minZ) * (maxZ - minZ));

        // Tolerance: use admesh iter-2 (shortest_edge + 2*BD/10000)
        float shortestEdge = computeShortestEdgeLinf(verts, N);
        float tolerance = shortestEdge + 2f * (float)(boundingDiameter / 10000.0);

        float[] minBounds = new float[]{(float)minX, (float)minY, (float)minZ};
        int totalVerts = N * 3;
        int[] parent = new int[totalVerts];
        for (int i = 0; i < totalVerts; i++) parent[i] = i;
        double[][] centroids = new double[totalVerts][3];
        int[] counts = new int[totalVerts];
        for (int i = 0; i < totalVerts; i++) {
            int fi = i / 3, vi = i % 3;
            centroids[i][0] = verts[fi][vi][0];
            centroids[i][1] = verts[fi][vi][1];
            centroids[i][2] = verts[fi][vi][2];
            counts[i] = 1;
        }

        Map<WeldCellKey, List<Integer>> cellMap = new HashMap<>();
        for (int i = 0; i < totalVerts; i++) {
            int fi = i / 3, vi = i % 3;
            int[] cell = gridCell(verts[fi][vi], minBounds, tolerance);
            cellMap.computeIfAbsent(new WeldCellKey(cell), k -> new ArrayList<>()).add(i);
        }

        int weldCount = 0;
        for (WeldCellKey key : cellMap.keySet()) {
            List<Integer> vIndices = cellMap.get(key);
            if (vIndices == null) continue;
            int[] cell = key.cell;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        List<Integer> nIndices = cellMap.get(new WeldCellKey(new int[]{cell[0] + dx, cell[1] + dy, cell[2] + dz}));
                        if (nIndices == null) continue;
                        for (int vi : vIndices) {
                            float[] va = verts[vi / 3][vi % 3];
                            for (int vj : nIndices) {
                                if (vj <= vi) continue;
                                float[] vb = verts[vj / 3][vj % 3];
                                float d = Math.max(Math.abs(va[0] - vb[0]),
                                    Math.max(Math.abs(va[1] - vb[1]), Math.abs(va[2] - vb[2])));
                                if (d <= tolerance) {
                                    int ri = ufFind(parent, vi), rj = ufFind(parent, vj);
                                    if (ri != rj) {
                                        ufUnion(parent, centroids, counts, ri, rj);
                                        weldCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        List<Facet> result = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            double[] c0 = centroids[ufFind(parent, i * 3)];
            double[] c1 = centroids[ufFind(parent, i * 3 + 1)];
            double[] c2 = centroids[ufFind(parent, i * 3 + 2)];
            V3d v0 = new V3d((float)c0[0], (float)c0[1], (float)c0[2]);
            V3d v1 = new V3d((float)c1[0], (float)c1[1], (float)c1[2]);
            V3d v2 = new V3d((float)c2[0], (float)c2[1], (float)c2[2]);
            if (v0.equals(v1) || v1.equals(v2) || v0.equals(v2)) continue;
            V3d normal = computeFaceNormal(Arrays.asList(v0, v1, v2));
            if (normal == null) continue;
            result.add(new Facet(new Triangle3d(v0, v1, v2), normal, facets.get(i).getColor()));
        }

        System.out.println("  Weld aggressive: tolerance=" + String.format("%.6f", tolerance) +
            ", merged=" + weldCount + " vertex pairs, facets=" + N + " -> " + result.size());
        return result;
    }

    /**
     * Fill holes by finding boundary cycles using Orca-compatible neighbor graph.
     * Only fills holes that would be detected by countOpenEdgesOrcaStyle.
     */
    private static List<Facet> fillSmallHoles(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return new ArrayList<>();

        float[][][] verts = new float[N][3][3];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
                minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY()); maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
            }
        }

        // Build neighbor graph using SAME logic as countOpenEdgesOrcaStyle
        int[][] neighbor = buildOrcaNeighborGraph(verts, N, (float)minX, (float)minY, (float)minZ,
            (maxX - minX), (maxY - minY), (maxZ - minZ));

        // Build adjacency from OPEN edges (after exact + nearby matching)
        Map<CoordK, List<CoordK>> adj = new HashMap<>();
        Map<CoordK, float[]> coordMap = new HashMap<>();

        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                if (neighbor[i][j] != -1) continue;
                float[] a = verts[i][j], b = verts[i][(j + 1) % 3];
                CoordK ka = coordKey(a), kb = coordKey(b);
                coordMap.putIfAbsent(ka, a.clone());
                coordMap.putIfAbsent(kb, b.clone());
                adj.computeIfAbsent(ka, k -> new ArrayList<>()).add(kb);
                adj.computeIfAbsent(kb, k -> new ArrayList<>()).add(ka);
            }
        }

        // Find boundary cycles
        List<Facet> newFacets = new ArrayList<>();
        Set<CoordK> visited = new HashSet<>();
        for (CoordK start : adj.keySet()) {
            if (visited.contains(start)) continue;
            List<CoordK> cycle = new ArrayList<>();
            CoordK current = start, prev = null;
            boolean closed = false;
            int steps = 0;
            while (steps < 500) {
                if (visited.contains(current)) break;
                visited.add(current);
                cycle.add(current);
                List<CoordK> neighbors = adj.get(current);
                CoordK next = null;
                if (neighbors != null) {
                    for (CoordK nb : neighbors) {
                        if (nb.equals(prev)) continue;
                        if (nb.equals(start) && cycle.size() >= 3) { closed = true; break; }
                        if (!visited.contains(nb)) { next = nb; break; }
                    }
                }
                if (closed || next == null) break;
                prev = current; current = next; steps++;
            }

            if (cycle.size() >= 3 && cycle.size() <= 2000) {
                float[] baseV = coordMap.get(cycle.get(0));
                if (baseV == null) continue;
                for (int k = 1; k < cycle.size() - 1; k++) {
                    float[] vb = coordMap.get(cycle.get(k));
                    float[] vc = coordMap.get(cycle.get(k + 1));
                    if (vb == null || vc == null) continue;
                    V3d p0 = new V3d(baseV[0], baseV[1], baseV[2]);
                    V3d p1 = new V3d(vb[0], vb[1], vb[2]);
                    V3d p2 = new V3d(vc[0], vc[1], vc[2]);
                    V3d normal = computeFaceNormal(Arrays.asList(p0, p1, p2));
                    if (normal != null) {
                        newFacets.add(new Facet(new Triangle3d(p0, p1, p2), normal, eu.printingin3d.javascad.utils.Color.GRAY));
                    }
                }
            }
        }

        List<Facet> result = new ArrayList<>(facets);
        result.addAll(newFacets);
        System.out.println("  Fill holes: " + (newFacets.size() / 3) + " holes filled, +" + newFacets.size() + " facets");
        return result;
    }

    /** Build neighbor graph using Orca-compatible logic (exact + nearby matching). */
    private static int[][] buildOrcaNeighborGraph(float[][][] verts, int N, float minX, float minY, float minZ,
                                                    double rangeX, double rangeY, double rangeZ) {
        int[][] neighbor = new int[N][3];
        for (int i = 0; i < N; i++) Arrays.fill(neighbor[i], -1);

        double bd = Math.sqrt(rangeX * rangeX + rangeY * rangeY + rangeZ * rangeZ);
        float shortestEdge = computeShortestEdgeLinf(verts, N);
        float[] minBounds = new float[]{minX, minY, minZ};

        // Exact matching
        Map<OrcaEdgeKey, OrcaEdgeRef> edgeMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                float[] a = verts[i][j], b = verts[i][(j + 1) % 3];
                OrcaEdgeKey key = exactEdgeKey(a, b);
                OrcaEdgeRef ref = new OrcaEdgeRef(i, j);
                OrcaEdgeRef existing = edgeMap.get(key);
                if (existing != null && existing.f != i) {
                    recordNeighborSimple(neighbor, i, j, existing.f, existing.j);
                } else if (existing == null) {
                    edgeMap.put(key, ref);
                }
            }
        }

        // Nearby matching (5 iterations with adaptive tolerance)
        float tolerance = shortestEdge;
        float increment = (float)(bd / 5000.0);
        for (int iter = 0; iter < 5; iter++) {
            int[] stats = computeConnectedStats(neighbor);
            if (stats[2] == N) break;
            Map<OrcaCellKey, OrcaEdgeRef> nearbyMap = new HashMap<>();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < 3; j++) {
                    if (neighbor[i][j] != -1) continue;
                    float[] a = verts[i][j], b = verts[i][(j + 1) % 3];
                    int[] ca = gridCell(a, minBounds, tolerance);
                    int[] cb = gridCell(b, minBounds, tolerance);
                    if (ca[0] == cb[0] && ca[1] == cb[1] && ca[2] == cb[2]) continue;
                    OrcaCellKey key = nearbyCellKey(ca, cb);
                    OrcaEdgeRef ref = new OrcaEdgeRef(i, j);
                    OrcaEdgeRef existing = nearbyMap.get(key);
                    if (existing != null && existing.f != i) {
                        recordNeighborSimple(neighbor, i, j, existing.f, existing.j);
                    } else if (existing == null) {
                        nearbyMap.put(key, ref);
                    }
                }
            }
            tolerance += increment;
        }

        return neighbor;
    }

    /** Exact coordinate key for float3 matching. */
    private static final class CoordK {
        final int hx, hy, hz;
        final int hash;
        CoordK(float[] v) {
            this.hx = Float.floatToIntBits(v[0]);
            this.hy = Float.floatToIntBits(v[1]);
            this.hz = Float.floatToIntBits(v[2]);
            this.hash = 31 * (31 * hx + hy) + hz;
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object o) {
            if (o instanceof CoordK) {
                CoordK c = (CoordK) o;
                return hx == c.hx && hy == c.hy && hz == c.hz;
            }
            return false;
        }
    }

    private static CoordK coordKey(float[] v) {
        return new CoordK(v);
    }

    private static float computeShortestEdgeLinf(float[][][] verts, int N) {
        float min = Float.POSITIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                float[] a = verts[i][j], b = verts[i][(j + 1) % 3];
                float d = Math.max(Math.abs(a[0] - b[0]),
                    Math.max(Math.abs(a[1] - b[1]), Math.abs(a[2] - b[2])));
                if (d > 0 && d < min) min = d;
            }
        }
        return min == Float.POSITIVE_INFINITY ? 0.0f : min;
    }

    /**
     * Rebuild mesh with shared vertex map. All vertices with identical float32
     * coordinates share a single reference. This ensures exact edge matching.
     */
    private static List<Facet> rebuildWithSharedVertices(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return new ArrayList<>();

        float[][][] verts = new float[N][3][3];
        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
            }
        }

        Map<CoordK, Integer> shared = new HashMap<>();
        int[][] vertexMap = new int[N][3];
        List<float[]> sharedList = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                CoordK k = coordKey(verts[i][j]);
                Integer idx = shared.get(k);
                if (idx != null) {
                    vertexMap[i][j] = idx;
                } else {
                    int ni = sharedList.size();
                    vertexMap[i][j] = ni;
                    sharedList.add(verts[i][j]);
                    shared.put(k, ni);
                }
            }
        }

        List<Facet> result = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            float[] v0 = sharedList.get(vertexMap[i][0]);
            float[] v1 = sharedList.get(vertexMap[i][1]);
            float[] v2 = sharedList.get(vertexMap[i][2]);
            if (Arrays.equals(v0, v1) || Arrays.equals(v1, v2) || Arrays.equals(v0, v2)) continue;
            V3d p0 = new V3d(v0[0], v0[1], v0[2]);
            V3d p1 = new V3d(v1[0], v1[1], v1[2]);
            V3d p2 = new V3d(v2[0], v2[1], v2[2]);
            V3d normal = computeFaceNormal(Arrays.asList(p0, p1, p2));
            if (normal == null) continue;
            result.add(new Facet(new Triangle3d(p0, p1, p2), normal, facets.get(i).getColor()));
        }

        System.out.println("  Shared vertices: " + N + " facets, " + sharedList.size() + " unique vertices");
        return result;
    }

    /** Remove facets where all 3 edges are open (completely isolated from the mesh). */
    private static List<Facet> removeIsolatedFacets(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return new ArrayList<>();
        float[][][] verts = new float[N][3][3];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
                minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY()); maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
            }
        }
        int[][] neighbor = buildOrcaNeighborGraph(verts, N, (float)minX, (float)minY, (float)minZ,
            maxX - minX, maxY - minY, maxZ - minZ);
        List<Facet> result = new ArrayList<>(N);
        int removed = 0;
        for (int i = 0; i < N; i++) {
            if (neighbor[i][0] == -1 && neighbor[i][1] == -1 && neighbor[i][2] == -1) {
                removed++;
            } else {
                result.add(facets.get(i));
            }
        }
        System.out.println("  Isolated removed: " + removed + " facets");
        return result;
    }

    /**
     * Weld ALL nearby vertices within tolerance using spatial hashing, then rebuild.
     * Similar to CGAL stl_generate_shared_vertices — only merges vertices that are
     * within tolerance, preserving overall mesh shape.
     */
    private static List<Facet> snapOpenEdgesAndRebuild(List<Facet> facets, float tolerance) {
        int N = facets.size();
        if (N == 0) return new ArrayList<>();

        float[][][] verts = new float[N][3][3];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
                minX = Math.min(minX, verts[i][j][0]); maxX = Math.max(maxX, verts[i][j][0]);
                minY = Math.min(minY, verts[i][j][1]); maxY = Math.max(maxY, verts[i][j][1]);
                minZ = Math.min(minZ, verts[i][j][2]); maxZ = Math.max(maxZ, verts[i][j][2]);
            }
        }

        float[] minBounds = new float[]{(float)minX, (float)minY, (float)minZ};
        int totalVerts = N * 3;
        int[] parent = new int[totalVerts];
        for (int i = 0; i < totalVerts; i++) parent[i] = i;
        double[][] centroids = new double[totalVerts][3];
        int[] counts = new int[totalVerts];
        for (int i = 0; i < totalVerts; i++) {
            int fi = i / 3, vi = i % 3;
            centroids[i][0] = verts[fi][vi][0];
            centroids[i][1] = verts[fi][vi][1];
            centroids[i][2] = verts[fi][vi][2];
            counts[i] = 1;
        }

        // Spatial hash for efficient neighbor lookup
        Map<WeldCellKey, List<Integer>> cellMap = new HashMap<>();
        for (int i = 0; i < totalVerts; i++) {
            int fi = i / 3, vi = i % 3;
            int[] cell = gridCell(verts[fi][vi], minBounds, tolerance);
            cellMap.computeIfAbsent(new WeldCellKey(cell), k -> new ArrayList<>()).add(i);
        }

        int weldCount = 0;
        for (WeldCellKey key : cellMap.keySet()) {
            List<Integer> vIndices = cellMap.get(key);
            if (vIndices == null) continue;
            int[] cell = key.cell;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        List<Integer> nIndices = cellMap.get(new WeldCellKey(new int[]{cell[0] + dx, cell[1] + dy, cell[2] + dz}));
                        if (nIndices == null) continue;
                        for (int vi : vIndices) {
                            float[] va = verts[vi / 3][vi % 3];
                            for (int vj : nIndices) {
                                if (vj <= vi) continue;
                                float[] vb = verts[vj / 3][vj % 3];
                                float d = Math.max(Math.abs(va[0] - vb[0]),
                                    Math.max(Math.abs(va[1] - vb[1]), Math.abs(va[2] - vb[2])));
                                if (d > 0 && d <= tolerance) {
                                    int ri = ufFind(parent, vi), rj = ufFind(parent, vj);
                                    if (ri != rj) {
                                        ufUnion(parent, centroids, counts, ri, rj);
                                        weldCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Rebuild with welded vertices
        List<Facet> result = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            double[] c0 = centroids[ufFind(parent, i * 3)];
            double[] c1 = centroids[ufFind(parent, i * 3 + 1)];
            double[] c2 = centroids[ufFind(parent, i * 3 + 2)];
            V3d v0 = new V3d((float)c0[0], (float)c0[1], (float)c0[2]);
            V3d v1 = new V3d((float)c1[0], (float)c1[1], (float)c1[2]);
            V3d v2 = new V3d((float)c2[0], (float)c2[1], (float)c2[2]);
            if (v0.equals(v1) || v1.equals(v2) || v0.equals(v2)) continue;
            V3d normal = computeFaceNormal(Arrays.asList(v0, v1, v2));
            if (normal == null) continue;
            result.add(new Facet(new Triangle3d(v0, v1, v2), normal, facets.get(i).getColor()));
        }

        System.out.println("  Weld: tol=" + String.format("%.4f", tolerance) +
            "mm, merged=" + weldCount + " pairs, facets=" + N + " -> " + result.size());
        return result;
    }

    private static int ufFind(int[] parent, int i) {
        while (parent[i] != i) {
            parent[i] = parent[parent[i]];
            i = parent[i];
        }
        return i;
    }

    private static void ufUnion(int[] parent, double[][] centroids, int[] counts, int ri, int rj) {
        if (counts[ri] < counts[rj]) {
            parent[ri] = rj;
            double w = (double)counts[ri] / (counts[ri] + counts[rj]);
            centroids[rj][0] = centroids[ri][0] * w + centroids[rj][0] * (1 - w);
            centroids[rj][1] = centroids[ri][1] * w + centroids[rj][1] * (1 - w);
            centroids[rj][2] = centroids[ri][2] * w + centroids[rj][2] * (1 - w);
            counts[rj] += counts[ri];
        } else {
            parent[rj] = ri;
            double w = (double)counts[rj] / (counts[ri] + counts[rj]);
            centroids[ri][0] = centroids[rj][0] * w + centroids[ri][0] * (1 - w);
            centroids[ri][1] = centroids[rj][1] * w + centroids[ri][1] * (1 - w);
            centroids[ri][2] = centroids[rj][2] * w + centroids[ri][2] * (1 - w);
            counts[ri] += counts[rj];
        }
    }

    private static final class WeldCellKey {
        final int[] cell;
        final int hash;
        WeldCellKey(int[] cell) {
            this.cell = cell;
            this.hash = 31 * (31 * cell[0] + cell[1]) + cell[2];
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object o) {
            if (o instanceof WeldCellKey) {
                WeldCellKey other = (WeldCellKey) o;
                return cell[0] == other.cell[0] && cell[1] == other.cell[1] && cell[2] == other.cell[2];
            }
            return false;
        }
    }

    private static List<Facet> removeDegenerateFloat32(List<Facet> facets) {
        List<Facet> result = new ArrayList<>();
        for (Facet f : facets) {
            var points = f.getTriangle().getPoints();
            float[] v0 = {(float)points.get(0).getX(), (float)points.get(0).getY(), (float)points.get(0).getZ()};
            float[] v1 = {(float)points.get(1).getX(), (float)points.get(1).getY(), (float)points.get(1).getZ()};
            float[] v2 = {(float)points.get(2).getX(), (float)points.get(2).getY(), (float)points.get(2).getZ()};
            if (Arrays.equals(v0, v1) || Arrays.equals(v1, v2) || Arrays.equals(v0, v2)) continue;
            result.add(f);
        }
        return result;
    }

    private static List<Facet> fixNormalsFacet(List<Facet> facets) {
        List<Facet> result = new ArrayList<>(facets.size());
        for (Facet f : facets) {
            var points = f.getTriangle().getPoints();
            V3d computed = computeFaceNormal(points);
            if (computed != null && f.getNormal().dot(computed) < 0) {
                result.add(new Facet(new Triangle3d(points.get(0), points.get(2), points.get(1)), computed, f.getColor()));
            } else {
                result.add(f);
            }
        }
        return result;
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
        // OpenSCAD approach: collect ALL vertices into a shared vertex array first,
        // so that adjacent polygons share the exact same vertex instances.
        // Round to 1e-7 precision to fix accumulated floating-point gaps from BSP splits.
        double factor = 1.0 / 1e-7;
        Map<SnapK, V3d> sharedVertices = new HashMap<>();

        List<Facet> facets = new ArrayList<>();
        for (Polygon polygon : polygons) {
            try {
                List<V3d> roundedVerts = new ArrayList<>();
                for (V3d v : polygon.getVertices()) {
                    long sx = Math.round(v.getX() * factor);
                    long sy = Math.round(v.getY() * factor);
                    long sz = Math.round(v.getZ() * factor);
                    SnapK key = new SnapK(sx, sy, sz);
                    V3d shared = sharedVertices.get(key);
                    if (shared == null) {
                        shared = new V3d(sx / factor, sy / factor, sz / factor);
                        sharedVertices.put(key, shared);
                    }
                    roundedVerts.add(shared);
                }
                List<Triangle3d> triangles = Triangulator.triangulate(roundedVerts, polygon.getNormal());
                for (Triangle3d triangle : triangles) {
                    facets.add(new Facet(triangle, polygon.getNormal(), polygon.getColor()));
                }
            } catch (Exception e) {
                System.out.println("StlValidator: ошибка триангуляции: " + e.getMessage());
            }
        }
        System.out.println("convertPolygonsToFacets: " + polygons.size() + " polygons -> " + facets.size() + " facets, " + sharedVertices.size() + " shared vertices");
        return facets;
    }

    private static final class SnapK {
        final long x, y, z;
        SnapK(long x, long y, long z) { this.x = x; this.y = y; this.z = z; }
        @Override public int hashCode() { return Long.hashCode(x ^ (y << 7) ^ (z << 14)); }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof SnapK)) return false;
            SnapK s = (SnapK)o;
            return x == s.x && y == s.y && z == s.z;
        }
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
     * Подсчитывает open edges (non-manifold) как в Orca Slicer.
     * Алгоритм: exact matching → nearby matching (2 iter) → count unconnected edges.
     * Returns count matching Orca Slicer's "non-manifold edges" display.
     */
    public static int countNonManifoldEdges(List<Facet> facets) {
        return countOpenEdgesOrcaStyle(facets);
    }

    /**
     * Orca Slicer compatible open edge count.
     * Based on admesh: remove degenerate → exact matching → nearby matching (2 iter) → count unconnected.
     */
    public static int countOpenEdgesOrcaStyle(List<Facet> facets) {
        int N = facets.size();
        if (N == 0) return 0;

        // Extract float32 vertices for each facet: float[N][3][3]
        float[][][] verts = new float[N][3][3];
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < N; i++) {
            var points = facets.get(i).getTriangle().getPoints();
            for (int j = 0; j < 3; j++) {
                V3d v = points.get(j);
                verts[i][j][0] = (float)v.getX();
                verts[i][j][1] = (float)v.getY();
                verts[i][j][2] = (float)v.getZ();
                minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY()); maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
            }
        }

        double boundingDiameter = Math.sqrt(
            (maxX - minX) * (maxX - minX) +
            (maxY - minY) * (maxY - minY) +
            (maxZ - minZ) * (maxZ - minZ));

        // Remove degenerate facets (2+ vertices identical) — matching admesh
        List<Integer> validIndices = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            float[] v0 = verts[i][0], v1 = verts[i][1], v2 = verts[i][2];
            if (v0[0] == v1[0] && v0[1] == v1[1] && v0[2] == v1[2]) continue;
            if (v1[0] == v2[0] && v1[1] == v2[1] && v1[2] == v2[2]) continue;
            if (v0[0] == v2[0] && v0[1] == v2[1] && v0[2] == v2[2]) continue;
            validIndices.add(i);
        }
        int removedDegenerate = N - validIndices.size();
        System.out.println("Removed " + removedDegenerate + " degenerate facets");

        // Rebuild verts array without degenerate facets
        int M = validIndices.size();
        float[][][] v2 = new float[M][3][3];
        int[] indexMap = new int[M];
        for (int i = 0; i < M; i++) {
            int idx = validIndices.get(i);
            v2[i][0] = verts[idx][0];
            v2[i][1] = verts[idx][1];
            v2[i][2] = verts[idx][2];
            indexMap[i] = idx;
        }
        verts = v2;
        N = M;

        // Compute shortest edge (L-infinity metric, matching admesh)
        float shortestEdge = Float.POSITIVE_INFINITY;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                float[] a = verts[i][j];
                float[] b = verts[i][(j + 1) % 3];
                float diff = Math.max(Math.abs(a[0] - b[0]),
                    Math.max(Math.abs(a[1] - b[1]), Math.abs(a[2] - b[2])));
                if (diff > 0 && diff < shortestEdge) shortestEdge = diff;
            }
        }
        if (shortestEdge == Float.POSITIVE_INFINITY) shortestEdge = 0.0f;

        // Neighbor graph: neighbor[facet][edge] = facet_index or -1
        int[][] neighbor = new int[N][3];
        for (int i = 0; i < N; i++)
            Arrays.fill(neighbor[i], -1);

        // === Phase 1: Exact matching (bit-for-bit float32, normalize negative zero) ===
        Map<OrcaEdgeKey, OrcaEdgeRef> exactEdgeMap = new HashMap<>();
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                float[] a = verts[i][j];
                float[] b = verts[i][(j + 1) % 3];
                OrcaEdgeKey key = exactEdgeKey(a, b);
                OrcaEdgeRef ref = new OrcaEdgeRef(i, j);
                OrcaEdgeRef existing = exactEdgeMap.get(key);
                if (existing != null && existing.f != i) {
                    recordNeighborSimple(neighbor, i, j, existing.f, existing.j);
                } else if (existing == null) {
                    exactEdgeMap.put(key, ref);
                }
            }
        }

        int[] stats = computeConnectedStats(neighbor);

        // === Phase 2: Nearby matching (2 iterations) ===
        float tolerance = shortestEdge;
        float increment = (float)(boundingDiameter / 10000.0);
        float[] minBounds = new float[]{(float)minX, (float)minY, (float)minZ};

        for (int iter = 0; iter < 2; iter++) {
            if (stats[2] == N) break;

            Map<OrcaCellKey, OrcaEdgeRef> nearbyEdgeMap = new HashMap<>();
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < 3; j++) {
                    if (neighbor[i][j] != -1) continue;

                    float[] a = verts[i][j];
                    float[] b = verts[i][(j + 1) % 3];

                    int[] cellA = gridCell(a, minBounds, tolerance);
                    int[] cellB = gridCell(b, minBounds, tolerance);

                    if (cellA[0] == cellB[0] && cellA[1] == cellB[1] && cellA[2] == cellB[2])
                        continue;

                    OrcaCellKey key = nearbyCellKey(cellA, cellB);
                    OrcaEdgeRef ref = new OrcaEdgeRef(i, j);
                    OrcaEdgeRef existing = nearbyEdgeMap.get(key);
                    if (existing != null && existing.f != i) {
                        recordNeighborSimple(neighbor, i, j, existing.f, existing.j);
                    } else if (existing == null) {
                        nearbyEdgeMap.put(key, ref);
                    }
                }
            }

            stats = computeConnectedStats(neighbor);
            tolerance += increment;
        }

        // === Count open edges ===
        int openEdges = 0;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < 3; j++) {
                if (neighbor[i][j] == -1) openEdges++;
            }
        }

        System.out.println("Orca-style: facets=" + N + ", open_edges=" + openEdges +
            ", shortest_edge=" + shortestEdge +
            ", bounding_diameter=" + boundingDiameter +
            ", connected_1=" + stats[0] +
            ", connected_2=" + stats[1] +
            ", connected_3=" + stats[2]);

        return openEdges;
    }

    // ===== Exact edge key (bit-for-bit float32) =====
    private static OrcaEdgeKey exactEdgeKey(float[] a, float[] b) {
        // vertex_lower: lexicographic comparison on float values
        boolean aLower = vertexLower(a, b);
        float[] first = aLower ? a : b;
        float[] second = aLower ? b : a;
        int[] key = new int[]{
            f3bits(first[0]), f3bits(first[1]), f3bits(first[2]),
            f3bits(second[0]), f3bits(second[1]), f3bits(second[2])
        };
        return new OrcaEdgeKey(key);
    }

    private static boolean vertexLower(float[] a, float[] b) {
        if (a[0] != b[0]) return a[0] < b[0];
        if (a[1] != b[1]) return a[1] < b[1];
        return a[2] < b[2];
    }

    private static int f3bits(float f) {
        int bits = Float.floatToRawIntBits(f);
        // Normalize negative zero
        if (bits == 0x80000000) return 0;
        return bits;
    }

    // ===== Nearby cell key (grid cell indices) =====
    private static OrcaCellKey nearbyCellKey(int[] cellA, int[] cellB) {
        // Canonical order by cell index
        boolean aLower = (cellA[0] != cellB[0]) ? (cellA[0] < cellB[0]) :
                         (cellA[1] != cellB[1]) ? (cellA[1] < cellB[1]) :
                                                    (cellA[2] < cellB[2]);
        int[] first = aLower ? cellA : cellB;
        int[] second = aLower ? cellB : cellA;
        return new OrcaCellKey(first[0], first[1], first[2], second[0], second[1], second[2]);
    }

    private static int[] gridCell(float[] v, float[] min, float tol) {
        return new int[]{
            (int)((v[0] - min[0]) / tol),
            (int)((v[1] - min[1]) / tol),
            (int)((v[2] - min[2]) / tol)
        };
    }

    private static void recordNeighborSimple(int[][] neighbor, int fi, int ei, int fj, int ej) {
        if (neighbor[fi][ei] == -1 && neighbor[fj][ej] == -1) {
            neighbor[fi][ei] = fj;
            neighbor[fj][ej] = fi;
        }
    }

    private static int[] computeConnectedStats(int[][] neighbor) {
        int n1 = 0, n2 = 0, n3 = 0;
        for (int i = 0; i < neighbor.length; i++) {
            int count = 0;
            for (int j = 0; j < 3; j++)
                if (neighbor[i][j] != -1) count++;
            if (count == 1) n1++;
            else if (count == 2) n2++;
            else if (count == 3) n3++;
        }
        return new int[]{n1, n2, n3};
    }

    // ===== Key classes =====
    private static class OrcaEdgeKey {
        final int k0, k1, k2, k3, k4, k5;
        OrcaEdgeKey(int[] k) { k0=k[0]; k1=k[1]; k2=k[2]; k3=k[3]; k4=k[4]; k5=k[5]; }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrcaEdgeKey)) return false;
            OrcaEdgeKey e = (OrcaEdgeKey) o;
            return k0==e.k0 && k1==e.k1 && k2==e.k2 && k3==e.k3 && k4==e.k4 && k5==e.k5;
        }
        @Override public int hashCode() {
            int h = k0 ^ (k1 << 7) ^ (k2 << 14);
            h ^= k3 ^ (k4 << 7) ^ (k5 << 14);
            return h;
        }
    }

    private static class OrcaCellKey {
        final int c0, c1, c2, c3, c4, c5;
        OrcaCellKey(int c0,int c1,int c2,int c3,int c4,int c5) {
            this.c0=c0; this.c1=c1; this.c2=c2; this.c3=c3; this.c4=c4; this.c5=c5;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OrcaCellKey)) return false;
            OrcaCellKey c = (OrcaCellKey) o;
            return c0==c.c0 && c1==c.c1 && c2==c.c2 && c3==c.c3 && c4==c.c4 && c5==c.c5;
        }
        @Override public int hashCode() {
            int h = c0 ^ (c1 << 7) ^ (c2 << 14);
            h ^= c3 ^ (c4 << 7) ^ (c5 << 14);
            return h;
        }
    }

    private static class OrcaEdgeRef {
        final int f, j;
        OrcaEdgeRef(int f, int j) { this.f = f; this.j = j; }
    }

    public static int countNonManifoldEdgesEpsilon(List<Facet> facets, double eps) {
        // Snap all vertices to eps grid
        Map<String, V3d> vertexMap = new HashMap<>();
        List<V3d[]> snappedFacets = new ArrayList<>();

        for (Facet f : facets) {
            var points = f.getTriangle().getPoints();
            V3d[] snapped = new V3d[3];
            for (int i = 0; i < 3; i++) {
                V3d v = points.get(i);
                String key = snapKey(v, eps);
                V3d canonical = vertexMap.computeIfAbsent(key, k ->
                    new V3d(
                        Math.round(v.getX() / eps) * eps,
                        Math.round(v.getY() / eps) * eps,
                        Math.round(v.getZ() / eps) * eps
                    ));
                snapped[i] = canonical;
            }
            snappedFacets.add(snapped);
        }

        // Count edges
        Map<Edge6Int, Integer> edgeCount = new HashMap<>();
        for (V3d[] verts : snappedFacets) {
            for (int i = 0; i < 3; i++) {
                V3d a = verts[i];
                V3d b = verts[(i + 1) % 3];
                Edge6Int key = makeEdgeKey6(a, b);
                edgeCount.merge(key, 1, Integer::sum);
            }
        }

        int nonManifold = 0;
        for (int count : edgeCount.values()) {
            if (count > 2) nonManifold++;
        }

        System.out.println("Non-manifold edges (eps=" + eps + "): " + nonManifold + " / " + edgeCount.size() + " unique edges, " + snappedFacets.size() + " facets");
        return nonManifold;
    }

    private static String snapKey(V3d v, double eps) {
        return Math.round(v.getX() / eps) + "_" + Math.round(v.getY() / eps) + "_" + Math.round(v.getZ() / eps);
    }

    private static double calculateModelScaleFromFacets(List<Facet> facets) {
        double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        for (Facet f : facets) {
            for (V3d v : f.getTriangle().getPoints()) {
                minX = Math.min(minX, v.getX()); maxX = Math.max(maxX, v.getX());
                minY = Math.min(minY, v.getY()); maxY = Math.max(maxY, v.getY());
                minZ = Math.min(minZ, v.getZ()); maxZ = Math.max(maxZ, v.getZ());
            }
        }
        return Math.max(Math.max(maxX - minX, maxY - minY), maxZ - minZ);
    }

    private static class Edge6Int {
        final int x1, y1, z1, x2, y2, z2;
        Edge6Int(int x1, int y1, int z1, int x2, int y2, int z2) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edge6Int)) return false;
            Edge6Int e = (Edge6Int) o;
            return x1 == e.x1 && y1 == e.y1 && z1 == e.z1 && x2 == e.x2 && y2 == e.y2 && z2 == e.z2;
        }
        @Override public int hashCode() {
            int result = x1;
            result = 31 * result + y1;
            result = 31 * result + z1;
            result = 31 * result + x2;
            result = 31 * result + y2;
            result = 31 * result + z2;
            return result;
        }
    }

    private static Edge6Int makeEdgeKey6(V3d a, V3d b) {
        int ax = Float.floatToRawIntBits((float)a.getX());
        int ay = Float.floatToRawIntBits((float)a.getY());
        int az = Float.floatToRawIntBits((float)a.getZ());
        int bx = Float.floatToRawIntBits((float)b.getX());
        int by = Float.floatToRawIntBits((float)b.getY());
        int bz = Float.floatToRawIntBits((float)b.getZ());

        long ha = ((long)ax << 32) | (ay & 0xFFFFFFFFL);
        long hb = ((long)bx << 32) | (by & 0xFFFFFFFFL);
        if (ha > hb || (ha == hb && az > bz))
            return new Edge6Int(bx, by, bz, ax, ay, az);
        return new Edge6Int(ax, ay, az, bx, by, bz);
    }
}
