package com.github.grishberg.javascad.tranzitions;


import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.exceptions.NotImplementedException;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Complex3dModel;
import com.github.grishberg.javascad.utils.ListUtils;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Represents a hull of models. It is a descendant of {@link Model}, which means you
 * can use the convenient methods on unions too.</p>
 * <p>You don't have to worry about the optimization either, because the generated OpenSCAD code will be 
 * the optimal one in every case. The parameters could even contain null elements, those will
 * be ignored during the model generation.</p>
 */
public class Hull extends Complex3dModel {
	protected final List<Model> models;

	/**
	 * Construct the object.
	 * @param models list of models
	 */
	public Hull(List<Model> models) {
		this.models = models==null ? Collections.<Model>emptyList() : ListUtils.removeNulls(models);
	}

	/**
	 * Construct the object.
	 * @param models array of models
	 */
	public Hull(Model... models) {
		this(Arrays.asList(models));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		List<Boundaries3d> boundaries = new ArrayList<>();
		for (Model model : models) {
			boundaries.add(model.getBoundaries());
		}
		return Boundaries3d.combine(boundaries);
	}

	@Override
	protected Model innerCloneModel() {
		return new Hull(new ArrayList<Model>(models));
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		return new com.github.grishberg.javascad.models.Hull(models).toNativeMesh(context);
	}

	@Override
	protected Model innerSubModel(IScadGenerationContext context) {
		List<Model> subModels = new ArrayList<>();
		for (Model m : models) {
			subModels.add(m.subModel(context));
		}
		
		return new Hull(subModels);
	}

    @Override
    protected List<Model> getChildrenModels() {
        return models;
    }
}
