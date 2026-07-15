package eu.printingin3d.javascad.tranzitions;

import eu.printingin3d.javascad.context.IScadGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.manifold.Manifold3dEngine;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Complex3dModel;
import eu.printingin3d.javascad.utils.ListUtils;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Intersection operation. The result of this operation is the common part of the child models.
 * If there is no common part the getBoundaries method call and all operations depending on that
 * (eg. align) will throw an exception.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Intersection extends Complex3dModel {
	private final List<Abstract3dModel> models;

	/**
	 * Creates the object with the models given.
	 * @param models the models used to create the intersection
	 */
	public Intersection(List<Abstract3dModel> models) {
		this.models = models;
	}

	/**
	 * Creates the object with the models given.
	 * @param models the models used to create the intersection
	 */
	public Intersection(Abstract3dModel... models) {
		this(Arrays.asList(models));
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Intersection(new ArrayList<Abstract3dModel>(models));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		List<Boundaries3d> boundaries = new ArrayList<>();
		for (Abstract3dModel model : models) {
			boundaries.add(model.getBoundaries());
		}
		return boundaries.isEmpty() ? Boundaries3d.EMPTY : Boundaries3d.intersect(boundaries);
	}

    @Override
    protected CSG toInnerCSG(FacetGenerationContext context) {
        CSG csg = null;
        for (Abstract3dModel model : models) {
            if (csg==null) {
                csg = model.toCSG(context);
            }
            else {
                csg = csg.intersect(model.toCSG(context));
            }
        }
        return csg;
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        long result = 0L;
        try {
            for (Abstract3dModel model : models) {
                long m = model.toNativeMesh(context);
                if (result == 0L) {
                    result = m;
                } else {
                    long r = Manifold3dEngine.INSTANCE.intersectionNative(result, m);
                    Manifold3dEngine.INSTANCE.delete(m);
                    Manifold3dEngine.INSTANCE.delete(result);
                    result = r;
                }
            }
        } catch (Exception e) {
            if (result != 0L) Manifold3dEngine.INSTANCE.delete(result);
            throw e;
        }
        return result;
    }

	@Override
	protected Abstract3dModel innerSubModel(IScadGenerationContext context) {
		List<Abstract3dModel> subModels = new ArrayList<>();
		for (Abstract3dModel model : models) {
			subModels.add(model.subModel(context));
		}
		return new Intersection(ListUtils.removeNulls(subModels));
	}

    @Override
    protected List<Abstract3dModel> getChildrenModels() {
        return models;
    }
}
