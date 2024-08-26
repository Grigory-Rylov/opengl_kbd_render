package eu.printingin3d.javascad.tranzitions;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.enums.AlignType;
import eu.printingin3d.javascad.enums.Side;
import eu.printingin3d.javascad.models.Abstract3dModel;

/**
 * Denotes the direction. It is used internally by the {@link Mirror} operation, and used by the 
 * {@link Slicer} as well.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public enum Direction {
	/**
	 * X direction.
	 */
	X(new V3d(1,0,0)),
	
	/**
	 * Y direction.
	 */
	Y(new V3d(0,1,0)),
	
	/**
	 * z direction.
	 */
	Z(new V3d(0,0,1));
	
	private final V3d coords;

	Direction(V3d coords) {
		this.coords = coords;
	}

	/**
	 * Get the {@link V3d} representation of this direction.
	 * @return the {@link V3d} representation of this direction
	 */
	public V3d getCoords() {
		return coords;
	}
	
	/**
	 * Move the <code>model</code> to the side of <code>alignTo</code> determined by this direction and the 
	 * given <code>side</code>. 
	 * @param model the model to be aligned
	 * @param alignTo the model align to
	 * @param side the side of the alignment - MIN or MAX is the most commonly used and NONE does nothing
	 * @return a new object representing <code>model</code>, but aligned to <code>alignTo</code>
	 */
	public Abstract3dModel moveTo(Abstract3dModel model, Abstract3dModel alignTo, AlignType side) {
		return model
				.align(getSide(side), alignTo, true);
	}
	
	private Side getSide(AlignType alignType) {
		return new Side(
				coords.isXZero() ? AlignType.NONE : alignType, 
				coords.isYZero() ? AlignType.NONE : alignType,
				coords.isZZero() ? AlignType.NONE : alignType
				);
	}
}
