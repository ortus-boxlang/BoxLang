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
package ortus.boxlang.runtime.android.mvc;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.IService;

/**
 * The BoxLang Android MVC routing service.
 * <p>
 * Owns the singleton {@link Router} that {@code Application.bx} {@code configureRouter()}
 * populates, plus the per-runtime {@link FlashScope}. It implements {@link IService} so it
 * registers and participates in the runtime lifecycle exactly like other BoxLang services.
 * <p>
 * Kept Android-free so it can be unit-tested on a plain JVM and reused across targets.
 */
public class RoutingService implements IService {

	/**
	 * The unique service name.
	 */
	public static final Key		NAME	= Key.of( "RoutingService" );

	/**
	 * The route table.
	 */
	private final Router		router	= new Router();

	/**
	 * The per-runtime flash scope.
	 */
	private final FlashScope	flash	= new FlashScope();

	/**
	 * @return The router this service owns
	 */
	public Router getRouter() {
		return this.router;
	}

	/**
	 * @return The flash scope this service owns
	 */
	public FlashScope getFlash() {
		return this.flash;
	}

	/**
	 * Resolve an incoming request to a handler + action via the router.
	 *
	 * @param path   The request path
	 * @param method The HTTP method (may be {@code null})
	 *
	 * @return A non-null {@link RouteMatch}
	 */
	public RouteMatch resolve( String path, String method ) {
		return this.router.resolve( path, method );
	}

	@Override
	public Key getName() {
		return NAME;
	}

	@Override
	public void onConfigurationLoad() {
		// No-op: routes are configured from Application.bx configureRouter().
	}

	@Override
	public void onStartup() {
		// No-op: the router is ready on construction.
	}

	@Override
	public void onShutdown( Boolean force ) {
		this.flash.clear();
	}
}
