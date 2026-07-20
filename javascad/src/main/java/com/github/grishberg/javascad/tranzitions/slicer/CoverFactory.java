package com.github.grishberg.javascad.tranzitions.slicer;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.coords.Dims3d;
import com.github.grishberg.javascad.enums.AlignType;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Cube;
import com.github.grishberg.javascad.tranzitions.Direction;

/**
 * <p>Helper class to support the slicing functionality by Slicer. It is only used internally and 
 * shouldn't be created outside of the Slicer class!</p>
 * <p>It represents the cover cube(s) which is used to slicing the unnecessary parts of the model 
 * to be sliced.</p>
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public final class CoverFactory {

	private CoverFactory() {}
	
	/**
	 * Create a helper object to be used.
	 * @param model model to be sliced
	 * @param direction the direction of the slicing
	 * @param sizeRate the rate of the part to be cut off
	 * @param lower which end should be cut off
	 * @return the cover object which hides the proper part of the given model
	 */
	public static Model createCover(Model model, Direction direction, 
			double sizeRate, boolean lower) {
		Model result = new Cube(calculateSize(model, direction, sizeRate));
		Boundaries3d b3d = model.getBoundaries();
		result = result.move(new V3d(
				b3d.getX().getMiddle(), 
				b3d.getY().getMiddle(),
				b3d.getZ().getMiddle()));
		if (lower) {
			return direction
					.moveTo(result, model, AlignType.MIN)
					.move(direction.getCoords().inverse());
		} else {
			return direction
					.moveTo(result, model, AlignType.MAX)
					.move(direction.getCoords());
		}
	}

	private static Dims3d calculateSize(Model model, Direction direction, double sizeRate) {
		V3d dirSizeRate = direction.getCoords().mul(sizeRate)
				.add(new V3d(1, 1, 1).add(direction.getCoords().inverse()));
		return model.getBoundaries()
				.getSize().mul(dirSizeRate)
				.increase();
	}

}
