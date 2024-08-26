package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Minkowski extends Atomic3dModel {

    private final List<Abstract3dModel> models;

    public Minkowski(Abstract3dModel... obj) {
        this.models = new ArrayList<>();
        Collections.addAll(models, obj);
    }

    public Minkowski(List<Abstract3dModel> obj) {
        this.models = new ArrayList<>();
        models.addAll(obj);
    }

    @Override
    protected Abstract3dModel innerCloneModel() {
        return new Minkowski(models);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        //TODO calculate real boundary
        ArrayList<Boundaries3d> boundaries3ds = new ArrayList<>();
        for (Abstract3dModel model : models) {
            boundaries3ds.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries3ds);
    }

    @Override
    protected CSG toInnerCSG(FacetGenerationContext context) {
        if (models.isEmpty()) {
            return new CSG(new ArrayList<>());
        }

        CSG result = models.get(0).toInnerCSG(context);
        for (int i = 1; i < models.size(); i++) {
            result = minkowskiSumPair(result, models.get(i).toInnerCSG(context), context);
        }
        return result;
    }

    private static CSG minkowskiSumPair(CSG csg1, CSG csg2, FacetGenerationContext context) {
        List<V3d> vertices = new ArrayList<>();

        for (Polygon poly1 : csg1.getPolygons()) {
            for (Polygon poly2 : csg2.getPolygons()) {
                for (V3d v1 : poly1.getVertices()) {
                    for (V3d v2 : poly2.getVertices()) {
                        vertices.add(v1.add(v2));
                    }
                }
            }
        }

        return new CSG(Hull.generateHull(context, vertices));
    }
}
