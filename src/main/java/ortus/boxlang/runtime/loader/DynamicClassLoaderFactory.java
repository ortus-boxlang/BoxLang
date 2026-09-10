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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The default, standard-JVM class loader factory.
 * <p>
 * Builds {@link DynamicClassLoader}s ({@code URLClassLoader}s) for both the runtime loader and
 * each module loader, reproducing the behavior that previously lived inline in the
 * {@link BoxRuntime} constructor and {@code ModuleRecord.register()}.
 */
public class DynamicClassLoaderFactory implements IClassLoaderFactory {

	@Override
	public ClassLoader createRuntimeClassLoader( BoxRuntime runtime, ClassLoader appParent ) {
		return new DynamicClassLoader(
		    Key.runtime,
		    runtime.getConfiguration().getJavaLibraryPaths(),
		    appParent,
		    true
		);
	}

	@Override
	public IModuleClassLoader createModuleClassLoader( ModuleRecord record, ClassLoader parent ) {
		DynamicClassLoader classLoader;
		try {
			// Load *.class files under the `modules.<name>` package prefix from the module dir.
			classLoader = new DynamicClassLoader(
			    record.name,
			    record.physicalPath.toUri().toURL(),
			    parent,
			    false
			);
		} catch ( MalformedURLException e ) {
			throw new BoxRuntimeException( "Error creating module [" + record.name + "] class loader", e );
		}

		// Seed the loader with the module's libs/*.jar dependencies (jars ONLY).
		Path libsPath = record.physicalPath.resolve( ModuleService.MODULE_LIBS );
		if ( Files.exists( libsPath ) && Files.isDirectory( libsPath ) ) {
			try {
				classLoader.addURLs( DynamicClassLoader.getJarURLs( libsPath ) );
			} catch ( IOException e ) {
				throw new BoxRuntimeException(
				    "Error while seeding the module [" + record.name + "] class loader with the libs folder", e );
			}
		}

		return classLoader;
	}

	/**
	 * Builds a {@link DiskClassLoader} for the generated class — the standard JVM behavior that
	 * previously lived inline in {@code ClassInfo.getClassLoader()}: a {@code URLClassLoader} that
	 * {@code defineClass()}es bytecode and JIT-compiles on a cache miss, parented to the boxpiler's
	 * own class loader and reading from the configured class generation directory.
	 */
	@Override
	public ClassLoader createGeneratedClassLoader( BoxRuntime runtime, IBoxpiler boxpiler, String classPoolName ) {
		return new DiskClassLoader(
		    new URL[] {},
		    boxpiler.getClass().getClassLoader(),
		    Paths.get( runtime.getConfiguration().classGenerationDirectory ),
		    boxpiler,
		    classPoolName
		);
	}
}
