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
package ortus.boxlang.runtime.loader.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

class ClassDiscoveryTest {

	@DisplayName( "It can get class files by package name" )
	@Test
	void testGetClassFiles() throws IOException {
		String[] classes = ClassDiscovery.getClassFiles( "ortus.boxlang.runtime.loader", false );
		System.out.println( Arrays.deepToString( classes ) );
		assertThat( classes ).isNotEmpty();
		assertThat( classes ).asList().contains( "ortus.boxlang.runtime.loader.ClassLocator" );
	}

	@DisplayName( "It can get class files by package name recursively" )
	@Test
	void testGetClassFilesRecursively() throws IOException {
		String[] classes = ClassDiscovery.getClassFiles( "ortus.boxlang.runtime.loader", true );
		System.out.println( Arrays.deepToString( classes ) );
		assertThat( classes ).isNotEmpty();
		assertThat( classes ).asList().contains( "ortus.boxlang.runtime.loader.util.ClassDiscovery" );
	}

	@DisplayName( "Test getFileFromResource returns a valid File object for a directory" )
	@Test
	void testGetDirectoryFromResource() {
		// Given
		String	resourceName	= "modules";
		// When
		File	file			= ClassDiscovery.getFileFromResource( resourceName );
		// Then
		assertThat( file ).isNotNull();
		assertThat( file.exists() ).isTrue();
		assertThat( file.isDirectory() ).isTrue();
	}

	@DisplayName( "Test getFileFromResource returns a valid File object for a file" )
	@Test
	void testGetFileFromResource() {
		// Given
		String	resourceName	= "ortus/boxlang/runtime/modules/ModuleRecord.class";
		// When
		File	file			= ClassDiscovery.getFileFromResource( resourceName );
		// Then
		assertThat( file ).isNotNull();
		assertThat( file.exists() ).isTrue();
		assertThat( file.isDirectory() ).isFalse();
	}

	@DisplayName( "Test getFileFromResource throws exception for non-existent resource" )
	@Test
	void testGetFileFromResourceThrowsException() {
		// Given
		String nonExistentResourceName = "nonexistent/resource";
		assertThrows( BoxRuntimeException.class, () -> ClassDiscovery.getFileFromResource( nonExistentResourceName ) );
	}

}
