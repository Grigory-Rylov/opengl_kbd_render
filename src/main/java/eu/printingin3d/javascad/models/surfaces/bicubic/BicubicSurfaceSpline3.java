package eu.printingin3d.javascad.models.surfaces.bicubic;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

public class BicubicSurfaceSpline3 implements SurfaceStrategy {

    private static final int kernelRadius = 170 / 5;
    private final V3d[] inPoints;
    private final int inWidth;
    private final int inHeight;
    private final int resolution;

    private static final double[] gaussianKernel = CommonMath.calcGaussianKernel(kernelRadius, false);

    public BicubicSurfaceSpline3(V3d[] controlPoints, int width, int height, int resolution) {
        this.inPoints = controlPoints;
        inWidth = width;
        inHeight = height;
        this.resolution = resolution;
    }

    //  +-----> X (row)
    //  |                               pseg1
    //  |                 p00-------p01-------p02-------p03
    //  V                  |         |         |         |
    //  Z (col)            |         |         |         |
    //                     |         |   seg1  |         |
    //                    p10-------p11-------p12-------p13
    //                     |         |         |         |
    //               pseg2 |    seg2 |  COONS  | seg4    | pseg4
    //                     |         |         |         |
    //                    p20-------p21-------p22-------p23
    //                     |         |   seg3  |         |
    //                     |         |         |         |
    //                     |         |         |         |
    //                    p30-------p31-------p32-------p33
    //                                  pseg3
    //
    // p00..p33 are points from the input array.
    // p11, p12, p21, p22 are the points around the interpolation zone.
    // The surrounding points are needed for bicubic blending.
    // seg1..seg4 and pseg1..pseg4 are curve segments calculated above.
    // seg1, seg3, pseg1 and pseg3 are in rowSegments array and their indices correspond to the
    // indices of
    // p11, p21, p01 and p31 respectively.
    // seg2, seg4, pseg2 and pseg4 are in colSegments array and their indices correspond to the
    // transposed
    // indices of p11, p12, p10 and p13 respectively.
    @Override
    public SurfaceStrategy.Result buildSurface() {
        int outWidth = (resolution - 1) * (inWidth - 1) + 1;
        int outHeight = (resolution - 1) * (inHeight - 1) + 1;
        List<V3d> result = new ArrayList<>(outHeight);

        Vertex[] vertices = build(resolution);
        //int[]indexes = triangulateGrid(outWidth, outHeight);
        //computeNormals(vertices, indexes);
        //smoothNormalsWithKernel(vertices, outWidth, outHeight,gaussianKernel, kernelRadius);

        for (int i = 0; i < vertices.length; i++) {
            result.add(vertices[i].position);
        }
        return new SurfaceStrategy.Result(result, outWidth, outHeight);
    }

