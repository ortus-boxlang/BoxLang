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

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.android.aot.PreloadedClassLoader;
import ortus.boxlang.runtime.loader.IClassLoaderFactory;
import ortus.boxlang.runtime.loader.IModuleClassLoader;
import ortus.boxlang.runtime.modules.ModuleRecord;

/**
 * The Android class loader factory — installed at boot via
 * {@link BoxRuntime#setClassLoaderFactory(IClassLoaderFactory)} so the runtime never tries to
 * construct a {@code DynamicClassLoader} ({@code URLClassLoader}, absent on Android).
 * <ul>
 * <li><b>Runtime loader</b>: the application class loader itself. Everything (core, libs, and
 * AOT-compiled app classes) is dexed into the APK and resolvable by the app loader, so there
 * is nothing to seed and no {@code URLClassLoader} needed.</li>
 * <li><b>Module loaders</b>: an {@link AndroidModuleClassLoader} ({@code DexClassLoader}) over
 * each module's pre-built {@code modules/<name>.jar}, parented to the runtime loader —
 * preserving per-module isolation, hierarchy, and {@code ServiceLoader} discovery.</li>
 * </ul>
 */
public class AndroidClassLoaderFactory implements IClassLoaderFactory {

	/**
	 * The Android context (for per-module code-cache dirs).
	 */
	private final Context	context;

	/**
	 * The on-device app home holding the seeded module archives ({@code <appHome>/modules}).
	 */
	private final File		appHome;

	/**
	 * @param context The Android context
	 * @param appHome The app home directory (seeded from assets)
	 */
	public AndroidClassLoaderFactory( Context context, File appHome ) {
		this.context	= context;
		this.appHome	= appHome;
	}

	/**
	 * The runtime loader on Android is simply the application class loader — all code is dexed
	 * into the APK and resolvable there. The {@code appParent} passed in is that loader.
	 */
	@Override
	public ClassLoader createRuntimeClassLoader( BoxRuntime runtime, ClassLoader appParent ) {
		return appParent;
	}

	/**
	 * Build a {@link DexClassLoader}-backed loader over the module's pre-built archive.
	 */
	@Override
	public IModuleClassLoader createModuleClassLoader( ModuleRecord record, ClassLoader parent ) {
		File moduleArchive = new File( new File( this.appHome, "modules" ), record.name.getName() + ".jar" );
		return new AndroidModuleClassLoader( record.name, moduleArchive, this.context, parent );
	}

	/**
	 * Resolve-only loader for AOT-compiled {@code boxgenerated.*} classes. On Android these are
	 * dexed into the APK and already loadable by the application class loader; ART forbids
	 * {@code defineClass(byte[])} of raw JVM bytecode, so instead of a {@link ortus.boxlang.runtime.loader.DiskClassLoader}
	 * we return a {@link PreloadedClassLoader} that resolves purely by parent-first delegation to the
	 * loader that holds those classes (the same loader that loaded the boxpiler).
	 */
	@Override
	public ClassLoader createGeneratedClassLoader( BoxRuntime runtime, IBoxpiler boxpiler, String classPoolName ) {
		return new PreloadedClassLoader( boxpiler.getClass().getClassLoader() );
	}
}
