package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * <p>There are cases, when we want to have an object, which boundaries differ from the calculated ones.
 * An example could be a wardrobe, when we don't want to include the handles into the boundary calculation,
 * when we align the object.</p>
 * <p>This object could be used to achieve that goal.</p>
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class BoundedModel extends Complex3dModel {
	private final Model baseModel;
	private final Boundaries3d boundaries3d;

	/**
	 * Creates the object.
	 * @param baseModel the object used to generate the output
	 * @param boundaries3d the boundary used by the alignment methods
	 * @throws com.github.grishberg.javascad.exceptions.IllegalValueException if either of the parameters are null
	 */
	public BoundedModel(Model baseModel, Boundaries3d boundaries3d) {
		AssertValue.isNotNull(baseModel, "The baseModel parameter must not be null!");
		AssertValue.isNotNull(boundaries3d, "The boundaries3d parameter must not be null!");
		
		this.baseModel = baseModel;
		this.boundaries3d = boundaries3d;
	}

	@Override
	protected Model innerCloneModel() {
		return new BoundedModel(baseModel, boundaries3d);
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return boundaries3d;
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		return baseModel.toNativeMesh(context);
	}

	@Override
	protected Model innerSubModel(IScadGenerationContext context) {
		Model subModel = baseModel.subModel(context);
		return subModel==null ? null : new BoundedModel(subModel, boundaries3d);
	}

    @Override
    protected List<Model> getChildrenModels() {
        return Collections.singletonList(baseModel);
    }
}
