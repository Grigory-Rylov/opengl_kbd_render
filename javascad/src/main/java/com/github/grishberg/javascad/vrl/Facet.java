package com.github.grishberg.javascad.vrl;

import java.util.ArrayList;
import java.util.List;

import com.github.grishberg.javascad.coords.Triangle3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"triangle\":");
        sb.append(triangle.toJson());

        sb.append(",\"normal\":");
        sb.append(normal.toJson());
        sb.append("}");
        return sb.toString();
    }

    public String toJson() {
        return toString();
    }

    /**
     * Creates a Facet instance from JSON string.
     * Expected format: {"triangle":{"vertices":{...}},"normal":{"x":...}}
     * @param json the JSON string
     * @return the Facet instance
     */
    public static Facet fromJson(String json) {
        // Simple JSON parsing for the expected format
        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) {
            throw new IllegalArgumentException("Invalid JSON format for Facet: " + json);
        }
        
        // Extract triangle JSON
        String triangleJson = extractJsonValue(json, "\"triangle\":");
        Triangle3d triangle = Triangle3d.fromJson(triangleJson);
        
        // Extract normal JSON
        String normalJson = extractJsonValue(json, "\"normal\":");
        V3d normal = V3d.fromJson(normalJson);
        
        // Default color (Facet doesn't store color in JSON)
        Color color = Color.BLACK;
        
        return new Facet(triangle, normal, color);
    }
    
    private static String extractJsonValue(String json, String key) {
        int start = json.indexOf(key) + key.length();
        int braceCount = 0;
        int end = start;
        boolean inObject = false;
        
        for (int i = start; i < json.length(); i++) {
            char ch = json.charAt(i);
            if (ch == '{') {
                if (!inObject) inObject = true;
                braceCount++;
            } else if (ch == '}') {
                braceCount--;
                if (braceCount == 0 && inObject) {
                    end = i + 1;
                    break;
                }
            }
        }
        
        return json.substring(start, end);
    }
}
