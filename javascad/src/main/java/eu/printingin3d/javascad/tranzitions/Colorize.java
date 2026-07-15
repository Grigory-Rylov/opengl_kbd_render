package eu.printingin3d.javascad.tranzitions;

import eu.printingin3d.javascad.context.IScadGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Complex3dModel;
import eu.printingin3d.javascad.utils.AssertValue;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.utils.DoubleUtils;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import java.util.Collections;
import java.util.List;

/**
 * Colors the object it contains. Please pay attention: colors are only supported in the 
 * CSG view in OpenSCAD - neither CGAL rendering nor STL output does not support it yet.
 * This is a limitation of the OpenSCAD rendering.
 *
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class Colorize extends Complex3dModel {
	private final Color color;
	private final Abstract3dModel baseModel;

	/**
	 * Creates a Colorized object of the given object with the given color.
	 * @param color the color which will be used
	 * @param model the model which will be colored
	 * @throws eu.printingin3d.javascad.exceptions.IllegalValueException if either the model or the color null
	 */
	public Colorize(Color color, Abstract3dModel model) {
		AssertValue.isNotNull(model, "The model shouldn't be null for colorize");
		AssertValue.isNotNull(color, "The color shouldn't be null for colorize");
		
		this.color = color;
		this.baseModel = model;
	}

	@Override
	protected Abstract3dModel innerCloneModel() {
		return new Colorize(color, baseModel);
	}

	@Override
	protected Boundaries3d getModelBoundaries() {
		return baseModel.getBoundaries();
	}

	@Override
	protected long toInnerNativeMesh(FacetGenerationContext context) {
		return baseModel.toNativeMesh(context);
	}

	/**
	 * Generates the SCAD representation of the given color.
	 * @param color the color to be converted
	 * @return the string representation of the color
	 */
	public static String getStringRepresentation(Color color) {
		StringBuilder sb = new StringBuilder();
		sb.append('[').
				append(DoubleUtils.formatDouble(color.getRed()/255.0)).append(',').
				append(DoubleUtils.formatDouble(color.getGreen()/255.0)).append(',').
				append(DoubleUtils.formatDouble(color.getBlue()/255.0));

		if (color.getAlpha()<255) {
			sb.append(',').append(DoubleUtils.formatDouble(color.getAlpha()/255.0));
		}
		sb.append(']');
		
		return "color("+sb+")";
	}

	@Override
	protected Abstract3dModel innerSubModel(IScadGenerationContext context) {
		Abstract3dModel subModel = baseModel.subModel(context);
		return subModel==null ? null : new Colorize(color, subModel);
	}

    @Override
    protected List<Abstract3dModel> getChildrenModels() {
        return Collections.singletonList(baseModel);
    }
}
