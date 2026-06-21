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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Packages a single BoxLang module into one self-contained archive for Android's per-module
 * class-loading isolation.
 * <p>
 * On Android each module is loaded by its own {@code DexClassLoader} (the ART-legal analog of
 * the desktop {@code DynamicClassLoader}), giving the same isolation + hierarchy. That loader
 * reads a single archive that must contain BOTH the module's code and its resources:
 * <ul>
 * <li><b>Code</b> — the module's AOT-compiled classes (the module {@code .bx} extracted by
 * {@link BoxClassExtractor}, plus its {@code libs/*.jar} contents). On device these are
 * converted to {@code classes.dex} by {@code d8}; the jar this class produces is the input to
 * {@code d8}.</li>
 * <li><b>Resources</b> — {@code META-INF/services/*} (so {@code ServiceLoader} can still
 * discover the module's BIFs/components/interceptors via the module loader), the
 * {@code ModuleConfig.bx} descriptor, templates, and {@code public/} assets.</li>
 * </ul>
 * Bundling resources alongside the dex is exactly why we ship a jar-with-dex per module rather
 * than a bare {@code .dex} — {@code ServiceLoader} relies on resource files that a bare dex
 * cannot carry.
 * <p>
 * Pure JVM — no Android dependencies — so it is unit-tested on a plain JVM (a
 * {@code URLClassLoader} over the produced jar stands in for the device's {@code DexClassLoader}).
 */
public final class ModuleArchiver {

	private ModuleArchiver() {
		// static utility
	}

	/**
	 * Build a module archive (jar) from one or more content roots. Each root's tree is copied
	 * into the jar preserving relative paths, so callers typically pass the extracted-classes
	 * directory and the module's resource directory.
	 *
	 * @param outputJar The jar file to create (parent dirs are created)
	 * @param roots     The content roots to include (later roots overwrite earlier on conflict)
	 *
	 * @return The list of entry names written into the jar
	 *
	 * @throws IOException If reading or writing fails
	 */
	public static List<String> archive( Path outputJar, Path... roots ) throws IOException {
		Files.createDirectories( outputJar.toAbsolutePath().getParent() );
		List<String> entries = new ArrayList<>();

		try ( JarOutputStream jar = new JarOutputStream( Files.newOutputStream( outputJar ) ) ) {
			for ( Path root : roots ) {
				if ( root == null || !Files.exists( root ) ) {
					continue;
				}
				addRoot( jar, root, entries );
			}
		}
		return entries;
	}

	private static void addRoot( JarOutputStream jar, Path root, List<String> entries ) throws IOException {
		try ( var stream = Files.walk( root ) ) {
			for ( Path file : ( Iterable<Path> ) stream.filter( Files::isRegularFile )::iterator ) {
				String entryName = root.relativize( file ).toString().replace( '\\', '/' );
				if ( entries.contains( entryName ) ) {
					continue;		// first root wins for a given path
				}
				jar.putNextEntry( new JarEntry( entryName ) );
				Files.copy( file, ( OutputStream ) jar );
				jar.closeEntry();
				entries.add( entryName );
			}
		}
	}
}
