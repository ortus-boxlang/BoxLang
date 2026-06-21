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
import android.content.res.AssetManager;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.android.mvc.MVCDispatcher;
import ortus.boxlang.runtime.android.mvc.RoutingService;
import ortus.boxlang.runtime.android.mvc.ViewRenderer;

/**
 * The singleton entry point for running BoxLang on Android.
 * <p>
 * Boots {@link BoxRuntime} in Ahead-Of-Time mode (the NoOp compiler is selected
 * automatically because only it ships in the slim Android distribution — ART cannot
 * {@code defineClass()} raw JVM bytecode at runtime). It points the runtime home at an
 * app-private directory seeded from the APK's {@code bx/} asset payload, loads the bundled
 * {@code boxlang.json}, and wires up the MVC front controller (router + view renderer +
 * dispatcher) for the WebView track.
 * <p>
 * Tied to the Android application lifecycle: {@link BoxAndroidApplication#onCreate()}
 * calls {@link #boot(Context)} once, and process teardown calls {@link #shutdown()}.
 */
public final class AndroidBoxRuntime {

	/**
	 * The directory (under the app's files dir) the runtime home is seeded into.
	 */
	public static final String			HOME_DIR_NAME	= "boxlang";

	/**
	 * The asset directory inside the APK holding the BoxLang app payload.
	 */
	public static final String			ASSET_APP_DIR	= "bx";

	private static AndroidBoxRuntime	instance;

	private final BoxRuntime			runtime;
	private final File					appHome;
	private final RoutingService		routingService;
	private final MVCDispatcher			dispatcher;

	private AndroidBoxRuntime( BoxRuntime runtime, File appHome, RoutingService routingService, MVCDispatcher dispatcher ) {
		this.runtime		= runtime;
		this.appHome		= appHome;
		this.routingService	= routingService;
		this.dispatcher		= dispatcher;
	}

	/**
	 * Boot the runtime once for the given Android context.
	 *
	 * @param context The Android application context
	 *
	 * @return The booted singleton
	 */
	public static synchronized AndroidBoxRuntime boot( Context context ) {
		if ( instance != null ) {
			return instance;
		}

		// 1. Seed the runtime home from the APK assets on first launch.
		File appHome = new File( context.getFilesDir(), HOME_DIR_NAME );
		seedFromAssets( context.getAssets(), appHome );

		// 2. Point cache/temp at app-private storage; ART can't write to a global home.
		System.setProperty( "boxlang.home", appHome.getAbsolutePath() );

		// 3. Install the Android class loader factory BEFORE booting. This makes the runtime
		// loader the app class loader and modules DexClassLoader-backed — no URLClassLoader,
		// no runtime defineClass. Must be set before getInstance() builds the runtime loader.
		BoxRuntime.setClassLoaderFactory( new AndroidClassLoaderFactory( context, appHome ) );

		// 4. Boot the core runtime with the bundled boxlang.json. The NoOp boxpiler is
		// chosen automatically via ServiceLoader because it is the only one on the
		// classpath in the Android distribution.
		File			configFile		= new File( appHome, "boxlang.json" );
		BoxRuntime		runtime			= BoxRuntime.getInstance(
		    /* debugMode */ false,
		    configFile.exists() ? configFile.getAbsolutePath() : null,
		    appHome.getAbsolutePath()
		);

		// 5. Register the routing service and build the MVC front controller.
		RoutingService	routingService	= new RoutingService();
		runtime.getConfiguration();		// ensure config is materialized

		ViewRenderer	viewRenderer	= new ViewRenderer(
		    runtime,
		    new File( appHome, "views" ).getAbsolutePath(),
		    new File( appHome, "layouts" ).getAbsolutePath()
		);
		MVCDispatcher	dispatcher		= new MVCDispatcher( runtime, routingService, viewRenderer, "handlers" );

		instance = new AndroidBoxRuntime( runtime, appHome, routingService, dispatcher );
		return instance;
	}

	/**
	 * @return The booted singleton (must call {@link #boot(Context)} first)
	 */
	public static AndroidBoxRuntime getInstance() {
		if ( instance == null ) {
			throw new IllegalStateException( "AndroidBoxRuntime has not been booted. Call boot(context) first." );
		}
		return instance;
	}

	/**
	 * @return The underlying BoxLang runtime
	 */
	public BoxRuntime getRuntime() {
		return this.runtime;
	}

	/**
	 * @return The app home directory (seeded from assets)
	 */
	public File getAppHome() {
		return this.appHome;
	}

	/**
	 * @return The routing service (owns the router)
	 */
	public RoutingService getRoutingService() {
		return this.routingService;
	}

	/**
	 * @return The MVC front-controller dispatcher
	 */
	public MVCDispatcher getDispatcher() {
		return this.dispatcher;
	}

	/**
	 * Shut the runtime down (process teardown).
	 */
	public static synchronized void shutdown() {
		if ( instance != null ) {
			instance.runtime.shutdown();
			instance = null;
		}
	}

	/**
	 * Recursively copy the APK's {@code bx/} asset payload into the app home on first run.
	 * In dev mode the {@link BoxDevServer} pushes updated files here for hot reload.
	 *
	 * @param assets The Android asset manager
	 * @param target The destination directory
	 */
	private static void seedFromAssets( AssetManager assets, File target ) {
		if ( new File( target, "boxlang.json" ).exists() ) {
			return;		// already seeded
		}
		target.mkdirs();
		copyAssetDir( assets, ASSET_APP_DIR, target );
	}

	private static void copyAssetDir( AssetManager assets, String assetPath, File target ) {
		try {
			String[] children = assets.list( assetPath );
			if ( children == null || children.length == 0 ) {
				// It's a file — copy it.
				copyAssetFile( assets, assetPath, target );
				return;
			}
			target.mkdirs();
			for ( String child : children ) {
				copyAssetDir( assets, assetPath + "/" + child, new File( target, child ) );
			}
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to seed BoxLang app from assets: " + assetPath, e );
		}
	}

	private static void copyAssetFile( AssetManager assets, String assetPath, File target ) {
		try ( var in = assets.open( assetPath ); var out = new java.io.FileOutputStream( target ) ) {
			in.transferTo( out );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to copy asset: " + assetPath, e );
		}
	}
}
