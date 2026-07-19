/**
 * Polygon.java
 * <p>
 * Copyright 2014-2014 Michael Hoffer <info@michaelhoffer.de>. All rights
 * reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p>
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * <p>
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY Michael Hoffer <info@michaelhoffer.de> "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL Michael Hoffer <info@michaelhoffer.de> OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * <p>
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of Michael Hoffer
 * <info@michaelhoffer.de>.
 */
package com.github.grishberg.javascad.vrl;

import com.github.grishberg.javascad.coords.Triangle3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.coords2d.LineSegment;
import com.github.grishberg.javascad.tranform.ITransformation;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.utils.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a convex polygon. A polygon is represented by its points which should be on the same plane 
 * (not tested, but guaranteed by the used algorithms), its normal (calculated from the points), 
 * its color and its distance from the origin.
 *
 */
public final class Polygon {

    /**
     * Polygon vertices.
     */
    private final List<V3d> vertices;
    /**
     * Normal vector.
     */
    private final V3d normal;
    /**
     * Square of the distance to origin.
     */
    private final double dist;
    /**
     * The color of the polygon.
     */
    private final Color color;

    private Polygon(List<V3d> vertices, V3d normal, double dist, Color color) {
        this.vertices = vertices;
        this.normal = normal;
        this.dist = dist;
        this.color = color;

        for (V3d v : vertices) {
            VertexPosition position = calculateVertexPosition(v);
        //    AssertValue.isTrue(position == VertexPosition.COPLANAR,
        //        "Every vertex in a polygon must be coplanar, but was " + position + "!");
        }
    }

    public Polygon(List<V3d> vertices, V3d normal, Color color) {
        this.vertices = vertices;
        this.normal = normal;
        this.dist = normal.dot(vertices.get(0));
        this.color = color;

        for (V3d v : vertices) {
            VertexPosition position = calculateVertexPosition(v);
            AssertValue.isTrue(position == VertexPosition.COPLANAR,
                "Every vertex in a polygon must be coplanar, but was " + position + "!");
        }
    }

    public Color getColor() {
        return color;
    }

    public V3d getNormal() {
        return normal;
    }

    public double getDist() {
        return dist;
    }

    /**
     * Creates a new polygon that consists of the specified vertices.
     *
     * <b>Note:</b> the vertices used to initialize a polygon must be coplanar
     * and form a convex loop.
     *
     * @param vertices polygon vertices
     * @param color the color of the polygon
     * @return a new polygon that consists of the specified vertices
     */
    public static Polygon fromPolygons(List<V3d> vertices, Color color) {
        AssertValue.isTrue(vertices.size() >= 3, "The coordinate list should contain at least 3 points.");

        V3d a = vertices.get(0);
        V3d b = vertices.get(1);
        V3d c = vertices.get(2);
        V3d n = b.add(a.inverse()).cross(c.add(a.inverse())).unit();

        return new Polygon(vertices, n, n.dot(a), color);
    }

    public static Polygon fromPolygons(List<V3d> vertices, V3d normal, Color color) {
        AssertValue.isTrue(vertices.size() >= 3, "The coordinate list should contain at least 3 points.");

        V3d a = vertices.get(0);

        return new Polygon(vertices, normal, normal.dot(a), color);
    }

    public static Polygon fromPolygons(V3d a, V3d b, V3d c, Color color) {
        V3d n = b.add(a.inverse()).cross(c.add(a.inverse())).unit();
        ArrayList<V3d> vertices = new ArrayList<>();
        vertices.add(a);
        vertices.add(b);
        vertices.add(c);
        return new Polygon(vertices, n, n.dot(a), color);
    }

