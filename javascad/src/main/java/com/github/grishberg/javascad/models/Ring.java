package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.basic.Radius;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords2d.Boundaries2d;
import com.github.grishberg.javascad.exceptions.NotImplementedException;
import com.github.grishberg.javascad.models2d.Abstract2dModel;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;

/**
 * A ring 3D object based on a 2D object.
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Ring extends Atomic3dModel {
	private final Abstract2dModel model;
	private final Radius radius;
	
	/**
	 * Creates the ring with the given parameters. The ready object will be the 2D model extruded into a ring
	 * with the given radius between the origin and the origin of the 2D model.
	 * @param radius the radius of the extrusion
	 * @param model the model to be rotated
	 * @deprecated use the constructor with the Radius parameter instead
	 */
	@Deprecated
	public Ring(double radius, Abstract2dModel model) {
		this(Radius.fromRadius(radius), model);
	}
	
	/**
	 * Creates the ring with the given parameters. The ready object will be the 2D model extruded into a ring
	 * with the given radius between the origin and the origin of the 2D model.
	 * @param radius the radius of the extrusion
	 * @param model the model to be rotated
	 */
	public Ring(Radius radius, Abstract2dModel model) {
		this.model = model;
		this.radius = radius;
	}

	@Override
	protected Model innerCloneModel() {
		return new Ring(radius, model);
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		Boundaries2d modelBoundaries = model.getBoundaries2d();
		Boundary z = modelBoundaries.getY();
		Boundary xy = Boundary.createSymmetricBoundary(modelBoundaries.getX().getMax()+radius.getRadius());
		return new Boundaries3d(xy, xy, z);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		throw new UnsupportedOperationException("Ring is not supported in native mode");
	}

}
