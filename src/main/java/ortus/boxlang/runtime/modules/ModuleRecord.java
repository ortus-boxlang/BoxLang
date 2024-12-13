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
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BoxLangBIFProxy;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.components.BoxLangComponentProxy;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.Component.ComponentBody;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.jdbc.drivers.DriverShim;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.IService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DynamicFunction;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.RegexBuilder;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
	public String				mapping;

	/**
	 * If the module is enabled for activation, defaults to false
	 */
	public boolean				enabled						= true;

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
	 * The BIFS collaborated by the module
	 */
	public Array				bifs						= new Array();

	/**
	 * The Components collaborated by the module
	 */
	public Array				components					= new Array();

	/**
	 * The member Methods collaborated by the module
	 */
	public Array				memberMethods				= new Array();

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
	public String				invocationPath;

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
	 * The Dynamic class loader for the module
	 */
	public DynamicClassLoader	classLoader					= null;

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
	 * The name of the descriptor file for the module based on CommandBox
	 */
	private static final String	MODULE_CONFIG_FILE			= "box.json";

	/**
	 * Logger
	 */
	private BoxLangLogger		logger;

	/**
	 * Runtime
	 */
	private BoxRuntime			runtime;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param physicalPath The physical path of the module
	 */
	public ModuleRecord( String physicalPath ) {
		this.runtime	= BoxRuntime.getInstance();
		this.logger		= this.runtime.getLoggingService().getLogger( "modules" );

		Path	directoryPath	= Path.of( physicalPath );
		Path	boxjsonPath		= directoryPath.resolve( MODULE_CONFIG_FILE );

		// Load the module name from the box.json file if it exists
		if ( Files.exists( boxjsonPath ) ) {
			DataNavigator
			    .of( boxjsonPath )
			    .from( "boxlang" )
			    .ifPresent( "moduleName", value -> this.name = Key.of( value ) )
			    .ifPresent( "minimumVersion",
			        value -> this.runtime.getModuleService().verifyModuleAndBoxLangVersion( ( String ) value, directoryPath )
			    );
		}

		// Default to the directory name if the box.json file does not exist
		if ( this.name == null ) {
			this.name = Key.of( directoryPath.getFileName().toString() );
		}
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
		String	packageName		= MODULE_PACKAGE_NAME + this.name.getNameNoCase() + EncryptionUtil.hash( physicalPath.toString() );

		// Load the Class, Construct it and store it
		this.moduleConfig = ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            null,
		            null,
		            packageName.replace( ".", Matcher.quoteReplacement( File.separator ) ) + File.separator + ModuleService.MODULE_DESCRIPTOR,
		            descriptorPath
		        ),
		        context
		    )
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
		this.enabled		= ( Boolean ) thisScope.getOrDefault( Key.enabled, true );

		// Verify if we disabled the loading of the module in the runtime config
		if ( this.runtime.getConfiguration().modules.containsKey( this.name ) ) {
			ModuleConfig config = ( ModuleConfig ) this.runtime.getConfiguration().modules.get( this.name );
			this.enabled = config.enabled;
		}

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

		/*
		 * --------------------------------------------------------------------------
		 * DI Injections
		 * --------------------------------------------------------------------------
		 * Inject the following references into the class
		 * - moduleRecord : The ModuleRecord instance
		 * - boxRuntime : The BoxRuntime instance
		 * - interceptorService : The BoxLang InterceptorService
		 * - log : A logger for the module config itself
		 */

		variablesScope.put( Key.moduleRecord, this );
		variablesScope.put( Key.boxRuntime, this.runtime );
		variablesScope.put( Key.interceptorService, this.runtime.getInterceptorService() );
		variablesScope.put( Key.log, this.logger );

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
		ThisScope			thisScope			= this.moduleConfig.getThisScope();
		VariablesScope		variablesScope		= this.moduleConfig.getVariablesScope();
		InterceptorService	interceptorService	= this.runtime.getInterceptorService();
		FunctionService		functionService		= this.runtime.getFunctionService();
		ComponentService	componentService	= this.runtime.getComponentService();

		// Register the module mapping in the this.runtime
		// Called first in case this is used in the `configure` method
		this.runtime.getConfiguration().registerMapping( this.mapping, this.path );

		// Create the module class loader and seed it with the physical path to the module
		// This traverses the module and looks for *.class files to load (NOT JARs)
		// Using the `modules.{module_name}` package prefix
		// This is important for module developers to include this as their package prefix.
		try {
			this.classLoader = new DynamicClassLoader(
			    this.name,
			    this.physicalPath.toUri().toURL(),
			    this.runtime.getRuntimeLoader(),
			    false
			);
		} catch ( MalformedURLException e ) {
			this.logger.error( "Error creating module [{}] class loader.", this.name, e );
			throw new BoxRuntimeException( "Error creating module [" + this.name + "] class loader", e );
		}

		// Do we have libs to add to the class loader? These are jars ONLY
		// All dependencies on Java libs must be on this folder as JARs
		Path libsPath = this.physicalPath.resolve( ModuleService.MODULE_LIBS );
		if ( Files.exists( libsPath ) && Files.isDirectory( libsPath ) ) {
			try {
				this.classLoader.addURLs( DynamicClassLoader.getJarURLs( libsPath ) );
			} catch ( IOException e ) {
				this.logger.error( "Error while seeding the module [{}] class loader with the libs folder.", this.name, e );
				throw new BoxRuntimeException( "Error while seeding the module [" + this.name + "] class loader with the libs folder", e );
			}
		}

		// Call the configure() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.configure ) ) {
			this.moduleConfig.dereferenceAndInvoke(
			    context,
			    Key.configure,
			    DynamicObject.EMPTY_ARGS,
			    false
			);
		}

		// Register descriptor configurations into the record
		this.settings = ( Struct ) variablesScope.getAsStruct( Key.settings );

		// Append any module settings found in the runtime configuration
		if ( this.runtime.getConfiguration().modules.containsKey( this.name ) ) {
			ModuleConfig config = ( ModuleConfig ) this.runtime.getConfiguration().modules.get( this.name );
			StructUtil.deepMerge( this.settings, config.settings, true );
		}

		this.interceptors				= variablesScope.getAsArray( Key.interceptors );
		this.customInterceptionPoints	= variablesScope.getAsArray( Key.customInterceptionPoints );
		this.objectMappings				= ( Struct ) variablesScope.getAsStruct( Key.objectMappings );
		this.datasources				= ( Struct ) variablesScope.getAsStruct( Key.datasources );

		// Register Interception points with the InterceptorService
		if ( !this.customInterceptionPoints.isEmpty() ) {
			interceptorService.registerInterceptionPoint( this.customInterceptionPoints.stream().map( Key::of ).toArray( Key[]::new ) );
		}

		// Register BoxLang Bifs if they exist
		Path bifsPath = this.physicalPath.resolve( ModuleService.MODULE_BIFS );
		if ( Files.exists( bifsPath ) && Files.isDirectory( bifsPath ) ) {
			// Iterate over all files *.cfc/bx and register them
			// These are the BoxLang Bifs
			for ( File targetFile : bifsPath.toFile().listFiles() ) {
				registerBIF( targetFile, context );
			}
		}

		// Register BoxLang Components if they exists
		Path componentPaths = this.physicalPath.resolve( ModuleService.MODULE_COMPONENTS );
		if ( Files.exists( componentPaths ) && Files.isDirectory( componentPaths ) ) {
			// Iterate over all files *.cfc/bx and register them
			for ( File targetFile : componentPaths.toFile().listFiles() ) {
				registerComponent( targetFile, context );
			}
		}

		// Register any global services
		ServiceLoader.load( IService.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( service -> this.runtime.putGlobalService( service.getName(), service ) );

		// Load any JDBC drivers into the JVM
		ServiceLoader.load( Driver.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> {
			    try {
				    DriverManager.registerDriver( new DriverShim( driver ) );
			    } catch ( SQLException e ) {
				    throw new BoxRuntimeException( e.getMessage() );
			    }
		    } );

		// Load any BoxLang IJDBC Driver classes
		ServiceLoader.load( IJDBCDriver.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> this.runtime.getDataSourceService().registerDriver( driver ) );

		// Do we have any Java BIFs to load?
		ServiceLoader.load( BIF.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( clazz -> functionService.processBIFRegistration( clazz, null, this.name.getName() ) );

		// Do we have any Java Component Tags to load?
		ServiceLoader.load( Component.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( targetClass -> componentService.registerComponent( targetClass, null, this.name.getName() ) );

		// Do we have any Java Schedulers to register in the SchedulerService
		ServiceLoader.load( IScheduler.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( scheduler -> this.runtime.getSchedulerService().loadScheduler( Key.of( scheduler.getSchedulerName() + "@" + this.name ), scheduler ) );

		// Do we have any Java ICacheProviders to register in the CacheService
		ServiceLoader.load( ICacheProvider.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( provider -> this.runtime.getCacheService().registerProvider( Key.of( provider.getSimpleName() ), provider ) );

		// Do we have any Java IInterceptor to register in the InterceptorService
		ServiceLoader.load( IInterceptor.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( interceptorService::register );

		// Finalize Registration
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
		InterceptorService	interceptorService	= this.runtime.getInterceptorService();

		// Call the onLoad() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.onUnload ) ) {
			try {
				this.moduleConfig.dereferenceAndInvoke(
				    context,
				    Key.onUnload,
				    DynamicObject.EMPTY_ARGS,
				    false
				);
			} catch ( Exception e ) {
				this.logger.error( "Error while unloading module [{}]", this.name, e );
			}
		}

		// Unregister all interceptors from all states
		if ( !this.interceptors.isEmpty() ) {
			for ( Object interceptor : this.interceptors ) {
				IStruct			interceptorRecord	= ( IStruct ) interceptor;
				IClassRunnable	interceptorInstance	= ( IClassRunnable ) interceptorRecord.get( Key.interceptor );
				if ( interceptorInstance != null ) {
					interceptorService.unregister( DynamicObject.of( interceptorInstance ) );
				}
			}
		}

		// Unregister the ModuleConfig
		interceptorService.unregister( DynamicObject.of( this.moduleConfig ) );

		// Destroy the ClassLoader
		try {
			this.classLoader.close();
		} catch ( IOException e ) {
			this.logger.error( "Error while closing the DynamicClassLoader for module [{}]", this.name, e );
		} finally {
			this.classLoader = null;
		}

		return this;
	}

	/**
	 * Find a class in the module class loader first and then the parent.
	 *
	 * @param className The name of the class to find in the module's libs
	 * @param safe      Whether to throw an exception if the class is not found
	 * @param context   The current context of execution
	 *
	 * @return The class if found, null otherwise
	 *
	 * @throws ClassNotFoundException If the class is not found
	 */
	public Class<?> findModuleClass( String className, Boolean safe, IBoxContext context ) throws ClassNotFoundException {
		return this.classLoader.findClass( className, safe );
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
		InterceptorService	interceptorService	= this.runtime.getInterceptorService();

		/*
		 * --------------------------------------------------------------------------
		 * Register the ModuleConfig as an Interceptor
		 * --------------------------------------------------------------------------
		 */
		interceptorService.register( this.moduleConfig );

		/*
		 * --------------------------------------------------------------------------
		 * Register module BoxLang Interceptors
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

		/*
		 * --------------------------------------------------------------------------
		 * onLoad()
		 * --------------------------------------------------------------------------
		 */
		// Call the onLoad() method if it exists in the descriptor
		if ( thisScope.containsKey( Key.onLoad ) ) {
			this.moduleConfig.dereferenceAndInvoke(
			    context,
			    Key.onLoad,
			    DynamicObject.EMPTY_ARGS,
			    false
			);
		}

		// Finalize
		this.activated		= true;
		this.activatedOn	= Instant.now();

		return this;
	}

	/**
	 * Execute the module via the BoxRunner
	 *
	 * @param context The current context of execution
	 * @param args    The arguments to pass to the module
	 *
	 * @throws BoxRuntimeException If the module is not executable, meaning it doesn't have a main method
	 */
	public void execute( IBoxContext context, String[] args ) {
		ThisScope thisScope = this.moduleConfig.getThisScope();

		if ( !thisScope.containsKey( Key.main ) ) {
			throw new BoxRuntimeException( "Module " + this.id + " is not executable. It must have a 'main' method" );
		}

		this.moduleConfig.dereferenceAndInvoke(
		    context,
		    Key.main,
		    new Object[] { Array.fromArray( args ) },
		    false
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * If the module is enabled for activation
	 *
	 * @return {@code true} if the module is enabled for activation, {@code false} otherwise
	 */
	public boolean isEnabled() {
		return enabled;
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
		    "enabled", enabled,
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

	/**
	 * --------------------------------------------------------------------------
	 * Private Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register a BoxLang based Component with the runtime
	 *
	 * @param targetFile The target file to register that represents the Component on disk
	 * @param context    The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	private ModuleRecord registerComponent( File targetFile, IBoxContext context ) {
		// System.out.println( "Processing component: " + targetFile.getAbsolutePath() );

		// Skip directories and non CFC/BX files
		// We are not doing recursive registration for the moment.
		if ( targetFile.isDirectory() || !RegexBuilder.of( targetFile.getName(), RegexBuilder.CFC_OR_BX_FILE ).matches() ) {
			return this;
		}

		// Nice References
		ComponentService		componentService	= this.runtime.getComponentService();
		// Try to load the BoxLang class and proxy
		Key						className			= Key.of( FilenameUtils.getBaseName( targetFile.getAbsolutePath() ) );
		IClassRunnable			oComponent			= loadClassRunnable( targetFile, ModuleService.MODULE_COMPONENTS, context );
		BoxLangComponentProxy	oComponentProxy		= new BoxLangComponentProxy( oComponent );
		oComponentProxy.setName( className );

		// Inject some helpers
		oComponent.getVariablesScope().put( Key.newBuffer, new DynamicFunction(
		    Key.newBuffer,
		    ( context1, fnc ) -> new StringBuffer()
		) );
		oComponent.getVariablesScope().put( Key.newBuilder, new DynamicFunction(
		    Key.newBuilder,
		    ( context1, fnc ) -> new StringBuilder()
		) );

		// ProcessBody Delegate
		oComponent.getVariablesScope().put( Key.processBody, new DynamicFunction(
		    Key.processBody,
		    ( context1, fnc ) -> {
			    ArgumentsScope args	= context1.getArgumentsScope();

			    Object		buffer	= args.get( Key.buffer );

			    return oComponentProxy.processBody(
			        ( IBoxContext ) args.get( Key.context ),
			        ( ComponentBody ) args.get( Key.body ),
			        buffer instanceof StringBuffer ? ( StringBuffer ) buffer : context.getBuffer()
			    );
		    },
		    new Argument[] {
		        new Argument( true, "any", Key.context ),
		        new Argument( true, "any", Key.body ),
		        new Argument( true, "any", Key.buffer )
		    }
		) );
		// Get Name Delegate
		oComponent.getVariablesScope().put( Key.getName, new DynamicFunction(
		    Key.getName,
		    ( context1, fnc ) -> oComponent.bxGetName()
		) );

		/**
		 * --------------------------------------------------------------------------
		 * Component Registration
		 * --------------------------------------------------------------------------
		 */

		IStruct				annotations			= oComponent.getBoxMeta().getMeta().getAsStruct( Key.annotations );
		ComponentDescriptor	descriptor			= new ComponentDescriptor(
		    oComponent.bxGetName(),
		    oComponent.getClass(),
		    this.name.getName(),
		    null,
		    oComponentProxy,
		    BooleanCaster.cast( annotations.getOrDefault( "AllowsBody", false ) ),
		    BooleanCaster.cast( annotations.getOrDefault( "RequiresBody", false ) )
		);
		Key[]				componentAliases	= buildAnnotationAliases( oComponent, className, Key.boxComponent );

		// Register all components with their aliases
		for ( Key thisAlias : componentAliases ) {
			componentService.registerComponent( descriptor, thisAlias, true );
			this.logger.info(
			    "> Registered Module [{}] Component [{}] with alias [{}]",
			    this.name.getName(),
			    className.getName(),
			    thisAlias.getName()
			);
			this.components.push( thisAlias );
		}

		return this;
	}

	/**
	 * Register a BoxLang based BIF with the runtime
	 *
	 * @param targetFile The target file to register that represents the BIF on disk
	 * @param context    The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	private ModuleRecord registerBIF( File targetFile, IBoxContext context ) {
		// System.out.println( "Processing BIF: " + targetFile.getAbsolutePath() );

		// Skip directories and non CFC/BX files
		// We are not doing recursive registration for the moment.
		if ( targetFile.isDirectory() || !RegexBuilder.of( targetFile.getName(), RegexBuilder.CFC_OR_BX_FILE ).matches() ) {
			return this;
		}

		// Nice References
		FunctionService	functionService	= this.runtime.getFunctionService();
		// Try to load the BoxLang class
		Key				className		= Key.of( FilenameUtils.getBaseName( targetFile.getAbsolutePath() ) );
		IClassRunnable	oBIF			= loadClassRunnable( targetFile, ModuleService.MODULE_BIFS, context );

		/**
		 * --------------------------------------------------------------------------
		 * BIF Registration
		 * --------------------------------------------------------------------------
		 */
		BIFDescriptor	bifDescriptor	= new BIFDescriptor(
		    className,
		    oBIF.getClass(),
		    this.name.getName(),
		    null,
		    true,
		    new BoxLangBIFProxy( oBIF )
		);
		Key[]			bifAliases		= buildAnnotationAliases( oBIF, className, Key.boxBif );
		for ( Key bifAlias : bifAliases ) {
			// Register the mapping in the runtime
			functionService.registerGlobalFunction(
			    bifDescriptor,
			    bifAlias,
			    true
			);
			this.logger.info(
			    "> Registered Module [{}] BIF [{}] with alias [{}]",
			    this.name.getName(),
			    className.getName(),
			    bifAlias.getName()
			);
			this.bifs.push( bifAlias );
		}

		/**
		 * --------------------------------------------------------------------------
		 * BIF Member Method(s) Registration
		 * --------------------------------------------------------------------------
		 */
		Array bifMemberMethods = discoverMemberMethods( oBIF, className );
		for ( Object memberMethod : bifMemberMethods ) {
			Key			memberKey		= Key.of( ( ( IStruct ) memberMethod ).getAsString( Key._NAME ) );
			BoxLangType	memberType		= ( BoxLangType ) ( ( IStruct ) memberMethod ).get( Key.type );
			String		objectArgument	= ( ( IStruct ) memberMethod ).getAsString( Key.objectArgument );

			// Call to register
			functionService.registerMemberMethod(
			    memberKey,
			    new MemberDescriptor(
			        memberKey,
			        memberType,
			        java.lang.Object.class,
			        // Pass null if objectArgument is empty
			        objectArgument.isEmpty() ? null : Key.of( objectArgument ),
			        bifDescriptor
			    )
			);
			this.logger.info(
			    "> Registered Module [{}] MemberMethod [{}]",
			    this.name.getName(),
			    memberMethod
			);
			this.memberMethods.push( memberMethod );
		}

		return this;
	}

	/**
	 * Discover member methods by getting the {@code BoxMember} annotation on the Class.
	 *
	 * @param targetBIF The target BIF to discover member methods for
	 * @param className The class name of the BIF
	 *
	 * @return An array of member methods for the BIF: {@code [ { name : "", objectArgument: "", type : BoxLangType } ] }
	 */
	private Array discoverMemberMethods( IClassRunnable targetBIF, Key className ) {
		// Get the BoxMember annotation
		Object boxMembers = targetBIF.getBoxMeta().getMeta().getAsStruct( Key.annotations ).getOrDefault( Key.boxMember, null );

		// System.out.println( className.getName() + " BoxMembers Found [" + boxMembers + "]" );

		// Case 0: If null, then we don't have any :)
		if ( boxMembers == null ) {
			return new Array();
		}

		// Case 1 : This is a simple String with no value, throw an exception
		// @BoxMember
		if ( boxMembers instanceof String castedBoxMember && castedBoxMember.isBlank() ) {
			throw new BoxRuntimeException( className.getName() + " BoxMember annotation is missing it's type value, which is mandatory" );
		}

		// Case 2 : This is a simple String with a value which is the type. Validate it, default it's record and return it
		// ClassName : ArrayFoo
		// @BoxMember "array" -> { "name": "foo", "objectArgument": null, type: BoxLangType.ARRAY }
		if ( boxMembers instanceof String castedBoxMember && !castedBoxMember.isBlank() ) {
			// Validate the type is valid else throw an exception
			if ( !BoxLangType.isValid( castedBoxMember ) ) {
				throw new BoxRuntimeException(
				    className.getName() + " BoxMember annotation has an invalid type value [" + castedBoxMember + "]" +
				        "Valid types are: " + Arrays.toString( BoxLangType.values() )
				);
			}
			BoxLangType boxType = BoxLangType.valueOf( castedBoxMember.toUpperCase() );
			return Array.of(
			    Struct.of(
			        // Default member name for class ArrayFoo with BoxType of Array is just foo()
			        Key._NAME, className.getNameNoCase().replace( boxType.getKey().getNameNoCase(), "" ),
			        Key.objectArgument, "",
			        Key.type, boxType
			    )
			);
		}

		// Case 3 : We have a struct of member methods, validate them and return them
		// @BoxMember { "string": { "name": "append", "objectArgument": "string" } }
		if ( boxMembers instanceof IStruct castedBoxMember ) {
			Array result = new Array();

			// Iterate over all entries and validate them
			for ( IStruct.Entry<Key, ?> entry : castedBoxMember.entrySet() ) {
				// Validate Type first which is the key of the entry
				Key type = entry.getKey();
				if ( !BoxLangType.isValid( type ) ) {
					throw new BoxRuntimeException(
					    className.getName() + " BoxMember annotation has an invalid type value [" + type.getName() + "]" +
					        "Valid types are: " + Arrays.toString( BoxLangType.values() )
					);
				}

				// Now the value of this key must be a struct with the following keys: name, objectArgument
				// Validate the value is a struct
				if ( ! ( entry.getValue() instanceof IStruct memberRecord ) ) {
					throw new BoxRuntimeException(
					    className.getName() + " BoxMember annotation value must be a struct with the following keys: [name], [objectArgument]"
					);
				}

				// Prepare the record now
				BoxLangType boxType = BoxLangType.valueOf( type.getNameNoCase() );
				memberRecord.put( Key.type, boxType );
				memberRecord.computeIfAbsent( Key._NAME, k -> className.getNameNoCase().replace( type.getNameNoCase(), "" )
				);
				memberRecord.putIfAbsent( Key.objectArgument, "" );
				result.push( memberRecord );
			}

			return result;
		}

		// Who knows what this is, just return an empty struct
		return new Array();
	}

	/**
	 * Build an array of Key aliases for the BIF/Component based on the following rules:
	 * - If the target has any `{annotation}` annoations that have a value, use those
	 * - If the target has any `{annotation}` annoations that have no value, use the BIF name
	 *
	 * @param target     The target Component/BIF to build aliases for
	 * @param className  The class name of the target
	 * @param annotation The annotation to look for
	 *
	 * @return An array of Key aliases
	 */
	private Key[] buildAnnotationAliases( IClassRunnable target, Key className, Key annotation ) {
		// Get the requested annotation for the target
		Object annotations = target.getBoxMeta().getMeta().getAsStruct( Key.annotations ).getOrDefault( annotation, "" );

		// Case 1 : This is a simple String with no value, just return
		if ( annotations instanceof String castedAnnotation && castedAnnotation.isBlank() ) {
			return new Key[] { className };
		}

		// Case 2 : This is a simple String with a value, return the value as the alias instead of the name of the file on disk
		if ( annotations instanceof String castedAnnotationWithValue && !castedAnnotationWithValue.isBlank() ) {
			return new Key[] {
			    Key.of( castedAnnotationWithValue )
			};
		}

		// Case 3 : We have an Array of aliases, and they have values, return them alongside the class name
		if ( annotations instanceof Array castedAliases ) {
			// convert the values in the array to Keys
			return castedAliases.push( className ).stream().map( Key::of ).toArray( Key[]::new );
		}

		// Default : Just return the class name
		return new Key[] { className };
	}

	/**
	 * Load a BoxLang class from disk and return it as a {@link IClassRunnable}
	 *
	 * @param targetFile      The target file to load
	 * @param conventionsPath The conventions path to load the class from
	 * @param context         The current context of execution
	 *
	 * @return The loaded BoxLang class
	 */
	private IClassRunnable loadClassRunnable( File targetFile, String conventionsPath, IBoxContext context ) {
		var					oTargetObject		= ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            null,
		            null,
		            ( this.invocationPath + "." + conventionsPath )
		                .replace( ".", Matcher.quoteReplacement( File.separator ) )
		                + File.separator
		                + FilenameUtils.getBaseName( targetFile.getAbsolutePath() ),
		            targetFile.toPath()
		        ),
		        context
		    )
		).invokeConstructor( context )
		    .getTargetInstance();

		/**
		 * --------------------------------------------------------------------------
		 * DI Injections
		 * --------------------------------------------------------------------------
		 * Inject the following references into the BoxLang BIF
		 * - boxRuntime : BoxLangRuntime
		 * - log : A logger
		 * - functionService : The BoxLang FunctionService
		 * - componentService : The BoxLang ComponentService
		 * - interceptorService : The BoxLang InterceptorService
		 * - cacheService : The BoxLang CacheService
		 * - asyncService : The BoxLang AsyncService
		 * - schedulerService : The BoxLang SchedulerService
		 * - dataSourceService : The BoxLang DataSourceService
		 * - moduleRecord : The ModuleRecord instance
		 */

		FunctionService		functionService		= this.runtime.getFunctionService();
		InterceptorService	interceptorService	= this.runtime.getInterceptorService();

		oTargetObject.getVariablesScope().put( Key.moduleRecord, this );
		oTargetObject.getVariablesScope().put( Key.boxRuntime, this.runtime );
		oTargetObject.getVariablesScope().put( Key.functionService, functionService );
		oTargetObject.getVariablesScope().put( Key.componentService, this.runtime.getComponentService() );
		oTargetObject.getVariablesScope().put( Key.interceptorService, interceptorService );
		oTargetObject.getVariablesScope().put( Key.asyncService, this.runtime.getAsyncService() );
		oTargetObject.getVariablesScope().put( Key.schedulerService, this.runtime.getSchedulerService() );
		oTargetObject.getVariablesScope().put( Key.datasourceService, this.runtime.getDataSourceService() );
		oTargetObject.getVariablesScope().put( Key.cacheService, this.runtime.getCacheService() );
		oTargetObject.getVariablesScope().put( Key.log, this.logger );

		return oTargetObject;
	}

}
