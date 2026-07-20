package com.github.grishberg.javascad.tranzitions;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Angles3d;
import com.github.grishberg.javascad.coords.Boundaries3d;
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
 * This represents a rotate transition, but used rarely, because the convenient
 * methods of {@link Model} replace it most of the time.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Rotate extends Complex3dModel {
	private final Model model;
	private final Angles3d angles;

	/**
	 * Creates a rotation operation.
	 * @param model the model to be rotated
	 * @param angles the angles used by the rotation operation
	 * @throws IllegalValueException if either of the two parameters is null
	 */
	public Rotate(Model model, Angles3d angles) throws IllegalValueException {
		AssertValue.isNotNull(model, "The model should not be null for a rotation!");
		AssertValue.isNotNull(angles, "The angles should not be null for a rotation!");
		
		this.model = model;
		this.angles = angles;
	}

	/**
	 * This method is used internally by the {@link Model} - do not use it!
	 * @param angles the angles used by the rotate operation
	 * @return the string which represents the rotation in OpenSCAD
	 */
	public static String getRotate(Angles3d angles) {
		return "rotate("+angles+")";
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return model.getBoundaries().rotate(angles);
	}

	@Override
	protected Model innerCloneModel() {
		return new Rotate(model, angles);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		long m = model.toNativeMesh(context);
		long r = Manifold3dEngine.INSTANCE.rotate(m, angles.getX(), angles.getY(), angles.getZ());
		Manifold3dEngine.INSTANCE.delete(m);
		return r;
	}

	@Override
	protected Model innerSubModel(IScadGenerationContext context) {
		Model subModel = model.subModel(context);
		return subModel==null ? null : new Rotate(subModel, angles);
	}

    @Override
    protected List<Model> getChildrenModels() {
        return Collections.singletonList(model);
    }
}
