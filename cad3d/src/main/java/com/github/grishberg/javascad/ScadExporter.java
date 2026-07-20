package com.github.grishberg.javascad;

import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.basic.Angle;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/**
 * Generates OpenSCAD script from Model tree via reflection,
 * then renders via CGAL Nef polyhedron for guaranteed manifold output.
 */
public class ScadExporter {
    static final String OPENSCAD_PATH = System.getProperty("openscad.path", "/tmp/squashfs-root/usr/bin/openscad");

    public static void export(Model model, String outputStl, int fn) throws Exception {
        String scadFile = outputStl.replace(".stl", ".scad");
        String script = generateScript(model, fn);
        new File(scadFile).createNewFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(scadFile))) {
            pw.print(script);
        }

        ProcessBuilder pb = new ProcessBuilder(Arrays.asList(OPENSCAD_PATH, "-o", outputStl, scadFile));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        int exit = p.waitFor();

        if (exit != 0) {
            System.out.println("OpenSCAD output:\n" + output);
            throw new RuntimeException("OpenSCAD failed (exit " + exit + ")");
        }
        new File(scadFile).delete();

        // OpenSCAD 2021 exports ASCII STL; convert to binary for validation
        convertAsciiStlToBinary(outputStl);
    }

    /** Convert ASCII STL to binary in-place, preserving normals */
    static void convertAsciiStlToBinary(String path) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get(path)));
        if (!content.trim().startsWith("solid")) return; // already binary

        System.out.println("Converting ASCII STL to binary...");
        java.util.regex.Pattern normal = java.util.regex.Pattern.compile("facet normal\\s+([\\d.eE+-]+)\\s+([\\d.eE+-]+)\\s+([\\d.eE+-]+)");
        java.util.regex.Pattern vert = java.util.regex.Pattern.compile("vertex\\s+([\\d.eE+-]+)\\s+([\\d.eE+-]+)\\s+([\\d.eE+-]+)");

        List<float[]> normals = new ArrayList<>();
        List<float[]> vertices = new ArrayList<>();

        java.util.regex.Matcher nm = normal.matcher(content);
        while (nm.find()) {
            normals.add(new float[]{Float.parseFloat(nm.group(1)), Float.parseFloat(nm.group(2)), Float.parseFloat(nm.group(3))});
        }
        java.util.regex.Matcher vm = vert.matcher(content);
        while (vm.find()) {
            vertices.add(new float[]{Float.parseFloat(vm.group(1)), Float.parseFloat(vm.group(2)), Float.parseFloat(vm.group(3))});
        }
        int n = normals.size();
        if (vertices.size() != n * 3) throw new RuntimeException("Vertex count mismatch: " + vertices.size() + " vs " + (n * 3));

        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(path, "rw")) {
            raf.setLength(0);
            byte[] header = new byte[80];
            raf.write(header);
            java.nio.ByteBuffer buf = java.nio.ByteBuffer.allocate(4).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            buf.putInt(n);
            raf.write(buf.array());

            buf = java.nio.ByteBuffer.allocate(50 * n).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < n; i++) {
                float[] norm = normals.get(i);
                buf.putFloat(norm[0]).putFloat(norm[1]).putFloat(norm[2]);
                for (int v = 0; v < 3; v++) {
                    float[] p = vertices.get(i * 3 + v);
                    buf.putFloat(p[0]).putFloat(p[1]).putFloat(p[2]);
                }
                buf.putShort((short) 0);
            }
            raf.write(buf.array());
        }
        System.out.println("Converted " + n + " triangles to binary STL (with normals)");
    }

    public static String generateScript(Model model, int fn) {
        StringBuilder sb = new StringBuilder();
        sb.append("$fn=").append(fn).append(";\n\n");
        emit(sb, model, 0);
        return sb.toString();
    }

    static Object getField(Object obj, String name) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(obj);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name + " not found in " + obj.getClass().getName());
    }

    @SuppressWarnings("unchecked")
    static List<Model> getChildren(Model model) {
        try {
            return (List<Model>) getField(model, "childrenModels");
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    static void emit(StringBuilder sb, Model model, int depth) {
        int openBraces = 0;
        try {
            String pad = "  ".repeat(depth);
            String cls = model.getClass().getSimpleName();

            // Transforms
            V3d move = model.getMove();
            Object rotate = getField(model, "rotate");

            boolean hasMove = move != null && !move.isZero();
            boolean hasRotate = false;
            double rx = 0, ry = 0, rz = 0;
            if (rotate != null) {
                Object ax = getField(rotate, "x");
                Object ay = getField(rotate, "y");
                Object az = getField(rotate, "z");
                rx = getDegrees(ax);
                ry = getDegrees(ay);
                rz = getDegrees(az);
                hasRotate = Math.abs(rx) > 0.001 || Math.abs(ry) > 0.001 || Math.abs(rz) > 0.001;
            }

            if (hasMove) {
                sb.append(pad).append(String.format(Locale.US, "translate([%s, %s, %s]) {\n",
                        f(move.getX()), f(move.getY()), f(move.getZ())));
                openBraces++;
            }
            if (hasRotate) {
                sb.append(pad).append(String.format(Locale.US, "rotate([%s, %s, %s]) {\n",
                        f(rx), f(ry), f(rz)));
                openBraces++;
            }

            int innerDepth = depth + (hasMove ? 1 : 0) + (hasRotate ? 1 : 0);
            String ip = "  ".repeat(innerDepth);

            if (cls.equals("Cube")) {
                Object size = getField(model, "size");
                double x = (Double) getField(size, "x");
                double y = (Double) getField(size, "y");
                double z = (Double) getField(size, "z");
                sb.append(ip).append(String.format(Locale.US, "cube([%s, %s, %s], center=true);\n", f(x), f(y), f(z)));

            } else if (cls.equals("Cylinder")) {
                double h = (Double) getField(model, "length");
                double r1 = getRadius(getField(model, "bottomRadius"));
                double r2 = getRadius(getField(model, "topRadius"));
                sb.append(ip).append(String.format(Locale.US, "cylinder(h=%s, r1=%s, r2=%s, center=true);\n",
                        f(h), f(r1), f(r2)));

            } else if (cls.equals("Sphere")) {
                double r = getRadius(getField(model, "r"));
                sb.append(ip).append(String.format(Locale.US, "sphere(r=%s);\n", f(r)));

            } else if (cls.equals("Union")) {
                List<Model> models = (List<Model>) getField(model, "models");
                sb.append(ip).append("union() {\n");
                for (Model child : models) emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Difference")) {
                Model m1 = (Model) getField(model, "model1");
                List<Model> m2 = (List<Model>) getField(model, "model2");
                sb.append(ip).append("difference() {\n");
                emit(sb, m1, innerDepth + 1);
                for (Model child : m2) emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Intersection")) {
                List<Model> models = (List<Model>) getField(model, "models");
                sb.append(ip).append("intersection() {\n");
                for (Model child : models) emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Translate")) {
                V3d v = (V3d) getField(model, "move");
                Model child = (Model) getField(model, "model");
                sb.append(ip).append(String.format(Locale.US, "translate([%s, %s, %s]) {\n",
                        f(v.getX()), f(v.getY()), f(v.getZ())));
                emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Rotate")) {
                Object a = getField(model, "rotate");
                Model child = (Model) getField(model, "model");
                double ax = getDegrees(getField(a, "x"));
                double ay = getDegrees(getField(a, "y"));
                double az = getDegrees(getField(a, "z"));
                sb.append(ip).append(String.format(Locale.US, "rotate([%s, %s, %s]) {\n", f(ax), f(ay), f(az)));
                emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Scale")) {
                double s = (Double) getField(model, "scale");
                Model child = (Model) getField(model, "model");
                sb.append(ip).append(String.format(Locale.US, "scale([%s, %s, %s]) {\n", f(s), f(s), f(s)));
                emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Mirror")) {
                V3d n = (V3d) getField(model, "normal");
                Model child = (Model) getField(model, "model");
                sb.append(ip).append(String.format(Locale.US, "mirror([%s, %s, %s]) {\n",
                        f(n.getX()), f(n.getY()), f(n.getZ())));
                emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Hull")) {
                List<Model> models = (List<Model>) getField(model, "models");
                sb.append(ip).append("hull() {\n");
                for (Model child : models) emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else if (cls.equals("Colorize") || cls.equals("Slicer")) {
                Model child = (Model) getField(model, "model");
                emit(sb, child, innerDepth);

            } else if (cls.equals("LinearExtrude")) {
                double h = (Double) getField(model, "height");
                Object twist = getField(model, "twist");
                double tVal = 0;
                if (twist != null) tVal = getDegrees(twist);
                sb.append(ip).append(String.format(Locale.US, "linear_extrude(height=%s", f(h)));
                if (tVal != 0) sb.append(String.format(Locale.US, ", twist=%s", f(tVal)));
                sb.append(", convexity=10) {\n");
                List<Model> children = getChildren(model);
                for (Model child : children) emit(sb, child, innerDepth + 1);
                sb.append(ip).append("}\n");

            } else {
                List<Model> children = getChildren(model);
                if (!children.isEmpty()) {
                    sb.append(ip).append("union() {\n");
                    for (Model child : children) emit(sb, child, innerDepth + 1);
                    sb.append(ip).append("}\n");
                } else {
                    sb.append(ip).append("// ").append(cls).append("\n");
                }
            }

            // Close braces in finally to maintain balance
        } catch (Exception e) {
            sb.append("  ".repeat(depth)).append("// ERROR: ").append(e.getMessage()).append("\n");
        } finally {
            // Close any opened braces (rotate first, then move, since they were opened in that order)
            for (int i = 0; i < openBraces; i++) {
                sb.append("  ".repeat(depth + openBraces - 1 - i)).append("}\n");
            }
        }
    }

    static double getDegrees(Object obj) throws Exception {
        if (obj == null) return 0;
        // Angles3d stores x/y/z as double in degrees already (Basic3dFunc)
        // If it's an Angle, convert radian to degrees
        if (obj.getClass().getSimpleName().equals("Angle")) {
            Method m = obj.getClass().getMethod("asDegree");
            return (Double) m.invoke(obj);
        }
        // Otherwise it's already a Double (from Basic3dFunc.x)
        return (Double) obj;
    }

    static double getRadius(Object obj) throws Exception {
        if (obj == null) return 0;
        // Radius has getRadius() method
        Method m = obj.getClass().getMethod("getRadius");
        return (Double) m.invoke(obj);
    }

    static String f(double v) {
        if (v == 0) return "0";
        if (v == (long) v && Math.abs(v) < 1e12) return Long.toString((long) v);
        return String.format(Locale.US, "%.6g", v);
    }
}
