package com.github.grishberg.javascad.models2d;

import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords2d.Boundaries2d;
import com.github.grishberg.javascad.coords2d.Coords2d;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Represents a 2D polygon.
 * @author Ivan
 *
 */
public class Polygon2d extends Abstract2dModel {
	private final List<Coords2d> coords;

	protected Polygon2d(Coords2d move, List<Coords2d> coords) {
		super(move);
		AssertValue.isTrue(coords.size()>=3, "The list of coordinates should contain at least 3 elements!");
		this.coords = new ArrayList<>(coords);
	}
	
	/**
	 * <p>Creates a polygon. The polygon consists of the given coordinates. The polygon is always closed, so
	 * it has an edge between the last and first coordinates.</p>
	 * <p>The given list should contain at least 3 coordinates, otherwise an 
	 * {@link com.github.grishberg.javascad.exceptions.IllegalValueException IllegalValueException} is thrown.</p> 
	 * @param coords the coordinates
	 * @throws com.github.grishberg.javascad.exceptions.IllegalValueException in case the given list contains 
	 * 				fewer than 3 elements
	 */
	public Polygon2d(List<Coords2d> coords) {
		this(Coords2d.ZERO, coords);
	}

	public List<Coords2d> getCoords() {
		return new ArrayList<>(coords);
	}

	@Override
	protected Boundaries2d getModelBoundaries() {
		double[] x = new double[coords.size()];
		double[] y = new double[coords.size()];

		int i = 0;
		for (Coords2d c : coords) {
			x[i] = c.getX();
			y[i] = c.getY();
			i++;
		}
		
		return new Boundaries2d(new Boundary(x), new Boundary(y));
	}

	@Override
	public Abstract2dModel move(Coords2d delta) {
		return new Polygon2d(move.move(delta), coords);
	}

	@Override
	public Collection<Area2d> getInnerPointCircle(FacetGenerationContext context) {
		return Collections.singleton(new Area2d(coords));
	}

}
