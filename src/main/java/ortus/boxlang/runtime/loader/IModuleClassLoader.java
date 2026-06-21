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

import java.io.Closeable;

import ortus.boxlang.runtime.scopes.Key;

/**
 * The contract a BoxLang module class loader must satisfy.
 * <p>
 * Modules are loaded in isolation: each module gets its own class loader, parented to the
 * runtime loader, so module dependencies don't clash and modules can't see each other except
 * through the runtime. On the standard JVM this is fulfilled by {@link DynamicClassLoader}
 * (a {@code URLClassLoader}). Other deployment targets can provide a different implementation
 * — for example Android, where {@code URLClassLoader} is unavailable and runtime
 * {@code defineClass} of JVM bytecode is forbidden, can supply a {@code DexClassLoader}-backed
 * loader. The implementation is selected per-runtime via an
 * {@link IClassLoaderFactory}.
 * <p>
 * <b>Implementations MUST be a {@link java.lang.ClassLoader}</b> (so they can be passed to
 * {@link java.util.ServiceLoader}); {@link #toClassLoader()} exposes that view.
 */
public interface IModuleClassLoader extends Closeable {

	/**
	 * Find a class in this module loader.
	 *
	 * @param className   The fully-qualified class name
	 * @param safe        When {@code true}, return {@code null} instead of throwing if not found
	 * @param checkParent When {@code true}, delegate to the parent loader if not found locally
	 *
	 * @return The loaded class, or {@code null} when {@code safe} and not found
	 *
	 * @throws ClassNotFoundException If the class cannot be found and {@code safe} is {@code false}
	 */
	Class<?> findClass( String className, Boolean safe, boolean checkParent ) throws ClassNotFoundException;

	/**
	 * @return The unique name of this loader as a {@link Key}
	 */
	Key getNameAsKey();

	/**
	 * @return This loader viewed as a {@link ClassLoader} (for {@code ServiceLoader} and interop)
	 */
	ClassLoader toClassLoader();
}
