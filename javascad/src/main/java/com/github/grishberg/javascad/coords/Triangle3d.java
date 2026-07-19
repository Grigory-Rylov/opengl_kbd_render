package com.github.grishberg.javascad.coords;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.utils.AssertValue;

/**
 * An implementation of a 3D triangle. It is used by the OpenSCAD rendering. 
 * This class is also used by the Polyhedron class as input.  
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Triangle3d {
	private final V3d point1;
	private final V3d point2;
	private final V3d point3;

	/**
	 * Reads the given byte buffer and creates a triangle based on its content. It is the inverse 
	 * operation of {@link Triangle3d#toByteArray(ByteBuffer)}.
	 * @param bb the byte buffer the content will be read from
	 * @return a triangle created based on the content of the buffer
	 */
	public static Triangle3d fromByteArray(ByteBuffer bb) {
		return new Triangle3d(V3d.fromByteBuffer(bb), V3d.fromByteBuffer(bb), V3d.fromByteBuffer(bb));
	}
	
	/**
	 * Creates a list of triangles which covers the polygon defined by the given list of coordinates.
	 * The given list should contain at least three coordinates.
	 * @param coords the list of coordinates used to generate the triangles
	 * @return the triangles
	 * @throws IllegalValueException if the given coords list is null or the list contains less than three coordinates
	 */
	public static List<Triangle3d> getTriangles(List<V3d> coords) {
		AssertValue.isNotNull(coords, "coords should not be null");
		AssertValue.isTrue(coords.size() >= 3, 
				"There should be at least 3 verticies given, but was only "+coords.size());
		
    	List<Triangle3d> triangles = new ArrayList<>();
    	V3d firstVertex = coords.get(0);
        for (int i = 0; i < coords.size() - 2; i++) {
        	Triangle3d triangle = new Triangle3d(
        			firstVertex, 
        			coords.get(i + 1), 
        			coords.get(i + 2));
			triangles.add(triangle);
        }
        return triangles;
	}
	
	/**
	 * Creates a list of triangles which covers the polygon defined by the given list of coordinates.
	 * The given list should contain at least three coordinates.
	 * @param coords the list of coordinates used to generate the triangles
	 * @return the triangles
	 * @throws IllegalValueException if the given coords list is null or the list contains less than three coordinates
	 */
	public static List<Triangle3d> getTriangles(V3d... coords) {
		AssertValue.isNotNull(coords, "coords should not be null");
		
		return getTriangles(Arrays.asList(coords));
	}
	
	/**
	 * Created the triangle by defining the three corner.
	 * @param point1 the first corner
	 * @param point2 the second corner
	 * @param point3 the third corner
	 * @throws IllegalValueException if any of the parameters is null
	 */
	public Triangle3d(V3d point1, V3d point2, V3d point3) throws IllegalValueException {
		AssertValue.isNotNull(point1, "point1 should not be null");
		AssertValue.isNotNull(point2, "point2 should not be null");
		AssertValue.isNotNull(point3, "point3 should not be null");

		this.point1 = point1;
		this.point2 = point2;
		this.point3 = point3;
	}

	public Triangle3d(V3d point1, V3d point2, V3d point3, V3d normal) throws IllegalValueException {
		AssertValue.isNotNull(point1, "point1 should not be null");
		AssertValue.isNotNull(point2, "point2 should not be null");
		AssertValue.isNotNull(point3, "point3 should not be null");
		AssertValue.isNotNull(normal, "normal should not be null");

		// Вычисляем нормаль треугольника по текущему порядку точек
		V3d edge1 = point2.subtract(point1);
		V3d edge2 = point3.subtract(point1);
		V3d computedNormal = edge1.cross(edge2).unit();

		// Сравниваем вычисленную нормаль с переданной
		if (computedNormal.dot(normal) < 0) {
			// Если направление не совпадает, меняем порядок двух точек
			this.point1 = point1;
			this.point2 = point3;
			this.point3 = point2;
		} else {
			// Иначе оставляем как есть
			this.point1 = point1;
			this.point2 = point2;
			this.point3 = point3;
		}
	}
	
	/**
	 * Returns with the corners of the triangle as a list. It always contains three points 
	 * in an order as it was given in the constructor.
	 * @return the list of the corners
	 */
	public List<V3d> getPoints() {
		return Arrays.asList(point1, point2, point3);
	}
	
	/**
	 * Renders the triangle as is used by OpenSCAD: three index of a list of points - 
	 * that's why this method requires a coordinate list as a parameter.
	 * @param coords the list of coordinates which is used to determine the indexes
	 * @return the OpenSCAD version of the triangle
	 * @throws IllegalValueException if any of the three corners of the triangle cannot be 
	 * found in the given list
	 */
	public String toTriangleString(List<V3d> coords) throws IllegalValueException {
		int p1 = coords.indexOf(point1);
		int p2 = coords.indexOf(point2);
		int p3 = coords.indexOf(point3);
		
		AssertValue.isNotNegative(p1, "point1 ("+point1+") cannot be found in the coordinate list!");
		AssertValue.isNotNegative(p2, "point2 ("+point2+") cannot be found in the coordinate list!");
		AssertValue.isNotNegative(p3, "point3 ("+point3+") cannot be found in the coordinate list!");
		
		return "[" + p1 + ',' + p2 + ',' + p3 + ']';
	}

	/**
	 * Writes the byte array representation of this triangle to the given buffer.
	 * @param byteBuffer the byte buffer the representation of this vertex will be written to
	 */
	public void toByteArray(ByteBuffer byteBuffer) {
		point1.toByteArray(byteBuffer);
		point2.toByteArray(byteBuffer);
		point3.toByteArray(byteBuffer);

	}

	@Override
	public String toString() {
		return "Triangle3d " + toJson();
	}

	public String toJson() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"vertices\":{");

		sb.append("\"a\":");
		sb.append(point1.toJson());
		sb.append(",\"b\":");
		sb.append(point2.toJson());
		sb.append(",\"c\":");
		sb.append(point3.toJson());
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Creates a Triangle3d instance from JSON string.
	 * Expected format: {"vertices":{"a":{"x":...},"b":{"x":...},"c":{"x":...}}}
	 * @param json the JSON string
	 * @return the Triangle3d instance
	 */
	public static Triangle3d fromJson(String json) {
		// Simple JSON parsing for the expected format
		json = json.trim();
		if (!json.startsWith("{") || !json.endsWith("}")) {
			throw new IllegalArgumentException("Invalid JSON format for Triangle3d: " + json);
		}
		
		// Find vertices object
		int verticesStart = json.indexOf("\"vertices\":{") + 12;
		int verticesEnd = json.lastIndexOf("}");
		String verticesJson = json.substring(verticesStart, verticesEnd);
		
		// Extract individual vertex JSON strings
		String aJson = extractVertexJson(verticesJson, "\"a\":");
		String bJson = extractVertexJson(verticesJson, "\"b\":");
		String cJson = extractVertexJson(verticesJson, "\"c\":");
		
		V3d a = V3d.fromJson(aJson);
		V3d b = V3d.fromJson(bJson);
		V3d c = V3d.fromJson(cJson);
		
		return new Triangle3d(a, b, c);
	}
	
	private static String extractVertexJson(String verticesJson, String key) {
		int start = verticesJson.indexOf(key) + key.length();
		int braceCount = 0;
		int end = start;
		
		for (int i = start; i < verticesJson.length(); i++) {
			char ch = verticesJson.charAt(i);
			if (ch == '{') braceCount++;
			else if (ch == '}') {
				braceCount--;
				if (braceCount == 0) {
					end = i + 1;
					break;
				}
			}
		}
		
		return verticesJson.substring(start, end);
	}
}
