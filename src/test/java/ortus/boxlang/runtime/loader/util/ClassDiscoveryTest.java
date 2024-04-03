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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassDiscoveryTest {

	@DisplayName( "It can get class files by package name" )
	@Test
	void testGetClassFiles() throws IOException {
		String[] classes = ClassDiscovery.getClassFiles( "ortus.boxlang.runtime.loader", false );
		assertThat( classes ).isNotEmpty();
		assertThat( classes ).asList().contains( "ortus.boxlang.runtime.loader.ClassLocator" );
	}

	@DisplayName( "It can get class files by package name recursively" )
	@Test
	void testGetClassFilesRecursively() throws IOException {
		String[] classes = ClassDiscovery.getClassFiles( "ortus.boxlang.runtime.loader", true );
		assertThat( classes ).isNotEmpty();
		assertThat( classes ).asList().contains( "ortus.boxlang.runtime.loader.util.ClassDiscovery" );
	}

	@DisplayName( "Test getFileFromResource returns a valid File object for a directory" )
	@Test
	void testGetDirectoryFromResource() {
		// Given
		String	resourceName	= "ortus/boxlang/runtime/modules";
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

	@Disabled( "This test is disabled because it requires a jar file to be present, used for manual testing purposes" )
	@Test
	void testFindClassesInJar() throws IOException, URISyntaxException {
		// jar:file:/Users/lmajano/Sites/projects/boxlang/build/libs/ortus-boxlang-1.0.0-all.jar!/modules
		String			jar		= "build/libs/ortus-boxlang-1.0.0-all.jar";
		Path			jarPath	= Paths.get( jar ).toAbsolutePath();
		URL				jarURL	= new URL( "jar:file:" + jarPath.toString() + "!/" );

		@SuppressWarnings( "unchecked" )
		List<Class<?>>	classes	= ClassDiscovery.findClassesInJar(
		    jarURL,
		    "ortus.boxlang.runtime.bifs.global".replace( '.', '/' ),
		    ClassLoader.getSystemClassLoader(),
		    new Class[] {}
		);

	}

}
