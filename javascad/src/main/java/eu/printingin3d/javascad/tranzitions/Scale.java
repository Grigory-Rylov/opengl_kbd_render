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
 * Scales a model by the given value on X, Y and Z plane. It is a descendant of {@link Abstract3dModel}, 
 * which means you can use the convenient methods on scales too.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Scale extends Complex3dModel {
	private final Abstract3dModel model;
	private final V3d scale;

	/**
	 * Creates the scale operation with the given model and scale value. If the scale equals to (1,1,1)
	 * this object does nothing, just gives back the underlying model.
	 * @param model the model to be scaled
	 * @param scale the scale values to be used
	 * @throws IllegalValueException if either of the parameters is null
	 */
	public Scale(Abstract3dModel model, V3d scale) throws IllegalValueException {
		AssertValue.isNotNull(model, "Model should not be null for scale operation!");
		AssertValue.isNotNull(scale, "Scale should not be null for scale operation!");
		
		this.model = model;
		this.scale = scale;
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		Boundaries3d result = model.getBoundaries();
		if (!scale.isIdent()) {
			result = result.scale(scale);
		}
		return result;
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Scale(model, scale);
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		long m = model.toNativeMesh(context);
		long r = Manifold3dEngine.INSTANCE.scale(m, scale.getX(), scale.getY(), scale.getZ());
		Manifold3dEngine.INSTANCE.delete(m);
		return r;
	}

	@Override
	protected Abstract3dModel innerSubModel(IScadGenerationContext context) {
		Abstract3dModel subModel = model.subModel(context);
		return subModel==null ? null : new Scale(subModel, scale);
	}

    @Override
    protected List<Abstract3dModel> getChildrenModels() {
        return Collections.singletonList(model);
    }
}
