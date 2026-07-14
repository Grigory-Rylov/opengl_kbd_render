package eu.printingin3d.javascad.vrl;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.manifold.Manifold3dEngine;
import eu.printingin3d.javascad.tranform.ITransformation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CSG {

    private final List<Polygon> polygons;

    public CSG(List<Polygon> polygons) {
        this.polygons = Collections.unmodifiableList(polygons);
    }

    public static CSG fromPolygons(Polygon... polygons) {
        return new CSG(Arrays.asList(polygons));
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }

    public List<V3d> getPoints() {
        List<V3d> result = new ArrayList<>();
        for (Polygon p : getPolygons()) {
            result.addAll(p.getVertices());
        }
        return result;
    }

    public CSG union(CSG csg) {
        List<Polygon> result = Manifold3dEngine.INSTANCE.union(this.polygons, csg.polygons);
        return new CSG(result);
    }

    public CSG difference(CSG csg) {
        List<Polygon> result = Manifold3dEngine.INSTANCE.difference(this.polygons, csg.polygons);
        return new CSG(result);
    }

    public CSG intersect(CSG csg) {
        List<Polygon> result = Manifold3dEngine.INSTANCE.intersection(this.polygons, csg.polygons);
        return new CSG(result);
    }

    public List<Facet> toFacets() {
        List<Facet> facets = new ArrayList<>();
        for (Polygon p : polygons) {
            facets.addAll(p.toFacets());
        }
        return facets;
    }

    public CSG transformed(ITransformation transform) {
        List<Polygon> newpolygons = new ArrayList<>();
        for (Polygon p : this.polygons) {
            newpolygons.add(p.transformed(transform));
        }
        return new CSG(newpolygons);
    }
}
