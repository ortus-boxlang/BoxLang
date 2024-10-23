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

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ConfigurationException;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.StructUtil;

/**
 * This class is responsible for loading the core configuration file from the `resources` folder
 * and parsing it into the Configuration class.
 *
 * It can also load from a custom location.
 */
public class ConfigLoader {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Path to the core config file in the `resources` folder
	 */
	private static final String	DEFAULT_CONFIG_FILE	= "config/boxlang.json";

	/**
	 * The ConfigLoader instance
	 */
	private static ConfigLoader	instance;

	/**
	 * Logger
	 */
	private static final Logger	logger				= LoggerFactory.getLogger( ConfigLoader.class );

	/**
	 * Env placeholders
	 */
	private static final String	ENV_PREFIX			= "BOXLANG_";
	private static final String	PROPERTY_PREFIX		= "boxlang.";

	/**
	 * --------------------------------------------------------------------------
	 * Singleton Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private ConfigLoader() {
		// Any initialization code can be placed here
	}

	/**
	 * Get an instance of the ConfigLoader
	 *
	 * @return The ConfigLoader instance
	 */
	public static synchronized ConfigLoader getInstance() {
		if ( instance == null ) {
			instance = new ConfigLoader();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Loaders
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Load the default internal core config file <code>resources/config/boxlang.json</code>
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadCore() {
		return loadFromResources( DEFAULT_CONFIG_FILE );
	}

	/**
	 * Load a config file from the BoxLang <code>resources</code> folder using the class loader
	 *
	 * @param configFile The path to the config file from the <code>resources</code> folder
	 *
	 * @throws ConfigurationException If the config file is not a JSON object
	 *
	 * @return The parsed configuration
	 */
	@SuppressWarnings( "unchecked" )
	public Configuration loadFromResources( String configFile ) {
		// Parse it natively to Java objects
		Object rawConfig = JSONUtil.fromJSON(
		    // Load the file from the resources folder
		    ConfigLoader.class.getClassLoader().getResourceAsStream( configFile ),
		    true
		);

		// Verify it loaded the configuration map
		if ( rawConfig instanceof Map ) {
			logger.debug( "Loaded internal BoxLang configuration file [{}]", configFile );
			return loadFromMap( ( Map<Object, Object> ) rawConfig );
		} else {
			throw new ConfigurationException( "The config map is not a JSON object. Can't work with it." );
		}
	}

	/**
	 * Load the config from a Struct of settings
	 *
	 * @param configMap The configuration structure to load as a Configuration object
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromMap( IStruct configMap ) {
		return new Configuration().process( mergeEnvironmentOverrides( configMap ) );
	}

	/**
	 * Load the config from a Map of settings
	 *
	 * @param configMap The configuration Map to load as a Configuration object
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromMap( Map<Object, Object> configMap ) {
		return loadFromMap( new Struct( configMap ) );
	}

	/**
	 * Load the config from a file
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromFile( File source ) {
		IStruct rawConfig = deserializeConfig( source );
		logger.debug( "Loaded custom BoxLang configuration file [{}]", source );
		return loadFromMap( rawConfig );
	}

	/**
	 * Load the config from a file Path
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromFile( Path source ) {
		return loadFromFile( source.toFile() );
	}

	/**
	 * Load the config from a URL file source
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromFile( URL source ) {
		return loadFromFile( new File( source.getFile() ) );
	}

	/**
	 * Load the config from a String file source
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The parsed configuration
	 */
	public Configuration loadFromFile( String source ) {
		return loadFromFile( new File( source ) );
	}

	/**
	 * Load the config from a file source and return the raw config map
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The raw config map as a Struct
	 */
	@SuppressWarnings( "unchecked" )
	public IStruct deserializeConfig( File source ) {
		// Parse it natively to Java objects
		Object rawConfig = JSONUtil.fromJSON( source, true );

		// Verify it loaded the configuration map
		if ( rawConfig instanceof Map ) {
			return new Struct( ( Map<Object, Object> ) rawConfig );
		}

		throw new ConfigurationException( "The config map is not a JSON object. Can't work with it." );
	}

	/**
	 * Load the config from a String path source and return the raw config map
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The raw config map as a Struct
	 */
	public IStruct deserializeConfig( String source ) {
		return deserializeConfig( new File( source ) );
	}

	/**
	 * Load the config from a URL path source and return the raw config map
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The raw config map as a Struct
	 */
	public IStruct deserializeConfig( URL source ) {
		return deserializeConfig( new File( source.getFile() ) );
	}

	/**
	 * Load the config from a path source and return the raw config map
	 *
	 * @param source The source to load the configuration from
	 *
	 * @return The raw config map as a Struct
	 */
	public IStruct deserializeConfig( Path source ) {
		return deserializeConfig( source.toFile() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Environment Overrides
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Merge environment overrides with the configuration
	 *
	 * @param config The configuration to merge the environment overrides with
	 **/
	public IStruct mergeEnvironmentOverrides( IStruct config ) {
		// We bring this in here in case a system property was dynamically set
		ImmutableStruct	collectedEnvironment	= ImmutableStruct.of(
		    Key.environment, ImmutableStruct.fromMap( System.getenv() ),
		    Key.properties, ImmutableStruct.fromMap( System.getProperties() )
		);

		IStruct			propertyOverrides		= filterEnv( collectedEnvironment.getAsStruct( Key.properties ) );

		IStruct			envOverrides			= filterEnv( collectedEnvironment.getAsStruct( Key.environment ) )
		    .entrySet()
		    .stream()
		    .filter( entry -> !propertyOverrides.containsKey( entry.getKey() ) )
		    .collect( BLCollector.toStruct() );

		if ( envOverrides.isEmpty() && propertyOverrides.isEmpty() ) {
			return config;
		}

		IStruct flatConfig = StructUtil.toFlatMap( config );

		if ( !propertyOverrides.isEmpty() ) {
			propertyOverrides.entrySet().stream().forEach( entry -> applyOverride( entry, flatConfig ) );
		}
		if ( !envOverrides.isEmpty() ) {
			envOverrides.entrySet().stream().forEach( entry -> applyOverride( entry, flatConfig ) );
		}

		return StructUtil.unFlattenKeys( flatConfig, true, false );
	}

	/**
	 * Filter the environment variables for BoxLang specific ones
	 *
	 * @param env The environment to filter
	 *
	 * @return The filtered environment
	 */
	public IStruct filterEnv( IStruct envCollection ) {
		return envCollection.entrySet()
		    .stream()
		    .filter( entry -> entry.getKey().getName().toUpperCase().startsWith( ENV_PREFIX )
		        || entry.getKey().getName().toLowerCase().startsWith( PROPERTY_PREFIX ) )
		    .map( entry -> {
			    Array keyList = ListUtil.asList( entry.getKey().getName().toLowerCase().replace( "_", "." ), "." );
			    keyList.remove( 0 );
			    String key = ListUtil.asString( keyList, "." );
			    return Map.entry( Key.of( key ), entry.getValue() );
		    } )
		    .collect( BLCollector.toStruct() );
	}

	/**
	 * Apply an override to the flattened configuration
	 * 
	 * @param entry
	 * @param flatConfig
	 */
	public static void applyOverride( Map.Entry<Key, Object> entry, IStruct flatConfig ) {
		logger.debug( "Overriding runtime config [{}] with Java System property value [{}]",
		    entry.getKey().getName(), entry.getValue() );
		Object existing = flatConfig.get( entry.getKey() );
		if ( existing != null ) {
			if ( existing instanceof List ) {
				flatConfig.put( entry.getKey(),
				    ListUtil.asList( StringCaster.cast( entry.getValue() ), "," ) );
			} else if ( existing instanceof Map ) {
				try {
					IStruct configValue = StructCaster
					    .cast( JSONUtil.fromJSON( StringCaster.cast( entry.getValue() ), true ) );
					flatConfig.put( entry.getKey(), configValue );
				} catch ( Exception e ) {
					logger.error(
					    "Failed to merge property override [{}]. The value of [{}] could not be converted to a struct",
					    entry.getKey().getName(), entry.getValue() );
				}
			} else {
				flatConfig.put( entry.getKey(), entry.getValue() );
			}
		} else {
			flatConfig.put( entry.getKey(), entry.getValue() );
		}
	}

}