    private Vertex[] build(int resolution) {
        final double c = 2.0;
        Segment[] rowSegments = getRowSegments(c);
        Segment[] colSegments = getColSegments(c);

        if (rowSegments.length == 0 || colSegments.length == 0) {
            return new Vertex[0];
        }

        --resolution;
        int outWidth = resolution * (inWidth - 1) + 1;
        int outHeight = resolution * (inHeight - 1) + 1;
        Vertex[] outPoints = new Vertex[outWidth * outHeight];

        for (int y = 0; y < inHeight; y++) {
            for (int x = 0; x < inWidth; x++) {
                int p11 = gridIndex(inWidth, inHeight, x, y);
                int p12 = gridIndex(inWidth, inHeight, x + 1, y);
                int p21 = gridIndex(inWidth, inHeight, x, y + 1);
                int p22 = gridIndex(inWidth, inHeight, x + 1, y + 1);

                if (p11 >= 0 && p12 >= 0 && p21 >= 0 && p22 >= 0) {
                    int p00 = gridIndexClamped(inWidth, inHeight, x - 1, y - 1);
                    int p01 = gridIndexClamped(inWidth, inHeight, x, y - 1);
                    int p02 = gridIndexClamped(inWidth, inHeight, x + 1, y - 1);
                    int p03 = gridIndexClamped(inWidth, inHeight, x + 2, y - 1);

                    int p10 = gridIndexClamped(inWidth, inHeight, x - 1, y);
                    int p13 = gridIndexClamped(inWidth, inHeight, x + 2, y);
                    int p20 = gridIndexClamped(inWidth, inHeight, x - 1, y + 1);
                    int p23 = gridIndexClamped(inWidth, inHeight, x + 2, y + 1);

                    int p30 = gridIndexClamped(inWidth, inHeight, x - 1, y + 2);
                    int p31 = gridIndexClamped(inWidth, inHeight, x, y + 2);
                    int p32 = gridIndexClamped(inWidth, inHeight, x + 1, y + 2);
                    int p33 = gridIndexClamped(inWidth, inHeight, x + 2, y + 2);

                    double[] pValues =
                        {
                            inPoints[p00].z, inPoints[p01].z, inPoints[p02].z, inPoints[p03].z,
                            inPoints[p10].z, inPoints[p11].z, inPoints[p12].z, inPoints[p13].z,
                            inPoints[p20].z, inPoints[p21].z, inPoints[p22].z, inPoints[p23].z,
                            inPoints[p30].z, inPoints[p31].z, inPoints[p32].z, inPoints[p33].z
                        };
                    double[] aValues = CommonMath.bicubicMatrix(pValues);

                    V3d point11 = inPoints[p11];
                    V3d point12 = inPoints[p12];
                    V3d point21 = inPoints[p21];
                    V3d point22 = inPoints[p22];

                    for (int dx = 0; dx < resolution; ++dx) {
                        double u = (double) dx / (double) resolution;
                        for (int dy = 0; dy < resolution; ++dy) {

                            double v = (double) dy / (double) resolution;

                            double leftX = point11.x + (point21.x - point11.x) * v;
                            double rightX = point12.x + (point22.x - point12.x) * v;
                            double topY = point11.y + (point12.y - point11.y) * u;
                            double bottomY = point21.y + (point22.y - point21.y) * u;

                            double targetX = leftX + (rightX - leftX) * u;
                            double targetY = topY + (bottomY - topY) * v;

                            if (dx == 0 && dy == 0) {
                                outPoints[outIndex(outWidth, resolution, x, y, dx, dy)] =
                                    new Vertex(inPoints[p11]);
                            } else {
                                double biSurface = CommonMath.bicubicInterpolate(aValues, v, u);
                                double targetZ = biSurface;

                                outPoints[outIndex(outWidth, resolution, x, y, dx, dy)] =
                                    new Vertex(new V3d(
                                        targetX,
                                        targetY,
                                        targetZ
                                    ));
                            }
                        }
                    }
                } else if (p11 >= 0 && p12 >= 0 && p21 < 0 && p22 < 0) {
                    // front horizontal edge
                    int seg1 = p11;

                    outPoints[outIndex(outWidth, resolution, x, y, 0, 0)] =
                        new Vertex(inPoints[p11]);

                    for (int dx = 1; dx < resolution; ++dx) {
                        double u = (double) dx / (double) resolution;
                        Vec2 c1 = rowSegments[seg1].calc(u, false);
                        outPoints[outIndex(outWidth, resolution, x, y, dx, 0)] =
                            new Vertex(new V3d(
                                inPoints[p11].x + u * (inPoints[p12].x - inPoints[p11].x),
                                inPoints[p11].y + u * (inPoints[p12].y - inPoints[p11].y),
                                c1.y

                            ));
                    }
                } else if (p11 >= 0 && p21 >= 0 && p12 < 0 && p22 < 0) {
                    // right edge
                    int seg2 = gridIndexClamped(inHeight, inWidth, y, x); // Transposed p11.

                    outPoints[outIndex(outWidth, resolution, x, y, 0, 0)] =
                        new Vertex(inPoints[p11]);

                    for (int dz = 1; dz < resolution; ++dz) {
                        double t = (double) dz / (double) resolution;
                        Vec2 c1 = colSegments[seg2].calc(t, false);
                        outPoints[outIndex(outWidth, resolution, x, y, 0, dz)] =
                            new Vertex(new V3d(
                                inPoints[p11].x + t * (inPoints[p21].x - inPoints[p11].x),
                                inPoints[p11].y + t * (inPoints[p21].y - inPoints[p11].y),
                                c1.y
                            ));
                    }
                } else if (p11 >= 0 && p12 < 0 && p21 < 0 && p22 < 0) {
                    outPoints[outIndex(outWidth, resolution, x, y, 0, 0)] =
                        new Vertex(inPoints[p11]);
                }
            }
        }

        return outPoints;
    }

    private int gridIndexClamped(int w, int h, int x, int y) {
        if (x < 0) {
            x = 0;
        } else if (x > w - 1) {
            x = w - 1;
        }
        if (y < 0) {
            y = 0;
        } else if (y > h - 1) {
            y = h - 1;
        }
        return index(w, x, y);
    }

    private int index(int w, int x, int y) {
        return y * w + x;
    }

