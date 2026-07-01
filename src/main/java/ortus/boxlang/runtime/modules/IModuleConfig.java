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
 * The runtime automatically registers the implementation as an {@link IInterceptor}. Annotate
 * listener methods with {@link ortus.boxlang.runtime.events.InterceptionPoint} to subscribe to
 * BoxLang events.
 * </p>
 *
 * <p>
 * When both a Java {@code IModuleConfig} and a {@code ModuleConfig.bx} exist in the same module
 * directory, the Java implementation takes priority and the BX file is ignored.
 * </p>
 */
public interface IModuleConfig extends IInterceptor {

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
	 * Satisfies the {@link IInterceptor} contract. The runtime passes the module's settings
	 * struct when registering the Java config as an interceptor; override if you need to
	 * react to those properties.
	 *
	 * @param properties The module settings struct
	 */
	@Override
	default void configure( IStruct properties ) {
	}

	/**
	 * Convenience constant for an empty dependencies array, used as the default field value.
	 */
	Array EMPTY_DEPENDENCIES = new Array();

}
