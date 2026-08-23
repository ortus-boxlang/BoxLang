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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.jdbc.drivers.DriverShim;
import ortus.boxlang.runtime.jdbc.drivers.IJDBCDriver;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.loader.IModuleClassLoader;
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
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.Mapping;
import ortus.boxlang.runtime.util.RegexBuilder;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This class represents a module record
 */
public class ModuleRecord {

	/**
	 * Unique internal ID for the module
	 */
	public final String				id							= UUID.randomUUID().toString();

	/**
	 * The name of the module, defaults to the folder name on disk
	 */
	public Key						name;

	/**
	 * The version of the module, defaults to 1.0.0
	 */
	public String					version						= "1.0.0";

	/**
	 * The author of the module or empty if not set
	 */
	public String					author						= "";

	/**
	 * The description of the module or empty if not set
	 */
	public String					description					= "";

	/**
	 * The web URL of the module or empty if not set
	 */
	public String					webURL						= "";

	/**
	 * The BoxLang mapping of the module used to construct classes from within it.
	 * All mappings have a prefix of
	 * {@link ModuleService#MODULE_MAPPING_INVOCATION_PREFIX}
	 *
	 */
	public Mapping					mapping						= null;

	/**
	 * A public conventioin mapping for a /public folder inside the module
	 */
	public Mapping					publicMapping				= null;

	/**
	 * If the module is enabled for activation, defaults to false
	 */
	public boolean					enabled						= true;

	/**
	 * Flag to indicate if the module has been activated or not yet
	 */
	public boolean					activated					= false;

	/**
	 * The settings of the module
	 */
	public Struct					settings					= new Struct();

	/**
	 * Any module activation dependencies
	 */
	public Array					dependencies				= new Array();

	/**
	 * The names of the modules nested inside this module's own {@code modules} folder
	 * (module inception). Populated by the {@link ModuleService} during discovery.
	 * <p>
	 * Nested modules are registered and activated <strong>before</strong> this module, and
	 * unloaded <strong>after</strong> it, since their class loaders are parented to this one.
	 */
	public Array					nestedModules				= new Array();

	/**
	 * The name of the module this one is nested inside, or {@code null} for a top-level module.
	 * <p>
	 * When set, this module's class loader is parented to the parent module's class loader, and
	 * the parent may override this module's settings via {@link IModuleConfig#modules()}.
	 */
	public Key						parentModule				= null;

	/**
	 * The interceptors of the module
	 */
	public Array					interceptors				= new Array();

	/**
	 * The BIFS collaborated by the module
	 */
	public Array					bifs						= new Array();

	/**
	 * The Components collaborated by the module
	 */
	public Array					components					= new Array();

	/**
	 * The member Methods collaborated by the module
	 */
	public Array					memberMethods				= new Array();

	/**
	 * The custom interception points of the module
	 */
	public Array					customInterceptionPoints	= new Array();

	/**
	 * The physical path of the module on disk as a Java {@link Path}
	 */
	public Path						physicalPath;

	/**
	 * The physical path of the module but in string format. Used by BoxLang code
	 * mostly
	 * Same as the {@link ModuleRecord#physicalPath} but in string format
	 */
	public String					path;

	/**
	 * The invocation path of the module which is a composition of the
	 * {@link ModuleService#MODULE_MAPPING_INVOCATION_PREFIX} and the module name.
	 * Example: {@code /bxModules/MyModule} is the mapping for the module
	 * the invocation path would be {@code bxModules.MyModule}
	 */
	public String					invocationPath;

	/**
	 * The timestamp when the module was registered
	 */
	public Instant					registeredOn;

	/**
	 * The time it took in ms to register the module
	 */
	public long						registrationTime			= 0;

	/**
	 * The timestamp when the module was activated
	 */
	public Instant					activatedOn;

	/**
	 * The time it took in ms to activate the module
	 */
	public long						activationTime				= 0;

	/**
	 * The class loader for the module (isolated, parented to the runtime loader). The concrete
	 * implementation is chosen by the runtime's
	 * {@link ortus.boxlang.runtime.loader.IClassLoaderFactory}.
	 * <p>
	 * Declared as the concrete {@link DynamicClassLoader} type purely for binary compatibility
	 * with modules compiled against pre-{@code IModuleClassLoader} BoxLang releases; the JVM
	 * resolves fields by (name, declared type), so downstream bytecode that does
	 * {@code getField("classLoader")} against the old {@code DynamicClassLoader classLoader}
	 * signature would otherwise fail with {@link NoSuchFieldError}.
	 * <p>
	 * New code — both inside the runtime and in downstream modules — should use
	 * {@link #moduleClassLoader} instead, which is typed against the runtime-neutral
	 * {@link IModuleClassLoader} contract and works on every supported deployment target
	 * (standard JVM, Android, etc.).
	 *
	 * @deprecated Use {@link #moduleClassLoader} instead. This field is retained only for
	 *             binary compatibility and will be removed in a future major release.
	 */
	@Deprecated( since = "1.15.0", forRemoval = true )
	public DynamicClassLoader		classLoader					= null;

	/**
	 * The class loader for the module (isolated, parented to the runtime loader). The concrete
	 * implementation is chosen by the runtime's
	 * {@link ortus.boxlang.runtime.loader.IClassLoaderFactory}.
	 * <p>
	 * This is the preferred accessor for module class loaders. Unlike {@link #classLoader},
	 * it is typed against the runtime-neutral {@link IModuleClassLoader} contract, so it works
	 * on every supported deployment target (standard JVM, Android, etc.). The field itself
	 * is private — access it via {@link #getModuleClassLoader()}.
	 * <p>
	 * Use {@link IModuleClassLoader#toClassLoader()} when a {@link ClassLoader} is required
	 * (e.g. for {@link ServiceLoader}).
	 */
	private IModuleClassLoader		moduleClassLoader			= null;

