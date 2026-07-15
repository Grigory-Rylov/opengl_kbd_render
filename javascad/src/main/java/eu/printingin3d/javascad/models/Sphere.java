package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.basic.Angle;
import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.Boundary;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.exceptions.IllegalValueException;
import eu.printingin3d.javascad.manifold.Manifold3dEngine;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sphere. It is a descendant of {@link Abstract3dModel}, which means you
 * can use the convenient methods on spheres too.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Sphere extends Atomic3dModel {
	private final Radius r;

	/**
	 * Creates the sphere with the given radius.
	 * @param r the radius to be used
	 * @throws IllegalValueException if the given radius is negative
	 */
	public Sphere(Radius r) throws IllegalValueException {
		this.r = r;
	}
	
	/**
	 * Creates the sphere with the given radius.
	 * @param r the radius to be used
	 * @throws IllegalValueException if the given radius is negative
	 */
	public Sphere(double r) throws IllegalValueException {
		this(Radius.fromRadius(r));
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return new Boundaries3d(
				Boundary.createSymmetricBoundary(r.getRadius()), 
				Boundary.createSymmetricBoundary(r.getRadius()), 
				Boundary.createSymmetricBoundary(r.getRadius()));
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Sphere(r);
	}

	private List<V3d> getVertices(Angle oneSlice, int numStacks, int i, int j) {
		List<V3d> vertices = new ArrayList<>();

		vertices.add(
		        sphereVertex(oneSlice.mul(i), oneSlice.mul(j))
		);
		if (j > 0) {
		    vertices.add(
		            sphereVertex(oneSlice.mul(i + 1), oneSlice.mul(j))
		    );
		}
		if (j < numStacks - 1) {
		    vertices.add(
		            sphereVertex(oneSlice.mul(i + 1), oneSlice.mul(j + 1))
		    );
		}
		vertices.add(
		        sphereVertex(oneSlice.mul(i), oneSlice.mul(j + 1))
		);
		return vertices;
	}

    private V3d sphereVertex(Angle theta, Angle phi) {
        V3d dir = new V3d(
                theta.cos() * phi.sin(),
                phi.cos(),
                theta.sin() * phi.sin()
        );
        return dir.mul(r.getRadius());
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        return Manifold3dEngine.INSTANCE.bindings().sphere(r.getRadius(), context.calculateNumberOfSlices(r));
    }
}
