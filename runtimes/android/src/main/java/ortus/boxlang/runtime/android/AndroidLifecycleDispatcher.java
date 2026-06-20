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

import ortus.boxlang.runtime.application.ApplicationClassListener;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Dispatches Android lifecycle callbacks to the optional Android-specific hook methods on
 * the app's {@code Application.bx}, using convention-over-configuration: a hook fires only
 * when the developer has defined a matching function, otherwise it is a clean no-op.
 * <p>
 * This mirrors how {@link ApplicationClassListener} invokes standard lifecycle methods
 * (check {@code thisScope} for the key, then {@code dereferenceAndInvoke}). Supported hooks:
 * {@code onActivityCreate}, {@code onActivityStart}, {@code onActivityResume},
 * {@code onActivityPause}, {@code onActivityStop}, {@code onActivityDestroy},
 * {@code onActivityResult}, {@code onPermissionResult}, {@code onBackPressed},
 * {@code onLowMemory}, {@code onConfigurationChanged}.
 */
public class AndroidLifecycleDispatcher {

	private final IBoxContext context;

	/**
	 * @param context The request context whose application listener owns the hooks
	 */
	public AndroidLifecycleDispatcher( IBoxContext context ) {
		this.context = context;
	}

	/**
	 * Invoke an optional Android hook on {@code Application.bx} if it is defined.
	 *
	 * @param hook The hook name (e.g. {@code onActivityResume})
	 * @param args The positional arguments to pass
	 *
	 * @return The hook's return value, or {@code null} if the hook is not defined
	 */
	public Object invokeHook( String hook, Object... args ) {
		IClassRunnable listener = resolveListenerClass();
		if ( listener == null ) {
			return null;
		}
		Key hookKey = Key.of( hook );
		if ( !listener.getThisScope().containsKey( hookKey ) ) {
			return null;		// hook not defined — clean no-op
		}
		return listener.dereferenceAndInvoke( this.context, hookKey, args, false );
	}

	/**
	 * @param hook The hook name
	 *
	 * @return {@code true} if {@code Application.bx} defines the hook
	 */
	public boolean hasHook( String hook ) {
		IClassRunnable listener = resolveListenerClass();
		return listener != null && listener.getThisScope().containsKey( Key.of( hook ) );
	}

	private IClassRunnable resolveListenerClass() {
		BaseApplicationListener listener = this.context.getParentOfType( ortus.boxlang.runtime.context.RequestBoxContext.class )
		    .getApplicationListener();
		if ( listener instanceof ApplicationClassListener classListener ) {
			return classListener.getListenerClass();
		}
		return null;
	}
}
