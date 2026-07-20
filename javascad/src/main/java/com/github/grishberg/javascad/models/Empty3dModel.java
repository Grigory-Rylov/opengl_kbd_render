package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;

/**
 * <p>Empty model - use this as the base class if you want to build a complex object.</p>
 * <p>Example:</p>
 * <pre>
* {@code
* Model result = new Empty3dModel();
* if (isCrit1()) {
*   result = result.addModel(addModel1());
* }
* if (isCrit2()) {
*   result = result.addModel(addModel2());
* }
* </pre>
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Empty3dModel extends Atomic3dModel {
	@Override
	protected Model innerCloneModel() {
		return new Empty3dModel();
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return Boundaries3d.EMPTY;
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		return 0L;
	}

	@Override
	public Model addModel(Model model) {
		return model;
	}
	
	@Override
	public Model subtractModel(Model model) {
		return this;
	}
}
