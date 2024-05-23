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
package ortus.boxlang.runtime.config.segments;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

/**
 * The runtime configuration for the BoxLang runtime
 */
public class RuntimeConfig {

	/**
	 * The Timezone to use for the runtime;
	 * Uses the Java Timezone format: {@code America/New_York}
	 * Uses the default system timezone if not set
	 */
	public ZoneId				timezone			= TimeZone.getDefault().toZoneId();

	/**
	 * The default locale to use for the runtime
	 */
	public Locale				locale				= Locale.getDefault();

	/**
	 * The request timeout for a request in milliseconds
	 * {@code 0} means no timeout
	 */
	public long					requestTimeout		= 0;

	/**
	 * A sorted struct of mappings
	 */
	public IStruct				mappings			= new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR );

	/**
	 * An array of directories where modules are located and loaded from.
	 * {@code [ /{boxlang-home}/modules ]}
	 */
	public List<String>			modulesDirectory	= new ArrayList<>( Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/modules" ) );

	/**
	 * The default logs directory for the runtime
	 */
	public String				logsDirectory		= Paths.get( BoxRuntime.getInstance().getRuntimeHome().toString(), "/logs" ).normalize().toString();

	/**
	 * An array of directories where custom tags are located and loaded from.
	 * {@code [ /{boxlang-home}/customTags ]}
	 */
	public List<String>			customTagsDirectory	= new ArrayList<>( Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/customTags" ) );

	/**
	 * An array of directories where jar files will be loaded from at runtime.
	 */
	public List<String>			javaLibraryPaths	= new ArrayList<>( Arrays.asList( BoxRuntime.getInstance().getRuntimeHome().toString() + "/lib" ) );

	/**
	 * Cache registrations
	 */
	public IStruct				caches				= new Struct();

	/**
	 * Default datasource registration
	 */
	public String				defaultDatasource	= "";

	/**
	 * Global datasource registrations
	 */
	public IStruct				datasources			= new Struct();

	/**
	 * Default cache registration
	 */
	public CacheConfig			defaultCache		= new CacheConfig();

	/**
	 * Logger
	 */
	private static final Logger	logger				= LoggerFactory.getLogger( RuntimeConfig.class );

	/**
	 * The modules configuration
	 */
	public IStruct				modules				= new Struct();

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
	public RuntimeConfig registerMapping( String mapping, String path ) {
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
	public RuntimeConfig registerMapping( Key mapping, String path ) {
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
	 * Get the java library paths as an array of URLs of Jar files
	 *
	 * @return The java library paths as an array of Jar URLs
	 */
	public URL[] getJavaLibraryPaths() {
		return this.javaLibraryPaths
		    .stream()
		    .filter( path -> Paths.get( path ).toFile().exists() )
		    .map( path -> {
			    try {
				    return DynamicClassLoader.getJarURLs( path );
			    } catch ( IOException e ) {
				    throw new BoxIOException( path + " is not a valid path", e );
			    }
		    } )
		    .flatMap( Arrays::stream )
		    .toArray( URL[]::new );
	}

	/**
	 * --------------------------------------------------------------------------
	 * JSON Processing
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * TODO: Once this get's big, start refactoring this into smaller methods
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public RuntimeConfig process( IStruct config ) {

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
			this.locale = LocalizationUtil.parseLocale( config.getAsString( Key.locale ) );
		}

		// Request Timeout
		if ( config.containsKey( Key.requestTimeout ) && StringCaster.cast( config.get( "requestTimeout" ) ).length() > 0 ) {
			this.requestTimeout = LongCaster.cast( PlaceholderHelper.resolve( config.get( "requestTimeout" ) ) );
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
				logger.warn( "The [runtime.javaLibraryPaths] configuration is not a JSON Object, ignoring it." );
			}
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
						    logger.warn( "The [runtime.caches.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [runtime.caches] configuration is not a JSON Object, ignoring it." );
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
		return this;
	}

	/**
	 * Helper method to validate datasource drivers configured in the runtime configuration
	 * This makes sure all declared drivers are registered with the datasource service
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
	 * Returns the configuration as a struct
	 * These values must be passed by reference, not by value so they can be modified downstream
	 * without affecting the values here. In this matter, contexts can override the values passed down
	 * from their parent.
	 *
	 * @return Struct
	 */
	public IStruct asStruct() {
		IStruct mappingsCopy = new Struct( Struct.KEY_LENGTH_LONGEST_FIRST_COMPARATOR );
		mappingsCopy.putAll( this.mappings );

		IStruct cachesCopy = new Struct();
		this.caches.entrySet().forEach( entry -> cachesCopy.put( entry.getKey(), ( ( CacheConfig ) entry.getValue() ).toStruct() ) );

		IStruct datsourcesCopy = new Struct();
		this.datasources.entrySet().forEach( entry -> datsourcesCopy.put( entry.getKey(), ( ( DatasourceConfig ) entry.getValue() ).toStruct() ) );

		IStruct modulesCopy = new Struct();
		this.modules.entrySet().forEach( entry -> modulesCopy.put( entry.getKey(), ( ( ModuleConfig ) entry.getValue() ).toStruct() ) );

		return Struct.of(
		    Key.caches, cachesCopy,
		    Key.customTagsDirectory, Array.fromList( this.customTagsDirectory ),
		    Key.datasources, datsourcesCopy,
		    Key.defaultCache, this.defaultCache.toStruct(),
		    Key.defaultDatasource, this.defaultDatasource,
		    Key.javaLibraryPaths, Array.fromList( this.javaLibraryPaths ),
		    Key.locale, this.locale,
		    Key.mappings, mappingsCopy,
		    Key.modules, modulesCopy,
		    Key.modulesDirectory, Array.fromList( this.modulesDirectory ),
		    Key.requestTimeout, this.requestTimeout,
		    Key.timezone, this.timezone
		);
	}

}