    public static boolean isValid(List<V3d> vertices, V3d normal, double dist) {
        for (V3d v : vertices) {
            double t = normal.dot(v) - dist;
            VertexPosition position = VertexPosition.fromSquareDistance(t);

            if (position != VertexPosition.COPLANAR) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValid(List<V3d> vertices) {
        V3d a = vertices.get(0);
        V3d b = vertices.get(1);
        V3d c = vertices.get(2);
        V3d normal = b.add(a.inverse()).cross(c.add(a.inverse())).unit();
        double dist = normal.dot(vertices.get(0));

        for (V3d v : vertices) {
            double t = normal.dot(v) - dist;
            VertexPosition position = VertexPosition.fromSquareDistance(t);

            if (position != VertexPosition.COPLANAR) {
                return false;
            }
        }
        return true;
    }

    /**
     * Flips this polygon.
     *
     * @return a new polygon with the vertices in reversed order
     */
    public Polygon flip() {
        List<V3d> newVertices = new ArrayList<>(vertices);

        Collections.reverse(newVertices);

        return new Polygon(newVertices, normal.inverse(), -dist, color);
    }

    /**
     * Converts this polygon to triangles.
     * @return a list of triangles
     */
    public List<Facet> toFacets() {
        List<Facet> facets = new ArrayList<>();
        if (this.vertices.size() >= 3) {
            V3d firstVertex = vertices.get(0);
            for (int i = 0; i < this.vertices.size() - 2; i++) {
                Triangle3d triangle = new Triangle3d(
                    firstVertex,
                    vertices.get(i + 1),
                    vertices.get(i + 2));
                facets.add(new Facet(triangle, normal, color));
            }
        }
        return facets;
    }

    public List<V3d> getVertices() {
        return vertices;
    }

    /**
     * Returns a transformed copy of this polygon.
     *
     * <b>Note:</b> if the applied transformation performs a mirror operation
     * the vertex order of this polygon is reversed.
     *
     * <b>Note:</b> this polygon is not modified
     *
     * @param transform the transformation to apply
     * @return a transformed copy of this polygon
     */
    public Polygon transformed(ITransformation transform) {
        List<V3d> newVertices = new ArrayList<>();

        for (V3d v : vertices) {
            newVertices.add(transform.transform(v));
        }

        Polygon result = fromPolygons(newVertices, color);

        return transform.isMirror() ? result.flip() : result;
    }

    private VertexPosition calculateVertexPosition(V3d v) {
        double t = this.normal.dot(v) - this.dist;
        return VertexPosition.fromSquareDistance(t);
    }

    /**
     * Splits a {@link Polygon} by this plane if needed. After that it puts the
     * polygons or the polygon fragments in the appropriate lists
     * ({@code front}, {@code back}). Coplanar polygons go into either
     * {@code coplanarFront}, {@code coplanarBack} depending on their
     * orientation with respect to this plane. Polygons in front or back of this
     * plane go into either {@code front} or {@code back}.
     *
     * @param polygon polygon to split
     * @param coplanarFront "coplanar front" polygons
     * @param coplanarBack "coplanar back" polygons
     * @param front front polygons
     * @param back back polgons
     */
    public void splitPolygon(
        Polygon polygon,
        List<Polygon> coplanarFront,
        List<Polygon> coplanarBack,
        List<Polygon> front,
        List<Polygon> back) {

        // Classify each point as well as the entire polygon into one of the four possible classes.
        VertexPosition polygonType = calculatePolygonPosition(polygon);

        // Put the polygon in the correct list, splitting it when necessary.
        switch (polygonType) {
            case COPLANAR:
                (this.normal.dot(polygon.normal) > 0 ? coplanarFront : coplanarBack).add(polygon);
                break;
            case FRONT:
                front.add(polygon);
                break;
            case BACK:
                back.add(polygon);
                break;
            case SPANNING:
                splitPolygon(polygon, front, back);
                break;
            default:
                break;
        }
    }

    // Classify the entire polygon into one of the four possible classes.
    private VertexPosition calculatePolygonPosition(Polygon polygon) {
        VertexPosition polygonType = VertexPosition.COPLANAR;
        for (V3d v : polygon.vertices) {
            polygonType = polygonType.add(calculateVertexPosition(v));
        }

        return polygonType;
    }

    private void splitPolygon(Polygon polygon, List<Polygon> front, List<Polygon> back) {
        List<V3d> f = new ArrayList<>();
        List<V3d> b = new ArrayList<>();
        for (LineSegment<V3d> ls : LineSegment.lineSegmentSeries(polygon.vertices)) {
            classifyAndSplitVertex(ls.getStart(), ls.getEnd(), f, b);
        }
        front.add(fromPolygons(f, polygon.color));
        back.add(fromPolygons(b, polygon.color));
    }

    private void classifyAndSplitVertex(V3d currentVertex, V3d nextVertex,
                                        List<V3d> f, List<V3d> b) {
        VertexPosition position = calculateVertexPosition(currentVertex);

        switch (position) {
            case FRONT:
                addVertexToList(f, currentVertex);
                break;
            case BACK:
                addVertexToList(b, currentVertex);
                break;
            default:
                addVertexToList(f, currentVertex);
                addVertexToList(b, currentVertex);
                break;
        }
        if (position.add(calculateVertexPosition(nextVertex)) == VertexPosition.SPANNING) {
            double t = (this.dist - this.normal.dot(currentVertex)) /
                this.normal.dot(nextVertex.add(currentVertex.inverse()));
            V3d v = currentVertex.lerp(nextVertex, t);
            addVertexToList(f, v);
            addVertexToList(b, v);
        }
    }

    private void addVertexToList(List<V3d> list, V3d newVertex) {
/*    	if (!list.isEmpty()) {
    		V3d lastVertex = list.get(list.size()-1);
    		
    		V3d prev = vertices.get(vertices.size()-1);
    		for (V3d c : vertices) {
    			V3d cross = EdgeCrossSolver.findIntersection(prev, c, lastVertex, newVertex);
    			if (cross!=null && !cross.equals(newVertex) && !cross.equals(lastVertex)) {
    				System.out.println(
    				"Added new vertex: "+cross+" between "+lastVertex+" and "+newVertex+" ("+prev+", "+c+")");
    				list.add(cross);
    				break;
    			}
    		}
    	}*/
        list.add(newVertex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polygon polygon = (Polygon) o;
        // Используем Double.doubleToLongBits для сравнения double значений,
        // чтобы корректно обрабатывать NaN и -0.0/+0.0
        return Double.doubleToLongBits(dist) == Double.doubleToLongBits(polygon.dist) &&
            Objects.equals(vertices, polygon.vertices) &&
            Objects.equals(normal, polygon.normal) &&
            Objects.equals(color, polygon.color);
    }

    @Override
    public int hashCode() {
        // Используем Objects.hash для комбинирования хэш-кодов полей
        return Objects.hash(vertices, normal, dist, color);
        // Альтернативно, можно явно использовать Double.hashCode() для dist:
        // return Objects.hash(vertices, normal, Double.hashCode(dist), color);
        // Или для максимальной совместимости с equals (используя doubleToLongBits):
        // return Objects.hash(vertices, normal, Double.doubleToLongBits(dist), color);
    }


    @Override
    public String toString() {
        return "Polygon " + toJson();
    }

    public String toJson() {
        if (vertices == null || vertices.isEmpty()) {
            return "{\"vertices\":[]}";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"vertices\":[");

        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            V3d v = vertices.get(i);
            // Формат точки: {"x":1.0,"y":2.0,"z":3.0}
            // Double.toString() обеспечивает точное представление без потери точности
            sb.append(v.toJson());
        }

        sb.append("]}");
        return sb.toString();

    }

    /**
     * Creates a Polygon instance from JSON string.
     * Expected format: {"vertices":[{"x":...,"y":...,"z":...},{"x":...,"y":...,"z":...},...]}
     * @param json the JSON string
     * @param color the color for the polygon
     * @return the Polygon instance
     */
    public static Polygon fromJson(String json, Color color) {
        // Simple JSON parsing for the expected format
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format for Polygon: " + json);
        }

        // Find vertices array
        int verticesStart = json.indexOf("\"vertices\":[") + 12;
        int verticesEnd = json.lastIndexOf("]");
        String verticesJson = json.substring(verticesStart, verticesEnd);

        List<V3d> vertices = new ArrayList<>();

        if (verticesJson.trim().isEmpty()) {
            return fromPolygons(vertices, color);
        }

        // Split by vertices (find each {...} block)
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < verticesJson.length(); i++) {
            char ch = verticesJson.charAt(i);
            if (ch == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            } else if (ch == '}') {
                braceCount--;
                if (braceCount == 0) {
                    String vertexJson = verticesJson.substring(start, i + 1);
                    vertices.add(V3d.fromJson(vertexJson));
                }
            }
        }

        return fromPolygons(vertices, color);
    }

    /**
     * Creates a Polygon instance from JSON string with default color.
     * @param json the JSON string
     * @return the Polygon instance
     */
    public static Polygon fromJson(String json) {
        return fromJson(json, Color.BLACK);
    }
}
