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
package ortus.boxlang.runtime.android.aot;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.compiler.BXCompiler;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Proves the Android AOT pipeline end-to-end on a plain JVM (the parts that are JVM-verifiable):
 * <ol>
 * <li>{@code BXCompiler} compiles a {@code .bx} to the binary class container.</li>
 * <li>{@link BoxClassExtractor} unpacks it into standard, loadable {@code .class} files
 * (this is exactly what D8/R8 would dex into an APK).</li>
 * <li>{@link PreloadedClassLoader} resolves those classes by <b>parent delegation only</b>
 * — never calling {@code defineClass} — which is the on-device (ART-safe) mechanism.</li>
 * </ol>
 */
class AOTPipelineTest {

	static BoxRuntime	runtime;
	static Path			extractedDir;
	static List<String>	classNames;
	static String		mainClass;

	@BeforeAll
	static void compileAndExtract( @TempDir Path work ) throws Exception {
		runtime = BoxRuntime.getInstance( true );

		// 1. AOT-compile a real fixture handler to the BoxLang class container.
		Path	source		= Paths.get( "src/test/resources/app/handlers/Items.bx" ).toAbsolutePath();
		Path	container	= work.resolve( "Items.bxclass" );
		boolean	ok			= BXCompiler.compileFile( source, container, true, runtime );
		assertThat( ok ).isTrue();
		assertThat( Files.exists( container ) ).isTrue();
		assertThat( BoxClassExtractor.isContainer( container ) ).isTrue();

		// 2. Extract the container to standard .class files.
		extractedDir	= work.resolve( "classes" );
		classNames		= BoxClassExtractor.extract( container, extractedDir );
		mainClass		= classNames.stream().filter( n -> !n.contains( "$" ) ).findFirst().orElse( classNames.get( 0 ) );
	}

	@DisplayName( "BXCompiler output extracts into real, loadable .class files" )
	@Test
	void testExtractionProducesLoadableClasses() throws Exception {
		assertThat( classNames ).isNotEmpty();
		assertThat( classNames ).contains( mainClass );

		// Every extracted name has a corresponding .class file on disk.
		for ( String name : classNames ) {
			Path classFile = extractedDir.resolve( name.replace( '.', '/' ) + ".class" );
			assertThat( Files.exists( classFile ) ).isTrue();
		}

		// They are valid JVM bytecode: a standard URLClassLoader can load the main class.
		try ( URLClassLoader appLoader = new URLClassLoader(
		    new URL[] { extractedDir.toUri().toURL() },
		    getClass().getClassLoader() ) ) {
			Class<?> loaded = appLoader.loadClass( mainClass );
			assertThat( loaded.getName() ).isEqualTo( mainClass );
			assertThat( mainClass ).startsWith( "boxgenerated" );
		}
	}

	@DisplayName( "PreloadedClassLoader resolves AOT classes by parent delegation, never defining them" )
	@Test
	void testPreloadedResolvesViaParent() throws Exception {
		// Simulate the APK: the dexed classes live on the application (parent) loader.
		try ( URLClassLoader appLoader = new URLClassLoader(
		    new URL[] { extractedDir.toUri().toURL() },
		    getClass().getClassLoader() ) ) {

			PreloadedClassLoader	preloaded	= new PreloadedClassLoader( appLoader );
			Class<?>				loaded		= preloaded.loadClass( mainClass );

			// Resolved — and resolved by the PARENT (i.e. via delegation, not defineClass here).
			assertThat( loaded.getName() ).isEqualTo( mainClass );
			assertThat( loaded.getClassLoader() ).isSameInstanceAs( appLoader );
		}
	}

	@DisplayName( "PreloadedClassLoader fails loudly for a class that was not dexed (no defineClass)" )
	@Test
	void testPreloadedFailsForMissingClass() {
		PreloadedClassLoader	preloaded	= new PreloadedClassLoader( getClass().getClassLoader() );
		ClassNotFoundException	ex			= assertThrows(
		    ClassNotFoundException.class,
		    () -> preloaded.loadClass( "boxgenerated.boxclass.does.not.Exist" )
		);
		assertThat( ex ).hasMessageThat().contains( "AOT-compiled" );
	}
}
