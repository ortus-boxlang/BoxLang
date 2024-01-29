/**
 * This is the module descriptor and entry point for your module in the Runtime.
 * The unique name of the moduel is the name of the directory on the modules folder.
 * A BoxLang Mapping will be created for you with the name of the module.
 *
 * A Module can have the following folders that will be automatically registered:
 * + bifs - Custom BIFs that will be registered into the runtime
 * + interceptors - Custom Interceptors that will be registered into the runtime via the configure() method
 * + libs - Custom Java libraries that your module leverages
 * + tags - Custom tags that will be registered into the runtime
 *
 * Every Module will have it's own ClassLoader that will be used to load the module libs and dependencies.
 */
component{

	/**
	 * --------------------------------------------------------------------------
	 * Module Properties
	 * --------------------------------------------------------------------------
	 * Here is where you define the properties of your module that the module service
	 * will use to register and activate your module
	 */

	/**
	 * Your module version. Try to use semantic versioning
	 * @mandatory
	 */
	this.version = "2.0.0";

	/**
	 * The BoxLang mapping for your module.  All BoxLang modules are registered with an internal
	 * mapping prefix of : bxModules.{this.mapping}, /bxmodules/{this.mapping}. Ex: bxModules.test, /bxmodules/test
	 */
	this.mapping = "test";

	/**
	 * Who built the module
	 */
	this.author = "Luis Majano";

	/**
	 * The module description
	 */
	this.description = "This module does amazing things";

	/**
	 * The module web URL
	 */
	this.webURL = "https://www.ortussolutions.com";

	/**
	 * This boolean flag tells the module service to skip the module registration/activation process.
	 */
	this.disabled = false;

	/**
	 * --------------------------------------------------------------------------
	 * Module Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Called by the ModuleService on module registration
	 */
	function configure(){
		/**
		 * Every module has a settings configuration object
		 */
		settings = {
			loadedOn : now(),
			loadedBy : "Luis Majano"
		};

		/**
		 * Every module can have a list of object mappings
		 * that can be created by boxLang.  This is a great way
		 * to create objects that can be used by the module
		 * or other modules.
		 * The mappings will be created in the following format:
		 * bxModules.{this.mapping}.{mappingName}
		 * Ex: bxModules.test.MyObject => bxModules.test.models.MyObject
		 */
		objectMappings = {
			// { name="MyObject", class="models.utilities.MyObject" }
		}

		/**
		 * Datasources can be defined by a module and they will be registered
		 * for you in the runtime
		 */
		datasources = {
			// { name="MyDSN", class="coldbox.system.datasources.ColdBoxDataSource", properties={dsn="mydsn"} }
		};

		/**
		 * The module interceptors to register into the runtime
		 */
		interceptors = [
			// { class="path.to.Interceptor", properties={} }
			{ class : "bxModules.test.interceptors.Listener", properties : {} }
		];

		/**
		 * A list of custom interception points to register into the runtime
		 */
		customInterceptionPoints = [ "onBxTestModule" ];
	}

	/**
	 * Called by the ModuleService on module activation
	 */
	function onLoad(){

	}

	/**
	 * Called by the ModuleService on module deactivation
	 */
	function onUnload(){

	}

	/**
	 * --------------------------------------------------------------------------
	 * Module Events
	 * --------------------------------------------------------------------------
	 * You can listen to any Runtime events by creating the methods
	 * that match the approved Runtime Interception Points
	 */

	 function onScopeCreation(){
		// do something
	 }
}
