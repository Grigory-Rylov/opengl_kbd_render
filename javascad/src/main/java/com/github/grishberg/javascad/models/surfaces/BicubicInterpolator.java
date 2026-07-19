package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.exceptions.IllegalValueException;

public class BicubicInterpolator {
    private final BicubicSurface xSurface;
    private final BicubicSurface ySurface;
    private final BicubicSurface zSurface;

    public BicubicInterpolator(V3d[][] controlPoints) {
        validateInput(controlPoints);
        this.xSurface = new BicubicSurface(controlPoints, CoordType.X);
        this.ySurface = new BicubicSurface(controlPoints, CoordType.Y);
        this.zSurface = new BicubicSurface(controlPoints, CoordType.Z);
    }

    private void validateInput(V3d[][] points) {
        if (points == null || points.length < 2) {
            throw new IllegalValueException("At least 2 rows required");
        }
        int cols = points[0].length;
        for (V3d[] row : points) {
            if (row == null || row.length != cols || row.length < 2) {
                throw new IllegalValueException("Invalid control points grid");
            }
        }
    }

    public V3d interpolate(double u, double v) {
        return new V3d(
            xSurface.getValue(u, v),
            ySurface.getValue(u, v),
            zSurface.getValue(u, v)
        );
    }

    public V3d[][] generateSurface(int resolution) {
        V3d[][] surface = new V3d[resolution][resolution];
        for (int i = 0; i < resolution; i++) {
            double v = safeParam((double)i / (resolution-1));
            for (int j = 0; j < resolution; j++) {
                double u = safeParam((double)j / (resolution-1));
                surface[i][j] = interpolate(u, v);
            }
        }
        return surface;
    }

    private double safeParam(double t) {
        return Math.min(1.0 - 1e-10, Math.max(0.0, t));
    }

    private enum CoordType { X, Y, Z }

    private static class BicubicSurface {
        private final double[][] grid;
        private final int rows;
        private final int cols;
        private final double[][][] coefficients;

        public BicubicSurface(V3d[][] controlPoints, CoordType type) {
            this.rows = controlPoints.length;
            this.cols = controlPoints[0].length;
            this.grid = extractGrid(controlPoints, type);
            this.coefficients = calculateCoefficients();
        }

