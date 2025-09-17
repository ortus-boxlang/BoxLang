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
package ortus.boxlang.runtime.config;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.config.segments.ExecutorConfig;
import ortus.boxlang.runtime.config.segments.IConfigSegment;
import ortus.boxlang.runtime.config.segments.LoggingConfig;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.config.segments.SchedulerConfig;
import ortus.boxlang.runtime.config.segments.SecurityConfig;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.util.DateTimeHelper;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;
import ortus.boxlang.runtime.util.LocalizationUtil;
import ortus.boxlang.runtime.util.Mapping;

/**
 * The BoxLang configuration object representing the core configuration.
 * This object is responsible for processing the configuration struct and
 * returning a new configuration object based on the overrides.
 * Each segment is processed individually from the initial configuration struct.
 * The configuration object can be converted to a struct for serialization.
 *
 * It also contains the original configuration struct for reference.
 *
 * @see IConfigSegment
 */
public class Configuration implements IConfigSegment {

	/**
	 * Change listener for mappings, etc. That will force a trailing slash on the key
	 * when the key is added to the struct.
	 */
	public static final IChangeListener<IStruct>	forceMappingTrailingSlash		= ( key, newValue, oldValue, object ) -> {
																						// Only fire for new values not ending with /
																						if ( newValue != null && !key.getName().endsWith( "/" ) ) {
																							object.remove( key );
																							object.put( Key.of( key.getName() + "/" ), newValue );
																							// don't insert original key
																							return null;
																						}
																						return newValue;
																					};

	/**
	 * --------------------------------------------------------------------------
	 * Configuration Keys
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The runtime version
	 */
	public String									version;

	/**
	 * The directory where the generated classes will be placed
	 * The default is the system temp directory + {@code /boxlang}
	 */
	public String									classGenerationDirectory		= System.getProperty( "java.io.tmpdir" ) + "boxlang";

	/**
	 * This setting if enabled will remove all the class files in the
	 * {@code classGenerationDirectory} on startup
	 * {@code false} by default
	 */
	public Boolean									clearClassFilesOnStartup		= false;

	/**
	 * The debug mode flag which turns on all kinds of debugging information
	 * {@code false} by default
	 */
	public Boolean									debugMode						= false;

	/**
	 * Turn on/off the resolver cache for Class Locators of Java/Box classes
	 * {@code true} by default
	 */
	public Boolean									classResolverCache				= true;

	/**
	 * Trusted cache setting - if enabled, once compiled a template will never be inspected for changes
	 */
	public Boolean									trustedCache					= false;

	/**
	 * The Timezone to use for the runtime;
	 * Uses the Java Timezone format: {@code America/New_York}
	 * Uses the default system timezone if not set
	 */
	public ZoneId									timezone						= TimeZone.getDefault().toZoneId();

	/**
	 * The default locale to use for the runtime
	 * Uses the default system locale if not set
	 */
	public Locale									locale							= Locale.getDefault();

	/**
	 * Enable whitespace compression in output. Only in use by the web runtimes currently.
	 */
	public boolean									whitespaceCompressionEnabled	= true;

	/**
	 * Invoke implicit getters and setters when using the implicit accessor
	 * {@code true} by default (defaulted in the BoxClassSupport class where it's used)
	 */
	public Boolean									invokeImplicitAccessor			= null;

	/**
	 * Use high precision math for all math operations, else it relies on Double
	 * precision
	 * {@code true} by default
	 */
	public Boolean									useHighPrecisionMath			= true;

	/**
	 * The maximum number of completed threads to track for a single request. Old threads will be flushed out to prevent memory from filling.
	 * This only applies to the "thread" component bx:thread name="mythread" {} which tracks execution status and scopes for the remainder of the request that fired it.
	 * ONLY threads which have been completed will be eligible to be flushed.
	 * Note: when the limit is reached, the thread component and related BIFs will no longer throw exceptions on invalid thread names, they will silently ignore attempts to interrupt or join those threads
	 */
	public Integer									maxTrackedCompletedThreads		= 1000;

