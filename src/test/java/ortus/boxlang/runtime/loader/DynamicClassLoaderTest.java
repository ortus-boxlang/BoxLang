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
package ortus.boxlang.runtime.loader;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;

public class DynamicClassLoaderTest {

	static BoxRuntime runtime;

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@Test
	@DisplayName( "Load Class Successfully" )
	void testLoadClassSuccessfully() throws ClassNotFoundException, IOException {

		// Given
		Path				libPath				= Paths.get( "src/test/resources/libs/" ).toAbsolutePath().normalize();
		URL[]				urls				= DynamicClassLoader.getJarURLs( libPath );
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestClassLoader" ), urls, parentClassLoader, false );

		try {
			assertThat( dynamicClassLoader.getURLs() ).hasLength( 3 );
			assertThat( dynamicClassLoader.getResource( "config.properties" ) ).isNotNull();

			// String targetClass = "com.github.benmanes.caffeine.cache.Caffeine";
			String		targetClass	= "HelloWorld";
			Class<?>	loadedClass	= dynamicClassLoader.loadClass(
			    targetClass
			); // Replace with an actual test class name

			// Then
			assertThat( loadedClass ).isNotNull();
			assertThat( loadedClass.getName() ).isEqualTo( targetClass );
			assertThat( loadedClass.getClassLoader() ).isEqualTo( dynamicClassLoader );
			// Check cache
			assertThat( dynamicClassLoader.isCacheEmpty() ).isFalse();
			assertThat( dynamicClassLoader.getCacheSize() ).isEqualTo( 1 );
			assertThat( dynamicClassLoader.getCacheKeys() ).contains( targetClass );
		} finally {
			dynamicClassLoader.close();
		}
	}

	@Test
	@DisplayName( "Class Not Found" )
	void testClassNotFound() throws Exception {
		// Given
		URL[]				urls				= new URL[] { /* Add your test URLs here */ };
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestClassLoader" ), urls, parentClassLoader, false );

		// When
		try {
			dynamicClassLoader.loadClass( "NonExistentClass" );
		} catch ( ClassNotFoundException e ) {
			// Then
			assertThat( e ).isNotNull();
			assertThat( e.getMessage() ).contains( "NonExistentClass" );
		} finally {
			dynamicClassLoader.close();
		}
	}

	@Test
	@DisplayName( "Close ClassLoader" )
	void testCloseClassLoader() throws Exception {
		// Given
		URL[]				urls				= new URL[] { /* Add your test URLs here */ };
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestClassLoader" ), urls, parentClassLoader, false );

		// When
		dynamicClassLoader.close();

		// Then
		assertThat( dynamicClassLoader.getDynamicParent() ).isNull();
		assertThat( dynamicClassLoader.isCacheEmpty() ).isTrue();
	}

	@Test
	@DisplayName( "Load Class from module" )
	void testLoadClassFromModule() throws ClassNotFoundException, IOException {

		var					runtime				= BoxRuntime.getInstance( true );
		URL[]				urls				= new URL[] {};
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestClassLoaderModule" ), urls, runtime.getRuntimeLoader(), false );

		Key					moduleName			= new Key( "test" );
		String				physicalPath		= Paths.get( "./modules/test" ).toAbsolutePath().toString();
		ModuleRecord		moduleRecord		= new ModuleRecord( physicalPath );
		IBoxContext			context				= new ScriptingRequestBoxContext();
		ModuleService		moduleService		= runtime.getModuleService();

		// When
		moduleRecord
		    .loadDescriptor( context )
		    .register( context )
		    .activate( context );

		moduleService.getRegistry().put( moduleName, moduleRecord );

		String		targetClass	= "HelloWorld";
		Class<?>	loadedClass	= dynamicClassLoader.loadClass( targetClass );
		dynamicClassLoader.close();

		assertThat( loadedClass ).isNotNull();
		assertThat( loadedClass.getName() ).isEqualTo( targetClass );

	}

	@Test
	@DisplayName( "Add Paths - Single String Path" )
	void testAddPathsSingleString() throws Exception {
		// Given
		Path				libPath				= Paths.get( "src/test/resources/libs/" ).toAbsolutePath().normalize();
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestAddPathsClassLoader" ), new URL[] {}, parentClassLoader, false );

		// Get initial URL count
		int					initialUrlCount		= dynamicClassLoader.getURLs().length;

		// When
		dynamicClassLoader.addPaths( libPath.toString() );

		// Then
		assertThat( dynamicClassLoader.getURLs().length ).isGreaterThan( initialUrlCount );

		// Should be able to load class from added path
		try {
			String		targetClass	= "HelloWorld";
			Class<?>	loadedClass	= dynamicClassLoader.loadClass( targetClass );
			assertThat( loadedClass ).isNotNull();
			assertThat( loadedClass.getName() ).isEqualTo( targetClass );
		} finally {
			dynamicClassLoader.close();
		}
	}

	@Test
	@DisplayName( "Add Paths - Array of Paths" )
	void testAddPathsArray() throws Exception {
		// Given
		Path				libPath				= Paths.get( "src/test/resources/libs/" ).toAbsolutePath().normalize();
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestAddPathsArrayClassLoader" ), new URL[] {}, parentClassLoader, false );

		// Create Array with multiple paths
		Array				paths				= new Array();
		paths.add( libPath.toString() );

		// Get initial URL count
		int initialUrlCount = dynamicClassLoader.getURLs().length;

		// When
		dynamicClassLoader.addPaths( paths );

		// Then
		assertThat( dynamicClassLoader.getURLs().length ).isGreaterThan( initialUrlCount );

		// Should be able to load class from added path
		try {
			String		targetClass	= "HelloWorld";
			Class<?>	loadedClass	= dynamicClassLoader.loadClass( targetClass );
			assertThat( loadedClass ).isNotNull();
			assertThat( loadedClass.getName() ).isEqualTo( targetClass );
		} finally {
			dynamicClassLoader.close();
		}
	}

	@Test
	@DisplayName( "Add Paths - Invalid Path Throws Exception" )
	void testAddPathsInvalidPath() throws Exception {
		// Given
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestInvalidPathClassLoader" ), new URL[] {}, parentClassLoader, false );

		try {
			// When/Then
			BoxIOException exception = assertThrows( BoxIOException.class, () -> {
				dynamicClassLoader.addPaths( "/non/existent/path/that/should/not/exist" );
			} );

			assertThat( exception.getMessage() ).contains( "Failed to add path" );
			assertThat( exception.getMessage() ).contains( "non/existent/path" );
		} finally {
			dynamicClassLoader.close();
		}
	}

	@Test
	@DisplayName( "Add Paths - Empty Array" )
	void testAddPathsEmptyArray() throws Exception {
		// Given
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestEmptyArrayClassLoader" ), new URL[] {}, parentClassLoader, false );

		// Create empty Array
		Array				emptyPaths			= new Array();

		// Get initial URL count
		int					initialUrlCount		= dynamicClassLoader.getURLs().length;

		// When
		dynamicClassLoader.addPaths( emptyPaths );

		// Then
		assertThat( dynamicClassLoader.getURLs().length ).isEqualTo( initialUrlCount );

		dynamicClassLoader.close();
	}

}
