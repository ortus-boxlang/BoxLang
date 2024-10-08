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
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.config.segments.DatasourceConfig;
import ortus.boxlang.runtime.config.segments.ExecutorConfig;
import ortus.boxlang.runtime.config.segments.IConfigSegment;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.DateTimeHelper;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;
import ortus.boxlang.runtime.util.LocalizationUtil;

/**
 * The BoxLang configuration object representing the core configuration.
 * This object is responsible for processing the configuration struct and returning a new configuration object based on the overrides.
 * Each segment is processed individually from the initial configuration struct.
 * The configuration object can be converted to a struct for serialization.
 *
 * It also contains the original configuration struct for reference.
 *
 * @see IConfigSegment
 */
public class Configuration implements IConfigSegment {

	/**
	 * --------------------------------------------------------------------------
	 * Configuration Keys
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The directory where the generated classes will be placed
	 * The default is the system temp directory + {@code /boxlang}
	 */
	public String				classGenerationDirectory			= System.getProperty( "java.io.tmpdir" ) + "boxlang";

	/**
	 * The debug mode flag which turns on all kinds of debugging information
	 * {@code false} by default
	 */
	public Boolean				debugMode							= false;

	/**
	 * The Timezone to use for the runtime;
	 * Uses the Java Timezone format: {@code America/New_York}
	 * Uses the default system timezone if not set
	 */
	public ZoneId				timezone							= TimeZone.getDefault().toZoneId();

	/**
	 * The default locale to use for the runtime
	 * Uses the default system locale if not set
	 */
	public Locale				locale								= Locale.getDefault();

	/**
	 * Invoke implicit getters and setters when using the implicit accessor
	 * {@code true} by default
	 */
	public Boolean				invokeImplicitAccessor				= true;

	/**
	 * Use high precision math for all math operations, else it relies on Double precision
	 * {@code true} by default
	 */
	public Boolean				useHighPrecisionMath				= true;

	/**
	 * The application timeout
	 * {@code 0} means no timeout and is the default
	 */
	public Duration				applicationTimeout					= Duration.ofDays( 0 );

	/**
	 * The request timeout
	 * {@code 0} means no timeout and is the default
	 */
	public Duration				requestTimeout						= Duration.ofSeconds( 0 );;

	/**
	 * The session timeout
	 * {@code 30} minutes by default
	 */
	public Duration				sessionTimeout						= Duration.ofMinutes( 30 );

	/**
	 * This flag enables/disables session management in the runtime for all applications by default.
	 * {@code false} by default
	 */
	public Boolean				sessionManagement					= false;

	/**
	 * The default session storage cache. This has to be the name of a registered cache
	 * or the keyword "memory" which indicates our internal cache.
	 * {@code memory} is the default
	 */
	public String				sessionStorage						= "memory";

	/**
	 * This determines whether to send CFID and CFTOKEN cookies to the client browser.
	 * {@code true} by default
	 */
	public Boolean				setClientCookies					= true;

	/**
	 * Sets CFID and CFTOKEN cookies for a domain (not a host) Required, for applications running on clusters
	 * {@code true} by default
	 */
	public Boolean				setDomainCookies					= true;

