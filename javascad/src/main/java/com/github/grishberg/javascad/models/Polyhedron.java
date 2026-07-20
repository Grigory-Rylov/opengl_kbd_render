package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Triangle3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>
 * Represents a set of triangles. It is good to know that some operations - such
 * as difference or intersection - are not always works perfectly with this
 * object.
 * </p>
 * 
 * @author Rob van der Veer
 */
public class Polyhedron extends Atomic3dModel {
	protected final List<Triangle3d> triangles;

	/**
	 * Constructs the object with the given triangles.
	 * 
	 * @param triangles
	 *            the triangles used to create this object
	 * @throws IllegalValueException
	 *             thrown if the given list is empty
	 */
	public Polyhedron(List<Triangle3d> triangles) throws IllegalValueException {
		AssertValue.isNotEmpty(triangles,
				"The triangle list should not be empty!");

		this.triangles = new ArrayList<>(triangles);
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		double minX = +Double.MAX_VALUE;
		double minY = +Double.MAX_VALUE;
		double minZ = +Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;

		for (final V3d p : getPoints()) {
			minX = Math.min(p.getX(), minX);
			minY = Math.min(p.getY(), minY);
			minZ = Math.min(p.getZ(), minZ);
			maxX = Math.max(p.getX(), maxX);
			maxY = Math.max(p.getY(), maxY);
			maxZ = Math.max(p.getZ(), maxZ);
		}
		V3d minCorner = new V3d(minX, minY, minZ);
		V3d maxCorner = new V3d(maxX, maxY, maxZ);
		return new Boundaries3d(minCorner, maxCorner);
	}

	private List<V3d> getPoints() {
		Set<V3d> result = new HashSet<>();
		for (Triangle3d triangle : triangles) {
			result.addAll(triangle.getPoints());
		}
		return new ArrayList<>(result);
	}

	@Override
	protected Model innerCloneModel() {
		return new Polyhedron(triangles);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		List<Polygon> polygons = new ArrayList<>();
		for (Triangle3d c : triangles) {
			polygons.add(Polygon.fromPolygons(c.getPoints(), context.getColor()));
		}
		return Manifold3dEngine.INSTANCE.polygonsToManifold(Manifold3dEngine.INSTANCE.bindings(), polygons);
	}
}
