/**
 * Copyright Since 2005 ColdBox Framework by Luis Majano and Ortus Solutions, Corp
 * www.ortussolutions.com
 * ---
 * This is the WireBox configuration Binder.  You use it to configure an injector instance.
 * This binder will hold all your object mappings and injector settings.
 */
component accessors="true" {

	/**
	 * The current mapping pointer for DSL configurations
	 */
	property name="currentMapping" type="array";

	/**
	 * The configuration properties/settings for the app context or the injector context (standalone)
	 */
	property name="properties" type="struct";

	/**
	 * The injector reference this binder is bound to
	 */
	property name="injector";

	/**
	 * The ColdBox reference this binder is bound to, this can be null
	 */
	property name="coldbox";

	/**
	 * Main WireBox configuration structure
	 */
	property name="wirebox" type="struct";

	/**
	 * Main CacheBox configuration structure
	 */
	property name="cachebox" type="struct";

	/**
	 * The configuration CFC for this binder
	 */
	property name="config";

	/**
	 * The shortcut application mapping string
	 */
	property name="appMapping";

	/**
	 * The LogBox config file
	 */
	property name="logBoxConfig";

	/**
	 * The Listeners
	 */
	property name="listeners" type="array";

	/**
	 * The scope registration config
	 */
	property name="scopeRegistration" type="struct";

	/**
	 * The custom DSL namespaces
	 */
	property name="customDSL" type="struct";

	/**
	 * The custom scopes
	 */
	property name="customScopes" type="struct";

	/**
	 * The scan locations for this binder
	 */
	property name="scanLocations" type="struct";

	/**
	 * The collection of mappings
	 */
	property name="mappings" type="struct";

	/**
	 * The aspects binded to mappings
	 */
	property name="aspectBindings" type="array";

	/**
	 * The parent injector reference
	 */
	property name="oParentInjector";

	/**
	 * The stop recursions for the binder
	 */
	property name="aStopRecursions" type="array";

	/**
	 * The metadata cache for this binder
	 */
	property name="metadataCache";

	/**
	 * Boolean indicator if on startup all mappings will be processed for metadata inspections or
	 * lazy loaded. We default to lazy load due to performance.
	 */
	property name="autoProcessMappings" type="boolean";

	/**
	 * Enable/disable transient injetion cache
	 */
	property
		name   ="transientInjectionCache"
		type   ="boolean"
		default="true";

	/**
	 * The configuration DEFAULTS struct
	 */
	property
		name  ="DEFAULTS"
		setter="false"
		type  ="struct";

	/**
	 * --------------------------------------------------
	 * Binder Public References
	 * --------------------------------------------------
	 * One day move as static references
	 */

	// Binder Marker
	this.$wbBinder = true;

	// WireBox Operational Defaults
	variables.DEFAULTS = {
		// LogBox Default Config
		logBoxConfig      : "coldbox.system.ioc.config.LogBox",
		// Scope Registration
		scopeRegistration : { enabled : true, scope : "application", key : "wireBox" },
		// CacheBox Integration Defaults
		cacheBox          : {
			enabled        : false,
			configFile     : "",
			cacheFactory   : "",
			classNamespace : "coldbox.system.cache"
		},
		// Auto process mappings on startup
		// We lazy process all mappings until requested
		autoProcessMappings : false
	};

	// Startup the configuration
	reset();

	/**
	 * Constructor
	 *
	 * @injector   The injector this binder is bound to
	 * @config     A binder CFC, a binder CFC path or a raw struct configuration DSL. Leave blank if using this configuration object programmatically
	 * @properties A structure of binding properties to passthrough to the Binder Configuration CFC
	 */
	function init(
		required injector,
		config,
		struct properties = {}
	){
		// Setup incoming properties
		variables.properties = arguments.properties;
		// Setup Injector this binder is bound to.
		variables.injector   = arguments.injector;
		println( variables )
		return this;
	}

	/**
	 * The main configuration method that must be overridden by a specific WireBox Binder configuration object
	 */
	function configure(){
		// Implemented by concrete classes
	}

	/**
	 * Reset the configuration back to the original binder defaults
	 */
	Binder function reset(){
		// Contains the mappings currently being affected by the DSL.
		variables.currentMapping          = [];
		// Main wirebox structure
		variables.wirebox                 = {};
		// Custom DSL namespaces
		variables.customDSL               = {};
		// Custom Storage Scopes
		variables.customScopes            = {};
		// Package Scan Locations
		variables.scanLocations           = structNew( "ordered" );
		// Parent Injector Mapping
		variables.oParentInjector         = "";
		// Stop Recursion classes
		variables.aStopRecursions         = [];
		// Listeners
		variables.listeners               = [];
		// Object Mappings
		variables.mappings                = {};
		// Aspect Bindings
		variables.aspectBindings          = [];
		// Binding Properties
		variables.properties              = {};
		// Metadata cache
		variables.metadataCache           = "";
		// Transient injection cache
		variables.transientInjectionCache = true;

		return this;
	}




}