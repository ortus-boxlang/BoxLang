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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.config.segments.CacheConfig;

class ConfigLoaderTest {

	@DisplayName( "It can load the core config file" )
	@Test
	void testItCanLoadTheCoreConfig() {
		Configuration config = ConfigLoader.getInstance().loadCore();

		// Compiler Checks
		assertThat( config.compiler ).isNotNull();
		assertThat( config.compiler.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.runtime.mappings ).isNotEmpty();
		assertThat( config.runtime.modulesDirectory.size() ).isGreaterThan( 0 );
		// First one should be the user home directory
		assertThat( config.runtime.modulesDirectory.get( 0 ) ).doesNotContainMatch( "(ignorecase)\\{user-home\\}" );

		// Cache Checks
		assertThat( config.runtime.caches ).isNotEmpty();
		assertThat( config.runtime.caches ).hasSize( 1 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.runtime.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXLANG" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 10 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentSoftReferenceStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.runtime.caches.get( "imports" );
		assertThat( importCache.name.getNameNoCase() ).isEqualTo( "IMPORTS" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXLANG" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
		assertThat( importCache.properties.get( "reapFrequency" ) ).isEqualTo( 2 );
		assertThat( importCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( importCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentStore" );
		assertThat( importCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );
	}

	@DisplayName( "It can register a new mapping" )
	@Test
	void testItCanRegisterAMapping() throws URISyntaxException {
		Configuration config = ConfigLoader.getInstance().loadCore();
		assertThat( config.runtime.mappings ).isNotEmpty();
		assertThat( config.runtime.mappings ).hasSize( 1 );

		var path = Path.of( getClass().getResource( "ConfigLoaderTest.class" ).toURI() )
		    .toAbsolutePath()
		    .getParent()
		    .toString();

		config.runtime.registerMapping( "test", path );
		assertThat( config.runtime.hasMapping( "/test" ) ).isTrue();

		config.runtime.registerMapping( "test/boxlang", path );
		assertThat( config.runtime.hasMapping( "/test/boxlang" ) ).isTrue();

		config.runtime.registerMapping( "/myMapping", path );
		assertThat( config.runtime.hasMapping( "/myMapping" ) ).isTrue();

		// Must be in the right order
		assertThat( config.runtime.getRegisteredMappings() ).isEqualTo( new String[] { "/test/boxlang", "/myMapping", "/test", "/" } );
	}

	@DisplayName( "It can unregister a mapping" )
	@Test
	void testItCanUnregisterAMapping() throws URISyntaxException {
		Configuration config = ConfigLoader.getInstance().loadCore();
		assertThat( config.runtime.mappings ).isNotEmpty();
		assertThat( config.runtime.mappings ).hasSize( 1 );

		// Register a new mapping and check it
		var path = Path.of( getClass().getResource( "ConfigLoaderTest.class" ).toURI() )
		    .toAbsolutePath()
		    .getParent()
		    .toString();

		config.runtime.registerMapping( "test", path );
		assertThat( config.runtime.mappings ).hasSize( 2 );
		assertThat( config.runtime.hasMapping( "/test" ) ).isTrue();

		config.runtime.unregisterMapping( "test" );
		assertThat( config.runtime.mappings ).hasSize( 1 );
		assertThat( config.runtime.hasMapping( "/test" ) ).isFalse();

		config.runtime.registerMapping( "test", path );
		assertThat( config.runtime.unregisterMapping( "/test" ) ).isTrue();

		assertThat( config.runtime.unregisterMapping( "bogus" ) ).isFalse();
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
		assertThat( config.compiler ).isNotNull();
		assertThat( config.compiler.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.runtime.mappings ).isEmpty();
		assertThat( config.runtime.modulesDirectory.size() ).isGreaterThan( 0 );

		// Cache Checks
		assertThat( config.runtime.caches ).isNotEmpty();
		assertThat( config.runtime.caches ).hasSize( 1 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.runtime.defaultCache;
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.provider.getNameNoCase() ).isEqualTo( "BOXLANG" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maxObjects" ) ).isEqualTo( 1000 );
		assertThat( defaultCache.properties.get( "reapFrequency" ) ).isEqualTo( 2 );
		assertThat( defaultCache.properties.get( "evictionPolicy" ) ).isEqualTo( "LRU" );
		assertThat( defaultCache.properties.get( "objectStore" ) ).isEqualTo( "ConcurrentSoftReferenceStore" );
		assertThat( defaultCache.properties.get( "useLastAccessTimeouts" ) ).isEqualTo( true );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.runtime.caches.get( "imports" );
		assertThat( importCache.name.getNameNoCase() ).isEqualTo( "IMPORTS" );
		assertThat( importCache.provider.getNameNoCase() ).isEqualTo( "BOXLANG" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maxObjects" ) ).isEqualTo( 200 );
	}

}
