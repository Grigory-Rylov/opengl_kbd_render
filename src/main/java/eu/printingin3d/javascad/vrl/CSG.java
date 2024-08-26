/**
 * CSG.java
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
package eu.printingin3d.javascad.vrl;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.coords.Triangle3d;
import eu.printingin3d.javascad.tranform.ITransformation;

import eu.printingin3d.javascad.utils.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Constructive Solid Geometry (CSG).
 * <p>
 * This implementation is a Java port of
 * <a
 * href="https://github.com/evanw/csg.js/">https://github.com/evanw/csg.js/</a>
 * with some additional features like polygon extrude, transformations etc.
 * Thanks to the author for creating the CSG.js library.<br><br>
 *
 * <b>Implementation Details</b>
 * <p>
 * All CSG operations are implemented in terms of two functions,
 * {@link Node#clipTo(Node)} and {@link Node#invert()},
 * which remove parts of a BSP tree inside another BSP tree and swap solid and
 * empty space, respectively. To find the union of {@code a} and {@code b}, we
 * want to remove everything in {@code a} inside {@code b} and everything in
 * {@code b} inside {@code a}, then combine polygons from {@code a} and
 * {@code b} into one solid:
 *
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 * <p>
 * The only tricky part is handling overlapping coplanar polygons in both trees.
 * The code above keeps both copies, but we need to keep them in one tree and
 * remove them in the other tree. To remove them from {@code b} we can clip the
 * inverse of {@code b} against {@code a}. The code for union now looks like
 * this:
 *
 * <blockquote><pre>
 *     a.clipTo(b);
 *     b.clipTo(a);
 *     b.invert();
 *     b.clipTo(a);
 *     b.invert();
 *     a.build(b.allPolygons());
 * </pre></blockquote>
 * <p>
 * Subtraction and intersection naturally follow from set operations. If union
 * is {@code A | B}, differenceion is {@code A - B = ~(~A | B)} and intersection
 * is {@code A & B =
 * ~(~A | ~B)} where {@code ~} is the complement operator.
 */
public class CSG {

    private final List<Polygon> polygons;

    /**
     * Creates a new CSG file based on the given polygons.
     *
     * @param polygons the polygons to be used
     */
    public CSG(List<Polygon> polygons) {
        this.polygons = Collections.unmodifiableList(polygons);
    }

    /**
     * Constructs a CSG from the specified {@link Polygon} instances.
     *
     * @param polygons polygons
     * @return a CSG instance
     */
    public static CSG fromPolygons(Polygon... polygons) {
        return new CSG(Arrays.asList(polygons));
    }

    /**
     * @return the polygons of this CSG
     */
    public List<Polygon> getPolygons() {
        return polygons;
    }

    /**
     * Get all the points this CSG holds.
     *
     * @return the points this CSG holds
     */
    public List<V3d> getPoints() {
        List<V3d> result = new ArrayList<>();

        for (Polygon p : getPolygons()) {
            result.addAll(p.getVertices());
        }

        return result;
    }

    /**
     * Return a new CSG solid representing the union of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are modified.
     *
     * <blockquote><pre>
     *    A.union(B)
     *
     *    +-------+            +-------+
     *    |       |            |       |
     *    |   A   |            |       |
     *    |    +--+----+   =   |       +----+
     *    +----+--+    |       +----+       |
     *         |   B   |            |       |
     *         |       |            |       |
     *         +-------+            +-------+
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return union of this csg and the specified csg
     */
    public CSG union(CSG csg) {
        Node a = Node.fromPoligons(this.polygons);
        Node b = Node.fromPoligons(csg.polygons);
        a = a.clipTo(b);
        b = b.clipTo(a);
        b = b.invert();
        b = b.clipTo(a);
        b = b.invert();
        a = a.build(b.allPolygons());
        return new CSG(a.allPolygons());
    }

    /**
     * Return a new CSG solid representing the difference of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are modified.
     *
     * <blockquote><pre>
     * A.difference(B)
     *
     * +-------+            +-------+
     * |       |            |       |
     * |   A   |            |       |
     * |    +--+----+   =   |    +--+
     * +----+--+    |       +----+
     *      |   B   |
     *      |       |
     *      +-------+
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return difference of this csg and the specified csg
     */
    public CSG difference(CSG csg) {
        Node a = Node.fromPoligons(this.polygons);
        Node b = Node.fromPoligons(csg.polygons);
        a = a.invert();
        a = a.clipTo(b);
        b = b.clipTo(a);
        b = b.invert();
        b = b.clipTo(a);
        b = b.invert();
        a = a.build(b.allPolygons());
        a = a.invert();
        return new CSG(a.allPolygons());
    }

