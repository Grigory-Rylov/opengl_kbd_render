package com.github.grishberg.javascad.models;

import static com.github.grishberg.javascad.utils.AssertValue.isNotNegative;
import static com.github.grishberg.javascad.utils.AssertValue.isNotNull;

import com.github.grishberg.javascad.basic.Angle;
import com.github.grishberg.javascad.basic.Radius;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents a cylinder, a truncated cone or a cone. It is a descendant of {@link Abstract3dModel}, 
 * which means you can use the convenient methods on cylinders too.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Cylinder extends Atomic3dModel {
	protected final double length;
	protected final Radius bottomRadius;
	protected final Radius topRadius;
	
	/**
	 * Creates a truncated cone. If one of the two radiuses is zero the result is a cone. 
	 * If the two radiuses are the same the result is a cylinder.
	 * @param length the length of the cylinder
	 * @param bottomRadius the bottom radius of the cylinder
	 * @param topRadius the top radius of the cylinder
	 * @throws IllegalValueException if the length or any any of the two radius parameter is negative 
	 */
	public Cylinder(double length, Radius bottomRadius, Radius topRadius) throws IllegalValueException {
		super();
		isNotNegative(length, "The length should be positive, but "+length);
		isNotNull(bottomRadius, "Both radius should be given, but bottomRadius was null");
		isNotNull(topRadius, "Both radius should be given, but topRadius was null");

		this.length = length;
		this.bottomRadius = bottomRadius;
		this.topRadius = topRadius;
	}
	
	/**
	 * Creates a cylinder with a given length and radius.
	 * @param length the length of the cylinder
	 * @param r the radius of the cylinder
	 * @throws IllegalValueException if the length or the radius parameter is negative 
	 */
	public Cylinder(double length, Radius r) throws IllegalValueException {
		super();
		isNotNegative(length, "The length should be positive, but "+length);
		isNotNull(r, "The radius should be given, but was null");
		
		this.length = length;
		this.bottomRadius = r;
		this.topRadius = r;
	}

	/**
	 * Creates a truncated cone. If one of the two radiuses is zero the result is a cone. 
	 * If the two radiuses are the same the result is a cylinder.
	 * @param length the length of the cylinder
	 * @param bottomRadius the bottom radius of the cylinder
	 * @param topRadius the top radius of the cylinder
	 * @throws IllegalValueException if the length or any any of the two radius parameter is negative
	 * @deprecated use the constructor with Radius parameters instead of doubles 
	 */
	@Deprecated
	public Cylinder(double length, double bottomRadius, double topRadius) throws IllegalValueException {
		this(length, Radius.fromRadius(bottomRadius), Radius.fromRadius(topRadius));
	}
	
	/**
	 * Creates a cylinder with a given length and radius.
	 * @param length the length of the cylinder
	 * @param r the radius of the cylinder
	 * @throws IllegalValueException if the length or the radius parameter is negative
	 * @deprecated use the constructor with Radius parameters instead of doubles 
	 */
	@Deprecated
	public Cylinder(double length, double r) throws IllegalValueException {
		this(length, Radius.fromRadius(r));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		double r = Math.max(bottomRadius.getRadius(), topRadius.getRadius());
		double z = length/2.0;
		return new Boundaries3d(
				Boundary.createSymmetricBoundary(r), 
				Boundary.createSymmetricBoundary(r),
				Boundary.createSymmetricBoundary(z));
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Cylinder(length, bottomRadius, topRadius);
	}

    private V3d cylPoint(double z, Radius r, double slice) {
        return r.toCoordinate(Angle.A360.mul(slice)).withZ(z);
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        int segments = context.calculateNumberOfSlices(topRadius.min(bottomRadius));
        return Manifold3dEngine.INSTANCE.cylinderNative(
            length, bottomRadius.getRadius(), topRadius.getRadius(), segments, 1);
    }
}
