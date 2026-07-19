package com.github.grishberg.javascad.models2d;

import com.github.grishberg.javascad.context.IColorGenerationContext;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords2d.Boundaries2d;
import com.github.grishberg.javascad.coords2d.Coords2d;
import com.github.grishberg.javascad.coords2d.Dims2d;
import com.github.grishberg.javascad.exceptions.NotImplementedException;
import com.github.grishberg.javascad.utils.DoubleUtils;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.Collection;

/**
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Text extends Abstract2dModel {

    private final String text;
    private final Dims2d size;

    private Text(Coords2d move, String text, Dims2d size) {
        super(move);

        this.text = text;
        this.size = size;
    }

    /**
     * Constructs the object using the given parameters.
     *
     * @param text the text of the object
     * @param size the dimensions of the object - used by the alignment functions
     */
    public Text(String text, Dims2d size) {
        this(Coords2d.ZERO, text, size);
    }


    @Override
    protected Boundaries2d getModelBoundaries() {
        double x = size.getX() / 2;
        double y = size.getY() / 2;
        return new Boundaries2d(new Boundary(-x, +x), new Boundary(-y, +y));
    }

    @Override
    public Abstract2dModel move(Coords2d delta) {
        return new Text(this.move.move(delta), text, size);
    }

    @Override
    protected Collection<Area2d> getInnerPointCircle(FacetGenerationContext context) {
        throw new NotImplementedException();
    }
}
