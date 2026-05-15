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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.ArgumentsScope;

@BoxBIF( description = "Clear all the compiled classes from the Boxpiler's page pool and resolutions" )
public class PagePoolClear extends BIF {

	/**
	 * Constructor
	 */
	public PagePoolClear() {
		super();
	}

	/**
	 * Clears all the compiled classes from the Boxpiler's page pool and resolutions
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		clear();
		return true;
	}

	/**
	 * This method is used to clear the Boxpiler's page pool and resolutions, as well as the DynamicInteropService's method handle cache. It can be called from the BIF or from other parts of the runtime when needed.
	 * This is useful for scenarios where you want to free up memory or reset the state of the Boxpiler, such as during development or when dynamically loading/unloading code.
	 */
	public static void clear() {
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
	}
}