        private double[][] extractGrid(V3d[][] controlPoints, CoordType type) {
            double[][] grid = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    switch (type) {
                        case X: grid[i][j] = controlPoints[i][j].getX(); break;
                        case Y: grid[i][j] = controlPoints[i][j].getY(); break;
                        case Z: grid[i][j] = controlPoints[i][j].getZ(); break;
                    }
                }
            }
            return grid;
        }

        private double[][][] calculateCoefficients() {
            double[][][] derivatives = calculateDerivatives();
            double[][][] coeffs = new double[rows-1][cols-1][16];

            for (int i = 0; i < rows-1; i++) {
                for (int j = 0; j < cols-1; j++) {
                    coeffs[i][j] = calculatePatchCoefficients(i, j, derivatives);
                }
            }
            return coeffs;
        }

        private double[] calculatePatchCoefficients(int i, int j, double[][][] derivatives) {
            double[] alpha = new double[16];

            // Function values
            alpha[0] = grid[i][j];
            alpha[1] = grid[i][j+1];
            alpha[2] = grid[i+1][j];
            alpha[3] = grid[i+1][j+1];

            // df/dx
            alpha[4] = derivatives[i][j][0];
            alpha[5] = derivatives[i][j+1][0];
            alpha[6] = derivatives[i+1][j][0];
            alpha[7] = derivatives[i+1][j+1][0];

            // df/dy
            alpha[8] = derivatives[i][j][1];
            alpha[9] = derivatives[i][j+1][1];
            alpha[10] = derivatives[i+1][j][1];
            alpha[11] = derivatives[i+1][j+1][1];

            // d2f/dxdy
            alpha[12] = derivatives[i][j][2];
            alpha[13] = derivatives[i][j+1][2];
            alpha[14] = derivatives[i+1][j][2];
            alpha[15] = derivatives[i+1][j+1][2];

            return solveCoefficients(alpha);
        }

        private double[][][] calculateDerivatives() {
            double[][][] derivatives = new double[rows][cols][3];

            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    // df/dx
                    if (j == 0) {
                        derivatives[i][j][0] = (grid[i][1] - grid[i][0]);
                    } else if (j == cols-1) {
                        derivatives[i][j][0] = (grid[i][cols-1] - grid[i][cols-2]);
                    } else {
                        derivatives[i][j][0] = (grid[i][j+1] - grid[i][j-1]) / 2.0;
                    }

                    // df/dy
                    if (i == 0) {
                        derivatives[i][j][1] = (grid[1][j] - grid[0][j]);
                    } else if (i == rows-1) {
                        derivatives[i][j][1] = (grid[rows-1][j] - grid[rows-2][j]);
                    } else {
                        derivatives[i][j][1] = (grid[i+1][j] - grid[i-1][j]) / 2.0;
                    }

                    // d2f/dxdy
                    derivatives[i][j][2] = (derivatives[i][j][0] + derivatives[i][j][1]) / 2.0;
                }
            }
            return derivatives;
        }

        private double[] solveCoefficients(double[] alpha) {
            double[] a = new double[16];

            a[0] = alpha[0];
            a[1] = alpha[4];
            a[2] = -3*alpha[0] + 3*alpha[1] - 2*alpha[4] - alpha[5];
            a[3] = 2*alpha[0] - 2*alpha[1] + alpha[4] + alpha[5];

            a[4] = alpha[8];
            a[5] = alpha[12];
            a[6] = -3*alpha[8] + 3*alpha[9] - 2*alpha[12] - alpha[13];
            a[7] = 2*alpha[8] - 2*alpha[9] + alpha[12] + alpha[13];

            a[8] = -3*alpha[0] + 3*alpha[2] - 2*alpha[8] - alpha[10];
            a[9] = -3*alpha[4] + 3*alpha[6] - 2*alpha[12] - alpha[14];
            a[10] = 9*alpha[0] - 9*alpha[1] - 9*alpha[2] + 9*alpha[3]
                + 6*alpha[4] + 3*alpha[5] - 6*alpha[6] - 3*alpha[7]
                + 6*alpha[8] - 6*alpha[9] + 3*alpha[10] + 3*alpha[11]
                + 4*alpha[12] + 2*alpha[13] + 2*alpha[14] + alpha[15];
            a[11] = -6*alpha[0] + 6*alpha[1] + 6*alpha[2] - 6*alpha[3]
                - 3*alpha[4] - 3*alpha[5] + 3*alpha[6] + 3*alpha[7]
                - 4*alpha[8] + 4*alpha[9] - 2*alpha[10] - 2*alpha[11]
                - 2*alpha[12] - 2*alpha[13] - alpha[14] - alpha[15];

            a[12] = 2*alpha[0] - 2*alpha[2] + alpha[8] + alpha[10];
            a[13] = 2*alpha[4] - 2*alpha[6] + alpha[12] + alpha[14];
            a[14] = -6*alpha[0] + 6*alpha[1] + 6*alpha[2] - 6*alpha[3]
                - 4*alpha[4] - 2*alpha[5] + 4*alpha[6] + 2*alpha[7]
                - 3*alpha[8] + 3*alpha[9] - 3*alpha[10] - 3*alpha[11]
                - 2*alpha[12] - alpha[13] - 2*alpha[14] - alpha[15];
            a[15] = 4*alpha[0] - 4*alpha[1] - 4*alpha[2] + 4*alpha[3]
                + 2*alpha[4] + 2*alpha[5] - 2*alpha[6] - 2*alpha[7]
                + 2*alpha[8] - 2*alpha[9] + 2*alpha[10] + 2*alpha[11]
                + alpha[12] + alpha[13] + alpha[14] + alpha[15];

            return a;
        }

        public double getValue(double u, double v) {
            u = safeParam(u);
            v = safeParam(v);

            int i = (int)(v * (rows-1));
            int j = (int)(u * (cols-1));

            i = Math.min(i, rows-2);
            j = Math.min(j, cols-2);

            double x = u * (cols-1) - j;
            double y = v * (rows-1) - i;

            return evaluatePolynomial(coefficients[i][j], x, y);
        }

        private double evaluatePolynomial(double[] a, double x, double y) {
            double value = 0.0;
            for (int m = 0; m < 4; m++) {
                double yPow = Math.pow(y, m);
                for (int n = 0; n < 4; n++) {
                    value += a[m*4 + n] * yPow * Math.pow(x, n);
                }
            }
            return value;
        }

        private double safeParam(double t) {
            return Math.min(1.0 - 1e-10, Math.max(0.0, t));
        }
    }
}
