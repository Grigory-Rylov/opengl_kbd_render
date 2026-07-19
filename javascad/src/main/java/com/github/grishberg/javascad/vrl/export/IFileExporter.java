package com.github.grishberg.javascad.vrl.export;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import com.github.grishberg.javascad.vrl.Facet;

/**
 * An interface used to export facets to file. The interface is implemented by all exporters.
 * @author Ivan
 *
 */
public interface IFileExporter extends Closeable {
	
	/**
	 * Do the actual export to file. 
	 * @param facets the facets to export.
	 * See the {@link com.github.grishberg.javascad.vrl.CSG#toFacets() CSG.toFacets()} method
	 * @throws IOException if any IO error happens during the export.
	 */
	void writeToFile(List<Facet> facets) throws IOException;
}
