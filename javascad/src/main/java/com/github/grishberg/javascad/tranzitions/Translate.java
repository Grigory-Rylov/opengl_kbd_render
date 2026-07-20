package com.github.grishberg.javascad.tranzitions;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Complex3dModel;
import com.github.grishberg.javascad.tranform.TransformationFactory;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * This represents a move transition, but used rarely, because the convenient
 * methods of {@link Model} replace it most of the time.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Translate extends Complex3dModel {
	private final Model model;
	private final V3d move;

	/**
	 * Creates a move operation.
	 * @param model the model to be moved
	 * @param move the coordinates used by the move operation
	 * @throws IllegalValueException if either of the two parameters is null
	 */
	public Translate(Model model, V3d move) throws IllegalValueException {
		AssertValue.isNotNull(model, "Model must not be null for translation!");
		AssertValue.isNotNull(move, "Move must not be null for translation!");
		
		this.model = model;
		this.move = move;
	}

	/**
	 * This method is used internally by the {@link Model} - do not use it!
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
	protected Model innerCloneModel() {
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
	protected Model innerSubModel(IScadGenerationContext context) {
		Model subModel = model.subModel(context);
		return subModel==null ? null : new Translate(subModel, move);
	}

    @Override
    protected List<Model> getChildrenModels() {
        return Collections.singletonList(model);
    }
}
