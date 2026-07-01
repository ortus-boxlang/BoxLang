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
package ortus.boxlang.runtime.modules;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.IInterceptor;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Contract for a pure-Java BoxLang module descriptor, equivalent to {@code ModuleConfig.bx}.
 *
 * <p>
 * Implement this interface and register the implementation via Java's {@link java.util.ServiceLoader}
 * mechanism ({@code META-INF/services/ortus.boxlang.runtime.modules.IModuleConfig}) inside a JAR
 * that the module's class loader can discover.
 * </p>
 *
 * <p>
 * <strong>Metadata fields</strong> — declare public instance fields on your implementation class
 * to override module metadata. All are optional; defaults mirror the BX convention:
 * </p>
 *
 * <pre>{@code
 *
 * public String version = "1.0.0";
 * public String author = "";
 * public String description = "";
 * public String webURL = "";
 * public boolean enabled = true;
 * public Array dependencies = new Array();
 * public Object mapping = null;         // String or IStruct
 * public Object publicMapping = null;         // String or IStruct
 * }</pre>
 *
 * <p>
 * <strong>Lifecycle methods</strong> — all have default no-op implementations; override only
 * what your module needs:
 * </p>
 * <ul>
 * <li>{@link #configure(IBoxContext, ModuleRecord)} — called during registration; mutate
 * {@code moduleRecord.settings} to configure the module.</li>
 * <li>{@link #onLoad(IBoxContext, ModuleRecord)} — called during activation.</li>
 * <li>{@link #onUnload(IBoxContext, ModuleRecord)} — called during deactivation.</li>
 * <li>{@link #main(IBoxContext, String[])} — called when the module is executed from the CLI.</li>
 * </ul>
 *
 * <p>
 * <strong>Interceptor support</strong> — to subscribe to BoxLang events, also implement
 * {@link IInterceptor} on your class and annotate listener methods with
 * {@link ortus.boxlang.runtime.events.InterceptionPoint}. The runtime will automatically
 * register your implementation as an interceptor during module activation when it detects
 * that the class also implements {@link IInterceptor}.
 * </p>
 *
 * <p>
 * When both a Java {@code IModuleConfig} and a {@code ModuleConfig.bx} exist in the same module
 * directory, the Java implementation takes priority and the BX file is ignored.
 * </p>
 */
public interface IModuleConfig {

	/**
	 * Called during module registration. Use this method to configure the module:
	 * populate {@code moduleRecord.settings}, register custom interception points, etc.
	 *
	 * @param context      The current execution context
	 * @param moduleRecord The module record being registered; mutate its {@code settings} field directly
	 */
	default void configure( IBoxContext context, ModuleRecord moduleRecord ) {
	}

	/**
	 * Called during module activation, after all interceptors have been registered.
	 *
	 * @param context      The current execution context
	 * @param moduleRecord The fully-registered module record
	 */
	default void onLoad( IBoxContext context, ModuleRecord moduleRecord ) {
	}

	/**
	 * Called during module deactivation, before resources are released.
	 *
	 * @param context      The current execution context
	 * @param moduleRecord The module record being unloaded
	 */
	default void onUnload( IBoxContext context, ModuleRecord moduleRecord ) {
	}

	/**
	 * Called when the module is executed directly from the BoxLang CLI.
	 *
	 * @param context The current execution context
	 * @param args    Command-line arguments passed to the module
	 */
	default void main( IBoxContext context, String[] args ) {
	}

	/**
	 * Registers this config as an interceptor. If the implementation also implements
	 * {@link IInterceptor}, the default delegates to
	 * {@link InterceptorService#register(IInterceptor, IStruct)}, which discovers
	 * {@link ortus.boxlang.runtime.events.InterceptionPoint}-annotated Java methods via
	 * reflection. BX-based implementations (see {@link BxModuleConfig}) override this to use
	 * the {@link ortus.boxlang.runtime.runnables.IClassRunnable} registration path instead,
	 * which reads BoxLang metadata.
	 *
	 * @param interceptorService The runtime interceptor service
	 * @param settings           The module settings struct passed to the interceptor
	 */
	default void registerInterceptor( InterceptorService interceptorService, IStruct settings ) {
		if ( this instanceof IInterceptor interceptor ) {
			interceptorService.register( interceptor, settings );
		}
	}

	/**
	 * Unregisters this config from the interceptor service. If the implementation also
	 * implements {@link IInterceptor}, the default delegates to
	 * {@link InterceptorService#unregister(IInterceptor)}; BX-based implementations
	 * override to use the {@link ortus.boxlang.runtime.interop.DynamicObject} path instead.
	 *
	 * @param interceptorService The runtime interceptor service
	 */
	default void unregisterInterceptor( InterceptorService interceptorService ) {
		if ( this instanceof IInterceptor interceptor ) {
			interceptorService.unregister( interceptor );
		}
	}

	/**
	 * Convenience constant for an empty dependencies array, used as the default field value.
	 */
	Array EMPTY_DEPENDENCIES = new Array();

}
