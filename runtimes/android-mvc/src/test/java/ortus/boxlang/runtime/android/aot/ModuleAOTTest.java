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
 * Proves the Android <b>per-module</b> packaging + isolation mechanism on a plain JVM.
 * <p>
 * A module's {@code ModuleConfig.bx} is AOT-compiled and extracted, then {@link ModuleArchiver}
 * packages the classes together with the module's {@code META-INF/services} resources into one
 * archive. An <i>isolated</i> {@link URLClassLoader} over that archive stands in for the
 * device's {@code DexClassLoader} (the only Android-specific swap), and we assert:
 * <ul>
 * <li>the module class loads <b>in its own loader</b> (isolation from the parent),</li>
 * <li>the module's {@code META-INF/services} resource is discoverable through that loader (the
 * exact lookup {@code ServiceLoader} performs to find a module's BIFs/components).</li>
 * </ul>
 */
class ModuleAOTTest {

	static BoxRuntime	runtime;
	static Path			moduleArchive;
	static String		moduleClass;
	static ClassLoader	parentLoader;

	@BeforeAll
	static void buildModuleArchive( @TempDir Path work ) throws Exception {
		runtime			= BoxRuntime.getInstance( true );
		parentLoader	= ModuleAOTTest.class.getClassLoader();

		// 1. AOT-compile the module descriptor and extract to .class.
		Path	descriptor	= Paths.get( "src/test/resources/module/ModuleConfig.bx" ).toAbsolutePath();
		Path	container	= work.resolve( "ModuleConfig.bxclass" );
		assertThat( BXCompiler.compileFile( descriptor, container, true, runtime ) ).isTrue();

		Path			classesDir	= work.resolve( "classes" );
		List<String>	classNames	= BoxClassExtractor.extract( container, classesDir );
		moduleClass = classNames.stream().filter( n -> !n.contains( "$" ) ).findFirst().orElse( classNames.get( 0 ) );

		// 2. Package classes + module resources (META-INF/services, etc.) into one archive.
		// On device this jar is the input to d8 -> classes.dex, loaded by DexClassLoader.
		Path resources = Paths.get( "src/test/resources/module-resources" ).toAbsolutePath();
		moduleArchive = work.resolve( "modules/testmodule.jar" );
		List<String> entries = ModuleArchiver.archive( moduleArchive, classesDir, resources );

		assertThat( Files.exists( moduleArchive ) ).isTrue();
		assertThat( entries ).contains( "META-INF/services/com.example.Greeter" );
	}

	@DisplayName( "A module class loads in its own isolated loader (not the parent)" )
	@Test
	void testModuleIsolation() throws Exception {
		try ( URLClassLoader moduleLoader = new URLClassLoader(
		    new URL[] { moduleArchive.toUri().toURL() },
		    parentLoader ) ) {

			Class<?> loaded = moduleLoader.loadClass( moduleClass );

			// The module's own class is provided by the MODULE loader, not the parent —
			// i.e. it is isolated. (On device this loader is a DexClassLoader.)
			assertThat( loaded.getClassLoader() ).isSameInstanceAs( moduleLoader );
			assertThat( loaded.getClassLoader() ).isNotSameInstanceAs( parentLoader );
		}
	}

	@DisplayName( "The module's META-INF/services is discoverable through its loader (ServiceLoader path)" )
	@Test
	void testServiceLoaderResourceVisible() throws Exception {
		try ( URLClassLoader moduleLoader = new URLClassLoader(
		    new URL[] { moduleArchive.toUri().toURL() },
		    parentLoader ) ) {

			URL service = moduleLoader.getResource( "META-INF/services/com.example.Greeter" );
			assertThat( service ).isNotNull();

			String body = new String( moduleLoader.getResourceAsStream( "META-INF/services/com.example.Greeter" ).readAllBytes() );
			assertThat( body ).contains( "modules.testmodule.Greeter" );
		}
	}
}
