package com.mcdaniel.swagger;

import java.util.Map;

import io.swagger.models.Model;
import io.swagger.models.Path;

public class SwaggerReturnData {

	private Map<String, Path> newPaths;
	private Map<String, Model> models;
	
	public Map<String, Path> getNewPaths() {
		return newPaths;
	}
	public void setNewPaths(Map<String, Path> newPaths) {
		this.newPaths = newPaths;
	}
	public Map<String, Model> getModels() {
		return models;
	}
	public void setModels(Map<String, Model> models) {
		this.models = models;
	}
}