	/**
	 * The module config; either a {@link BoxModuleConfig} wrapping the compiled {@code ModuleConfig.bx}
	 * class, or a Java {@link IModuleConfig} implementation discovered via {@link java.util.ServiceLoader}.
	 * Null until {@link #register} completes successfully.
	 */
	public IModuleConfig			moduleConfig;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This prefix is used a virtual package name for the module
	 */
	private static final String		MODULE_PACKAGE_NAME			= "ortus.boxlang.runtime.modules.";

	/**
	 * The name of the descriptor file for the module based on CommandBox
	 */
	public static final String		MODULE_CONFIG_FILE			= "box.json";

	/**
	 * Whether this module is packaged as a single jar file placed directly in a modules folder,
	 * as opposed to the conventional module directory layout.
	 */
	private final boolean			jarModule;

	/**
	 * Guards {@link #loadDescriptor(IBoxContext)} so it is idempotent. A module with nested
	 * children has its descriptor loaded early (so its per-child overrides are readable) and
	 * would otherwise be re-compiled during its own registration.
	 */
	private boolean					descriptorLoaded			= false;

	/**
	 * Guards {@link #resolveModuleConfig(IBoxContext)} so it is idempotent, for the same reason
	 * as {@link #descriptorLoaded}.
	 */
	private boolean					configResolved				= false;

	/**
	 * Logger
	 */
	private BoxLangLogger			logger;

	/**
	 * Runtime
	 */
	private BoxRuntime				runtime;

	/**
	 * Tracks JDBC drivers registered by this module for proper cleanup (deregistration)
	 * during module unload. This ensures that drivers do not leak and are removed
	 * from the DriverManager when the module is unloaded.
	 */
	private Map<Key, DriverShim>	jdbcDrivers					= new HashMap<>();

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
		this.logger		= this.runtime.getLoggingService().MODULES_LOGGER;

		Path	directoryPath	= Path.of( physicalPath );
		Path	boxjsonPath		= directoryPath.resolve( MODULE_CONFIG_FILE );

		// A module packaged as a single jar file has no folder conventions to read from;
		// everything comes from its IModuleConfig, discovered later via ServiceLoader.
		this.jarModule = ModuleService.isModuleJar( directoryPath );

		// Load the module name from the box.json file if it exists
		if ( Files.exists( boxjsonPath ) ) {
			DataNavigator
			    .of( boxjsonPath )
			    .from( "boxlang" )
			    .ifPresent( "moduleName", value -> this.name = Key.of( value ) )
			    .ifPresent( "minimumVersion",
			        value -> this.runtime.getModuleService().verifyModuleAndBoxLangVersion(
			            ( String ) value,
			            directoryPath
			        )
			    );
		}

		// Default to the directory name if the box.json file does not exist.
		// Jar modules default to the jar's base name, so `my-module.jar` becomes `my-module`.
		if ( this.name == null ) {
			this.name = Key.of(
			    this.jarModule
			        ? FilenameUtils.getBaseName( directoryPath.getFileName().toString() )
			        : directoryPath.getFileName().toString()
			);
		}

		// Path to the module in string and Path formats
		this.path			= physicalPath;
		this.physicalPath	= Paths.get( physicalPath );

		// Register the automatic mapping by convention: /bxModules/{name}
		// Not visible externally
		this.mapping		= Mapping.of(
		    ModuleService.MODULE_MAPPING_PREFIX + name.getName(),
		    this.path,
		    false
		);

