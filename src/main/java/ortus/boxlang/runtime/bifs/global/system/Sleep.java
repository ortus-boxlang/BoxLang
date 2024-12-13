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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class Sleep extends BIF {

	/**
	 * Constructor
	 */
	public Sleep() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "long", Key.duration ),
		};
	}

	/**
	 * Sleeps the current thread for the specified duration in millisecons
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.duration The amount of time, in milliseconds to sleep the thread
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Long duration = arguments.getAsLong( Key.duration );
		try {
			Thread.sleep( duration );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException(
			    "The sleep thread was interrupted",
			    "InterruptedException",
			    e
			);
		}
		return null;
	}

}
