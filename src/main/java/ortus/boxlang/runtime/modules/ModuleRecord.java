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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.regex.Matcher;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BoxLangBIFProxy;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.jdbc.drivers.DriverShim;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.IService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;
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
	 * The BIFS collaborated by the module
	 */
	public Array				bifs						= new Array();

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

	/*
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This prefix is used a virtual package name for the module
	 */
	private static final String	MODULE_PACKAGE_NAME			= "ortus.boxlang.runtime.modules.";

	private static final String	MODULE_CONFIG_FILE			= "box.json";

	/**
	 * Moudule Logger
	 */
	private static final Logger	logger						= LoggerFactory.getLogger( ModuleRecord.class );

	/*
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
	public ModuleRecord( String physicalPath ) {
		Path directoryPath = Path.of( physicalPath );
		if ( Files.exists( directoryPath.resolve( MODULE_CONFIG_FILE ) ) ) {
			try {
				Object rawConfig = JSONUtil.fromJSON(
				    Files.readString( directoryPath.resolve( MODULE_CONFIG_FILE ), StandardCharsets.UTF_8 )
				);
				if ( rawConfig instanceof Map rawMap && rawMap.containsKey( "boxlang" ) ) {
					IStruct runtimeAttributes = StructCaster.cast( rawMap.get( "boxlang" ) );
					if ( runtimeAttributes.containsKey( Key.moduleName ) ) {
						this.name = Key.of( runtimeAttributes.get( Key.moduleName ) );
						System.out.println( "Module name set to box.json specified: " + this.name.getName() );
					}
				}
			} catch ( IOException e ) {
				logger.error( "Error reading module box.json file", this.name, e );
				// if the file cannot be read move on and the directory name will be used
			}
		}
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

	/*
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
		BoxRuntime	runtime			= BoxRuntime.getInstance();
		Path		descriptorPath	= physicalPath.resolve( ModuleService.MODULE_DESCRIPTOR );
		String		packageName		= MODULE_PACKAGE_NAME + this.name.getNameNoCase() + EncryptionUtil.hash( Instant.now() + id + physicalPath.toString() );

		// Load the Class, Construct it and store it
		this.moduleConfig = ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            null,
		            null,
		            packageName.replaceAll( "\\.", Matcher.quoteReplacement( File.separator ) ) + File.separator + ModuleService.MODULE_DESCRIPTOR,
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
		this.disabled		= ( Boolean ) thisScope.getOrDefault( Key.disabled, false );

		// Verify if we disabled the loading of the module in the runtime config
		if ( runtime.getConfiguration().runtime.modules.containsKey( this.name ) ) {
			ModuleConfig config = ( ModuleConfig ) runtime.getConfiguration().runtime.modules.get( this.name );
			this.disabled = config.disabled;
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
		 * Inject the following references into the CFC
		 * - moduleRecord : The ModuleRecord instance
		 * - boxRuntime : The BoxRuntime instance
		 * - interceptorService : The BoxLang InterceptorService
		 * - log : A logger for the module config itself
		 */

		variablesScope.put( Key.moduleRecord, this );
		variablesScope.put( Key.boxRuntime, BoxRuntime.getInstance() );
		variablesScope.put( Key.interceptorService, BoxRuntime.getInstance().getInterceptorService() );
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
		ThisScope			thisScope			= this.moduleConfig.getThisScope();
		VariablesScope		variablesScope		= this.moduleConfig.getVariablesScope();
		BoxRuntime			runtime				= BoxRuntime.getInstance();
		InterceptorService	interceptorService	= runtime.getInterceptorService();
		FunctionService		functionService		= runtime.getFunctionService();
		ComponentService	componentService	= runtime.getComponentService();

		// Register the module mapping in the runtime
		// Called first in case this is used in the `configure` method
		runtime.getConfiguration().runtime.registerMapping( this.mapping, this.path );

		// Create the module class loader and seed it with the physical path to the module
		// This traverses the module and looks for *.class files to load (NOT JARs)
		// Using the `modules.{module_name}` package prefix
		// This is important for module developers to include this as their package prefix.
		try {
			this.classLoader = new DynamicClassLoader(
			    this.name,
			    this.physicalPath.toUri().toURL(),
			    ClassLoader.getSystemClassLoader()
			);
		} catch ( MalformedURLException e ) {
			logger.error( "Error creating module [{}] class loader.", this.name, e );
			throw new BoxRuntimeException( "Error creating module [" + this.name + "] class loader", e );
		}

		// Do we have libs to add to the class loader? These are jars ONLY
		// All dependencies on Java libs must be on this folder as JARs
		Path libsPath = this.physicalPath.resolve( ModuleService.MODULE_LIBS );
		if ( Files.exists( libsPath ) && Files.isDirectory( libsPath ) ) {
			try {
				this.classLoader.addURLs( DynamicClassLoader.getJarURLs( libsPath ) );
			} catch ( IOException e ) {
				logger.error( "Error while seeding the module [{}] class loader with the libs folder.", this.name, e );
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
		if ( runtime.getConfiguration().runtime.modules.containsKey( this.name ) ) {
			// TODO: Later do a deep merge
			ModuleConfig config = ( ModuleConfig ) runtime.getConfiguration().runtime.modules.get( this.name );
			this.settings.putAll( config.settings );
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

		// Register any global services
		ServiceLoader.load( IService.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( service -> runtime.putGlobalService( service.getName(), service ) );

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
		    .forEach( driver -> runtime.getDataSourceService().registerDriver( driver ) );

		// Do we have any Java BIFs to load?
		ServiceLoader.load( BIF.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( clazz -> functionService.processBIFRegistration( clazz, null, null ) );

		// Do we have any Java Component Tags to load?
		ServiceLoader.load( Component.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( targetClass -> componentService.registerComponent( targetClass, null, null ) );

		// Do we have any Java Schedulers to register in the SchedulerService
		ServiceLoader.load( IScheduler.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( scheduler -> runtime.getSchedulerService().loadScheduler( Key.of( "bxScheduler@" + this.name ), scheduler ) );

		// Do we have any Java ICacheProviders to register in the CacheService
		ServiceLoader.load( ICacheProvider.class, this.classLoader )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( provider -> runtime.getCacheService().registerProvider( Key.of( provider.getSimpleName() ), provider ) );

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
	 * Register a BIF with the runtime
	 *
	 * @param targetFile The target file to register that represents the BIF on disk
	 * @param context    The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	private ModuleRecord registerBIF( File targetFile, IBoxContext context ) {
		// System.out.println( "Processing " + targetFile.getAbsolutePath() );

		// Skip directories and non CFC/BX files
		// We are not doing recursive registration for the moment.
		if ( targetFile.isDirectory() || !targetFile.getName().matches( "^.*\\.(cfc|bx)$" ) ) {
			return this;
		}

		// Nice References
		BoxRuntime			runtime				= BoxRuntime.getInstance();
		FunctionService		functionService		= runtime.getFunctionService();
		InterceptorService	interceptorService	= runtime.getInterceptorService();

		// Try to load the BoxLang class
		Key					className			= Key.of( FilenameUtils.getBaseName( targetFile.getAbsolutePath() ) );
		IClassRunnable		oBIF				= ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            null,
		            null,
		            ( this.invocationPath + "." + ModuleService.MODULE_BIFS ).replaceAll( "\\.", Matcher.quoteReplacement( File.separator ) ) + File.separator
		                + FilenameUtils.getBaseName( targetFile.getAbsolutePath() ),
		            targetFile.toPath()
		        ),
		        context
		    )
		).invokeConstructor( context )
		    .getTargetInstance();

		/*
		 * --------------------------------------------------------------------------
		 * DI Injections
		 * --------------------------------------------------------------------------
		 * Inject the following references into the BoxLang BIF
		 * - boxRuntime : BoxLangRuntime
		 * - log : A logger
		 * - functionService : The BoxLang FunctionService
		 * - interceptorService : The BoxLang InterceptorService
		 * - moduleRecord : The ModuleRecord instance
		 */

		oBIF.getVariablesScope().put( Key.moduleRecord, this );
		oBIF.getVariablesScope().put( Key.boxRuntime, runtime );
		oBIF.getVariablesScope().put( Key.functionService, functionService );
		oBIF.getVariablesScope().put( Key.interceptorService, interceptorService );
		oBIF.getVariablesScope().put( Key.log, LoggerFactory.getLogger( oBIF.getClass() ) );

		/*
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
		Key[]			bifAliases		= buildBIFAliases( oBIF, className );
		for ( Key bifAlias : bifAliases ) {
			// Register the mapping in the runtime
			functionService.registerGlobalFunction(
			    bifDescriptor,
			    bifAlias,
			    true
			);
			logger.info(
			    "> Registered Module [{}] BIF [{}] with alias [{}]",
			    this.name.getName(),
			    className.getName(),
			    bifAlias.getName()
			);
			this.bifs.push( bifAlias );
		}

		/*
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
			logger.info(
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
			        Key._NAME, className.getNameNoCase().replaceAll( boxType.getKey().getNameNoCase(), "" ),
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
				memberRecord.computeIfAbsent( Key._NAME, k -> className.getNameNoCase().replaceAll( type.getNameNoCase(), "" )
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
	 * Build an array of Key aliases for the BIF based on the following rules:
	 * - If the BIF has any `BoxBIF` annoations that have a value, use those
	 * - If the BIF has any `BoxBIF` annoations that have no value, use the BIF name
	 *
	 * {@code
	 * // No value : use the class name
	 * @BoxBIF
	 * // Use the MyBif value as a simple string
	 *
	 * @BoxBIF "MyBif"
	 *         // Use the array of aliases
	 * @BoxBIF [ "bif1", "bif2"]
	 *         }
	 *
	 * @param targetBIF The target BIF to build aliases for
	 * @param className The class name of the BIF
	 *
	 * @return An array of Key aliases for the BIF
	 */
	private Key[] buildBIFAliases( IClassRunnable targetBIF, Key className ) {
		// Get the BoxBIF annotation
		Object boxBifAnnotation = targetBIF.getBoxMeta().getMeta().getAsStruct( Key.annotations ).getOrDefault( Key.boxBif, "" );

		// Case 1 : This is a simple String with no value, just return
		if ( boxBifAnnotation instanceof String castedAnnotation && castedAnnotation.isBlank() ) {
			return new Key[] { className };
		}

		// Case 2 : This is a simple String with a value, return the value as the alias instead of the name of the file on disk
		if ( boxBifAnnotation instanceof String castedAnnotationWithValue && !castedAnnotationWithValue.isBlank() ) {
			return new Key[] {
			    Key.of( castedAnnotationWithValue )
			};
		}

		// Case 3 : We have an Array of aliases, and they have values, return them alongside the class name
		if ( boxBifAnnotation instanceof Array castedAliases ) {
			// convert the values in the array to Keys
			return castedAliases.push( className ).stream().map( Key::of ).toArray( Key[]::new );
		}

		// Default : Just return the class name
		return new Key[] { className };
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
				    DynamicObject.EMPTY_ARGS,
				    false
				);
			} catch ( Exception e ) {
				logger.error( "Error while unloading module [{}]", this.name, e );
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
			logger.error( "Error while closing the DynamicClassLoader for module [{}]", this.name, e );
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
	 *
	 * @return The class if found, null otherwise
	 *
	 * @throws ClassNotFoundException If the class is not found
	 */
	public Class<?> findModuleClass( String className, Boolean safe ) throws ClassNotFoundException {
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
		InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

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

	/*
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
