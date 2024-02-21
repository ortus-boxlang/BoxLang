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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayMap extends BIF {

	/**
	 * Constructor
	 */
	public ArrayMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * Iterates over every entry of the array and calls the closure function to work on the element of the array. The returned value will be set at the
	 * same index in a new array and the new array will be returned
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to reduce
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the current item, and the
	 *                    current index, and the original array. The function should return the value that will be set at the same index in the new array.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.map(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsFunction( Key.callback ),
		    context, arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads )
		);
	}
}
