package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.vrl.FacetGenerationContext;

/**
 * Represents a renderable 3D model.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public interface IModel {

	/**
	 * Renders this model to a native manifold handle.
	 * @param context the context to be used during the generation process.
	 * @return native manifold handle (Long), caller is responsible for deleting it
	 */
	long toNativeMesh(FacetGenerationContext context);
}
