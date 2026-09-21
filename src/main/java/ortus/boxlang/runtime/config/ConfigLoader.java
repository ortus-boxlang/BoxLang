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

import ortus.boxlang.runtime.config.segments.SecurityConfig;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ConfigurationException;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.ConfigSecretUtil;

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
	public static final String	DEFAULT_CONFIG_FILE	= "config/boxlang.json";

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
	public static ConfigLoader getInstance() {
		if ( instance == null ) {
			synchronized ( ConfigLoader.class ) {
				if ( instance == null ) {
					instance = new ConfigLoader();
				}
			}
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
	public Configuration loadFromResources( String configFile ) {
		// Parse it natively to Java objects
		Object rawConfig = JSONUtil.fromJSON(
		    // Load the file from the resources folder
		    ConfigLoader.class.getClassLoader().getResourceAsStream( configFile ),
		    true
		);

		// Decrypt prefixed values before resolving placeholders in the raw config.
		rawConfig = resolveConfigValues( rawConfig );

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
	 */
	public Configuration loadFromFile( URL source ) {
		return loadFromFile( new File( source.getFile() ) );
	}

	/**
	 * Load the config from a String file source
	 *
	 * @param source The source to load the configuration from
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

		// Decrypt prefixed values before resolving placeholders in the raw config.
		rawConfig = resolveConfigValues( rawConfig );

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
		UnmodifiableStruct	collectedEnvironment	= UnmodifiableStruct.of(
		    Key.environment, UnmodifiableStruct.fromMap( System.getenv() ),
		    Key.properties, UnmodifiableStruct.fromMap( System.getProperties() )
		);

		SecurityConfig		secretConfig			= getSecretConfig( config );
		IStruct				propertyOverrides		= decryptEnvironmentOverrides(
		    filterEnv( collectedEnvironment.getAsStruct( Key.properties ) ), secretConfig );

		IStruct				envOverrides			= decryptEnvironmentOverrides( filterEnv( collectedEnvironment.getAsStruct( Key.environment ) ),
		    secretConfig )
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

	@SuppressWarnings( "unchecked" )
	private Object resolveConfigValues( Object rawConfig ) {
		SecurityConfig secretConfig = getSecretConfig(
		    rawConfig instanceof Map<?, ?> configMap ? new Struct( ( Map<Object, Object> ) configMap ) : new Struct() );
		// Env/property seed overrides win when decrypting a JSON config file.
		applySecretSeedOverrides( secretConfig );
		return resolveConfigValues( rawConfig, secretConfig );
	}

	/**
	 * Decrypts and resolves a raw configuration value.
	 * <p>
	 * Prefixed values are decrypted first, then placeholders are resolved with each
	 * placeholder value decrypted individually as it is substituted. This is only used
	 * by configuration-file processing, never by general runtime placeholder call sites.
	 *
	 * @param rawConfig    The raw configuration value to process.
	 * @param secretConfig The {@link SecurityConfig} used to decrypt prefixed values.
	 *
	 * @return The decrypted and resolved configuration value.
	 */
	private Object resolveConfigValues( Object rawConfig, SecurityConfig secretConfig ) {
		decryptConfigValues( rawConfig, secretConfig );
		return resolvePlaceholdersWithSecrets( rawConfig, secretConfig );
	}

	/**
	 * Resolves placeholders in a configuration tree, decrypting each placeholder's value
	 * individually as it is substituted so that a value like {@code "${a}-${b}"} where both
	 * {@code a} and {@code b} hold {@code bxsecret:} values resolves correctly.
	 *
	 * @param value        The value tree to resolve.
	 * @param secretConfig The {@link SecurityConfig} used to decrypt placeholder values.
	 *
	 * @return The resolved value tree.
	 */
	@SuppressWarnings( "unchecked" )
	private Object resolvePlaceholdersWithSecrets( Object value, SecurityConfig secretConfig ) {
		return resolvePlaceholdersWithSecrets( value, secretConfig, PlaceholderHelper.getPlaceholderMap() );
	}

	/**
	 * Resolves placeholders in a configuration tree using the supplied placeholder map.
	 * Encrypted placeholder values are decrypted at the point of replacement (only for
	 * placeholders that are actually referenced) by supplying a decryptor to the resolver.
	 * This runs only from configuration-file processing.
	 *
	 * @param value          The value tree to resolve.
	 * @param secretConfig   The {@link SecurityConfig} used to decrypt placeholder values.
	 * @param placeholderMap The placeholder map used for substitution.
	 *
	 * @return The resolved value tree.
	 */
	@SuppressWarnings( "unchecked" )
	Object resolvePlaceholdersWithSecrets( Object value, SecurityConfig secretConfig, IStruct placeholderMap ) {
		return PlaceholderHelper.resolveAll( value, placeholderMap, replacement -> ConfigSecretUtil.decryptIfEncrypted(
		    replacement, secretConfig.getSecretSeed(), secretConfig.secretAlgorithm ) );
	}

	@SuppressWarnings( "unchecked" )
	private Object decryptConfigValues( Object rawConfig, SecurityConfig secretConfig ) {
		return decryptConfigValues( rawConfig, secretConfig, "" );
	}

	@SuppressWarnings( "unchecked" )
	private Object decryptConfigValues( Object rawConfig, SecurityConfig secretConfig, String path ) {
		if ( rawConfig instanceof IStruct struct ) {
			for ( Key key : struct.keySet() ) {
				struct.put( key, decryptConfigValues( struct.get( key ), secretConfig, appendPath( path, key.getName() ) ) );
			}
		} else if ( rawConfig instanceof Array array ) {
			for ( int i = 0; i < array.size(); i++ ) {
				array.set( i, decryptConfigValues( array.get( i ), secretConfig, path + "[" + i + "]" ) );
			}
		} else if ( rawConfig instanceof Map<?, ?> rawMap ) {
			Map<Object, Object> configMap = ( Map<Object, Object> ) rawMap;
			for ( Object key : List.copyOf( configMap.keySet() ) ) {
				configMap.put( key, decryptConfigValues( configMap.get( key ), secretConfig, appendPath( path, key.toString() ) ) );
			}
		} else if ( rawConfig instanceof List<?> rawList ) {
			List<Object> configList = ( List<Object> ) rawList;
			for ( int i = 0; i < configList.size(); i++ ) {
				configList.set( i, decryptConfigValues( configList.get( i ), secretConfig, path + "[" + i + "]" ) );
			}
		} else if ( rawConfig instanceof String value ) {
			if ( ConfigSecretUtil.isEncrypted( value ) ) {
				try {
					return ConfigSecretUtil.decryptIfEncrypted( value, secretConfig.getSecretSeed(), secretConfig.secretAlgorithm );
				} catch ( Exception e ) {
					throw new ConfigurationException(
					    "Failed to decrypt the [bxsecret:] value at configuration path [" + path + "] using algorithm ["
					        + secretConfig.secretAlgorithm
					        + "]. The value cannot be read with the runtime's current secret seed. Re-encrypt the value with the same secret seed this runtime uses.",
					    e );
				}
			}
		}

		return rawConfig;
	}

	/**
	 * Appends a path segment to an existing configuration path, separating segments with a dot.
	 *
	 * @param path    The current configuration path.
	 * @param segment The segment to append.
	 *
	 * @return The combined configuration path.
	 */
	private String appendPath( String path, String segment ) {
		return path.isEmpty() ? segment : path + "." + segment;
	}

	/**
	 * Builds the {@link SecurityConfig} used to decrypt a configuration source.
	 * <p>
	 * The file's own {@code security} section is processed first. The secret seed override
	 * from the environment or JVM system properties is applied by
	 * {@link #resolveConfigValues(Object)} at the JSON-config-file boundary, not here, so
	 * that it does not run while environment/property overrides are merged during startup.
	 *
	 * @param config The configuration struct being processed.
	 *
	 * @return The resolved {@link SecurityConfig}.
	 */
	SecurityConfig getSecretConfig( IStruct config ) {
		SecurityConfig secretConfig = new SecurityConfig();
		if ( config.get( Key.security ) instanceof Map<?, ?> securityConfig ) {
			IStruct securitySettings = new Struct( securityConfig );
			if ( ConfigSecretUtil.isEncrypted( securitySettings.getAsString( Key.secretAlgorithm ) ) ) {
				throw new ConfigurationException( "The [security.secretAlgorithm] setting cannot be encrypted." );
			}
			secretConfig.process( securitySettings );
		}
		return secretConfig;
	}

	/**
	 * Applies the secret-seed override from the environment or JVM system properties so it wins over any
	 * seed in the file being decrypted. The same seed is used for all {@code bxsecret:} values because
	 * environment and system-property overrides are applied only after file decryption completes.
	 * <p>
	 * This intentionally uses only plain Java maps to avoid loading BoxLang types (Array, Struct, etc.)
	 * during early config bootstrap, before the runtime services are fully wired.
	 *
	 * @param secretConfig The per-file {@link SecurityConfig} to apply the override to.
	 */
	private void applySecretSeedOverrides( SecurityConfig secretConfig ) {
		String overrideSeed = firstNonBlank(
		    getCaseInsensitiveProperty( PROPERTY_PREFIX + "security.secretSeed" ),
		    getCaseInsensitiveProperty( ENV_PREFIX + "SECURITY_SECRETSEED" ),
		    getCaseInsensitiveEnv( ENV_PREFIX + "SECURITY_SECRETSEED" ) );
		if ( overrideSeed != null ) {
			secretConfig.process( Struct.of( Key.secretSeed, overrideSeed ) );
		}
	}

	/**
	 * Reads a JVM system property matching the given name, ignoring case.
	 *
	 * @param name The property name to look up.
	 *
	 * @return The matching property value, or null when absent.
	 */
	private String getCaseInsensitiveProperty( String name ) {
		String lowerName = name.toLowerCase();
		return System.getProperties().entrySet().stream()
		    .filter( entry -> entry.getKey().toString().toLowerCase().equals( lowerName ) )
		    .map( entry -> entry.getValue().toString() )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * Reads an environment variable matching the given name, ignoring case.
	 *
	 * @param name The environment variable name to look up.
	 *
	 * @return The matching environment variable value, or null when absent.
	 */
	private String getCaseInsensitiveEnv( String name ) {
		String upperName = name.toUpperCase();
		return System.getenv().entrySet().stream()
		    .filter( entry -> entry.getKey().toUpperCase().equals( upperName ) )
		    .map( Map.Entry::getValue )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * Returns the first non-blank value from the supplied candidates, trimmed, or null when all are blank.
	 *
	 * @param candidates The candidate values to inspect.
	 *
	 * @return The first non-blank trimmed value, or null.
	 */
	private String firstNonBlank( String... candidates ) {
		for ( String candidate : candidates ) {
			if ( candidate != null && !candidate.isBlank() ) {
				return candidate.trim();
			}
		}
		return null;
	}

	private IStruct decryptEnvironmentOverrides( IStruct overrides, SecurityConfig secretConfig ) {
		return ( IStruct ) resolveConfigValues( overrides, secretConfig );
	}

	/**
	 * Filter the environment variables for BoxLang specific ones
	 *
	 * @param envCollection The environment collection to filter
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
					// Note, we're not expanding placeholders here since this JSON would have already been in an env var or sys prop.
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
