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

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.modules.ModuleRecord;

/**
 * Factory for the runtime's class loaders — both the root runtime loader and each module's
 * isolated loader.
 * <p>
 * A single factory governs all class-loader construction so a deployment target can swap the
 * whole strategy in one place, without forking {@link BoxRuntime}, {@code ModuleService}, or
 * {@code ModuleRecord}. The default {@link DynamicClassLoaderFactory} builds
 * {@link DynamicClassLoader}s ({@code URLClassLoader}s). Targets that cannot use
 * {@code URLClassLoader} or runtime {@code defineClass} (e.g. Android) provide their own — the
 * runtime loader can simply be the application class loader, and modules can be
 * {@code DexClassLoader}-backed.
 * <p>
 * Install via {@link BoxRuntime#setClassLoaderFactory(IClassLoaderFactory)} <b>before the
 * runtime is booted</b> ({@code getInstance(...)}), because the runtime loader is built during
 * construction.
 */
public interface IClassLoaderFactory {

	/**
	 * Create the runtime's root class loader (parent of all module loaders, used for
	 * runtime-level Java interop / dynamic lookups). Consumed purely as a {@link ClassLoader}.
	 *
	 * @param runtime   The runtime being booted (provides configuration such as javaLibraryPaths)
	 * @param appParent The application/system class loader to parent to
	 *
	 * @return The runtime class loader
	 */
	ClassLoader createRuntimeClassLoader( BoxRuntime runtime, ClassLoader appParent );

	/**
	 * Create a module's isolated class loader.
	 *
	 * @param record The module record (provides name, physical path, libs location, etc.)
	 * @param parent The parent class loader (typically the runtime loader)
	 *
	 * @return The module's class loader
	 */
	IModuleClassLoader createModuleClassLoader( ModuleRecord record, ClassLoader parent );

	/**
	 * Create the class loader used to resolve a generated (compiled) BoxLang class for a given
	 * class pool. This is the loader behind {@code ClassInfo.getClassLoader()}.
	 * <p>
	 * The default {@link DynamicClassLoaderFactory} returns a {@link DiskClassLoader} (a
	 * {@code URLClassLoader}) that {@code defineClass()}es bytecode and JIT-compiles on miss.
	 * Targets that cannot {@code defineClass} raw bytecode at runtime (e.g. Android AOT, where
	 * ART forbids it) return a resolve-only loader that delegates to a parent already holding the
	 * pre-compiled (dexed) classes, so resolution happens purely by parent-first delegation.
	 *
	 * @param runtime       The running runtime (provides configuration such as the class generation directory)
	 * @param boxpiler      The boxpiler that owns the class pool (its loader becomes the parent in the default case)
	 * @param classPoolName The name of the class pool the generated class belongs to
	 *
	 * @return The class loader for generated classes, consumed purely as a {@link ClassLoader}
	 */
	ClassLoader createGeneratedClassLoader( BoxRuntime runtime, IBoxpiler boxpiler, String classPoolName );
}
