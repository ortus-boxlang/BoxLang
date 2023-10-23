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

import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.config.segments.CacheConfig;

public class ConfigLoaderTest {

	@DisplayName( "It can load the core config file" )
	@Test
	void testItCanLoadTheCoreConfig() {
		Configuration config = ConfigLoader.getInstance().loadCore();

		// Compiler Checks
		assertThat( config.compiler ).isNotNull();
		assertThat( config.compiler.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.runtime.mappings ).isEmpty();
		assertThat( config.runtime.modulesDirectory ).doesNotContainMatch( "(ignorecase)\\{user-home\\}" );

		// Cache Checks
		assertThat( config.runtime.caches ).isNotEmpty();
		assertThat( config.runtime.caches ).hasSize( 2 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.runtime.caches.get( "default" );
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.type.getNameNoCase() ).isEqualTo( "CAFFEINE" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maximumSize" ) ).isEqualTo( 1000 );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.runtime.caches.get( "imports" );
		assertThat( importCache.name.getNameNoCase() ).isEqualTo( "IMPORTS" );
		assertThat( importCache.type.getNameNoCase() ).isEqualTo( "CAFFEINE" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maximumSize" ) ).isEqualTo( 1000 );
	}

	@DisplayName( "It can load a custom config file using a string" )
	@Test
	void testItCanLoadACustomConfig() {
		Configuration config = ConfigLoader.getInstance().loadFromFile( "src/test/resources/test-config.json" );
		assertConfigTest( config );
	}

	@DisplayName( "It can load a custom config file using a URL" )
	@Test
	void testItCanLoadACustomConfigUsingAURL() {
		URL				url		= ConfigLoaderTest.class.getClassLoader().getResource( "test-config.json" );
		Configuration	config	= ConfigLoader.getInstance().loadFromFile( url );
		assertConfigTest( config );
	}

	@DisplayName( "It can load a custom config file using a Path" )
	@Test
	void testItCanLoadACustomConfigUsingAPath() {
		Configuration config = ConfigLoader.getInstance().loadFromFile(
		    Path.of( "src/test/resources/test-config.json" )
		);
		assertConfigTest( config );
	}

	private void assertConfigTest( Configuration config ) {
		// Compiler Checks
		assertThat( config.compiler ).isNotNull();
		assertThat( config.compiler.classGenerationDirectory ).doesNotContainMatch( "(ignorecase)\\{java-temp\\}" );

		// Runtime Checks
		assertThat( config.runtime.mappings ).isEmpty();
		assertThat( config.runtime.modulesDirectory ).doesNotContainMatch( "(ignorecase)\\{user-home\\}" );

		// Cache Checks
		assertThat( config.runtime.caches ).isNotEmpty();
		assertThat( config.runtime.caches ).hasSize( 2 );

		// Default Cache Checks
		CacheConfig defaultCache = ( CacheConfig ) config.runtime.caches.get( "default" );
		assertThat( defaultCache ).isNotNull();
		assertThat( defaultCache.name.getNameNoCase() ).isEqualTo( "DEFAULT" );
		assertThat( defaultCache.type.getNameNoCase() ).isEqualTo( "CAFFEINE" );
		assertThat( defaultCache.properties ).isNotNull();
		assertThat( defaultCache.properties.get( "maximumSize" ) ).isEqualTo( 500 );

		// Import Cache Checks
		CacheConfig importCache = ( CacheConfig ) config.runtime.caches.get( "imports" );
		assertThat( importCache.name.getNameNoCase() ).isEqualTo( "IMPORTS" );
		assertThat( importCache.type.getNameNoCase() ).isEqualTo( "CAFFEINE" );
		assertThat( importCache.properties ).isNotNull();
		assertThat( importCache.properties.get( "maximumSize" ) ).isEqualTo( 500 );
	}

}
