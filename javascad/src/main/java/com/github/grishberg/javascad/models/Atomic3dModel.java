package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * Represents an atomic 3D object. Every primitive is a descendant of this class.
 * @author Ivan
 */
public abstract class Atomic3dModel extends Model {
	
	
	@Override
	protected final Model innerSubModel(IScadGenerationContext context) {
		if (context.isTagIncluded()) {
			return this;
		}
		return null;
	}
	
	@Override
    protected final List<Model> getChildrenModels() {
	    return Collections.emptyList();
	}
}
