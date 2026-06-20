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

import android.app.Application;

/**
 * The generic BoxLang {@link Application}. Apps declare THIS class directly in their
 * manifest's {@code android:name} (no per-app subclass needed). It boots the BoxLang
 * runtime once for the process and fires {@code onApplicationStart} via {@code Application.bx}.
 * <p>
 * Process teardown fires {@code onApplicationEnd} and shuts the runtime down.
 */
public class BoxAndroidApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		// Boot the BoxLang runtime (AOT/NoOp mode) and seed the app from assets.
		// onApplicationStart is fired lazily on the first request context that loads
		// Application.bx, consistent with the web/lambda listener contract.
		AndroidBoxRuntime.boot( this );
	}

	@Override
	public void onTerminate() {
		// Note: onTerminate is not called on real devices, only emulators; teardown is
		// best-effort. The runtime also registers a JVM shutdown hook.
		AndroidBoxRuntime.shutdown();
		super.onTerminate();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		// Best-effort: clear caches if the runtime is up.
		try {
			AndroidBoxRuntime.getInstance().getRuntime().getCacheService();
		} catch ( IllegalStateException ignored ) {
			// runtime not booted yet
		}
	}
}
