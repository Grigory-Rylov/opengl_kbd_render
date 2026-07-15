package eu.printingin3d.javascad.tranzitions;

import eu.printingin3d.javascad.context.IScadGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.exceptions.IllegalValueException;
import eu.printingin3d.javascad.manifold.Manifold3dEngine;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Complex3dModel;
import eu.printingin3d.javascad.tranform.TransformationFactory;
import eu.printingin3d.javascad.utils.AssertValue;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * This represents a move transition, but used rarely, because the convenient
 * methods of {@link Abstract3dModel} replace it most of the time.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Translate extends Complex3dModel {
	private final Abstract3dModel model;
	private final V3d move;

	/**
	 * Creates a move operation.
	 * @param model the model to be moved
	 * @param move the coordinates used by the move operation
	 * @throws IllegalValueException if either of the two parameters is null
	 */
	public Translate(Abstract3dModel model, V3d move) throws IllegalValueException {
		AssertValue.isNotNull(model, "Model must not be null for translation!");
		AssertValue.isNotNull(move, "Move must not be null for translation!");
		
		this.model = model;
		this.move = move;
	}

	/**
	 * This method is used internally by the {@link Abstract3dModel} - do not use it!
	 * @param move the coordinates used by the move operation
	 * @return the string which represents the move in OpenSCAD
	 */
	public static String getTranslate(V3d move) {
		if (move.isZero()) {
			return "";
		}
		return "translate("+move+")";
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return model.getBoundaries().move(move);
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Translate(model, move);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		long m = model.toNativeMesh(context);
		long r = Manifold3dEngine.INSTANCE.translate(m, move.getX(), move.getY(), move.getZ());
		Manifold3dEngine.INSTANCE.delete(m);
		return r;
	}

	@Override
	protected Abstract3dModel innerSubModel(IScadGenerationContext context) {
		Abstract3dModel subModel = model.subModel(context);
		return subModel==null ? null : new Translate(subModel, move);
	}

    @Override
    protected List<Abstract3dModel> getChildrenModels() {
        return Collections.singletonList(model);
    }
}
