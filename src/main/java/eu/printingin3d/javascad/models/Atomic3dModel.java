package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.context.IScadGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * Represents an atomic 3D object. Every primitive is a descendant of this class.
 * @author Ivan
 */
public abstract class Atomic3dModel extends Abstract3dModel {
	
	@Override
	protected final boolean isPrimitive() {
		return true;
	}
	
	@Override
	protected final Abstract3dModel innerSubModel(IScadGenerationContext context) {
		if (context.isTagIncluded()) {
			return this;
		}
		return null;
	}
	
	@Override
    protected final List<Abstract3dModel> getChildrenModels() {
	    return Collections.emptyList();
	}
}
