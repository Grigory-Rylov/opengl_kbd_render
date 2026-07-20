package com.github.grishberg.javascad.tranzitions;

import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Complex3dModel;
import com.github.grishberg.javascad.utils.AssertValue;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.utils.DoubleUtils;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
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
	private final Model baseModel;

	/**
	 * Creates a Colorized object of the given object with the given color.
	 * @param color the color which will be used
	 * @param model the model which will be colored
	 * @throws com.github.grishberg.javascad.exceptions.IllegalValueException if either the model or the color null
	 */
	public Colorize(Color color, Model model) {
		AssertValue.isNotNull(model, "The model shouldn't be null for colorize");
		AssertValue.isNotNull(color, "The color shouldn't be null for colorize");
		
		this.color = color;
		this.baseModel = model;
	}

	@Override
	protected Model innerCloneModel() {
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
	protected Model innerSubModel(IScadGenerationContext context) {
		Model subModel = baseModel.subModel(context);
		return subModel==null ? null : new Colorize(color, subModel);
	}

    @Override
    protected List<Model> getChildrenModels() {
        return Collections.singletonList(baseModel);
    }
}
