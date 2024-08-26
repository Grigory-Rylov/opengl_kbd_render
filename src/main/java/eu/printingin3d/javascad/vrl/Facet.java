package eu.printingin3d.javascad.vrl;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.coords.Triangle3d;
import eu.printingin3d.javascad.utils.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Immutable representation of one triangle in the mesh with a normal pointing outward from
 * the object.</p>
 * <p>It is used internally by the STL output generation, you don't really have to use it
 * directly.</p>
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Facet {

    private final Triangle3d triangle;
    private final V3d normal;
    private final Color color;

    /**
     * Creates a facet based on a triangle and a normal vector.
     *
     * @param triangle the triangle
     * @param normal the normal vector
     * @param color the color of the facet
     */
    public Facet(Triangle3d triangle, V3d normal, Color color) {
        this.triangle = triangle;
        this.normal = normal;
        this.color = color;
    }

    /**
     * Returns all the vertices this facet holds. The result will always contain exactly three
     * vertices.
     *
     * @return all the vertices this facet holds
     */
    public List<Vertex> getVertexes() {
        List<Vertex> vertexes = new ArrayList<>(3);
        for (V3d c : triangle.getPoints()) {
            vertexes.add(new Vertex(c, color));
        }
        return vertexes;
    }

    /**
     * Returns the triangle within this facet. Added for testing purposes.
     *
     * @return the triangle within this facet.
     */
    public Triangle3d getTriangle() {
        return triangle;
    }

    /**
     * Returns the normal of this facet.
     *
     * @return the normal of this facet
     */
    public V3d getNormal() {
        return normal;
    }

    public Color getColor() {
        return color;
    }
}
