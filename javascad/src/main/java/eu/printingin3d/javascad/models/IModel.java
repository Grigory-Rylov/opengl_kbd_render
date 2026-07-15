package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;

/**
 * Represents a renderable 3D model.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public interface IModel {

	/**
	 * Renders this model to its CSG interpretation.
	 * @param context the context to be used during the generation process.
	 * @return the CSG interpretation
	 */
	CSG toCSG(FacetGenerationContext context);

	/**
	 * Renders this model to a native manifold handle.
	 * @param context the context to be used during the generation process.
	 * @return native manifold handle (Long), caller is responsible for deleting it
	 */
	default long toNativeMesh(FacetGenerationContext context) {
		return eu.printingin3d.javascad.manifold.Manifold3dEngine.INSTANCE.polygonsToManifold(
			eu.printingin3d.javascad.manifold.Manifold3dEngine.INSTANCE.bindings(), toCSG(context).getPolygons());
	}
}
