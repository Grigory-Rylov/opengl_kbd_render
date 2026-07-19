package com.github.grishberg.javascad.vrl.export;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import com.github.grishberg.javascad.enums.OutputFormat;
import com.github.grishberg.javascad.vrl.Facet;
import com.github.grishberg.javascad.vrl.Vertex;

/**
 * An IFileExporter implementation for the textual STL file format. The usual extension of the file
 * is .stl. The exported file does not contain the color information of the objects.
 * @author ivivan <ivivan@printingin3d.eu>
 */
public class StlTextFile implements IFileExporter {
	private final OutputStream stream;
	
	/**
	 * Constructs the object with the given stream.
	 * @param stream the stream to write to
	 */
	public StlTextFile(OutputStream stream) {
		this.stream = stream;
	}

	@Override
	public void writeToFile(List<Facet> facets) throws IOException {
		try (PrintStream ps = new PrintStream(stream)) { 
	        ps.println("solid v3d.csg");
	        for (Facet facet : facets) {
	        	ps.println(facetToStlString(facet));
	        }
	        ps.println("endsolid v3d.csg");
		}
	}
	
	private static String facetToStlString(Facet facet) {
		StringBuilder sb = new StringBuilder().
	        append("  facet normal ").append(facet.getNormal().format(OutputFormat.STL)).append('\n').
	        append("    outer loop\n");

		for (Vertex v : facet.getVertexes()) {
	        sb.append("    vertex ").append(v.format(OutputFormat.STL)).append('\n');
		}
		
		return sb.append("    endloop\n").append("  endfacet").toString();
	}

	@Override
	public void close() throws IOException {
		stream.close();
	}
	
}
