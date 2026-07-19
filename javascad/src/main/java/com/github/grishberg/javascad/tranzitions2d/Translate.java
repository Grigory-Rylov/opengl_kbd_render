package com.github.grishberg.javascad.tranzitions2d;

import com.github.grishberg.javascad.coords2d.Boundaries2d;
import com.github.grishberg.javascad.coords2d.Coords2d;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.models2d.Abstract2dModel;
import com.github.grishberg.javascad.models2d.Area2d;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.Collection;

/**
 * This represents a move transition, but used rarely, because the convenient
 * methods of {@link Abstract2dModel} replace it most of the time.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Translate extends Abstract2dModel {
	private final Abstract2dModel model;

	/**
	 * Creates a move operation.
	 * @param model the model to be moved
	 * @param move the coordinates used by the move operation
	 * @throws IllegalValueException if either of the two parameters is null
	 */
	public Translate(Abstract2dModel model, Coords2d move) throws IllegalValueException {
		super(move);
		
		AssertValue.isNotNull(model, "Model must not be null for translation!");
		AssertValue.isNotNull(move, "Move must not be null for translation!");
		
		this.model = model;
	}

	/**
	 * This method is used internally by the {@link Abstract2dModel} - do not use it!
	 * @param move the coordinates used by the move operation
	 * @return the string which represents the move in OpenSCAD
	 */
	public static String getTranslate(Coords2d move) {
		if (move.isZero()) {
			return "";
		}
		return "translate("+move+")";
	}

	@Override
	protected Boundaries2d getModelBoundaries() {
		return model.getBoundaries2d();
	}

	@Override
	public Abstract2dModel move(Coords2d delta) {
		return new Translate(model, this.move.move(delta));
	}

	@Override
	protected Collection<Area2d> getInnerPointCircle(FacetGenerationContext context) {
		return model.getPointCircle(context);
	}
}
