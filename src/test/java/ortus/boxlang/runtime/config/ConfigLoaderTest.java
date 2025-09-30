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

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

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
		if ( System.getProperty( "boxlang.experimental.compiler" ) != null ) {
			System.clearProperty( "boxlang.experimental.compiler" );
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
		URL				url		= ConfigLoaderTest.class.getClassLoader().getResource( "test-boxlang.json" );
		Configuration	config	= ConfigLoader.getInstance().loadFromFile( url );
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
	}

	@DisplayName( "It can merge environmental properties in to the config" )
	@Test
	@Disabled( "This test passes, but is not thread safe to run in CI in parallel with other tests" )
	void testItCanMergeEnvironmentalProperties() {
		System.setProperty( "BOXLANG_SECURITY_ALLOWEDFILEOPERATIONEXTENSIONS", ".exe" );
		System.setProperty( "boxlang.experimental.compiler", "asm" );
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
		assertThat( config.experimental.getAsString( Key.of( "compiler" ) ) ).isEqualTo( "asm" );
	}

	@DisplayName( "It can load complex mappings with path and external properties" )
	@Test
	void testItCanLoadComplexMappings() {
		Configuration config = ConfigLoader.getInstance().loadFromFile( "src/test/resources/test-complex-mappings.json" );

		// Should have 4 mappings
		assertThat( config.mappings ).hasSize( 4 );

		// Simple mapping - should be external by default
		assertThat( config.hasMapping( "/simple/" ) ).isTrue();
		var simpleMapping = ( ortus.boxlang.runtime.util.Mapping ) config.mappings.get( "/simple/" );
		assertThat( simpleMapping.path() ).isEqualTo( System.getProperty( "user.dir" ) );
		assertThat( simpleMapping.external() ).isTrue(); // default is true for server-level mappings

		// Complex mapping with external = false
		assertThat( config.hasMapping( "/complex/" ) ).isTrue();
		var complexMapping = ( ortus.boxlang.runtime.util.Mapping ) config.mappings.get( "/complex/" );
		assertThat( complexMapping.path() ).isEqualTo( System.getProperty( "user.home" ) );
		assertThat( complexMapping.external() ).isFalse();

		// Complex mapping with external = true
		assertThat( config.hasMapping( "/complexExternal/" ) ).isTrue();
		var complexExternalMapping = ( ortus.boxlang.runtime.util.Mapping ) config.mappings.get( "/complexExternal/" );
		assertThat( complexExternalMapping.path() ).isEqualTo( System.getProperty( "java.io.tmpdir" ) );
		assertThat( complexExternalMapping.external() ).isTrue();

		// Complex mapping without external property - should default to true
		assertThat( config.hasMapping( "/complexNoExternal/" ) ).isTrue();
		var complexNoExternalMapping = ( ortus.boxlang.runtime.util.Mapping ) config.mappings.get( "/complexNoExternal/" );
		assertThat( complexNoExternalMapping.path() ).isEqualTo( System.getProperty( "user.dir" ) );
		assertThat( complexNoExternalMapping.external() ).isTrue(); // defaults to true
	}

}
