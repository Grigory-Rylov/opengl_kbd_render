package com.github.grishberg.javascad.models2d;

import com.github.grishberg.javascad.basic.Angle;
import com.github.grishberg.javascad.basic.Radius;
import com.github.grishberg.javascad.coords2d.Coords2d;
import com.github.grishberg.javascad.coords2d.Dims2d;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a 2D rounded square - the corners of the square will be rounded by the given radius.
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class RoundedSquare extends Square {
	private final Radius radius;

	private RoundedSquare(Coords2d move, Dims2d size, Radius radius) {
		super(move, size);
		AssertValue.isNotNegative(size.getX()-radius.getRadius()*2.0, "The X size should be at least the double "
				+ "of the radius, but X size was "+size.getX()+" and radius was "+radius);
		AssertValue.isNotNegative(size.getY()-radius.getRadius()*2.0, "The Y size should be at least the double "
				+ "of the radius, but Y size was "+size.getY()+" and radius was "+radius);
		this.radius = radius;
	}

	/**
	 * Create a 2D rounded square.
	 * @param size the size of the object including the rounding
	 * @param radius the radius of the rounding
	 * @throws com.github.grishberg.javascad.exceptions.IllegalValueException 
	 * 			if either dimension is less the double the radius
	 */
	public RoundedSquare(Dims2d size, Radius radius) {
		this(Coords2d.ZERO, size, radius);
	}
	
	/**
	 * Create a 2D rounded square.
	 * @param size the size of the object including the rounding
	 * @param radius the radius of the rounding
	 * @throws com.github.grishberg.javascad.exceptions.IllegalValueException 
	 * 			if either dimension is less the double the radius
	 * @deprecated use the constructor with Radius parameters instead of doubles 
	 */
	@Deprecated
	public RoundedSquare(Dims2d size, double radius) {
		this(size, Radius.fromRadius(radius));
	}
	
	@Override
	public Abstract2dModel move(Coords2d delta) {
		return new RoundedSquare(this.move.move(delta), this.size, this.radius);
	}

	@Override
	protected Collection<Area2d> getInnerPointCircle(FacetGenerationContext context) {
        List<Coords2d> points = new ArrayList<>();

        int numSlices = context.calculateNumberOfSlices(radius)*4;
        Angle oneSlice = Angle.A360.divide(numSlices);
        for (int i = numSlices; i > 0; i--) {
        	Coords2d base = radius.toCoordinate(oneSlice.mul(i));
            double x = base.getX() + Math.signum(base.getX())*(size.getX()/2-radius.getRadius());
			double y = base.getY() + Math.signum(base.getY())*(size.getY()/2-radius.getRadius());
			points.add(new Coords2d(x, y));
        }
        return Collections.singleton(new Area2d(points));
	}

}
