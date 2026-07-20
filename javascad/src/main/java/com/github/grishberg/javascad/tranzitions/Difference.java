package com.github.grishberg.javascad.tranzitions;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.exceptions.IllegalValueException;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Complex3dModel;
import com.github.grishberg.javascad.models.Cube;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.utils.ListUtils;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Difference operation. It subtracts from the first model all the others.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Difference extends Complex3dModel {
	private final Model model1;
	private final List<Model> model2;
	
	/**
	 * <p>Creates the object with the models given. The first parameter will be the model
	 * and the second list of models will be subtracted by it.</p>
	 * <p>If the first parameter is null it throws an IllegalValueException.
	 * If the second parameter is null or empty the operation do nothing,
	 * just gives back the first model.</p> 
	 * @param model1 the model subtracted from
	 * @param model2 the model to be subtracted
	 * @throws IllegalValueException if the first model is null
	 */
	public Difference(Model model1, List<Model> model2) throws IllegalValueException {
		AssertValue.isNotNull(model1, "The first parameter of the difference operation should not be null!");
		
		this.model1 = model1;
		this.model2 = model2==null ? Collections.<Model>emptyList() : ListUtils.removeNulls(model2);
		this.setColor(model1.getColor());
	}
	
	/**
	 * <p>Creates the object with the list of models given. The first parameter is used as a model
	 * and all subsequent models will be subtracted from it.</p>
	 * <p>If the first parameter is null it throws an IllegalValueException.
	 * If any subsequent parameter is null that parameter is ignored. If only one parameter is
	 * given this operation does nothing, just gives back the first model.</p> 
	 * @param model1 the model subtracted from
	 * @param model2 the model to be subtracted
	 * @throws IllegalValueException if the first model is null
	 */
	public Difference(Model model1, Model... model2) throws IllegalValueException {
		this(model1, Arrays.asList(model2));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		Boundaries3d boundaries = model1.getBoundaries();
		Boundary x = boundaries.getX();
		Boundary y = boundaries.getY();
		Boundary z = boundaries.getZ();
		
		for (Model model : model2) {
			if (model instanceof Cube && !model.isRotated()) {
				Boundaries3d b = model.getBoundaries();
				if (x.isInsideOf(b.getX())) {
					if (y.isInsideOf(b.getY())) {
						z = z.remove(b.getZ());
					}
					else if (z.isInsideOf(b.getZ())) {
						y = y.remove(b.getY());
					}
				}
				else if (y.isInsideOf(b.getY()) && 
						z.isInsideOf(b.getZ())) {
					x = x.remove(b.getX());
				}
			}
		}
		return new Boundaries3d(x, y, z);
	}

	@Override
	protected Model innerCloneModel() {
		return new Difference(model1, new ArrayList<Model>(model2));
	}

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        long result = model1.toNativeMesh(context);
        if (result == 0L) {
            return 0L;
        }
        try {
            for (Model model : model2) {
                long m = model.toNativeMesh(context);
                if (m == 0L) {
                    continue;
                }
                long r = Manifold3dEngine.INSTANCE.differenceNative(result, m);
                Manifold3dEngine.INSTANCE.delete(m);
                Manifold3dEngine.INSTANCE.delete(result);
                result = r;
            }
        } catch (Exception e) {
            Manifold3dEngine.INSTANCE.delete(result);
            throw e;
        }
        return result;
    }
	
	@Override
	public Model subtractModel(Model model) {
		if (isMoved() || isRotated()) {
			return super.subtractModel(model);
		}
		
		List<Model> newModel2 = new ArrayList<>(model2);
		newModel2.add(model);
		return new Difference(model1, newModel2);
	}

	@Override
	protected Model innerSubModel(IScadGenerationContext context) {
		Model subModel = model1.subModel(context);
		if (subModel==null) {
			return null;
		}
		List<Model> subModels = new ArrayList<>();
		for (Model model : model2) {
			subModels.add(model.subModel(context));
		}
		return new Difference(subModel, subModels);
	}

    @Override
    protected List<Model> getChildrenModels() {
        List<Model> result = new ArrayList<>(model2);
        result.add(model1);
        return result;
    }
}
