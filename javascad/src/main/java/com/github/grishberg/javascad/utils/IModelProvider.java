package com.github.grishberg.javascad.utils;

import java.util.List;

import com.github.grishberg.javascad.models.Model;

/**
 * Represents a complex 3D object, which assembled by several different parts. This interface provides 
 * access to the assembled object, the parts of the object and the model paths to save the parts.
 * @author Ivan
 *
 */
public interface IModelProvider {
	/**
	 * Returns the assembled object. The object should be assembled using the parts 
	 * returned by getParts method.
	 * @return the assembled object
	 */
	Model getAssembledModel();
	
	/**
	 * The parts of the object with the path for them.
	 * @return the models and paths
	 */
	List<ModelWithPath> getModelsAndPaths();
}
