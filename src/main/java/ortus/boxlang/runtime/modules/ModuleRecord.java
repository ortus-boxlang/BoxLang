/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.modules;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BoxLangBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * This class represents a module record
 */
public class ModuleRecord {

	/**
	 * Unique internal ID for the module
	 */
	public final String			id							= UUID.randomUUID().toString();

	/**
	 * The name of the module, defaults to the folder name on disk
	 */
	public Key					name;

	/**
	 * The version of the module, defaults to 1.0.0
	 */
	public String				version						= "1.0.0";

	/**
	 * The author of the module or empty if not set
	 */
	public String				author						= "";

	/**
	 * The description of the module or empty if not set
	 */
	public String				description					= "";

	/**
	 * The web URL of the module or empty if not set
	 */
	public String				webURL						= "";

	/**
	 * The BoxLang mapping of the module used to construct classes from within it.
	 * All mappings have a prefix of {@link ModuleService#MODULE_MAPPING_INVOCATION_PREFIX}
	 *
	 */
	public String				mapping						= "";

	/**
	 * If the module is disabled for activation, defaults to false
	 */
	public boolean				disabled					= false;

	/**
	 * Flag to indicate if the module has been activated or not yet
	 */
	public boolean				activated					= false;

	/**
	 * The settings of the module
	 */
	public Struct				settings					= new Struct();

	/**
	 * The object mappings of the module
	 */
	public Struct				objectMappings				= new Struct();

	/**
	 * The datasources to register by the module
	 */
	public Struct				datasources					= new Struct( Struct.TYPES.LINKED );

	/**
	 * The interceptors of the module
	 */
	public Array				interceptors				= new Array();

	/**
	 * The BIFS of the module
	 */
	public Array				bifs						= new Array();

	/**
	 * The custom interception points of the module
	 */
	public Array				customInterceptionPoints	= new Array();

	/**
	 * The physical path of the module on disk as a Java {@link Path}
	 */
	public Path					physicalPath;

	/**
	 * The physical path of the module but in string format. Used by BoxLang code mostly
	 * Same as the {@link ModuleRecord#physicalPath} but in string format
	 */
	public String				path;

	/**
	 * The invocation path of the module which is a composition of the
	 * {@link ModuleService#MODULE_MAPPING_INVOCATION_PREFIX} and the module name.
	 * Example: {@code /bxModules/MyModule} is the mapping for the module
	 * the invocation path would be {@code bxModules.MyModule}
	 */
	public String				invocationPath				= "";

	/**
	 * The timestamp when the module was registered
	 */
	public Instant				registeredOn;

	/**
	 * The time it took in ms to register the module
	 */
	public long					registrationTime			= 0;

	/**
	 * The timestamp when the module was activated
	 */
	public Instant				activatedOn;

	/**
	 * The time it took in ms to activate the module
	 */
	public long					activationTime				= 0;

	/**
	 * The class loader for the module
	 */
	public ClassLoader			classLoader					= null;

	/**
	 * The descriptor for the module
	 */
	public IClassRunnable		moduleConfig;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This prefix is used a virtual package name for the module
	 */
	private static final String	MODULE_PACKAGE_NAME			= "ortus.boxlang.runtime.modules.";

