package com.github.grishberg.javascad.openscad;

import com.github.grishberg.javascad.models.IModel;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;

/**
 * Represents the constants for $fs and $fa which controls the resolution of every circle, 
 * cylinder or sphere.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Consts implements IModel {
	private final int fn;

	/**
	 * Constructs default constants with $fs = 0.25 and $fa = 6. These default values results
	 * medium detailed objects which are good for most of the cases. 
	 */
	public Consts() {
		this(6);
	}
	
	/**
	 * Constructs a constant with the given fs and fn values.
	 * @param fn angle in degrees
	 */
	public Consts(int fn) {
		this.fn = fn;
	}

	@Override
	public long toNativeMesh(FacetGenerationContext context) {
		context.setFn(fn);
		return 0L;
	}
}