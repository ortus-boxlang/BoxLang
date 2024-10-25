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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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

	@AfterAll
	public static void teardown() {

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
		assertThat( config.logsDirectory ).isNotEmpty();

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.caches.get( "bxImports" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
		assertThat( importCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( importCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( importCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( importCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );
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
		assertThat( config.hasMapping( "/test" ) ).isTrue();

		config.registerMapping( "test/boxlang", path );
		assertThat( config.hasMapping( "/test/boxlang" ) ).isTrue();

		config.registerMapping( "/myMapping", path );
		assertThat( config.hasMapping( "/myMapping" ) ).isTrue();

		// Must be in the right order
		assertThat( config.getRegisteredMappings() ).isEqualTo( new String[] { "/test/boxlang", "/myMapping", "/test", "/" } );
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
		assertThat( config.hasMapping( "/test" ) ).isTrue();

		config.unregisterMapping( "test" );
		assertThat( config.mappings ).hasSize( 1 );
		assertThat( config.hasMapping( "/test" ) ).isFalse();

		config.registerMapping( "test", path );
		assertThat( config.unregisterMapping( "/test" ) ).isTrue();

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
		assertThat( config.caches ).hasSize( 1 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 2 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentSoftReferenceStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.caches.get( "bxImports" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
	}

	@DisplayName( "It can merge environmental properties in to the config" )
	@Test
	void testItCanMergeEnvironmentalProperties() {
		System.setProperty( "boxlang.security.allowedFileOperationExtensions", ".exe" );
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
		assertThat( config.logsDirectory ).isNotEmpty();

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.caches.get( "bxImports" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
		assertThat( importCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( importCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( importCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( importCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Check the debug mode
		assertThat( config.security.allowedFileOperationExtensions ).isInstanceOf( List.class );
		assertThat( config.security.allowedFileOperationExtensions ).contains( ".exe" );
		assertThat( config.experimental ).isInstanceOf( IStruct.class );
		assertThat( config.experimental.getAsString( Key.of( "compiler" ) ) ).isEqualTo( "asm" );
	}

	@DisplayName( "It can merge environmental properties in to the config using the alternate syntax" )
	@Test
	void testItCanMergeEnvironmentalPropertiesAlt() {
		System.setProperty( "BOXLANG_SECURITY_ALLOWEDFILEOPERATIONEXTENSIONS", ".exe" );
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
		assertThat( config.logsDirectory ).isNotEmpty();

		// Cache Checks
		assertThat( config.caches ).isNotEmpty();

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.caches.get( "bxImports" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXCACHEPROVIDER" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
		assertThat( importCache.properties.get( "reapFrequency" ) ).isEqualTo( 120 );
		assertThat( importCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( importCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( importCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Check the debug mode
		assertThat( config.security.allowedFileOperationExtensions ).isInstanceOf( List.class );
		assertThat( config.security.allowedFileOperationExtensions ).contains( ".exe" );
	}

}
