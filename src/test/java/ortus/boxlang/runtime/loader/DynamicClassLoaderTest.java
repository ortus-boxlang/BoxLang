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

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;

public class DynamicClassLoaderTest {

	@Test
	@DisplayName( "Load Class Successfully" )
	void testLoadClassSuccessfully() throws ClassNotFoundException, IOException {

		// Given
		Path				libPath				= Paths.get( "src/test/resources/libs/" ).toAbsolutePath().normalize();
		URL[]				urls				= DynamicClassLoader.getJarURLs( libPath );
		ClassLoader			parentClassLoader	= getClass().getClassLoader();
		DynamicClassLoader	dynamicClassLoader	= new DynamicClassLoader( Key.of( "TestClassLoader" ), urls, parentClassLoader, false );

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
	}

	@Test
	@DisplayName( "Class Not Found" )
	void testClassNotFound() {
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
}
