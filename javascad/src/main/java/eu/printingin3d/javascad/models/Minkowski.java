package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
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
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        throw new UnsupportedOperationException("Minkowski is not supported in native mode");
    }
}
