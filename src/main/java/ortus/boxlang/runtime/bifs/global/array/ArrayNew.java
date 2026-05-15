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
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

@BoxBIF( description = "Create a new array" )
public class ArrayNew extends BIF {

	/**
	 * Constructor
	 */
	public ArrayNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "integer", Key.dimensions, 1 ),
		    new Argument( false, "boolean", Key.isSynchronized, true )
		};
	}

	/**
	 * Create a new array.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.dimension The dimension of the array to create (currently only 1 is supported).
	 *
	 * @argument.isSynchronized Whether the array should be thread-safe (synchronized). Default is false.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new Array(
		    arguments.getAsBoolean( Key.isSynchronized ),
		    arguments.getAsInteger( Key.dimensions )
		);
	}

}