		// Register the public mapping by convention /bxModules/{name}/public
		this.publicMapping	= Mapping.of(
		    ModuleService.MODULE_MAPPING_PREFIX + name.getName() + "/" + ModuleService.MODULE_PUBLIC_FOLDER,
		    this.physicalPath.resolve( "public" ).toString(),
		    true
		);

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
	 * This happens before module registration.
	 * Then populate the module record with the information from the descriptor
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord loadDescriptor( IBoxContext context ) {
		// Idempotent: a module with nested children loads its descriptor early so its
		// per-child overrides are readable before those children register.
		if ( this.descriptorLoaded ) {
			return this;
		}
		this.descriptorLoaded = true;

		// Java-only modules (no ModuleConfig.bx) skip BX loading entirely.
		// Java config detection happens in resolveModuleConfig() once the classloader is ready.
		if ( !Files.exists( this.physicalPath.resolve( ModuleService.MODULE_DESCRIPTOR ) ) ) {
			return this;
		}

		Path			descriptorPath	= physicalPath.resolve( ModuleService.MODULE_DESCRIPTOR );
		String			packageName		= MODULE_PACKAGE_NAME
		    + this.name.getNameNoCase()
		    + EncryptionUtil.hash( physicalPath.toString() );

		// Load the ModuleConfig.bx, Construct it and store it
		IClassRunnable	bxClass			= ( IClassRunnable ) RequestBoxContext.runInContext( context, ctx -> DynamicObject.of(
		    RunnableLoader.getInstance().loadClass(
		        ResolvedFilePath.of(
		            "/bxModules/" + name.getName() + "/",
		            physicalPath.normalize().toString(),
		            packageName.replace( ".", Matcher.quoteReplacement( File.separator ) ) + File.separator
		                + ModuleService.MODULE_DESCRIPTOR,
		            descriptorPath ),
		        ctx ) )
		    .invokeConstructor( ctx )
		    .getTargetInstance() );

		// Wrap in the BxModuleConfig proxy so ModuleRecord only ever sees IModuleConfig
		this.moduleConfig = new BoxModuleConfig( bxClass );

		// Nice References
		ThisScope		thisScope		= bxClass.getThisScope();
		VariablesScope	variablesScope	= bxClass.getVariablesScope();

		// Store the descriptor information into the record
		this.version		= ( String ) thisScope.getOrDefault( Key.version, "1.0.0" );
		this.author			= ( String ) thisScope.getOrDefault( Key.author, "" );
		this.description	= ( String ) thisScope.getOrDefault( Key.description, "" );
		this.webURL			= ( String ) thisScope.getOrDefault( Key.webURL, "" );
		this.enabled		= BooleanCaster.cast( thisScope.getOrDefault( Key.enabled, true ) );
		this.dependencies	= ArrayCaster.cast( thisScope.getOrDefault( Key.dependencies, Array.of() ) );

		// Do we have a custom mapping to override?
		if ( thisScope.containsKey( Key.mapping ) ) {
			this.mapping = resolveMapping( thisScope.get( Key.mapping ) );
		}

		// Do we have a public mapping to override?
		if ( thisScope.containsKey( Key.publicMapping ) ) {
			this.publicMapping = resolvePublicMapping( thisScope.get( Key.publicMapping ) );
		}

		// Verify the internal config structures exist, else default them
		variablesScope.computeIfAbsent( Key.settings, k -> new Struct() );
		variablesScope.computeIfAbsent( Key.interceptors, k -> Array.of() );
		variablesScope.computeIfAbsent( Key.customInterceptionPoints, k -> Array.of() );

		/**
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
	 * Builds this module's isolated class loader, on demand and only once.
	 * <p>
	 * The class loader hierarchy mirrors the module hierarchy: a nested module's loader is
	 * parented to its parent module's loader, chaining up until a top-level module, whose loader
	 * is parented to the runtime loader. A child can therefore see the classes and {@code libs}
	 * its parent bundles, while remaining isolated from its siblings.
	 * <p>
	 * Creation is lazy because nested modules register <em>before</em> their parent, yet need the
	 * parent's loader to exist first. Each level creates its own loader on demand, so a grandchild
	 * transparently forces the whole chain up to the runtime loader.
	 *
	 * @return The module class loader
	 */
	public IModuleClassLoader getOrCreateModuleClassLoader() {
		if ( this.moduleClassLoader != null ) {
			return this.moduleClassLoader;
		}

		// Nested modules chain to their parent's loader; top-level modules to the runtime loader
		ClassLoader parentLoader = this.runtime.getRuntimeLoader();
		if ( this.parentModule != null ) {
			ModuleRecord parentRecord = this.runtime.getModuleService().getModuleRecord( this.parentModule );
			if ( parentRecord != null ) {
				parentLoader = parentRecord.getOrCreateModuleClassLoader().toClassLoader();
			} else {
				this.logger.warn(
				    "+ Module [{}] declares parent module [{}] which is not in the registry; parenting to the runtime loader instead",
				    this.name,
				    this.parentModule
				);
			}
		}

		// Create the module's (isolated) class loader via the runtime's configured factory.
		// The default JVM factory builds a DynamicClassLoader over the module directory
		// (loading *.class under the `modules.{module_name}` prefix) seeded with libs/*.jar;
		// other targets (e.g. Android) supply a different loader without forking this class.
		this.moduleClassLoader	= this.runtime.getClassLoaderFactory().createModuleClassLoader( this, parentLoader );
		// Mirror onto the legacy, concrete-typed field for binary compatibility with modules
		// compiled against the pre-IModuleClassLoader API. On the standard JVM the factory's
		// return value is always a DynamicClassLoader; on other targets this assignment is a
		// no-op for old bytecode but `moduleClassLoader` still works.
		// TODO: Drop by 2.x
		this.classLoader		= ( this.moduleClassLoader instanceof DynamicClassLoader dcl )
		    ? dcl
		    : null;

		return this.moduleClassLoader;
	}

	/**
	 * Resolves this module's {@link IModuleConfig} and its metadata, without registering anything
	 * with the runtime services. Java configs discovered via {@link ServiceLoader} always win over
	 * a {@code ModuleConfig.bx} descriptor.
	 * <p>
	 * This is split out of {@link #register(IBoxContext)} because a module carrying nested children
	 * must have its config resolved <em>before</em> those children register, so that its per-child
	 * overrides ({@link IModuleConfig#modules()}) are readable — while its own full registration
	 * still happens after them. Idempotent.
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord resolveModuleConfig( IBoxContext context ) {
		if ( this.configResolved ) {
			return this;
		}
		this.configResolved = true;

		getOrCreateModuleClassLoader();

		// Detect a Java IModuleConfig via ServiceLoader (diskless-safe — uses the module classloader abstraction).
		// Java always wins: if found, replace any BX config and discard BX-derived metadata/state.
		ServiceLoader.load( IModuleConfig.class, this.moduleClassLoader.toClassLoader() )
		    .findFirst()
		    .ifPresent( javaConfig -> {
			    // Reset to conventional defaults so ModuleConfig.bx is truly ignored when Java config is present
			    this.version				= "1.0.0";
			    this.author					= "";
			    this.description			= "";
			    this.webURL					= "";
			    this.enabled				= true;
			    this.dependencies			= new Array();
			    this.settings				= new Struct();
			    this.interceptors			= new Array();
			    this.customInterceptionPoints = new Array();
			    this.mapping				= Mapping.of( ModuleService.MODULE_MAPPING_PREFIX + name.getName(), this.path, false );
			    this.publicMapping			= Mapping.of( ModuleService.MODULE_MAPPING_PREFIX + name.getName() + "/" + ModuleService.MODULE_PUBLIC_FOLDER,
			        this.physicalPath.resolve( "public" ).toString(), true );
			    this.moduleConfig			= javaConfig;
			    extractJavaMetadata();
		    } );

		return this;
	}

	/**
	 * This method registers the module with all the runtime services.
	 * This is called by the ModuleService if the module is allowed to be registered
	 * or not
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord register( IBoxContext context ) {
		// Convenience References
		InterceptorService	interceptorService	= this.runtime.getInterceptorService();
		FunctionService		functionService		= this.runtime.getFunctionService();
		ComponentService	componentService	= this.runtime.getComponentService();

		// Build the class loader and resolve the descriptor (Java wins over BX).
		// No-op when a parent module already resolved us early to read its child overrides.
		resolveModuleConfig( context );

		if ( this.moduleConfig == null ) {
			// Neither ServiceLoader nor loadDescriptor() produced a valid config.
			this.logger.warn(
			    "+ Module Service: Module [{}] has no valid descriptor (no IModuleConfig via ServiceLoader and no ModuleConfig.bx). Disabling.",
			    this.name
			);
			this.enabled = false;
			return this;
		}

		// Register the module mapping in the this.runtime
		// Called first in case this is used in the `configure` method
		this.runtime.getConfiguration().registerMapping( this.mapping );
		this.runtime.getConfiguration().registerMapping( this.publicMapping );

		// Unified configure() call — BxModuleConfig reads variablesScope; Java impls mutate settings directly
		this.moduleConfig.configure( context, this );

		/**
		 * --------------------------------------------------------------------------
		 * Settings Precedence
		 * --------------------------------------------------------------------------
		 * Later wins: own configure() defaults < parent module overrides < global app config.
		 */

		// A parent module may override its nested children's settings and enablement
		applyParentOverrides();

		// Merge any runtime-config settings on top; the global app config always wins
		if ( this.runtime.getConfiguration().modules.containsKey( this.name ) ) {
			ModuleConfig config = ( ModuleConfig ) this.runtime.getConfiguration().modules.get( this.name );
			StructUtil.deepMerge( this.settings, config.settings, true );
		}

		// Register Interception points with the InterceptorService
		if ( !this.customInterceptionPoints.isEmpty() ) {
			interceptorService
			    .registerInterceptionPoint( this.customInterceptionPoints.stream().map( Key::of ).toArray( Key[]::new ) );
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
		ServiceLoader.load( IService.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( service -> this.runtime.putGlobalService( service.getName(), service ) );

		// Load any JDBC drivers into the JVM
		ServiceLoader.load( Driver.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> {
			    DriverShim driverShim = new DriverShim( driver );
			    this.jdbcDrivers.put( Key.of( driver.getClass().getName() ), driverShim );
			    try {
				    DriverManager.registerDriver( driverShim );
			    } catch ( SQLException e ) {
				    throw new DatabaseException( e );
			    }
		    } );

		// Load any BoxLang IJDBC Driver classes
		ServiceLoader.load( IJDBCDriver.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> this.runtime.getDataSourceService().registerDriver( driver ) );

		// Do we have any Java BIFs to load?
		ServiceLoader.load( BIF.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( clazz -> functionService.processBIFRegistration( clazz, null, this.name.getName() ) );

		// Do we have any Java Component Tags to load?
		ServiceLoader.load( Component.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( targetClass -> componentService.registerComponent( targetClass, null, this.name.getName() ) );

		// Do we have any Java Schedulers to register in the SchedulerService
		ServiceLoader.load( IScheduler.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( scheduler -> this.runtime.getSchedulerService()
		        .loadScheduler( Key.of( scheduler.getSchedulerName() + "@" + this.name ), scheduler ) );

		// Do we have any Java ICacheProviders to register in the CacheService
		ServiceLoader.load( ICacheProvider.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( provider -> this.runtime.getCacheService().registerProvider( Key.of( provider.getSimpleName() ),
		        provider ) );

		// Do we have any Java IInterceptor to register in the InterceptorService
		ServiceLoader.load( IInterceptor.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    // Only load interceptors that are set to auto-load by default or by
		    // configuration
		    .filter( provider -> interceptorService.canLoadInterceptor( provider.type() ) )
		    // Register the interceptor with the module settings
		    .map( ServiceLoader.Provider::get )
		    .forEach( targetInterceptor -> interceptorService.register( targetInterceptor, this.settings ) );

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
		if ( this.moduleConfig != null ) {
			try {
				this.moduleConfig.onUnload( context, this );
			} catch ( AbortException ae ) {
				throw ae;
			} catch ( Exception e ) {
				this.logger.error( "Error while unloading module [{}]", this.name, e );
			}
		}

		this.unregister( context );

		// Destroy the ClassLoader
		try {
			this.moduleClassLoader.close();
		} catch ( IOException e ) {
			this.logger.error( "Error while closing the DynamicClassLoader for module [{}]", this.name, e );
		} finally {
			this.moduleClassLoader	= null;
			this.classLoader		= null;
		}

		return this;
	}

	/**
	 * Unregisters the module from the runtime, removing global services, JDBC drivers,
	 * and other resources associated with this module.
	 *
	 * @param context The current context of execution
	 *
	 * @return The ModuleRecord instance
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.DatabaseException if a SQL error occurs while deregistering JDBC drivers
	 */
	public ModuleRecord unregister( IBoxContext context ) {
		InterceptorService interceptorService = this.runtime.getInterceptorService();

		// Unregister any global services
		ServiceLoader.load( IService.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( service -> this.runtime.removeGlobalService( service.getName() ) );

		// Unload JDBC drivers from the JVM
		ServiceLoader.load( Driver.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> {
			    Key driverKey = Key.of( driver.getClass().getName() );
			    try {
				    DriverManager.deregisterDriver( this.jdbcDrivers.get( driverKey ) );
			    } catch ( SQLException e ) {
				    throw new DatabaseException( e );
			    }
			    this.jdbcDrivers.remove( driverKey );
		    } );

		// Unregister JDBC drivers from the datasource service
		ServiceLoader.load( IJDBCDriver.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( driver -> this.runtime.getDataSourceService().removeDriver( driver.getName() ) );

		// @TODO: Unregister BIFs; we're lacking an unregister method in the FunctionService
		// as a counterpart to `functionService.processBIFRegistration`.

		// @TODO: Unregister components; we're lacking an unregisterComponent method in the ComponentService

		// unregister schedulers
		ServiceLoader.load( IScheduler.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::get )
		    .forEach( scheduler -> this.runtime.getSchedulerService()
		        .removeScheduler( Key.of( scheduler.getSchedulerName() + "@" + this.name ), true, 500 ) );

		// Unregister cache providers
		ServiceLoader.load( ICacheProvider.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    .map( ServiceLoader.Provider::type )
		    .forEach( provider -> this.runtime.getCacheService().removeProvider( Key.of( provider.getSimpleName() ) ) );

		// Unregister java interceptors
		ServiceLoader.load( IInterceptor.class, this.getModuleClassLoader().toClassLoader() )
		    .stream()
		    // Only load interceptors that are set to auto-load by default or by
		    // configuration
		    .filter( provider -> interceptorService.canLoadInterceptor( provider.type() ) )
		    // Register the interceptor with the module settings
		    .map( ServiceLoader.Provider::get )
		    .forEach( targetInterceptor -> interceptorService.unregister( targetInterceptor ) );

		// unregister the module mapping from the runtime
		this.runtime.getConfiguration().unregisterMapping( this.mapping );
		this.runtime.getConfiguration().unregisterMapping( this.publicMapping );

		// Unregister all BX interceptors from all states
		if ( !this.interceptors.isEmpty() ) {
			for ( Object interceptor : this.interceptors ) {
				IStruct			interceptorRecord	= ( IStruct ) interceptor;
				IClassRunnable	interceptorInstance	= ( IClassRunnable ) interceptorRecord.get( Key.interceptor );
				if ( interceptorInstance != null ) {
					interceptorService.unregister( DynamicObject.of( interceptorInstance ) );
				}
			}
		}

		// Unregister the module config from the interceptor service (BxModuleConfig wraps DynamicObject; Java uses IInterceptor path)
		if ( this.moduleConfig != null ) {
			this.moduleConfig.unregisterInterceptor( interceptorService );
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
		if ( this.moduleClassLoader == null ) {
			return null;
		}
		return this.moduleClassLoader.findClass( className, safe, false );
	}

	/**
	 * This method activates the module in the runtime.
	 * Called by the ModuleService if the module is allowed to be activated or not
	 *
	 * @param context The current context of execution
	 *
	 * @throws BoxRuntimeException If an interceptor record is missing the [class]
	 *                             which is mandatory
	 * @throws BoxRuntimeException If an interceptor class is not found locally or
	 *                             with any mappings
	 *
	 * @return The ModuleRecord
	 */
	public ModuleRecord activate( IBoxContext context ) {
		// Convenience References
		InterceptorService interceptorService = this.runtime.getInterceptorService();

		// Register the module config as an interceptor (BxModuleConfig uses IClassRunnable path; Java uses IInterceptor path)
		this.moduleConfig.registerInterceptor( interceptorService, this.settings );

		// Register additional BX interceptors declared in variables.interceptors.
		// For Java modules this array is empty by default, so the loop is a no-op.
		if ( !this.interceptors.isEmpty() ) {
			for ( Object interceptor : this.interceptors ) {
				IStruct interceptorRecord = ( IStruct ) interceptor;
				// Verify the class else throw an exception
				if ( !interceptorRecord.containsKey( Key._CLASS ) ) {
					throw new BoxRuntimeException( "Interceptor record is missing the [class] key which is mandatory" );
				}

				String interceptorClass = ensureModuleInvocationAsset( interceptorRecord.getAsString( Key._CLASS ) );
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
				        this ) );
			}
		}

		// Unified onLoad() call
		this.moduleConfig.onLoad( context, this );

		// Finalize
		this.activated		= true;
		this.activatedOn	= Instant.now();

		return this;
	}

	/**
	 * Reads the per-child overrides this module declares for one of its nested modules.
	 * <p>
	 * BX descriptors declare these as {@code this.modules}; Java descriptors override
	 * {@link IModuleConfig#modules()}. The returned struct follows the {@code boxlang.json}
	 * module shape: <code>{ enabled : boolean, settings : { ... } }</code>.
	 *
	 * @param childName The nested module to read overrides for
	 *
	 * @return The overrides for that child, or an empty struct when none are declared
	 */
	public IStruct getChildOverrides( Key childName ) {
		if ( this.moduleConfig == null ) {
			return new Struct();
		}

		Object childOverrides = this.moduleConfig.modules().get( childName );

		return childOverrides instanceof IStruct castedOverrides ? castedOverrides : new Struct();
	}

	/**
	 * Applies the parent module's overrides for this module, if this is a nested module.
	 * <p>
	 * Called during registration after {@code configure()} but before the global app config is
	 * merged, so the precedence is: own defaults, then parent overrides, then global config.
	 */
	private void applyParentOverrides() {
		if ( this.parentModule == null ) {
			return;
		}

		ModuleRecord parentRecord = this.runtime.getModuleService().getModuleRecord( this.parentModule );
		if ( parentRecord == null ) {
			return;
		}

		IStruct overrides = parentRecord.getChildOverrides( this.name );
		if ( overrides.isEmpty() ) {
			return;
		}

		if ( overrides.containsKey( Key.enabled ) ) {
			this.enabled = BooleanCaster.cast( overrides.get( Key.enabled ) );
		}

		if ( overrides.get( Key.settings ) instanceof IStruct parentSettings ) {
			StructUtil.deepMerge( this.settings, parentSettings, true );
		}

		this.logger.debug(
		    "+ Module [{}] applied setting overrides from its parent module [{}]",
		    this.name,
		    this.parentModule
		);
	}

	/**
	 * Get the record of a module nested inside this one (module inception).
	 *
	 * @param name The name of the nested module
	 *
	 * @return The nested module's record, or {@code null} when this module has no such child
	 */
	public ModuleRecord getNestedModule( Key name ) {
		if ( !hasNestedModule( name ) ) {
			return null;
		}
		return this.runtime.getModuleService().getModuleRecord( name );
	}

	/**
	 * Verify if a module is nested inside this one (module inception).
	 *
	 * @param name The name of the nested module
	 *
	 * @return {@code true} if the module is a direct child of this one, {@code false} otherwise
	 */
	public boolean hasNestedModule( Key name ) {
		return this.nestedModules.stream()
		    .anyMatch( nestedName -> Key.of( ( String ) nestedName ).equals( name ) );
	}

	/**
	 * If this module is packaged as a single jar file placed directly in a modules folder,
	 * as opposed to the conventional module directory layout.
	 *
	 * @return {@code true} if this is a jar module, {@code false} otherwise
	 */
	public boolean isJarModule() {
		return this.jarModule;
	}

	/**
	 * Reads {@link BoxModule @BoxModule} annotation metadata from a Java {@link IModuleConfig} implementation
	 * and populates the corresponding {@link ModuleRecord} fields.
	 * If the annotation is absent, all convention defaults from the constructor are kept.
	 */
	private void extractJavaMetadata() {
		// moduleConfig may be null before loadDescriptor/register
		if ( this.moduleConfig == null ) {
			return;
		}

		BoxModule meta = this.moduleConfig.getClass().getAnnotation( BoxModule.class );

		// If the annotation is absent, keep convention defaults
		if ( meta == null ) {
			return;
		}

		// An explicit name overrides the convention default, which for a jar module is only
		// the jar's base name. Re-derive everything keyed off the name to keep them in sync.
		if ( !meta.name().isBlank() ) {
			this.name			= Key.of( meta.name() );
			this.mapping		= Mapping.of(
			    ModuleService.MODULE_MAPPING_PREFIX + this.name.getName(),
			    this.path,
			    false
			);
			this.publicMapping	= Mapping.of(
			    ModuleService.MODULE_MAPPING_PREFIX + this.name.getName() + "/" + ModuleService.MODULE_PUBLIC_FOLDER,
			    this.physicalPath.resolve( ModuleService.MODULE_PUBLIC_FOLDER ).toString(),
			    true
			);
			this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + this.name.getName();
		}

		// Simple fields
		this.version		= meta.version();
		this.author			= meta.author();
		this.description	= meta.description();
		this.webURL			= meta.webURL();
		this.enabled		= meta.enabled();

		// Dependencies: convert String[] → BoxLang Array
		String[] deps = meta.dependencies();
		if ( deps.length > 0 ) {
			this.dependencies = Array.of( ( Object[] ) deps );
		}

		// Module mapping
		BoxMapping mappingAnn = meta.mapping();
		if ( !mappingAnn.value().isBlank() ) {
			this.mapping = resolveMapping( mappingAnn.value() );
		} else if ( !mappingAnn.name().isBlank() ) {
			IStruct struct = new Struct();
			struct.put( Key._name, mappingAnn.name() );
			struct.put( Key.usePrefix, mappingAnn.usePrefix() );
			struct.put( Key.external, mappingAnn.external() );
			if ( !mappingAnn.path().isBlank() ) {
				struct.put( Key.path, mappingAnn.path() );
			}
			this.mapping = resolveMapping( struct );
		}

		// Public mapping
		BoxMapping pubMappingAnn = meta.publicMapping();
		if ( !pubMappingAnn.value().isBlank() ) {
			this.publicMapping = resolvePublicMapping( pubMappingAnn.value() );
		} else if ( !pubMappingAnn.name().isBlank() ) {
			IStruct struct = new Struct();
			struct.put( Key._name, pubMappingAnn.name() );
			struct.put( Key.usePrefix, pubMappingAnn.usePrefix() );
			struct.put( Key.external, pubMappingAnn.external() );
			if ( !pubMappingAnn.path().isBlank() ) {
				struct.put( Key.path, pubMappingAnn.path() );
			}
			this.publicMapping = resolvePublicMapping( struct );
		}
	}

	/**
	 * This verifies if the class is absolute and returns it as is. Else,
	 * it prepend the module invocation path to it.
	 *
	 * @param targetClass The class to verify
	 *
	 * @return The class name to use, either absolute or with the module invocation
	 *         path
	 */
	private String ensureModuleInvocationAsset( String targetClass ) {
		if ( targetClass.startsWith( this.invocationPath ) ) {
			return targetClass;
		}

		return this.invocationPath + "." + targetClass;
	}

	/**
	 * Execute the module via the BoxRunner
	 *
	 * @param context The current context of execution
	 * @param args    The arguments to pass to the module
	 *
	 * @throws BoxRuntimeException If the module is not executable, meaning it
	 *                             doesn't have a main method
	 */
	public void execute( IBoxContext context, String[] args ) {
		if ( this.moduleConfig == null ) {
			throw new BoxRuntimeException( "Module " + this.id + " is not executable. It has no valid descriptor." );
		}

		try {
			this.moduleConfig.main( context, args );
		} catch ( AbortException ae ) {
			throw ae;
		} catch ( Exception e ) {
			runtime.getLoggingService().getExceptionLogger().error( e.getMessage(), e );
			throw new BoxRuntimeException( e.getMessage(), e );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * If the module is enabled for activation
	 *
	 * @return {@code true} if the module is enabled for activation, {@code false}
	 *         otherwise
	 */
	public boolean isEnabled() {
		return this.enabled;
	}

	/**
	 * If the module is activated
	 *
	 * @return {@code true} if the module is activated, {@code false} otherwise
	 */
	public boolean isActivated() {
		return this.activated;
	}

	/**
	 * The module's class loader, typed against the runtime-neutral
	 * {@link IModuleClassLoader} contract. Prefer this over the deprecated
	 * {@link #classLoader} field in new code; it works on every supported deployment target.
	 *
	 * @return The module class loader, or {@code null} if the module has not been registered
	 *         yet (or has been unloaded).
	 */
	public IModuleClassLoader getModuleClassLoader() {
		return this.moduleClassLoader;
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
		    "activatedOn", this.activatedOn,
		    "activationTime", this.activationTime,
		    "activated", this.activated,
		    "author", this.author,
		    "bifs", Array.copyOf( this.bifs ),
		    "classLoader", this.moduleClassLoader,
		    "components", Array.copyOf( this.components ),
		    "customInterceptionPoints", Array.copyOf( this.customInterceptionPoints ),
		    "description", this.description,
		    "dependencies", Array.copyOf( this.dependencies ),
		    "enabled", this.enabled,
		    "Id", this.id,
		    "interceptors", Array.copyOf( this.interceptors ),
		    "invocationPath", this.invocationPath,
		    "jdbcDrivers", Array.of( this.jdbcDrivers.keySet().stream().map( Key::getName ).toArray() ),
		    "jarModule", this.jarModule,
		    "mapping", this.mapping.toStruct(),
		    "memberMethods", Array.copyOf( this.memberMethods ),
		    "name", this.name,
		    "nestedModules", Array.copyOf( this.nestedModules ),
		    "parentModule", this.parentModule,
		    "physicalPath", this.physicalPath.toString(),
		    "publicMapping", this.publicMapping.toStruct(),
		    "registeredOn", this.registeredOn,
		    "registrationTime", this.registrationTime,
		    "settings", this.settings,
		    "version", this.version,
		    "webURL", this.webURL );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Register a BoxLang based Component with the runtime
	 *
	 * @param targetFile The target file to register that represents the Component
	 *                   on disk
	 * @param context    The current context of execution
	 *
	 * @return The ModuleRecord
	 */
	private ModuleRecord registerComponent( File targetFile, IBoxContext context ) {
		// System.out.println( "Processing component: " + targetFile.getAbsolutePath()
		// );

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
		    ( context1, fnc ) -> new StringBuffer() ) );
		oComponent.getVariablesScope().put( Key.newBuilder, new DynamicFunction(
		    Key.newBuilder,
		    ( context1, fnc ) -> new StringBuilder() ) );

		// ProcessBody Delegate
		oComponent.getVariablesScope().put( Key.processBody, new DynamicFunction(
		    Key.processBody,
		    ( context1, fnc ) -> {
			    ArgumentsScope args	= context1.getArgumentsScope();

			    Object		buffer	= args.get( Key.buffer );

			    return oComponentProxy.processBody(
			        ( IBoxContext ) args.get( Key.context ),
			        ( ComponentBody ) args.get( Key.body ),
			        buffer instanceof StringBuffer ? ( StringBuffer ) buffer : context.getBuffer() );
		    },
		    new Argument[] {
		        new Argument( true, "any", Key.context ),
		        new Argument( true, "any", Key.body ),
		        new Argument( true, "any", Key.buffer )
		    } ) );
		// Get Name Delegate
		oComponent.getVariablesScope().put( Key.getName, new DynamicFunction(
		    Key.getName,
		    ( context1, fnc ) -> oComponent.bxGetName() ) );

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
		    BooleanCaster.cast( annotations.getOrDefault( "RequiresBody", false ) ),
		    BooleanCaster.cast( annotations.getOrDefault( "ignoreEnableOutputOnly", false ) ),
		    BooleanCaster.cast( annotations.getOrDefault( "autoEvaluateBodyExpressions", false ) ) );
		Key[]				componentAliases	= buildAnnotationAliases( oComponent, className, Key.boxComponent );

		// Register all components with their aliases
		for ( Key thisAlias : componentAliases ) {
			componentService.registerComponent( descriptor, thisAlias, true );
			this.logger.info(
			    "> Registered Module [{}] Component [{}] with alias [{}]",
			    this.name.getName(),
			    className.getName(),
			    thisAlias.getName() );
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
		    new BoxLangBIFProxy( oBIF ) );
		Key[]			bifAliases		= buildAnnotationAliases( oBIF, className, Key.boxBif );
		for ( Key bifAlias : bifAliases ) {
			// Register the mapping in the runtime
			functionService.registerGlobalFunction(
			    bifDescriptor,
			    bifAlias,
			    true );
			this.logger.info(
			    "> Registered Module [{}] BIF [{}] with alias [{}]",
			    this.name.getName(),
			    className.getName(),
			    bifAlias.getName() );
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
			        bifDescriptor ) );
			this.logger.info(
			    "> Registered Module [{}] MemberMethod [{}]",
			    this.name.getName(),
			    memberMethod );
			this.memberMethods.push( memberMethod );
		}

		return this;
	}

	/**
	 * Discover member methods by getting the {@code BoxMember} annotation on the
	 * Class.
	 *
	 * @param targetBIF The target BIF to discover member methods for
	 * @param className The class name of the BIF
	 *
	 * @return An array of member methods for the BIF: {@code [ { name : "",
	 *         objectArgument: "", type : BoxLangType } ] }
	 */
	private Array discoverMemberMethods( IClassRunnable targetBIF, Key className ) {
		// Get the BoxMember annotation
		Object boxMembers = targetBIF.getBoxMeta().getMeta().getAsStruct( Key.annotations ).getOrDefault( Key.boxMember,
		    null );

		// System.out.println( className.getName() + " BoxMembers Found [" + boxMembers
		// + "]" );

		// Case 0: If null, then we don't have any :)
		if ( boxMembers == null ) {
			return new Array();
		}

		// Case 1 : This is a simple String with no value, throw an exception
		// @BoxMember
		if ( boxMembers instanceof String castedBoxMember && castedBoxMember.isBlank() ) {
			throw new BoxRuntimeException(
			    className.getName() + " BoxMember annotation is missing it's type value, which is mandatory" );
		}

		// Case 2 : This is a simple String with a value which is the type. Validate it,
		// default it's record and return it
		// ClassName : ArrayFoo
		// @BoxMember "array" -> { "name": "foo", "objectArgument": null, type:
		// BoxLangType.ARRAY }
		if ( boxMembers instanceof String castedBoxMember && !castedBoxMember.isBlank() ) {
			// Validate the type is valid else throw an exception
			if ( !BoxLangType.isValid( castedBoxMember ) ) {
				throw new BoxRuntimeException(
				    className.getName() + " BoxMember annotation has an invalid type value [" + castedBoxMember
				        + "]" +
				        "Valid types are: " + Arrays.toString( BoxLangType.values() ) );
			}
			BoxLangType boxType = BoxLangType.valueOf( castedBoxMember.toUpperCase() );
			return Array.of(
			    Struct.of(
			        // Default member name for class ArrayFoo with BoxType of Array is just foo()
			        Key._NAME, className.getNameNoCase().replace( boxType.getKey().getNameNoCase(), "" ),
			        Key.objectArgument, "",
			        Key.type, boxType ) );
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
					    className.getName() + " BoxMember annotation has an invalid type value [" + type.getName()
					        + "]" +
					        "Valid types are: " + Arrays.toString( BoxLangType.values() ) );
				}

				// Now the value of this key must be a struct with the following keys: name,
				// objectArgument
				// Validate the value is a struct
				if ( ! ( entry.getValue() instanceof IStruct memberRecord ) ) {
					throw new BoxRuntimeException(
					    className.getName()
					        + " BoxMember annotation value must be a struct with the following keys: [name], [objectArgument]" );
				}

				// Prepare the record now
				BoxLangType boxType = BoxLangType.valueOf( type.getName().toUpperCase() );
				memberRecord.put( Key.type, boxType );
				memberRecord.computeIfAbsent( Key._NAME,
				    k -> className.getNameNoCase().replace( type.getNameNoCase(), "" ) );
				memberRecord.putIfAbsent( Key.objectArgument, "" );
				result.push( memberRecord );
			}

			return result;
		}

		// Who knows what this is, just return an empty struct
		return new Array();
	}

	/**
	 * Build an array of Key aliases for the BIF/Component based on the following
	 * rules:
	 * - If the target has any `{annotation}` annoations that have a value, use
	 * those
	 * - If the target has any `{annotation}` annoations that have no value, use the
	 * BIF name
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

		// Case 2 : This is a simple String with a value, return the value as the alias
		// instead of the name of the file on disk
		if ( annotations instanceof String castedAnnotationWithValue && !castedAnnotationWithValue.isBlank() ) {
			return new Key[] {
			    Key.of( castedAnnotationWithValue )
			};
		}

		// Case 3 : We have an Array of aliases, and they have values, return them
		// alongside the class name
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
		            "/bxModules/" + name.getName() + "/",
		            physicalPath.normalize().toString(),
		            ( this.invocationPath + "." + conventionsPath )
		                .replace( ".", Matcher.quoteReplacement( File.separator ) )
		                + File.separator
		                + targetFile.getName(),
		            targetFile.toPath() ),
		        context ) )
		    .invokeConstructor( context )
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

	/**
	 * Resolve the mapping for the module based on the targetMapping object
	 * which can be either a string or a struct.
	 *
	 * @param targetMapping The target mapping to resolve
	 *
	 * @return The resolved Mapping object
	 */
	private Mapping resolveMapping( Object targetMapping ) {
		Mapping result = null;

		// If it's a string, then we assume it's the name and not visibly externally
		if ( targetMapping instanceof String castedMapping && !castedMapping.isBlank() ) {
			result				= Mapping.of(
			    ModuleService.MODULE_MAPPING_PREFIX + castedMapping,
			    // Always points to the module path
			    this.path,
			    false
			);
			this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + castedMapping;
		}
		// If it's a struct, then we assume it's a full mapping definition
		// It must have a `name` key and optionally:
		// - usePrefix (boolean, default true)
		// - external (boolean, default false)
		// - path (string, relative from the module path, default is the module path)
		else if ( targetMapping instanceof IStruct structMapping ) {
			result				= mappingFromStruct( structMapping, false );
			this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + structMapping.getAsString( Key._name );
		}

		return result;
	}

	/**
	 * Create a Mapping from a struct definition
	 *
	 * @param definition        The struct definition
	 * @param isExternalDefault The default value for the external key if not
	 *
	 * @return The Mapping object
	 */
	private Mapping mappingFromStruct( IStruct definition, boolean isExternalDefault ) {
		// Default usePrefix to true
		boolean usePrefix = BooleanCaster.cast( definition.getOrDefault( Key.usePrefix, true ) );
		// If the definition doesn't have a name, use the module name
		if ( !definition.containsKey( Key._name ) || definition.getAsString( Key._name ).isBlank() ) {
			definition.put( Key._name, this.name.getName() );
		}

		// Compose the mapping name
		String mappingName = usePrefix
		    ? ModuleService.MODULE_MAPPING_PREFIX + definition.getAsString( Key._name )
		    : definition.getAsString( Key._name );

		// Compose the mapping path
		String mappingPath = this.path;
		// The path is relative to the module path, so compose it.
		if ( definition.containsKey( Key.path ) && !definition.getAsString( Key.path ).isBlank() ) {
			mappingPath = this.physicalPath.resolve( definition.getAsString( Key.path ) ).toString();
		}

		// Now create the mapping
		return Mapping.fromData(
		    mappingName,
		    mappingPath,
		    BooleanCaster.cast( definition.getOrDefault( Key.external, isExternalDefault ) )
		);
	}

	/**
	 * Resolve the public mapping for the module based on the targetMapping object
	 * which can be either a string or a struct.
	 *
	 * @param targetMapping The target mapping to resolve
	 *
	 * @return The resolved Mapping object
	 */
	private Mapping resolvePublicMapping( Object targetMapping ) {
		// If it's a string, then basically the value is the relative folder or 'path'
		// this.publicMapping = "public" -> /bxModules/{this.name}/public
		// this.publicMapping = "www" -> /bxModules/{this.name}/www
		if ( targetMapping instanceof String castedMapping && !castedMapping.isBlank() ) {
			return Mapping.of(
			    this.mapping.name() + castedMapping,
			    this.physicalPath.resolve( castedMapping ).toString(),
			    true
			);
		}

		// If it's a struct then
		// - name (optional) defaults to the module mapping
		// - usePrefix (optional, default true)
		// - path (relative from the module path)
		if ( targetMapping instanceof IStruct structMapping ) {
			return mappingFromStruct( structMapping, true );
		}

		// Return original mapping if we can't resolve it
		return this.publicMapping;
	}

}
