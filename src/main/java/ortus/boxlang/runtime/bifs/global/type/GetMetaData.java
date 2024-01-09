/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.type;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
public class GetMetaData extends BIF {

	/**
	 * Constructor
	 */
	public GetMetaData() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object )
		};
	}

	/**
	 * Gets metadata (the methods, properties, and parameters of a component) associated with an object.
	 * This only exists for backwards compat with Adobe and Lucee and this BIF should be moved to a compat module
	 * at a later date. In BoxLang, use the obj.$bx.meta object instead.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.object The object to get metadata for.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object object = arguments.get( Key.object );

		// Functions have a legacy metadata view that matches CF engines
		if ( object instanceof Function fun ) {
			return fun.getMetaData();
		}

		// Classes have a legacy metadata view that matches CF engines
		if ( object instanceof IClassRunnable cfc ) {
			return cfc.getMetaData();
		}

		// TODO: add any other custom types that CF engines return a specific metadata for.

		// All other types return the class of the object to match CF engines
		return object.getClass();
	}

}
