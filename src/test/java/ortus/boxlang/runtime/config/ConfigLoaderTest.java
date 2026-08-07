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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.config.segments.ModuleConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ConfigurationException;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.ConfigSecretUtil;

class ConfigLoaderTest {

	static BoxRuntime runtime;

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		if ( System.getProperty( "boxlang.security.allowedFileOperationExtensions" ) != null ) {
			System.clearProperty( "boxlang.security.allowedFileOperationExtensions" );
		}
		if ( System.getProperty( "BOXLANG_SECURITY_ALLOWEDFILEOPERATIONEXTENSIONS" ) != null ) {
			System.clearProperty( "BOXLANG_SECURITY_ALLOWEDFILEOPERATIONEXTENSIONS" );
		}
		if ( System.getProperty( "boxlang.compiler" ) != null ) {
			System.clearProperty( "boxlang.compiler" );
		}
	}

	@DisplayName( "It can load the core config file" )
	@Test
	void testItCanLoadTheCoreConfig() {
		Configuration config = ConfigLoader.getInstance().loadCore();

		// Compiler Checks
		assertThat( config.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.mappings ).isNotEmpty();
		assertThat( config.modulesDirectory.size() ).isGreaterThan( 0 );
		// First one should be the user home directory
		assertThat( config.modulesDirectory.get( 0 ) ).doesNotContainMatch( "(ignorecase)\\{boxlang-home\\}" );

		// Log Directory Check
		assertThat( config.logging.logsDirectory ).isNotEmpty();

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.caches.get( "default" );
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name ).isEqualTo( Key.of( "DEFAULT" ) );
		assertThat( defaultCache.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig regexCache = ( CacheConfig ) config.caches.get( "bxRegex" );
		assertThat( regexCache.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( regexCache.properties ).isNotNull();
		assertThat( regexCache.properties.get( "maxObjects" ) ).isEqualTo( 500 );
		assertThat( regexCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( regexCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( regexCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentSoftReferenceStore" );
		assertThat( regexCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );
	}

	@DisplayName( "It can register a new mapping" )
	@Test
	void testItCanRegisterAMapping() throws URISyntaxException {
		Configuration config = ConfigLoader.getInstance().loadCore();
		assertThat( config.mappings ).isNotEmpty();
		assertThat( config.mappings ).hasSize( 1 );

		var path = Path.of( getClass().getResource( "ConfigLoaderTest.class" ).toURI() )
		    .toAbsolutePath()
		    .getParent()
		    .toString();

		config.registerMapping( "test", path );
		assertThat( config.hasMapping( "/test/" ) ).isTrue();

		config.registerMapping( "test/boxlang", path );
		assertThat( config.hasMapping( "/test/boxlang/" ) ).isTrue();

		config.registerMapping( "/myMapping", path );
		assertThat( config.hasMapping( "/myMapping/" ) ).isTrue();

		// Must be in the right order
		assertThat( config.getRegisteredMappings() ).isEqualTo( new String[] { "/test/boxlang/", "/myMapping/", "/test/", "/" } );
	}

	@DisplayName( "It can unregister a mapping" )
	@Test
	void testItCanUnregisterAMapping() throws URISyntaxException {
		Configuration config = ConfigLoader.getInstance().loadCore();
		assertThat( config.mappings ).isNotEmpty();
		assertThat( config.mappings ).hasSize( 1 );

		// Register a new mapping and check it
		var path = Path.of( getClass().getResource( "ConfigLoaderTest.class" ).toURI() )
		    .toAbsolutePath()
		    .getParent()
		    .toString();

		config.registerMapping( "test", path );
		assertThat( config.mappings ).hasSize( 2 );
		assertThat( config.hasMapping( "/test/" ) ).isTrue();

		config.unregisterMapping( "test" );
		assertThat( config.mappings ).hasSize( 1 );
		assertThat( config.hasMapping( "/test/" ) ).isFalse();

		config.registerMapping( "test", path );
		assertThat( config.unregisterMapping( "/test/" ) ).isTrue();

		assertThat( config.unregisterMapping( "bogus" ) ).isFalse();
	}

	@DisplayName( "It can load a custom config file using a string" )
	@Test
	void testItCanLoadACustomConfig() {
		Configuration config = ConfigLoader.getInstance().loadFromFile( "src/test/resources/test-boxlang.json" );
		assertConfigTest( config );
	}

	@DisplayName( "It can load a custom config file using a URL" )
	@Test
	void testItCanLoadACustomConfigUsingAURL() {
		URL url;
		try {
			url = Path.of( "src/test/resources/test-boxlang.json" ).toUri().toURL();
		} catch ( Exception e ) {
			throw new MissingIncludeException( "Invalid template path to execute.", "", getClass().getResource( "/test-templates/BoxRuntime.bxs" ).toString(),
			    e );
		}

		Configuration config = ConfigLoader.getInstance().loadFromFile( url );
		assertConfigTest( config );
	}

	@DisplayName( "It can load a custom config file using a Path" )
	@Test
	void testItCanLoadACustomConfigUsingAPath() {
		Configuration config = ConfigLoader.getInstance().loadFromFile(
		    Path.of( "src/test/resources/test-boxlang.json" )
		);
		assertConfigTest( config );
	}

	private void assertConfigTest( Configuration config ) {
		// Compiler Checks
		assertThat( config.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.mappings ).isEmpty();
		assertThat( config.modulesDirectory.size() ).isGreaterThan( 0 );

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();
		assertThat( config.caches ).hasSize( 2 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.caches.get( "default" );
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name ).isEqualTo( Key.of( "DEFAULT" ) );
		assertThat( defaultCache.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentSoftReferenceStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig regexCacheConfig = ( CacheConfig ) config.caches.get( "bxRegex" );
		assertThat( regexCacheConfig.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( regexCacheConfig.properties ).isNotNull();
		assertThat( regexCacheConfig.properties.get( "maxObjects" ) ).isEqualTo( 200 );

		// Queries Config Checks (from test-boxlang.json)
		assertThat( config.queries ).isNotNull();
		assertThat( config.queries.timeout ).isEqualTo( 30 );
		assertThat( config.queries.returnType ).isEqualTo( "array" );
		assertThat( config.queries.fetchSize ).isEqualTo( 100 );
		assertThat( config.queries.maxrows ).isEqualTo( 1000 );
		assertThat( config.queries.cacheProvider ).isEqualTo( "default" );
	}

	@DisplayName( "It can merge environmental properties in to the config" )
	@Test
	@Disabled( "This test passes, but is not thread safe to run in CI in parallel with other tests" )
	void testItCanMergeEnvironmentalProperties() {
		System.setProperty( "BOXLANG_SECURITY_ALLOWEDFILEOPERATIONEXTENSIONS", ".exe" );
		System.setProperty( "boxlang.compiler", "asm" );
		Configuration config = ConfigLoader.getInstance().loadCore();
		// Core config checks
		// Compiler Checks
		assertThat( config.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.mappings ).isNotEmpty();
		assertThat( config.modulesDirectory.size() ).isGreaterThan( 0 );
		// First one should be the user home directory
		assertThat( config.modulesDirectory.get( 0 ) ).doesNotContainMatch( "(ignorecase)\\{boxlang-home\\}" );

		// Log Directory Check
		assertThat( config.logging.logsDirectory ).isNotEmpty();

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.caches.get( "default" );
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name ).isEqualTo( Key.of( "DEFAULT" ) );
		assertThat( defaultCache.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig regexCache = ( CacheConfig ) config.caches.get( "bxRegex" );
		assertThat( regexCache.provider ).isEqualTo( Key.of( "BOXCACHEPROVIDER" ) );
		assertThat( regexCache.properties ).isNotNull();
		assertThat( regexCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
		assertThat( regexCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( regexCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( regexCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( regexCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Check the debug mode
		assertThat( config.security.allowedFileOperationExtensions ).isInstanceOf( List.class );
		assertThat( config.security.allowedFileOperationExtensions ).contains( ".exe" );
		assertThat( config.experimental ).isInstanceOf( IStruct.class );
		assertThat( config.compiler ).isEqualTo( "asm" );
	}

	@DisplayName( "It can override the secret seed from a generic environment variable" )
	@Test
	void testGenericEnvironmentOverrideForSecretSeed() {
		ConfigLoader	loader		= ConfigLoader.getInstance();
		IStruct			environment	= Struct.of( "BOXLANG_SECURITY_SECRETSEED", "environment-seed" );
		IStruct			flatConfig	= StructUtil.toFlatMap( Struct.of( Key.security, Struct.of() ) );

		loader.filterEnv( environment ).entrySet().forEach( entry -> ConfigLoader.applyOverride( entry, flatConfig ) );
		Configuration config = new Configuration().process( StructUtil.unFlattenKeys( flatConfig, true, false ) );

		assertThat( config.security.getSecretSeed() ).isEqualTo( "environment-seed" );
	}

	@DisplayName( "It decrypts encrypted system-property overrides" )
	@Test
	void testEncryptedSystemPropertyOverride() {
		String propertyName = "boxlang.compiler";
		System.setProperty( propertyName, ConfigSecretUtil.encryptWithPrefix( "environment-secret" ) );

		try {
			Configuration config = new Configuration().process( ConfigLoader.getInstance().mergeEnvironmentOverrides( Struct.of(
			    Key.compiler, "plaintext-compiler"
			) ) );

			assertThat( config.compiler ).isEqualTo( "environment-secret" );
		} finally {
			System.clearProperty( propertyName );
		}
	}

	@DisplayName( "It decrypts nested JSON configuration values before resolving placeholders" )
	@Test
	void testEncryptedConfigurationValues() throws Exception {
		String	encryptedValue	= ConfigSecretUtil.encryptWithPrefix( "${user-home}/secret" )
		    .replace( "\\", "\\\\" )
		    .replace( "\"", "\\\"" );
		Path	configFile		= Files.createTempFile( "boxlang-encrypted-config", ".json" );
		String	configJson		= """
		                          {
		                            "modules": {
		                              "test": {
		                                "settings": {
		                                  "secret": "%s",
		                                  "values": ["%s"]
		                                }
		                              }
		                            }
		                          }
		                          """.formatted( encryptedValue, encryptedValue );

		try {
			Files.writeString( configFile, configJson, StandardCharsets.UTF_8 );
			Configuration	config	= ConfigLoader.getInstance().loadFromFile( configFile );
			ModuleConfig	module	= ( ModuleConfig ) config.modules.get( Key.of( "test" ) );

			assertThat( module.settings.getAsString( Key.of( "secret" ) ) ).isEqualTo( System.getProperty( "user.home" ) + "/secret" );
			assertThat( ( ( List<?> ) module.settings.get( "values" ) ).get( 0 ) ).isEqualTo( System.getProperty( "user.home" ) + "/secret" );
		} finally {
			Files.deleteIfExists( configFile );
		}
	}

	@DisplayName( "It rejects an encrypted secret algorithm before decrypting configuration values" )
	@Test
	void testEncryptedSecretAlgorithm() throws Exception {
		Path configFile = Files.createTempFile( "boxlang-encrypted-algorithm", ".json" );

		try {
			Files.writeString( configFile, """
			                               {
			                                 "security": {
			                                   "secretAlgorithm": "BxSeCrEt:not-an-algorithm"
			                                 }
			                               }
			                               """, StandardCharsets.UTF_8 );

			ConfigurationException exception = assertThrows(
			    ConfigurationException.class,
			    () -> ConfigLoader.getInstance().loadFromFile( configFile )
			);

			assertThat( exception ).hasMessageThat().contains( "security.secretAlgorithm" );
		} finally {
			Files.deleteIfExists( configFile );
		}
	}

	@DisplayName( "It will reject invalid env vars" )
	@Test
	@Disabled( "This test passes, but is not thread safe to run in CI in parallel with other tests" )
	void testItWillRejectInvalidEnvVars() {
		System.setProperty( "BOXLANG_FOO", "bar" );
		System.setProperty( "BOXLANG_FOO_BAR", "baz" );
		System.setProperty( "BOXLANG_FOO_BAR_BAZ", "bum" );
		System.setProperty( "BOXLANG_FOO_BAR_BAZ_BUM", "qux" );
		Throwable t = assertThrows( BoxRuntimeException.class, () -> ConfigLoader.getInstance().loadCore() );
		// While importing BOXLANG_XXX env vars, error un-flattening key [foo.bar.baz.bum] because [foo] is not a struct, but instead [String]
		assertThat( t.getMessage() ).contains( "error un-flattening key" );

	}

}