	/**
	 * Moudule Logger
	 */
	private static final Logger	logger						= LoggerFactory.getLogger( ModuleRecord.class );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name         The name of the module
	 * @param physicalPath The physical path of the module
	 */
	public ModuleRecord( Key name, String physicalPath ) {
		// Beautiful name
		this.name			= name;
		// Path to the module in string and Path formats
		this.path			= physicalPath;
		this.physicalPath	= Paths.get( physicalPath );
		// Register the automatic mapping by convention: /bxModules/{name}
		this.mapping		= ModuleService.MODULE_MAPPING_PREFIX + name.getName();
		// Register the invocation path by convention: bxModules.{name}
		this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + name.getName();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Loaders
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Load the ModuleConfig.bx from disk, construct it and store it
	 * Then populate the module record with the information from the descriptor
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord loadDescriptor( IBoxContext context ) {
		Path	descriptorPath	= physicalPath.resolve( ModuleService.MODULE_DESCRIPTOR );
		String	packageName		= MODULE_PACKAGE_NAME + this.name.getNameNoCase() + EncryptionUtil.hash( Instant.now() + id + physicalPath.toString() );

		// Load the Class, Construct it and store it
		this.moduleConfig = ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass( descriptorPath, packageName, context )
		).invokeConstructor( context )
		    .getTargetInstance();

		// Nice References
		ThisScope		thisScope		= this.moduleConfig.getThisScope();
		VariablesScope	variablesScope	= this.moduleConfig.getVariablesScope();

		// Store the descriptor information into the record
		this.version		= ( String ) thisScope.getOrDefault( Key.version, "1.0.0" );
		this.author			= ( String ) thisScope.getOrDefault( Key.author, "" );
		this.description	= ( String ) thisScope.getOrDefault( Key.description, "" );
		this.webURL			= ( String ) thisScope.getOrDefault( Key.webURL, "" );
		this.disabled		= ( Boolean ) thisScope.getOrDefault( Key.disabled, false );

		// Do we have a custom mapping to override?
		// If so, recalculate it
		if ( thisScope.containsKey( Key.mapping ) &&
		    thisScope.get( Key.mapping ) instanceof String castedMapping &&
		    !castedMapping.isBlank() ) {
			this.mapping		= ModuleService.MODULE_MAPPING_PREFIX + castedMapping;
			this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + castedMapping;
		}

		// Verify the internal config structures exist, else default them
		variablesScope.computeIfAbsent( Key.settings, k -> new Struct() );
		variablesScope.computeIfAbsent( Key.objectMappings, k -> new Struct() );
		variablesScope.computeIfAbsent( Key.datasources, k -> new Struct( Struct.TYPES.LINKED ) );
		variablesScope.computeIfAbsent( Key.interceptors, k -> Array.of() );
		variablesScope.computeIfAbsent( Key.customInterceptionPoints, k -> Array.of() );

		/**
		 * --------------------------------------------------------------------------
		 * DI Injections
		 * --------------------------------------------------------------------------
		 * Inject the following references into the CFC
		 * - moduleRecord : The ModuleRecord instance
		 * - boxRuntime : The BoxRuntime instance
		 * - log : A logger for the module config itself
		 */

		variablesScope.put( Key.moduleRecord, this );
		variablesScope.put( Key.boxRuntime, BoxRuntime.getInstance() );
		variablesScope.put( Key.log, LoggerFactory.getLogger( this.moduleConfig.getClass() ) );

		return this;
	}

	/**
	 * This method registers the module with all the runtime services.
	 * This is called by the ModuleService if the module is allowed to be registered or not
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord register( IBoxContext context ) {
		// Convenience References
		ThisScope		thisScope		= this.moduleConfig.getThisScope();
		VariablesScope	variablesScope	= this.moduleConfig.getVariablesScope();
		FunctionService	functionService	= BoxRuntime.getInstance().getFunctionService();

		// Register the module mapping in the runtime
		// Called first in case this is used in the `configure` method
		BoxRuntime
		    .getInstance()
		    .getConfiguration().runtime.registerMapping( this.mapping, this.path );

		// Call the configure() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.configure ) ) {
			this.moduleConfig.dereferenceAndInvoke(
			    context,
			    Key.configure,
			    new Object[] {},
			    false
			);
		}

		// Register Module configurations now that they are set
		this.settings					= ( Struct ) variablesScope.getAsStruct( Key.settings );
		this.interceptors				= variablesScope.getAsArray( Key.interceptors );
		this.customInterceptionPoints	= variablesScope.getAsArray( Key.customInterceptionPoints );
		this.objectMappings				= ( Struct ) variablesScope.getAsStruct( Key.objectMappings );
		this.datasources				= ( Struct ) variablesScope.getAsStruct( Key.datasources );

		// Register Interception points with the InterceptorService
		if ( !this.customInterceptionPoints.isEmpty() ) {
			BoxRuntime
			    .getInstance()
			    .getInterceptorService()
			    .registerInterceptionPoint( this.customInterceptionPoints.stream().map( Key::of ).toArray( Key[]::new ) );
		}

		// Register Bifs if they exist on disk
		// TODO: Make it pretty
		Path bifsPath = this.physicalPath.resolve( ModuleService.MODULE_BIFS );
		if ( bifsPath.toFile().exists() ) {
			// Iterate over all files *.cfc/bx and register them
			for ( File targetFile : bifsPath.toFile().listFiles() ) {

				// System.out.println( "Processing " + targetFile.getAbsolutePath() );

				// Skip directories
				if ( targetFile.isDirectory() ) {
					continue;
				}

				// Skip non CFC/BX files
				if ( !targetFile.getName().matches( "^.*\\.(cfc|bx)$" ) ) {
					continue;
				}

				Key				className	= Key.of( FilenameUtils.getBaseName( targetFile.getAbsolutePath() ) );
				IClassRunnable	runnable	= ( IClassRunnable ) DynamicObject.of(
				    RunnableLoader.getInstance().loadClass( targetFile.toPath(), this.invocationPath + "." + ModuleService.MODULE_BIFS, context )
				).invokeConstructor( context )
				    .getTargetInstance();

				// System.out.println( "Registering BIF " + className + " from " + runnable.getClass().getName() );

				functionService.registerGlobalFunction(
				    new BIFDescriptor(
				        className,
				        runnable.getClass(),
				        this.name.getName(),
				        null,
				        true,
				        new BoxLangBIF( runnable )
				    ),
				    true
				);
				this.bifs.push( className );
			}
		}

		// Finalize
		this.registeredOn = Instant.now();

		return this;
	}

	/**
	 * Unload the module from the runtime
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord unload( IBoxContext context ) {
		// Convenience References
		ThisScope			thisScope			= this.moduleConfig.getThisScope();
		InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

		// Call the onLoad() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.onUnload ) ) {
			try {
				this.moduleConfig.dereferenceAndInvoke(
				    context,
				    Key.onUnload,
				    new Object[] {},
				    false
				);
			} catch ( Exception e ) {
				logger.error( "Error while unloading module [{}]", this.name, e );
			}
		}

		// Unregister all interceptors from all states
		if ( !this.interceptors.isEmpty() ) {
			for ( Object interceptor : this.interceptors ) {
				IStruct interceptorRecord = ( IStruct ) interceptor;
				interceptorService.unregister( DynamicObject.of( interceptorRecord.getAsString( Key.interceptor ) ) );
			}
		}

		// Unregister the ModuleConfig
		interceptorService.unregister( DynamicObject.of( this.moduleConfig ) );

		return this;
	}

	/**
	 * This method activates the module in the runtime.
	 * Called by the ModuleService if the module is allowed to be activated or not
	 *
	 * @param context The current context of execution
	 *
	 * @throws BoxRuntimeException If an interceptor record is missing the [class] which is mandatory
	 * @throws BoxRuntimeException If an interceptor class is not found locally or with any mappings
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord activate( IBoxContext context ) {
		// Convenience References
		ThisScope			thisScope			= this.moduleConfig.getThisScope();
		InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

		/**
		 * --------------------------------------------------------------------------
		 * Register the ModuleConfig as an Interceptor
		 * --------------------------------------------------------------------------
		 */
		interceptorService.register( this.moduleConfig );

