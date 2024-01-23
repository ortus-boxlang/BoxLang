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
package ortus.boxlang.runtime.services;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

class ModuleServiceTest {

	static BoxRuntime		runtime;
	static ModuleService	service;

	@BeforeAll
	public static void setupBeforeAll() {
		runtime	= BoxRuntime.getInstance( true );
		service	= runtime.getModuleService();
	}

	@AfterAll
	public static void tearDownAfterAll() {
		runtime.shutdown();
	}

	@DisplayName( "Test it can get an instance of the service" )
	@Test
	void testItCanGetInstance() {
		assertThat( service ).isNotNull();
	}

	@DisplayName( "Test it can get an load the initial module paths" )
	@Test
	void testItCanLoadModulePaths() {
		assertThat( service.getModulePaths().size() ).isGreaterThan( 0 );
		System.out.println( service.getModulePaths() );
	}

	@DisplayName( "Test it can run the startup service event" )
	@Test
	void testItCanRunStartupEvent() {
		assertDoesNotThrow( () -> service.onStartup() );
	}

	@DisplayName( "Test it can run the onShutdown service event" )
	@Test
	void testItCanRunOnShutdownEvent() {
		assertDoesNotThrow( () -> service.onShutdown() );
	}

	@DisplayName( "Test it can add an absolute path to the module search path" )
	@Test
	@Disabled( "Doesn't work on Windows" )
	void testItCanAddAbsolutePathToModuleSearchPath() {
		String path = "/tmp";
		assertDoesNotThrow( () -> service.addModulePath( path ) );
		assertThat( service.getModulePaths() ).contains( Paths.get( path ) );
	}

	@DisplayName( "Test it can ignore a null or empty path when adding to the module search path" )
	@Test
	void testItCanIgnoreNullPathWhenAddingToModuleSearchPath() {
		var count = service.getModulePaths().size();
		assertDoesNotThrow( () -> service.addModulePath( "" ) );
		assertThat( service.getModulePaths().size() ).isEqualTo( count );
	}

	@Test
	void testIt() throws IOException {
		// Path path = Paths.get( "/Users/lmajano/Sites/projects/boxlang/src/main/resources/modules" );

		System.out.println( service.getRegistry().keySet().toString() );

	}
}
