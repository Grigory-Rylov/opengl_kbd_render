package eu.printingin3d.javascad.vrl;


import eu.printingin3d.javascad.utils.Color;

public class ColorFacetGenerationContext extends FacetGenerationContext {
    private final Color color;

    public ColorFacetGenerationContext(
        Color color
    ) {
        super(null, null, 0);
        this.color = color;
    }

    @Override
    public Color getColor() {
        return color;
    }
}