	/**
	 * A sorted struct of mappings
	 */
	public IStruct				mappings							= new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR );

	/**
	 * An array of directories where modules are located and loaded from.
	 * {@code [ /{boxlang-home}/modules ]}
	 */
	public List<String>			modulesDirectory					= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/modules" ) );

	/**
	 * The default logs directory for the runtime
	 */
	public String				logsDirectory						= Paths.get( BoxRuntime.getInstance().getRuntimeHome().toString(), "/logs" ).normalize()
	    .toString();

	/**
	 * An array of directories where custom tags are located and loaded from.
	 * {@code [ /{boxlang-home}/customTags ]}
	 */
	public List<String>			customTagsDirectory					= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/customTags" ) );

	/**
	 * An array of directories where jar files will be loaded from at runtime.
	 */
	public List<String>			javaLibraryPaths					= new ArrayList<>(
	    Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/lib" ) );

	/**
	 * Cache registrations
	 */
	public IStruct				caches								= new Struct();

	/**
	 * Default datasource registration
	 */
	public String				defaultDatasource					= "";

	/**
	 * Global datasource registrations
	 */
	public IStruct				datasources							= new Struct();

	/**
	 * Default remote class method return format when executing a method from web runtimes.
	 * The default is JSON
	 */
	public String				defaultRemoteMethodReturnFormat		= "json";

	/**
	 * Default cache registration
	 */
	public CacheConfig			defaultCache						= new CacheConfig();

	/**
	 * The modules configuration
	 */
	public IStruct				modules								= new Struct();

	/**
	 * The last config struct loaded
	 */
	public IStruct				originalConfig						= new Struct();

	/**
	 * A collection of all the registered global executors
	 */
	public IStruct				executors							= new Struct();

	/**
	 * File extensions which are disallowed for file operations. The allowed array overrides any items in the disallow list.
	 */
	public List<String>			allowedFileOperationExtensions		= new ArrayList<>();
	public List<String>			disallowedFileOperationExtensions	= new ArrayList<>();

	/**
	 * Valid BoxLang class extensions
	 */
	public Set<String>			validClassExtensions				= new HashSet<>();

	/**
	 * Valid BoxLang template extensions
	 */
	public Set<String>			validTemplateExtensions				= new HashSet<>();

	/**
	 * Experimental Features
	 */
	public IStruct				experimental						= new Struct();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger	logger								= LoggerFactory.getLogger( Configuration.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Processes a configuration struct and returns a new configuration object based on the overrides.
	 *
	 * This method makes sure all elements in the incoming configuration struct are processed and applied to the configuration object.
	 *
	 * @param config the configuration struct
	 *
	 * @return The new configuration object based on the core + overrides
	 */
	public Configuration process( IStruct config ) {
		// Store original config
		this.originalConfig = config;

		// Debug Mode
		if ( config.containsKey( "debugMode" ) ) {
			this.debugMode = ( Boolean ) config.get( "debugMode" );
		}

		// Compiler
		if ( config.containsKey( "classGenerationDirectory" ) ) {
			this.classGenerationDirectory = PlaceholderHelper.resolve( config.get( "classGenerationDirectory" ) );
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

		// Use High Precision Math
		if ( config.containsKey( Key.useHighPrecisionMath ) ) {
			BooleanCaster.attempt( PlaceholderHelper.resolve( config.get( Key.useHighPrecisionMath ) ) )
			    .ifSuccessful( value -> this.useHighPrecisionMath = value );
		}

		// Application Timeout
		if ( config.containsKey( Key.applicationTimeout ) && StringCaster.cast( config.get( "applicationTimeout" ) ).length() > 0 ) {
			this.applicationTimeout = DateTimeHelper.timespanToDuration( PlaceholderHelper.resolve( config.get( "applicationTimeout" ) ) );
		}

		// Request Timeout
		if ( config.containsKey( Key.requestTimeout ) && StringCaster.cast( config.get( "requestTimeout" ) ).length() > 0 ) {
			this.requestTimeout = DateTimeHelper.timespanToDuration( PlaceholderHelper.resolve( config.get( "requestTimeout" ) ) );
		}

		// Session Timeout
		if ( config.containsKey( Key.sessionTimeout ) && StringCaster.cast( config.get( "sessionTimeout" ) ).length() > 0 ) {
			this.sessionTimeout = DateTimeHelper.timespanToDuration( PlaceholderHelper.resolve( config.get( "sessionTimeout" ) ) );
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
			if ( config.get( Key.mappings ) instanceof Map<?, ?> castedMap ) {
				castedMap.forEach( ( key, value ) -> this.mappings.put(
				    Key.of( key ),
				    PlaceholderHelper.resolve( value )
				) );
			} else {
				logger.warn( "The [runtime.mappings] configuration is not a JSON Object, ignoring it." );
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
				logger.warn( "The [runtime.modulesDirectory] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process customTags directories
		if ( config.containsKey( Key.customTagsDirectory ) ) {
			if ( config.get( Key.customTagsDirectory ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> {
					var resolvedItem = PlaceholderHelper.resolve( item );
					if ( !this.customTagsDirectory.contains( resolvedItem ) ) {
						this.customTagsDirectory.add( resolvedItem );
					}
				} );
			} else {
				logger.warn( "The [runtime.customTagsDirectory] configuration is not a JSON Array, ignoring it." );
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
			this.defaultRemoteMethodReturnFormat = PlaceholderHelper.resolve( config.get( Key.defaultRemoteMethodReturnFormat ) ).toLowerCase();
		}

		// Process default cache configuration
		if ( config.containsKey( Key.defaultCache ) ) {
			if ( config.get( Key.defaultCache ) instanceof Map<?, ?> castedMap ) {
				this.defaultCache = new CacheConfig().processProperties( new Struct( castedMap ) );
			} else {
				logger.warn( "The [runtime.defaultCache] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process declared cache configurations
		if ( config.containsKey( Key.caches ) ) {
			if ( config.get( Key.caches ) instanceof Map<?, ?> castedCaches ) {
				// Process each cache configuration
				castedCaches
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof Map<?, ?> castedMap ) {
						    CacheConfig cacheConfig = new CacheConfig( ( String ) entry.getKey() ).process( new Struct( castedMap ) );
						    this.caches.put( cacheConfig.name, cacheConfig );
					    } else {
						    logger.warn( "The [caches.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [caches] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process executors
		if ( config.containsKey( Key.executors ) ) {
			if ( config.get( Key.executors ) instanceof Map<?, ?> castedExecutors ) {
				// Process each executor configuration
				castedExecutors
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof Map<?, ?> castedMap ) {
						    ExecutorConfig executorConfig = new ExecutorConfig( ( String ) entry.getKey() ).process( new Struct( castedMap ) );
						    this.executors.put( executorConfig.name, executorConfig );
					    } else {
						    logger.warn( "The [executors.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
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
				castedList.forEach( item -> this.validClassExtensions.add( PlaceholderHelper.resolve( item ).toLowerCase() ) );
			} else {
				logger.warn( "The [validClassExtensions] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process validtemplateExtensions
		if ( config.containsKey( Key.validTemplateExtensions ) ) {
			if ( config.get( Key.validTemplateExtensions ) instanceof List<?> castedList ) {
				// iterate and add to the original list if it doesn't exist
				castedList.forEach( item -> this.validTemplateExtensions.add( PlaceholderHelper.resolve( item ).toLowerCase() ) );
			} else {
				logger.warn( "The [validTemplateExtensions] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process experimentals map
		if ( config.containsKey( Key.experimental ) ) {
			if ( config.get( Key.experimental ) instanceof Map<?, ?> castedMap ) {
				castedMap.forEach( ( key, value ) -> this.experimental.put( Key.of( key ), PlaceholderHelper.resolve( value ) ) );
			} else {
				logger.warn( "The [runtime.experimental] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process default datasource configuration
		if ( config.containsKey( Key.defaultDatasource ) ) {
			this.defaultDatasource = PlaceholderHelper.resolve( config.get( Key.defaultDatasource ) );
		}

		// Process Datasource Configurations
		if ( config.containsKey( Key.datasources ) ) {
			if ( config.get( Key.datasources ) instanceof Map<?, ?> castedDataSources ) {
				// Process each datasource configuration
				castedDataSources
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof Map<?, ?> castedMap ) {
						    DatasourceConfig datasourceConfig = new DatasourceConfig( Key.of( entry.getKey() ) ).process( new Struct( castedMap ) );
						    this.datasources.put( datasourceConfig.name, datasourceConfig );
					    } else {
						    logger.warn( "The [runtime.datasources.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [runtime.datasources] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process modules
		if ( config.containsKey( Key.modules ) ) {
			if ( config.get( Key.modules ) instanceof Map<?, ?> castedModules ) {
				// Process each module configuration
				castedModules
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof Map<?, ?> castedMap ) {
						    ModuleConfig moduleConfig = new ModuleConfig( KeyCaster.cast( entry.getKey() ).getName() ).process( new Struct( castedMap ) );
						    this.modules.put( moduleConfig.name, moduleConfig );
					    } else {
						    logger.warn( "The [runtime.modules.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
					    }
				    } );

			} else {
				logger.warn( "The [runtime.modules] configuration is not a JSON Object, ignoring it." );
			}
		}

		// File operation safety keys
		if ( config.containsKey( Key.allowedFileOperationExtensions ) ) {
			if ( config.get( Key.allowedFileOperationExtensions ) instanceof String ) {
				config.put( Key.allowedFileOperationExtensions,
				    ListUtil.asList( config.getAsString( Key.allowedFileOperationExtensions ), ListUtil.DEFAULT_DELIMITER ) );
			}

			// For some reason we have to re-cast this through a stream. Attempting to cast it directly throws a ClassCastException
			this.allowedFileOperationExtensions = ArrayCaster.cast( config.get( Key.allowedFileOperationExtensions ) ).stream().map( StringCaster::cast )
			    .toList();
		}

		if ( config.containsKey( Key.disallowedFileOperationExtensions ) ) {
			if ( config.get( Key.disallowedFileOperationExtensions ) instanceof String ) {
				config.put( Key.disallowedFileOperationExtensions,
				    ListUtil.asList( config.getAsString( Key.disallowedFileOperationExtensions ), ListUtil.DEFAULT_DELIMITER ) );
			}
			// For some reason we have to re-cast this through a stream. Attempting to cast it directly throws a ClassCastException
			this.disallowedFileOperationExtensions = ArrayCaster.cast( config.get( Key.disallowedFileOperationExtensions ) ).stream().map( StringCaster::cast )
			    .toList();
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
	 * @param mapping The mapping to verify: {@code /myMapping}, please note the leading slash
	 *
	 * @return True if the mapping exists, false otherwise
	 */
	public boolean hasMapping( String mapping ) {
		return this.hasMapping( Key.of( mapping ) );
	}

	/**
	 * Verify if a mapping exists
	 *
	 * @param mapping The mapping to verify: {@code /myMapping}, please note the leading slash
	 *
	 * @return True if the mapping exists, false otherwise
	 */
	public boolean hasMapping( Key mapping ) {
		// Check if mapping has a leading slash else add it
		if ( !mapping.getName().startsWith( "/" ) ) {
			mapping = Key.of( "/" + mapping.getName() );
		}
		return this.mappings.containsKey( mapping );
	}

	/**
	 * Register a mapping in the runtime configuration
	 *
	 * @param mapping The mapping to register: {@code /myMapping}, please note the leading slash
	 * @param path    The absolute path to the directory to map to the mapping
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( String mapping, String path ) {
		return this.registerMapping( Key.of( mapping ), path );
	}

	/**
	 * Register a mapping in the runtime configuration
	 *
	 * @param mapping The mapping to register: {@code /myMapping}, please note the leading slash
	 * @param path    The absolute path to the directory to map to the mapping
	 *
	 * @throws BoxRuntimeException If the path does not exist
	 *
	 * @return The runtime configuration
	 */
	public Configuration registerMapping( Key mapping, String path ) {
		// Check if mapping has a leading slash else add it
		if ( !mapping.getName().startsWith( "/" ) ) {
			mapping = Key.of( "/" + mapping.getName() );
		}

		// Convert the path to a Java Path
		Path pathObj = Path.of( path ).toAbsolutePath();

		// Verify it exists else throw an exception
		if ( !pathObj.toFile().exists() ) {
			throw new BoxRuntimeException(
			    String.format( "The path [%s] does not exist.", pathObj )
			);
		}

		// Now we can add it
		this.mappings.put( mapping, pathObj.toString() );

		return this;
	}

	/**
	 * Unregister a mapping in the runtime configuration
	 *
	 * @param mapping The String mapping to unregister: {@code /myMapping}, please note the leading slash
	 *
	 * @return True if the mapping was removed, false otherwise
	 */
	public boolean unregisterMapping( String mapping ) {
		return this.unregisterMapping( Key.of( mapping ) );
	}

	/**
	 * Unregister a mapping in the runtime configuration using a {@link Key}
	 *
	 * @param mapping The Key mapping to unregister: {@code /myMapping}, please note the leading slash
	 *
	 * @return True if the mapping was removed, false otherwise
	 */
	public boolean unregisterMapping( Key mapping ) {
		// Check if mapping has a leading slash else add it
		if ( !mapping.getName().startsWith( "/" ) ) {
			mapping = Key.of( "/" + mapping.getName() );
		}

		return this.mappings.remove( mapping ) != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the java library paths as an array of URLs of Jar files
	 * This is usually called by the runtime to load all the JARs in the paths to the runtime classloader
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
	 * Helper method to validate datasource drivers configured in the runtime configuration
	 * This makes sure all declared drivers are registered with the datasource service
	 *
	 * @throws BoxRuntimeException If a datasource driver is not registered with the datasource service
	 */
	public void validateDatsourceDrivers() {
		// iterate over all datasources and validate the drivers exists in the datasource service, else throw an exception
		this.datasources.entrySet().forEach( entry -> {
			DatasourceConfig datasource = ( DatasourceConfig ) entry.getValue();
			if ( !BoxRuntime.getInstance().getDataSourceService().hasDriver( datasource.getDriver() ) ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The datasource [%s] has a driver [%s] that is not registered with the datasource service.",
				        datasource.name,
				        datasource.getDriver()
				    )
				);
			}
		} );
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
	 * @param path The path to the object in the data structure. By default it's the root.
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
		extensions.addAll( this.validTemplateExtensions );
		return extensions;
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
		IStruct mappingsCopy = new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR );
		mappingsCopy.putAll( this.mappings );

		IStruct cachesCopy = new Struct();
		this.caches.entrySet().forEach( entry -> cachesCopy.put( entry.getKey(), ( ( CacheConfig ) entry.getValue() ).toStruct() ) );

		IStruct executorsCopy = new Struct();
		this.executors.entrySet().forEach( entry -> executorsCopy.put( entry.getKey(), ( ( ExecutorConfig ) entry.getValue() ).toStruct() ) );

		IStruct datsourcesCopy = new Struct();
		this.datasources.entrySet().forEach( entry -> datsourcesCopy.put( entry.getKey(), ( ( DatasourceConfig ) entry.getValue() ).asStruct() ) );

		IStruct modulesCopy = new Struct();
		this.modules.entrySet().forEach( entry -> modulesCopy.put( entry.getKey(), ( ( ModuleConfig ) entry.getValue() ).asStruct() ) );

		return Struct.of(
		    Key.allowedFileOperationExtensions, Array.fromList( this.allowedFileOperationExtensions ),
		    Key.applicationTimeout, this.applicationTimeout,
		    Key.caches, cachesCopy,
		    Key.classGenerationDirectory, this.classGenerationDirectory,
		    Key.customTagsDirectory, Array.fromList( this.customTagsDirectory ),
		    Key.datasources, datsourcesCopy,
		    Key.debugMode, this.debugMode,
		    Key.defaultCache, this.defaultCache.toStruct(),
		    Key.defaultDatasource, this.defaultDatasource,
		    Key.defaultRemoteMethodReturnFormat, this.defaultRemoteMethodReturnFormat,
		    Key.disallowedFileOperationExtensions, Array.fromList( this.disallowedFileOperationExtensions ),
		    Key.executors, executorsCopy,
		    Key.experimental, Struct.fromMap( this.experimental ),
		    Key.invokeImplicitAccessor, this.invokeImplicitAccessor,
		    Key.javaLibraryPaths, Array.fromList( this.javaLibraryPaths ),
		    Key.locale, this.locale,
		    Key.mappings, mappingsCopy,
		    Key.modules, modulesCopy,
		    Key.modulesDirectory, Array.fromList( this.modulesDirectory ),
		    Key.originalConfig, this.originalConfig,
		    Key.requestTimeout, this.requestTimeout,
		    Key.sessionManagement, this.sessionManagement,
		    Key.sessionStorage, this.sessionStorage,
		    Key.sessionTimeout, this.sessionTimeout,
		    Key.setClientCookies, this.setClientCookies,
		    Key.setDomainCookies, this.setDomainCookies,
		    Key.timezone, this.timezone,
		    Key.useHighPrecisionMath, this.useHighPrecisionMath,
		    Key.validExtensions, Array.fromSet( getValidExtensions() ),
		    Key.validClassExtensions, Array.fromSet( this.validClassExtensions ),
		    Key.validTemplateExtensions, Array.fromSet( this.validTemplateExtensions )
		);
	}
}
