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
package ortus.boxlang.runtime.runnables;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

public abstract class BoxScript implements IScriptRunnable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The source to the script
	 */
	protected String source;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invoke a BoxLang source
	 *
	 * @param context The context to invoke the source with
	 *
	 */
	public Object invoke( IBoxContext context ) {
		BoxRuntime runtime = BoxRuntime.getInstance();

		// Announcements
		runtime.announce(
		    BoxEvent.ON_PRE_SOURCE_INVOKE,
		    () -> Struct.ofNonConcurrent(
		        Key.context, context,
		        Key.source, this
		    )
		);
		try {
			return _invoke( context );
		} finally {
			runtime.announce(
			    BoxEvent.ON_POST_SOURCE_INVOKE,
			    () -> Struct.ofNonConcurrent(
			        Key.context, context,
			        Key.source, this
			    )
			);
		}

	}

	/**
	 * This method is called by the invoke method, it is the actual implementation of the source
	 *
	 * @param context The context to invoke the source with
	 *
	 */
	public abstract Object _invoke( IBoxContext context );

	// ISourceRunnable implementation methods

	/**
	 * The source to the script
	 */
	public String getRunnableSource() {
		return this.source;
	}

}