    /**
     * Return a new CSG solid representing the intersection of this csg and the
     * specified csg.
     *
     * <b>Note:</b> Neither this csg nor the specified csg are modified.
     *
     * <blockquote><pre>
     *     A.intersect(B)
     *
     *     +-------+
     *     |       |
     *     |   A   |
     *     |    +--+----+   =   +--+
     *     +----+--+    |       +--+
     *          |   B   |
     *          |       |
     *          +-------+
     * }
     * </pre></blockquote>
     *
     * @param csg other csg
     * @return intersection of this csg and the specified csg
     */
    public CSG intersect(CSG csg) {
        Node a = Node.fromPoligons(this.polygons);
        Node b = Node.fromPoligons(csg.polygons);
        a = a.invert();
        b = b.clipTo(a);
        b = b.invert();
        a = a.clipTo(b);
        b = b.clipTo(a);
        a = a.build(b.allPolygons());
        a = a.invert();
        return new CSG(a.allPolygons());
    }

    /**
     * Returns with all the facet this CSG object holds.
     *
     * @return all the facet this CSG object holds
     */
    public List<Facet> toFacets() {
        List<Facet> facets = new ArrayList<>();
        for (Polygon p : polygons) {
            facets.addAll(p.toFacets());
        }
        return facets;
    }

    /**
     * Returns a transformed copy of this CSG.
     *
     * @param transform the transform to apply
     * @return a transformed copy of this CSG
     */
    public CSG transformed(ITransformation transform) {
        List<Polygon> newpolygons = new ArrayList<>();
        for (Polygon p : this.polygons) {
            newpolygons.add(p.transformed(transform));
        }

        return new CSG(newpolygons);
    }


    // Метод для получения вершин в виде массива float
    public VertexHolder getVerticesAsFloatArray() {

        int verticesCount = 0;
        List<Facet> facets = toFacets();
        verticesCount = facets.size() * 3;


        float[] verticesArray = new float[verticesCount * 3];
        float[] normalsArray = new float[verticesCount * 3];

        int i = 0;
        for (Facet facet : facets) {

            final V3d normal = facet.getNormal();
            Triangle3d triangle3d = facet.getTriangle();
            for (V3d vertex : triangle3d.getPoints()) {
                verticesArray[i] = (float) vertex.getX();
                verticesArray[i + 1] = (float) vertex.getY();
                verticesArray[i + 2] = (float) vertex.getZ();

                normalsArray[i] = (float) normal.getX();
                normalsArray[i + 1] = (float) normal.getY();
                normalsArray[i + 2] = (float) normal.getZ();
                i += 3;
            }
        }

        return new VertexHolder(verticesArray, normalsArray, verticesCount);
    }

    // Метод для получения вершин в виде массива float
    public VertexHolder getVerticesAndColorsAsFloatArray() {

        int verticesCount = 0;
        List<Facet> facets = toFacets();
        verticesCount = facets.size() * 3;


        float[] verticesArray = new float[verticesCount * 7];
        float[] normalsArray = new float[verticesCount * 3];

        int normalArrayIndex = 0;
        int vertexArrayIndex = 0;
        for (Facet facet : facets) {
            final Color facetColor = facet.getColor();
            final V3d normal = facet.getNormal();
            Triangle3d triangle3d = facet.getTriangle();
            for (V3d vertex : triangle3d.getPoints()) {
                // X, Y, Z,
                // R, G, B, A
                verticesArray[vertexArrayIndex++] = (float) vertex.getX();
                verticesArray[vertexArrayIndex++] = (float) vertex.getY();
                verticesArray[vertexArrayIndex++] = (float) vertex.getZ();
                verticesArray[vertexArrayIndex++] = facetColor.getRed() / 255f;
                verticesArray[vertexArrayIndex++] = facetColor.getGreen() / 255f;
                verticesArray[vertexArrayIndex++] = facetColor.getBlue() / 255f;
                verticesArray[vertexArrayIndex++] = facetColor.getAlpha() / 255f;

                normalsArray[normalArrayIndex++] = (float) normal.getX();
                normalsArray[normalArrayIndex++] = (float) normal.getY();
                normalsArray[normalArrayIndex++] = (float) normal.getZ();
            }
        }

        return new VertexHolder(verticesArray, normalsArray, verticesCount);
    }

	public float[] getVerticesWithColors() {

        int verticesCount = 0;
        List<Facet> facets = toFacets();
        verticesCount = facets.size() * 3;


        float[] verticesArray = new float[verticesCount * 6];
        
        int i = 0;
        for (Facet facet : facets) {
            List<Vertex> vert = facet.getVertexes();
            for (Vertex vertex : vert) {
                int n = vertex.copyToArray(verticesArray,i);
				i += n;
            }
        }

        return verticesArray;
    }
}