    int gridIndex(int w, int h, int x, int y) {
        return x < w && y < h ? index(w, x, y) : -1;
    }

    int outIndex(int w, int r, int x, int y, int dx, int dy) {
        return (y * r + dy) * w + (x * r + dx);
    }

    private int[] triangulateGrid(int width, int height) {
        int n = (width - 1) * (height - 1) * 6;
        int[] indices = new int[n];
        int i = 0;
        for (int z = 0; z < height - 1; ++z) {
            for (int x = 0; x < width - 1; ++x) {
                // TL --- TR
                //  |  __/ |
                //  | /    |
                // BL --- BR
                int tl = index(width, x, z);
                int tr = index(width, x + 1, z);
                int bl = index(width, x, z + 1);
                int br = index(width, x + 1, z + 1);
                // First triangle
                indices[i++] = tr;
                indices[i++] = tl;
                indices[i++] = bl;
                // Second triangle
                indices[i++] = bl;
                indices[i++] = br;
                indices[i++] = tr;
            }
        }
        return indices;
    }

    private void computeNormals(Vertex[] vertices, int[] indices) {
        for (int i = 0, n = vertices.length; i < n; i += 3) {
            int n0 = indices[i];
            int n1 = indices[i + 1];
            int n2 = indices[i + 2];
            Vertex v0 = vertices[n0];
            Vertex v1 = vertices[n1];
            Vertex v2 = vertices[n2];
            V3d normal = CommonMath.normal(v0.position, v1.position, v2.position);
            v0.normal = v0.normal.add(normal);
            v1.normal = v1.normal.add(normal);
            v2.normal = v2.normal.add(normal);
            v0.normal = v0.normal.unit();
            v1.normal = v1.normal.unit();
            v2.normal = v2.normal.unit();
            vertices[n0] = v0;
            vertices[n1] = v1;
            vertices[n2] = v2;
        }
    }

    Vertex[] smoothNormalsWithKernel(
        Vertex[] inVertices,
        int width,
        int height,
        double[] kernel,
        int radius
    ) {
        int n = radius * 2 + 1;
        Vertex[] outVertices = inVertices;
        for (int z = 0; z < height; ++z) {
            for (int x = 0; x < width; ++x) {
                V3d normal = new V3d(0, 0, 0);
                for (int i = -radius; i < radius; ++i) {
                    for (int j = -radius; j < radius; ++j) {
                        int ind = gridIndex(width, height, x + j, z + i);
                        if (ind > -1) {
                            normal = normal.add(inVertices[ind].normal.mul(kernel[index(
                                n,
                                j + radius,
                                i + radius
                            )]));
                        }
                    }
                }
                normal = normal.unit();
                outVertices[index(width, x, z)].normal = normal;
            }
        }

        return outVertices;
    }


    private Segment[] getRowSegments(double c) {
        Segment[] segments = new Segment[inHeight * inWidth];
        for (int i = 0; i < inHeight * inWidth; i++) {
            segments[i] = new Segment();
        }
        Vec2[] points = new Vec2[inWidth];
        int segPtr = 0;
        for (int y = 0; y < inHeight; ++y) {
            for (int x = 0; x < inWidth; ++x) {
                int idx = index(inWidth, x, y);
                points[x] = new Vec2(inPoints[idx].x, inPoints[idx].z);
            }
            if (!CurveBuilder.build(points, segments, segPtr, c)) {
                return new Segment[0];
            }
            segPtr += inWidth;
        }
        return segments;
    }

    private Segment[] getColSegments(double c) {
        Segment[] segments = new Segment[inHeight * inWidth];
        for (int i = 0; i < inHeight * inWidth; i++) {
            segments[i] = new Segment();
        }
        Vec2[] points = new Vec2[inHeight];
        int segPtr = 0;
        for (int x = 0; x < inWidth; ++x) {
            for (int y = 0; y < inHeight; ++y) {
                int idx = index(inWidth, x, y);
                points[y] = new Vec2(inPoints[idx].y, inPoints[idx].z);
            }
            if (!CurveBuilder.build(points, segments, segPtr, c)) {
                return new Segment[0];
            }
            segPtr += inHeight;
        }
        return segments;
    }


    public static BicubicSurfaceSpline3 bSplineSurface(V3d[][] points, int resolution) {
        int h = points.length;
        int w = points[0].length;
        V3d[] result = new V3d[h * w];
        int index = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                result[index++] = points[y][x];
            }
        }
        return new BicubicSurfaceSpline3(result, w, h, resolution);
    }
}
