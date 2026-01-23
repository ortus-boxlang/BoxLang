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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Flattens nested arrays to the specified depth. (Infinite if no depth is specified.)" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFlatten extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFlatten() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( false, Argument.INTEGER, Key.depth )
		};
	}

	/**
	 * Flattens nested arrays to the specified depth. When depth is omitted, the array is flattened completely.
	 *
	 * <pre>
	 * nested = [ 1, [ 2, [ 3 ] ] ];
	 * nested.flatten(); // [ 1, 2, 3 ]
	 * nested.flatten( 1 ); // [ 1, 2, [ 3 ] ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to flatten.
	 *
	 * @argument.depth The depth to flatten. If omitted, flatten all nested arrays.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return ListUtil.flatten(
		    arguments.getAsArray( Key.array ),
		    arguments.getAsInteger( Key.depth )
		);
	}
}
