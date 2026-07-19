package com.github.grishberg.javascad.context;

import com.github.grishberg.javascad.utils.Color;
import java.util.Map;

/**
 * Represents the tag-color relation. It is most conveniently created by 
 * {@link com.github.grishberg.javascad.context.TagColorsBuilder TagColorsBuilder}.
 * @author Ivan
 *
 */
public class TagColors implements ITagColors {
	private final Map<Integer, Color> tagColorMap;
	
	/**
	 * Constructs the object based on the given map of tag-color mapping.
	 * @param tagColorMap the map storing the mapping
	 */
	public TagColors(Map<Integer, Color> tagColorMap) {
		this.tagColorMap = tagColorMap;
	}

	/* (non-Javadoc)
	 * @see com.github.grishberg.javascad.context.ITagColors#getColor(int)
	 */
	@Override
	public Color getColor(int tag) {
		return tagColorMap.get(Integer.valueOf(tag));
	}
}
