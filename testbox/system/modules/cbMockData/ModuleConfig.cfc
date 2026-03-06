/**
 * Copyright 2013 Ortus Solutions, Corp
 * www.ortussolutions.com
 * ---
 * Welcome to the world of Mocking Data
 */
component {

	// Module Properties
	this.title              = "cbMockData";
	this.author             = "Luis Majano";
	this.webURL             = "https://www.ortussolutions.com";
	this.description        = "cbMockData is a service to generate fake JSON data";
	// If true, looks for views in the parent first, if not found, then in the module. Else vice-versa
	this.viewParentLookup   = true;
	// If true, looks for layouts in the parent first, if not found, then in module. Else vice-versa
	this.layoutParentLookup = true;
	// Module Entry Point
	this.entryPoint         = "cbMockData";
	// Model Namespace
	this.modelNamespace     = "cbMockData";
	// CF Mapping
	this.cfmapping          = "cbMockData";

	/**
	 * Configure Module
	 */
	function configure(){
		// SES Routes
		routes = [
			// Module Entry Point
			{ pattern : "/", handler : "Main", action : "index" },
			// Convention Route
			{ pattern : "/:handler/:action?" }
		];
	}

	/**
	 * Fired when the module is registered and activated.
	 */
	function onLoad(){
	}

	/**
	 * Fired when the module is unregistered and unloaded
	 */
	function onUnload(){
	}

}