		/**
		 * --------------------------------------------------------------------------
		 * Register module Interceptors
		 * --------------------------------------------------------------------------
		 */
		if ( !this.interceptors.isEmpty() ) {
			for ( Object interceptor : this.interceptors ) {
				IStruct interceptorRecord = ( IStruct ) interceptor;
				// Verify the class else throw an exception
				if ( !interceptorRecord.containsKey( Key._CLASS ) ) {
					throw new BoxRuntimeException( "Interceptor record is missing the [class] key which is mandatory" );
				}
				// Quick Ref
				String interceptorClass = interceptorRecord.getAsString( Key._CLASS );
				// Default Properties struct
				interceptorRecord.computeIfAbsent( Key.properties, k -> new Struct() );
				// The default name is the class name + @ + the module name
				interceptorRecord.computeIfAbsent( Key._NAME, k -> interceptorClass + "@" + this.name );
				// Create and Register
				interceptorRecord.put(
				    Key.interceptor,
				    interceptorService.newAndRegister(
				        interceptorClass,
				        interceptorRecord.getAsStruct( Key.properties ),
				        interceptorRecord.getAsString( Key._NAME ),
				        this
				    )
				);
			}
		}

		/**
		 * --------------------------------------------------------------------------
		 * onLoad()
		 * --------------------------------------------------------------------------
		 */
		// Call the onLoad() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.onLoad ) ) {
			this.moduleConfig.dereferenceAndInvoke(
			    context,
			    Key.onLoad,
			    new Object[] {},
			    false
			);
		}

		// Finalize
		this.activated		= true;
		this.activatedOn	= Instant.now();

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * If the module is disabled for activation
	 *
	 * @return {@code true} if the module is disabled for activation, {@code false} otherwise
	 */
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * If the module is activated
	 *
	 * @return {@code true} if the module is activated, {@code false} otherwise
	 */
	public boolean isActivated() {
		return activated;
	}

	/**
	 * Get a string representation of the module record
	 */
	public String toString() {
		return asStruct().toString();
	}

	/**
	 * Get a struct representation of the module record
	 *
	 * @return A struct representation of the module record
	 */
	public IStruct asStruct() {
		return Struct.of(
		    "activatedOn", activatedOn,
		    "activationTime", activationTime,
		    "activated", activated,
		    "author", author,
		    "customInterceptionPoints", Array.copyOf( customInterceptionPoints ),
		    "description", description,
		    "disabled", disabled,
		    "Id", id,
		    "interceptors", Array.copyOf( interceptors ),
		    "invocationPath", invocationPath,
		    "mapping", mapping,
		    "name", name,
		    "physicalPath", physicalPath.toString(),
		    "registeredOn", registeredOn,
		    "registrationTime", registrationTime,
		    "settings", settings,
		    "version", version,
		    "webURL", webURL
		);
	}

}
