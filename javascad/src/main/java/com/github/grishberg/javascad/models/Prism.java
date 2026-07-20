package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.basic.Radius;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;

/**
 * Represents a prism or a pyramid.
 * It is a descendant of Model, 
 * which means you can use the convenient methods on prisms too.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Prism extends Cylinder {
	private final int numberOfSides;
	
	/**
	 * Creates a prism with a given length, radius and the number of sides.
	 * @param length the length of the prism
	 * @param r the radius of the prism
	 * @param numberOfSides the side number of the prism
	 * @throws IllegalValueException if any of its parameters is negative 
	 */
	public Prism(double length, Radius r, int numberOfSides) throws IllegalValueException {
		super(length, r);
		AssertValue.isNotNegative(numberOfSides, 
				"The number of sides should be positive, but "+numberOfSides);
		this.numberOfSides = numberOfSides;
	}
	
	/**
	 * Creates a prism, which base and top have different radius.
	 * If one of the two radiuses is zero the result is a pyramid. 
	 * If the two radiuses are the same the result is the same as {@link #Prism(double, double, int)}.
	 * @param length the length of the prism
	 * @param r1 the bottom radius of the prism
	 * @param r2 the top radius of the prism
	 * @param numberOfSides the side number of the prism
	 * @throws IllegalValueException if any of its parameters is negative 
	 */
	public Prism(double length, Radius r1, Radius r2, int numberOfSides) throws IllegalValueException {
		super(length, r1, r2);
		AssertValue.isNotNegative(numberOfSides, 
				"The number of sides should be positive, but "+numberOfSides);
		this.numberOfSides = numberOfSides;
	}
	
	/**
	 * Creates a prism with a given length, radius and the number of sides.
	 * @param length the length of the prism
	 * @param r the radius of the prism
	 * @param numberOfSides the side number of the prism
	 * @throws IllegalValueException if any of its parameters is negative
	 * @deprecated use the constructor with Radius parameters instead of doubles 
	 */
	@Deprecated
	public Prism(double length, double r, int numberOfSides) throws IllegalValueException {
		this(length, Radius.fromRadius(r), numberOfSides);
	}
	
	/**
	 * Creates a prism, which base and top have different radius.
	 * If one of the two radiuses is zero the result is a pyramid. 
	 * If the two radiuses are the same the result is the same as {@link #Prism(double, double, int)}.
	 * @param length the length of the prism
	 * @param r1 the bottom radius of the prism
	 * @param r2 the top radius of the prism
	 * @param numberOfSides the side number of the prism
	 * @throws IllegalValueException if any of its parameters is negative 
	 * @deprecated use the constructor with Radius parameters instead of doubles 
	 */
	@Deprecated
	public Prism(double length, double r1, double r2, int numberOfSides) throws IllegalValueException {
		this(length, Radius.fromRadius(r1), Radius.fromRadius(r2), numberOfSides);
	}

	@Override
	protected Model innerCloneModel() {
		return new Prism(length, bottomRadius, topRadius, numberOfSides);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		return Manifold3dEngine.INSTANCE.cylinderNative(
				length, bottomRadius.getRadius(), topRadius.getRadius(), numberOfSides, 1);
	}
}