	/**
	 * The application timeout
	 * {@code 0} means no timeout and is the default
	 */
	public Duration									applicationTimeout				= Duration.ofDays( 0 );

	/**
	 * The request timeout
	 * {@code 0} means no timeout and is the default
	 */
	public Duration									requestTimeout					= Duration.ofSeconds( 0 );;

	/**
	 * The session timeout
	 * {@code 30} minutes by default
	 */
	public Duration									sessionTimeout					= Duration.ofMinutes( 30 );

	/**
	 * This flag enables/disables session management in the runtime for all
	 * applications by default.
	 * {@code false} by default
	 */
	public Boolean									sessionManagement				= false;

	/**
	 * The default session storage cache. This has to be the name of a registered
	 * cache
	 * or the keyword "memory" which indicates our internal cache.
	 * {@code memory} is the default
	 */
	public String									sessionStorage					= "memory";

	/**
	 * This determines whether to send jSessionID cookies to the client browser.
	 * {@code true} by default
	 */
	public Boolean									setClientCookies				= true;

	/**
	 * Sets jSessionID cookies for a domain (not a host) Required, for applications
	 * running on clusters
	 * {@code true} by default
	 */
	public Boolean									setDomainCookies				= true;

	/**
	 * A sorted struct of mappings
	 */
	public IStruct									mappings						= new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR )
	    // ensure all keys to this struct have a trailing slash
	    .registerChangeListener( forceMappingTrailingSlash );

	/**
	 * An array of directories where modules are located and loaded from.
	 * {@code [ /{boxlang-home}/modules ]}
	 */
	public List<String>								modulesDirectory				= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/modules" ) );

	/**
	 * An array of directories where custom tags are located and loaded from.
	 * {@code [ /{boxlang-home}/global/components ]}
	 */
	public List<String>								customComponentsDirectory		= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/global/components" ) );

	/**
	 * An array of directories where box classes are located and loaded from.
	 */
	public List<String>								classPaths						= new ArrayList<>();

	/**
	 * An array of directories where jar files will be loaded from at runtime.
	 */
	public List<String>								javaLibraryPaths				= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/lib" ) );

	/**
	 * Cache registrations
	 */
	public IStruct									caches							= new Struct();

	/**
	 * Default datasource registration
	 */
	public String									defaultDatasource				= "";

	/**
	 * Global datasource registrations
	 */
	public IStruct									datasources						= new Struct();

	/**
	 * Default remote class method return format when executing a method from web
	 * runtimes.
	 * The default is JSON
	 */
	public String									defaultRemoteMethodReturnFormat	= "json";

	/**
	 * The modules configuration
	 */
	public IStruct									modules							= new Struct();

	/**
	 * The last config struct loaded
	 */
	public IStruct									originalConfig					= new Struct();

	/**
	 * A collection of all the registered global executors
	 */
	public IStruct									executors						= new Struct();

	/**
	 * Valid BoxLang class extensions
	 */
	public Set<String>								validClassExtensions			= new HashSet<>();

	/**
	 * Valid core BoxLang template extensions.
	 */
	public Set<String>								coreTemplateExtensions			= new HashSet<>(
	    Arrays.asList( "bxs", "bxm", "bxml", "cfm", "cfml", "cfs" )
	);

	/**
	 * Valid BoxLang template extensions.
	 * Private because I want to force people to use getValidTemplateExtensions(), which includes the core ones
	 */
	private Set<String>								validTemplateExtensions			= new HashSet<>();

	/**
	 * Experimental Features
	 */
	public IStruct									experimental					= new Struct();

	/**
	 * The scheduler configuration
	 */
	public SchedulerConfig							scheduler						= new SchedulerConfig();

	/**
	 * The security configuration
	 */
	public SecurityConfig							security						= new SecurityConfig();

	/**
	 * The logging configuration
	 */
	public LoggingConfig							logging							= new LoggingConfig();

	/**
	 * The container of runtimes configurations. Each runtime can collaborate settings by their name in this struct
	 */
	public IStruct									runtimes						= new Struct();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger						logger							= LoggerFactory.getLogger( Configuration.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Processes a configuration struct and returns a new configuration object based
	 * on the overrides.
	 *
	 * This method makes sure all elements in the incoming configuration struct are
	 * processed and applied to the configuration object.
	 *
	 * @param config the configuration struct
	 *
	 * @return The new configuration object based on the core + overrides
	 */
	public Configuration process( IStruct config ) {

		// Store original config
		this.originalConfig = config;

		// Debug Mode || Debbuging Enabled (cfconfig)
		if ( config.containsKey( Key.debugMode ) ) {
			this.debugMode = BooleanCaster.cast( PlaceholderHelper.resolve( config.get( Key.debugMode ) ) );
		}
		if ( config.containsKey( Key.debuggingEnabled ) ) {
			this.debugMode = BooleanCaster.cast( PlaceholderHelper.resolve( config.get( Key.debuggingEnabled ) ) );
		}

		// Class Resolver Cache
		if ( config.containsKey( Key.classResolverCache ) ) {
			this.classResolverCache = BooleanCaster.cast( PlaceholderHelper.resolve( config.get( Key.classResolverCache ) ) );
		}

		// Trusted Cache
		if ( config.containsKey( Key.trustedCache ) ) {
			this.trustedCache = BooleanCaster.cast( PlaceholderHelper.resolve( config.get( Key.trustedCache ) ) );
		}

		// Compiler
		if ( config.containsKey( Key.classGenerationDirectory ) ) {
			this.classGenerationDirectory = PlaceholderHelper.resolve( config.get( Key.classGenerationDirectory ) );
		}

		// Version
		if ( config.containsKey( Key.version ) ) {
			this.version = config.getAsString( Key.version );
		}

		// Clear Class Files on Startup
		if ( config.containsKey( Key.clearClassFilesOnStartup ) ) {
			this.clearClassFilesOnStartup = BooleanCaster.cast( PlaceholderHelper.resolve( config.get( Key.clearClassFilesOnStartup ) ) );
		}

		// Timezone
		if ( config.containsKey( Key.timezone )
		    &&
		    config.getAsString( Key.timezone ).length() > 0 ) {
			this.timezone = ZoneId.of( PlaceholderHelper.resolve( config.get( Key.timezone ) ) );
		}

		// Locale
		if ( config.containsKey( Key.locale )
		    &&
		    config.getAsString( Key.locale ).length() > 0 ) {
			this.locale = LocalizationUtil.parseLocale( PlaceholderHelper.resolve( config.getAsString( Key.locale ) ) );
		}

		// invokeImplicitAccessor
		if ( config.containsKey( Key.invokeImplicitAccessor ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.invokeImplicitAccessor ) ) )
			    .ifSuccessful( value -> this.invokeImplicitAccessor = value );
		}

		// whitespaceCompressionEnabled
		if ( config.containsKey( Key.whitespaceCompressionEnabled ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.whitespaceCompressionEnabled ) ) )
			    .ifSuccessful( value -> this.whitespaceCompressionEnabled = value );
		}

		// Use High Precision Math
		if ( config.containsKey( Key.useHighPrecisionMath ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.useHighPrecisionMath ) ) )
			    .ifSuccessful( value -> this.useHighPrecisionMath = value );
		}

		// maxTrackedCompletedThreads
		if ( config.containsKey( Key.maxTrackedCompletedThreads ) ) {
			IntegerCaster.attempt( PlaceholderHelper.resolve( config.get( Key.maxTrackedCompletedThreads ) ) )
			    .ifSuccessful( value -> this.maxTrackedCompletedThreads = value );
		}

		// Application Timeout
		if ( config.containsKey( Key.applicationTimeout )
		    && StringCaster.cast( config.get( "applicationTimeout" ) ).length() > 0 ) {
			this.applicationTimeout = DateTimeHelper
			    .timespanToDuration( PlaceholderHelper.resolve( config.get( "applicationTimeout" ) ) );
		}

		// Request Timeout
		if ( config.containsKey( Key.requestTimeout ) && StringCaster.cast( config.get( "requestTimeout" ) ).length() > 0 ) {
			this.requestTimeout = DateTimeHelper
			    .timespanToDuration( PlaceholderHelper.resolve( config.get( "requestTimeout" ) ) );
		}

		// Session Timeout
		if ( config.containsKey( Key.sessionTimeout ) && StringCaster.cast( config.get( "sessionTimeout" ) ).length() > 0 ) {
			this.sessionTimeout = DateTimeHelper
			    .timespanToDuration( PlaceholderHelper.resolve( config.get( "sessionTimeout" ) ) );
		}

		// Session Management
		if ( config.containsKey( Key.sessionManagement ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.sessionManagement ) ) )
			    .ifSuccessful( value -> this.sessionManagement = value );
		}

		// Session Storage
		if ( config.containsKey( Key.sessionStorage ) && StringCaster.cast( config.get( "sessionStorage" ) ).length() > 0 ) {
			this.sessionStorage = PlaceholderHelper.resolve( config.get( "sessionStorage" ) );
		}

		// Client Cookies
		if ( config.containsKey( Key.setClientCookies ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.setClientCookies ) ) )
			    .ifSuccessful( value -> this.setClientCookies = value );
		}

		// Domain Cookies
		if ( config.containsKey( Key.setDomainCookies ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.setDomainCookies ) ) )
			    .ifSuccessful( value -> this.setDomainCookies = value );
		}

		// Process mappings
		if ( config.containsKey( Key.mappings ) ) {
			if ( config.get( Key.mappings ) instanceof IStruct castedMap ) {
				castedMap.entrySet().forEach( entry -> {
					// Server-level mappings default to being external
					registerMapping(
					    entry.getKey(),
					    PlaceholderHelper.resolveAll( entry.getValue() ),
					    true
					);
				} );
			} else {
				logger.warn( "The [mappings] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process Modules directories
		if ( config.containsKey( Key.modulesDirectory ) ) {
			if ( config.get( Key.modulesDirectory ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> {
					var resolvedItem = PlaceholderHelper.resolve( item );
					if ( !this.modulesDirectory.contains( resolvedItem ) ) {
						this.modulesDirectory.add( resolvedItem );
					}
				} );
			} else {
				logger.warn( "The [modulesDirectory] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process customComponent directories
		if ( config.containsKey( Key.customComponentsDirectory ) ) {
			if ( config.get( Key.customComponentsDirectory ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> {
					var resolvedItem = PlaceholderHelper.resolve( item );
					if ( !this.customComponentsDirectory.contains( resolvedItem ) ) {
						this.customComponentsDirectory.add( resolvedItem );
					}
				} );
			} else {
				logger.warn( "The [customComponentsDirectory] configuration is not a JSON Array, ignoring it." );
			}
		}

		// process classPaths directories
		if ( config.containsKey( Key.classPaths ) ) {
			if ( config.get( Key.classPaths ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> {
					var resolvedItem = PlaceholderHelper.resolve( item );
					// Verify or add the path
					if ( !this.classPaths.contains( resolvedItem ) ) {
						this.classPaths.add( resolvedItem );
					}
				} );
			} else {
				logger.warn( "The [classPaths] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process javaLibraryPaths directories
		if ( config.containsKey( Key.javaLibraryPaths ) ) {
			if ( config.get( Key.javaLibraryPaths ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> {
					var resolvedItem = PlaceholderHelper.resolve( item );
					// Verify or add the path
					if ( !this.javaLibraryPaths.contains( resolvedItem ) ) {
						this.javaLibraryPaths.add( resolvedItem );
					}
				} );
			} else {
				logger.warn( "The [javaLibraryPaths] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process the default method return format
		if ( config.containsKey( Key.defaultRemoteMethodReturnFormat ) ) {
			this.defaultRemoteMethodReturnFormat = PlaceholderHelper
			    .resolve( config.get( Key.defaultRemoteMethodReturnFormat ) ).toLowerCase();
		}

		// Setup a 'default' cache, using the default cache configuration as it always needs to be present
		this.caches.put( Key._DEFAULT, new CacheConfig() );

		// Process declared cache configurations in the configuration
		if ( config.containsKey( Key.caches ) ) {
			if ( config.get( Key.caches ) instanceof IStruct castedCaches ) {
				castedCaches
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof IStruct castedStruct ) {
						    Key cacheName = KeyCaster.cast( entry.getKey() );
						    // Treat the default cache configuration as a special case, only process properties
						    // You can't change the name or the type of the default cache
						    if ( cacheName.equals( Key._DEFAULT ) ) {
							    CacheConfig defaultCacheConfig = ( CacheConfig ) this.caches.get( cacheName );
							    castedStruct.putIfAbsent( Key.properties, new Struct() );
							    defaultCacheConfig.processProperties( castedStruct.getAsStruct( Key.properties ) );
						    } else {
							    CacheConfig cacheConfig = new CacheConfig( cacheName ).process( castedStruct );
							    this.caches.put( cacheConfig.name, cacheConfig );
						    }
					    } else {
						    logger.warn(
						        "The [caches.{}] configuration is not a JSON Object, ignoring it.",
						        entry.getKey().getName()
						    );
					    }
				    } );
			} else {
				logger.warn( "The [caches] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process executors
		if ( config.containsKey( Key.executors ) ) {
			if ( config.get( Key.executors ) instanceof IStruct castedExecutors ) {
				// Process each executor configuration
				castedExecutors
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof IStruct castedMap ) {
						    ExecutorConfig executorConfig = new ExecutorConfig( entry.getKey() )
						        .process( StructCaster.cast( castedMap ) );
						    this.executors.put( executorConfig.name, executorConfig );
					    } else {
						    logger.warn( "The [executors.{}] configuration is not a JSON Object, ignoring it.",
						        entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [executors] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process validClassExtensions
		if ( config.containsKey( Key.validClassExtensions ) ) {
			if ( config.get( Key.validClassExtensions ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList
				    .forEach( item -> this.validClassExtensions.add( PlaceholderHelper.resolve( item ).toLowerCase() ) );
			} else {
				logger.warn( "The [validClassExtensions] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process validtemplateExtensions
		if ( config.containsKey( Key.validTemplateExtensions ) ) {
			if ( config.get( Key.validTemplateExtensions ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach(
				    item -> this.validTemplateExtensions.add( PlaceholderHelper.resolve( item ).toLowerCase() ) );
			} else {
				logger.warn( "The [validTemplateExtensions] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process experimentals map
		if ( config.containsKey( Key.experimental ) ) {
			if ( config.get( Key.experimental ) instanceof IStruct castedStruct ) {
				castedStruct.entrySet().forEach( entry -> this.experimental.put( entry.getKey(), PlaceholderHelper.resolve( entry.getValue() ) ) );
			} else {
				logger.warn( "The [experimental] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process default datasource configuration
		if ( config.containsKey( Key.defaultDatasource ) ) {
			this.defaultDatasource = PlaceholderHelper.resolve( config.get( Key.defaultDatasource ) );
		}

		// Process Datasource Configurations
		if ( config.containsKey( Key.datasources ) ) {
			if ( config.get( Key.datasources ) instanceof IStruct castedDataSources ) {
				// Process each datasource configuration
				castedDataSources
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof IStruct castedStruct ) {
						    IStruct eventData = Struct.ofNonConcurrent(
						        Key._name, entry.getKey(),
						        Key.properties, castedStruct
						    );
						    BoxRuntime.getInstance().announce( BoxEvent.ON_DATASOURCE_CONFIG_LOAD, eventData );

						    DatasourceConfig datasourceConfig = new DatasourceConfig( eventData.getAsKey( Key._name ) )
						        .process( eventData.getAsStruct( Key.properties ) );
						    this.datasources.put( datasourceConfig.name, datasourceConfig );
					    } else {
						    logger.warn(
						        "The [datasources.{}] configuration is not a JSON Object, ignoring it.",
						        entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [datasources] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process modules
		if ( config.containsKey( Key.modules ) ) {
			if ( config.get( Key.modules ) instanceof IStruct castedModules ) {
				// Process each module configuration
				castedModules
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof IStruct castedMap ) {
						    ModuleConfig moduleConfig = new ModuleConfig( entry.getKey().getName() )
						        .process( castedMap );
						    this.modules.put( moduleConfig.name, moduleConfig );
					    } else {
						    logger.warn( "The [modules.{}] configuration is not a JSON Object, ignoring it.",
						        entry.getKey() );
					    }
				    } );

			} else {
				logger.warn( "The [modules] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process runtimes
		if ( config.containsKey( Key.runtimes ) ) {
			this.runtimes = config.getAsStruct( Key.runtimes );
		}

		// Process our security configuration
		if ( config.containsKey( Key.security ) ) {
			security.process( StructCaster.cast( config.get( Key.security ) ) );
		}

		// Process our scheduler configuration
		if ( config.containsKey( Key.scheduler ) ) {
			scheduler.process( StructCaster.cast( config.get( Key.scheduler ) ) );
		}

		// Process our logging configuration
		if ( config.containsKey( Key.logging ) ) {
			logging.process( StructCaster.cast( config.get( Key.logging ) ) );
		}

		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Mapping Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get all the mappings back as an array of strings in their length order
	 *
	 * @return The mappings as an array of strings
	 */
	public String[] getRegisteredMappings() {
		return this.mappings.keySet().stream()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * Verify if a mapping exists
	 *
	 * @param mapping The mapping to verify: {@code /myMapping}, please note the
	 *                leading slash
	 *
	 * @return True if the mapping exists, false otherwise
	 */
	public boolean hasMapping( String mapping ) {
		return this.hasMapping( Key.of( mapping ) );
	}

	/**
	 * Verify if a mapping exists
	 *
	 * @param mapping The mapping to verify: {@code /myMapping}, please note the
	 *                leading slash
	 *
	 * @return True if the mapping exists, false otherwise
	 */
	public boolean hasMapping( Key mapping ) {
		// Check if mapping has a leading slash else add it
		if ( !mapping.getName().startsWith( "/" ) ) {
			mapping = Key.of( "/" + mapping.getName() );
		}
		// Add traiing slash
		if ( !mapping.getName().endsWith( "/" ) ) {
			mapping = Key.of( mapping.getName() + "/" );
		}

		return this.mappings.containsKey( mapping );
	}

	/**
	 * Register a mapping in the runtime configuration
	 *
	 * @param name The mapping to register: {@code /myMapping}, please note the
	 *             leading slash
	 * @param data The absolute path to the directory to map to the mapping
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( String name, Object data ) {
		return this.registerMapping( Key.of( name ), data );
	}

	/**
	 * Register a mapping in the runtime configuration
	 * The mapping will default to not being external
	 *
	 * @param name The mapping to register: {@code /myMapping}, please note the
	 *             leading slash
	 * @param data The absolute path to the directory to map to the mapping
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( Key name, Object data ) {
		return registerMapping( name, data, false );
	}

	/**
	 * Register a mapping in the runtime configuration.
	 * The mapping will default to not being external
	 *
	 * @param name            The mapping to register: {@code /myMapping}, please note the
	 *                        leading slash
	 * @param data            The absolute path to the directory to map to the mapping
	 * @param defaultExternal If this mapping defaults to being external
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( String name, Object data, boolean defaultExternal ) {
		return this.registerMapping( Key.of( name ), data, defaultExternal );
	}

	/**
	 * Register a mapping in the runtime configuration
	 *
	 * @param name            The mapping to register: {@code /myMapping}, please note the
	 *                        leading slash
	 * @param data            The absolute path to the directory to map to the mapping
	 * @param defaultExternal If this mapping defaults to being external
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( Key name, Object data, boolean defaultExternal ) {
		var m = Mapping.fromData( name.getName(), data, defaultExternal );
		this.mappings.put( m.name(), m );
		return this;
	}

	/**
	 * Register a mapping in the runtime configuration
	 *
	 * @param mapping The mapping to register
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( Mapping mapping ) {
		this.mappings.put( mapping.name(), mapping );
		return this;
	}

	/**
	 * Unregister a mapping in the runtime configuration using a {@link Key}
	 *
	 * @param name The Key mapping to unregister: {@code /myMapping}, please note
	 *             the leading slash
	 *
	 * @return True if the mapping was removed, false otherwise
	 */
	public boolean unregisterMapping( Key name ) {
		return unregisterMapping( name.getName() );
	}

	/**
	 * Unregister a mapping in the runtime configuration
	 *
	 * @param mapping The mapping to unregister
	 *
	 * @return True if the mapping was removed, false otherwise
	 */
	public boolean unregisterMapping( Mapping mapping ) {
		return unregisterMapping( mapping.name() );
	}

	/**
	 * Unregister a mapping in the runtime configuration
	 *
	 * @param name The String mapping to unregister: {@code /myMapping}, please
	 *             note the leading slash
	 *
	 * @return True if the mapping was removed, false otherwise
	 */
	public boolean unregisterMapping( String name ) {
		return this.mappings.remove( Key.of( Mapping.cleanName( name ) ) ) != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the java library paths as an array of URLs of Jar files
	 * This is usually called by the runtime to load all the JARs in the paths to
	 * the runtime classloader
	 *
	 * @throws BoxIOException If a path is not a valid path
	 *
	 * @return The java library paths as an array of Jar/class URLs
	 */
	public URL[] getJavaLibraryPaths() {
		return this.javaLibraryPaths
		    .stream()
		    // Filter out paths that don't exist
		    .filter( path -> Paths.get( path ).toFile().exists() )
		    .map( path -> {
			    try {
				    Path targetPath = Paths.get( path );
				    // If this is a directory, then get all the JARs and classes in the directory
				    // else if it's a jar/class file then just return the URL
				    if ( Files.isDirectory( targetPath ) ) {
					    return DynamicClassLoader.getJarURLs( targetPath );
				    } else {
					    return new URL[] { targetPath.toUri().toURL() };
				    }
			    } catch ( IOException e ) {
				    throw new BoxIOException( path + " is not a valid path", e );
			    }
		    } )
		    .flatMap( Arrays::stream )
		    .distinct()
		    // .peek( url -> logger.debug( "Get java library URL: [{}]", url ) )
		    .toArray( URL[]::new );
	}

	/**
	 * Navigate the configuration with our cool DataNavigator!
	 *
	 * For example, if you want to navigate the original config:
	 *
	 * <pre>
	 * config.navigate( "originalConfig" )
	 * </pre>
	 *
	 * @param path The path to the object in the data structure. By default it's the
	 *             root.
	 *
	 * @return The navigator with a potential navigation path set
	 */
	public Navigator navigate( String... path ) {
		return DataNavigator.of( asStruct() ).from( path );
	}

	/**
	 * This returns all valid BoxLang extensions for classes and templates.
	 *
	 * @return A set of all valid class extensions
	 */
	public Set<String> getValidExtensions() {
		Set<String> extensions = new HashSet<>();
		extensions.addAll( this.validClassExtensions );
		extensions.addAll( getValidTemplateExtensions() );
		return extensions;
	}

	/**
	 * This returns all valid BoxLang class extensions as a Set.
	 * THis includes core extensions and custom extensions
	 *
	 * @return A list of all valid class extensions
	 */
	public Set<String> getValidTemplateExtensions() {
		Set<String> extensions = new HashSet<>();
		extensions.addAll( this.coreTemplateExtensions );
		extensions.addAll( this.validTemplateExtensions );
		return extensions;
	}

	/**
	 * This returns all valid BoxLang class extensions as a List.
	 *
	 * @return A list of all valid class extensions
	 */
	public List<String> getValidTemplateExtensionsList() {
		return new ArrayList<>( getValidTemplateExtensions() );
	}

	/**
	 * This returns all valid BoxLang class extensions.
	 *
	 * @return A list of all valid class extensions
	 */
	public List<String> getValidClassExtensionsList() {
		return new ArrayList<>( this.validClassExtensions );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Conversion
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the configuration as a struct with all the static typed segments
	 * and the <code>originalConfig</code> as a segment as well.
	 *
	 * @return A struct representation of the configuration segment
	 */
	public IStruct asStruct() {
		IStruct mappingsCopy = new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR ).registerChangeListener( forceMappingTrailingSlash );
		mappingsCopy.putAll( this.mappings );

		IStruct cachesCopy = new Struct( false );
		this.caches.keySet()
		    .forEach( key -> cachesCopy.put( key, ( ( CacheConfig ) this.caches.get( key ) ).toStruct() ) );

		IStruct executorsCopy = new Struct( false );
		this.executors.keySet()
		    .forEach( key -> executorsCopy.put( key, ( ( ExecutorConfig ) this.executors.get( key ) ).toStruct() ) );

		IStruct datasourcesCopy = new Struct( false );
		this.datasources.keySet()
		    .forEach( key -> datasourcesCopy.put( key, ( ( DatasourceConfig ) this.datasources.get( key ) ).asStruct() ) );

		IStruct modulesCopy = new Struct( false );
		this.modules.keySet()
		    .forEach( key -> modulesCopy.put( key, ( ( ModuleConfig ) this.modules.get( key ) ).asStruct() ) );

		IStruct runtimesCopy = new Struct( false );
		this.runtimes.keySet()
		    .forEach( key -> runtimesCopy.put( key, this.runtimes.get( key ) ) );

		return Struct.ofNonConcurrent(
		    Key.applicationTimeout, this.applicationTimeout,
		    Key.caches, cachesCopy,
		    Key.classGenerationDirectory, this.classGenerationDirectory,
		    Key.clearClassFilesOnStartup, this.clearClassFilesOnStartup,
		    Key.customComponentsDirectory, Array.copyFromList( this.customComponentsDirectory ),
		    Key.classPaths, Array.copyFromList( this.classPaths ),
		    Key.datasources, datasourcesCopy,
		    Key.debugMode, this.debugMode,
		    Key.classResolverCache, this.classResolverCache,
		    Key.defaultDatasource, this.defaultDatasource,
		    Key.defaultRemoteMethodReturnFormat, this.defaultRemoteMethodReturnFormat,
		    Key.executors, executorsCopy,
		    Key.experimental, Struct.fromMap( this.experimental ),
		    Key.invokeImplicitAccessor, this.invokeImplicitAccessor,
		    Key.whitespaceCompressionEnabled, this.whitespaceCompressionEnabled,
		    Key.javaLibraryPaths, Array.copyFromList( this.javaLibraryPaths ),
		    Key.locale, this.locale,
		    // Key.logging, this.logging.asStruct(),
		    Key.mappings, mappingsCopy,
		    Key.modules, modulesCopy,
		    Key.modulesDirectory, Array.copyFromList( this.modulesDirectory ),
		    Key.originalConfig, this.originalConfig,
		    Key.requestTimeout, this.requestTimeout,
		    Key.runtimes, runtimesCopy,
		    Key.sessionManagement, this.sessionManagement,
		    Key.sessionStorage, this.sessionStorage,
		    Key.sessionTimeout, this.sessionTimeout,
		    Key.setClientCookies, this.setClientCookies,
		    Key.setDomainCookies, this.setDomainCookies,
		    Key.security, this.security.asStruct(),
		    Key.scheduler, this.scheduler.asStruct(),
		    Key.timezone, this.timezone,
		    Key.trustedCache, this.trustedCache,
		    Key.useHighPrecisionMath, this.useHighPrecisionMath,
		    Key.maxTrackedCompletedThreads, this.maxTrackedCompletedThreads,
		    Key.validExtensions, Array.fromSet( getValidExtensions() ),
		    Key.validClassExtensions, Array.fromSet( this.validClassExtensions ),
		    Key.validTemplateExtensions, Array.fromSet( getValidTemplateExtensions() ),
		    Key.version, this.version
		);
	}
}
