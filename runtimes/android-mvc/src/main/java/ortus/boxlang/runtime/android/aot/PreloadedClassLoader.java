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

/**
 * An Android-safe class loader for AOT-compiled BoxLang classes.
 * <p>
 * Unlike {@code DiskClassLoader} (which extends {@code java.net.URLClassLoader} — a type
 * that <b>does not exist on Android</b> — and which calls {@code defineClass(byte[])} on raw
 * JVM bytecode, which <b>ART forbids</b>), this loader <b>never defines a class from bytes</b>.
 * It only <i>resolves</i> classes that are already present on the parent class loader.
 * <p>
 * On Android the generated {@code boxgenerated.*} classes are AOT-compiled to {@code .class}
 * (by {@code BXCompiler} + {@link BoxClassExtractor}), dexed into the APK by D8/R8, and thus
 * already loadable by the application class loader. Standard parent-first delegation finds
 * them; if a class is missing it means it was not AOT-compiled/dexed, which surfaces as a
 * clear error rather than an illegal runtime {@code defineClass}.
 */
public class PreloadedClassLoader extends ClassLoader {

	static {
		registerAsParallelCapable();
	}

	/**
	 * Construct a preloaded loader over the given parent (typically the application loader).
	 *
	 * @param parent The parent class loader that already holds the dexed classes
	 */
	public PreloadedClassLoader( ClassLoader parent ) {
		super( parent );
	}

	/**
	 * Construct a preloaded loader over the current thread's context class loader.
	 */
	public PreloadedClassLoader() {
		this( Thread.currentThread().getContextClassLoader() );
	}

	/**
	 * This loader never defines classes from bytecode. Reaching {@code findClass} means the
	 * class was not found by parent-first delegation — i.e. it was not AOT-compiled and dexed
	 * into the app. We fail loudly instead of attempting an (ART-illegal) {@code defineClass}.
	 *
	 * @param name The fully-qualified class name
	 *
	 * @return never returns normally
	 *
	 * @throws ClassNotFoundException always, with guidance
	 */
	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException {
		throw new ClassNotFoundException(
		    "BoxLang class [" + name + "] is not present on the app class loader. On Android, "
		        + "classes must be AOT-compiled (BXCompiler) and dexed into the APK — there is no "
		        + "runtime class definition from bytecode (ART forbids defineClass())." );
	}
}
