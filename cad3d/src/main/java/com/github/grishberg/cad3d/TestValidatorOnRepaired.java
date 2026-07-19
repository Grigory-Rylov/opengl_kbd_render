package com.github.grishberg.cad3d;

import com.github.grishberg.javascad.StlValidator;
import com.github.grishberg.javascad.vrl.Facet;

import java.util.*;

/**
 * Test StlValidator on already-repaired STL to make sure it doesn't break anything.
 */
public class TestValidatorOnRepaired {
    public static void main(String[] args) throws Exception {
        // Read repaired STL
        List<Facet> repaired = new ArrayList<>(CompareStl.readStl("/tmp/matrix_right_repaired.stl"));
        System.out.println("Loaded repaired STL: " + repaired.size() + " facets");
        System.out.println("Naked edges before validateAndRepair: " + StlValidator.countNakedEdges(repaired));

        // Run validateAndRepair on already-repaired STL
        long start = System.currentTimeMillis();
        List<Facet> afterRepair = StlValidator.validateAndRepair(repaired);
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("\nAfter validateAndRepair: " + afterRepair.size() + " facets (took " + elapsed + "ms)");
        System.out.println("Naked edges after: " + StlValidator.countNakedEdges(afterRepair));
        System.out.println("Expected: 0 naked edges");
    }
}
