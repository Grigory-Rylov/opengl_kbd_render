package com.github.grishberg.javascad.models2d;

import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords2d.Boundaries2d;
import com.github.grishberg.javascad.coords2d.Coords2d;
import com.github.grishberg.javascad.coords2d.Dims2d;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Represents a 2D square object.
 * 
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Square extends Abstract2dModel {
	protected final Dims2d size;

	protected Square(Coords2d move, Dims2d size) {
		super(move);
		this.size = size;
	}
	
	/**
	 * Creates a square with the given size values.
	 * @param size the 2D size value used to construct the square
	 */
	public Square(Dims2d size) {
		this(Coords2d.ZERO, size);
	}

	@Override
	protected Boundaries2d getModelBoundaries() {
		return new Boundaries2d(
				Boundary.createSymmetricBoundary(size.getX()/2.0), 
				Boundary.createSymmetricBoundary(size.getY()/2.0));
	}

	@Override
	public Abstract2dModel move(Coords2d delta) {
		return new Square(this.move.move(delta), this.size);
	}

	@Override
	protected Collection<Area2d> getInnerPointCircle(FacetGenerationContext context) {
		return Collections.singleton(new Area2d(Arrays.asList(
				new Coords2d(-size.getX()/2, -size.getY()/2),
				new Coords2d(+size.getX()/2, -size.getY()/2),
				new Coords2d(+size.getX()/2, +size.getY()/2),
				new Coords2d(-size.getX()/2, +size.getY()/2)
			)));
	}
}
