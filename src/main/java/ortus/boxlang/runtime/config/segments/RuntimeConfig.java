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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The runtime configuration for the BoxLang runtime
 */
public class RuntimeConfig {

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
	 * Cache registrations
	 */
	public IStruct				caches				= new Struct();

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
	 * --------------------------------------------------------------------------
	 * JSON Processing
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	public RuntimeConfig process( IStruct config ) {

		// Process mappings
		if ( config.containsKey( "mappings" ) ) {
			if ( config.get( "mappings" ) instanceof Map<?, ?> castedMap ) {
				castedMap.forEach( ( key, value ) -> this.mappings.put(
				    Key.of( key ),
				    PlaceholderHelper.resolve( value )
				) );
			} else {
				logger.warn( "The [runtime.mappings] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process Modules directories
		if ( config.containsKey( "modulesDirectory" ) ) {
			if ( config.get( "modulesDirectory" ) instanceof List<?> castedList ) {
				this.modulesDirectory = ( ( List<?> ) castedList ).stream()
				    .map( PlaceholderHelper::resolve )
				    .collect( Collectors.toList() );
			} else {
				logger.warn( "The [runtime.modulesDirectory] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process customTags directories
		if ( config.containsKey( "customTagsDirectory" ) ) {
			if ( config.get( "customTagsDirectory" ) instanceof List<?> castedList ) {
				this.customTagsDirectory = ( ( List<?> ) castedList ).stream()
				    .map( PlaceholderHelper::resolve )
				    .collect( Collectors.toList() );
			} else {
				logger.warn( "The [runtime.customTagsDirectory] configuration is not a JSON Array, ignoring it." );
			}
		}

		// Process default cache configuration
		if ( config.containsKey( "defaultCache" ) ) {
			if ( config.get( "defaultCache" ) instanceof Map<?, ?> castedMap ) {
				this.defaultCache = new CacheConfig().processProperties( new Struct( castedMap ) );
			} else {
				logger.warn( "The [runtime.defaultCache] configuration is not a JSON Object, ignoring it." );
			}
		}

		// Process declared cache configurations
		if ( config.containsKey( "caches" ) ) {
			if ( config.get( "caches" ) instanceof Map<?, ?> castedCaches ) {
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

		// Process Datasource Configurations
		if ( config.containsKey( "datasources" ) ) {
			if ( config.get( "datasources" ) instanceof Map<?, ?> castedDataSources ) {
				// Process each datasource configuration
				castedDataSources
				    .entrySet()
				    .forEach( entry -> {
					    if ( entry.getValue() instanceof Map<?, ?> castedMap ) {
						    DatasourceConfig datasourceConfig = new DatasourceConfig( ( String ) entry.getKey() ).process( new Struct( castedMap ) );
						    this.datasources.put( datasourceConfig.name, datasourceConfig );
					    } else {
						    logger.warn( "The [runtime.datasources.{}] configuration is not a JSON Object, ignoring it.", entry.getKey() );
					    }
				    } );
			} else {
				logger.warn( "The [runtime.datasources] configuration is not a JSON Object, ignoring it." );
			}
		}

		return this;
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

		return Struct.of(
		    Key.caches, cachesCopy,
		    Key.customTagsDirectory, Array.fromList( this.customTagsDirectory ),
		    Key.defaultCache, this.defaultCache.toStruct(),
		    Key.mappings, mappingsCopy,
		    Key.modulesDirectory, Array.fromList( this.modulesDirectory ),
		    Key.datasources, datsourcesCopy
		);
	}

}
