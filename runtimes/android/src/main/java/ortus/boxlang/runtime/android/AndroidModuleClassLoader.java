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
package ortus.boxlang.runtime.android;

import java.io.File;

import android.content.Context;

import dalvik.system.DexClassLoader;

/**
 * The Android per-module class loader — the ART-legal analog of the desktop
 * {@code DynamicClassLoader}, giving each BoxLang module its own isolated loader.
 * <p>
 * Where desktop modules use a {@code DynamicClassLoader} ({@code URLClassLoader}) over the
 * module directory + {@code libs/*.jar}, Android uses a {@link DexClassLoader} over a single
 * per-module archive ({@code modules/<name>.jar}, built at compile time: the module's
 * AOT-compiled classes + {@code libs} converted to {@code classes.dex} by {@code d8}, with
 * {@code META-INF/services} and other resources retained). Unlike raw bytecode
 * {@code defineClass} (forbidden on ART), loading <b>DEX</b> at runtime is allowed (API 26+),
 * so this preserves the desktop model exactly:
 * <ul>
 * <li><b>Isolation</b> — each module gets its own loader; module A's lib version cannot clash
 * with module B's.</li>
 * <li><b>Hierarchy</b> — the parent is the runtime loader, so modules see core but not each
 * other (mediated by the runtime).</li>
 * <li><b>ServiceLoader</b> — the archive carries {@code META-INF/services}, so
 * {@code ServiceLoader.load( BIF.class, moduleLoader )} still discovers the module's providers.</li>
 * </ul>
 */
public class AndroidModuleClassLoader extends DexClassLoader {

	/**
	 * The module name this loader serves.
	 */
	private final String moduleName;

	/**
	 * Create a per-module loader over the module's pre-built archive.
	 *
	 * @param moduleName    The module name (for diagnostics)
	 * @param moduleArchive The {@code modules/<name>.jar} (containing {@code classes.dex} + resources)
	 * @param context       The Android context (used for the optimized/code-cache directory)
	 * @param parent        The parent loader (typically the runtime loader)
	 */
	public AndroidModuleClassLoader( String moduleName, File moduleArchive, Context context, ClassLoader parent ) {
		super(
		    moduleArchive.getAbsolutePath(),
		    codeCacheDir( context, moduleName ).getAbsolutePath(),
		    null,
		    parent
		);
		this.moduleName = moduleName;
	}

	/**
	 * @return The module name this loader serves
	 */
	public String getModuleName() {
		return this.moduleName;
	}

	/**
	 * Resolve a per-module optimized-dex/code-cache directory under the app's code cache.
	 *
	 * @param context The Android context
	 * @param name    The module name
	 *
	 * @return The (created) directory
	 */
	private static File codeCacheDir( Context context, String name ) {
		File dir = new File( context.getCodeCacheDir(), "boxmodules/" + name );
		dir.mkdirs();
		return dir;
	}
}
