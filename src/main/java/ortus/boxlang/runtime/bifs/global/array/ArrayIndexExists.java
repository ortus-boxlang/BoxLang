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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxBIF( alias = "ArrayIsDefined" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "isDefined" )
public class ArrayIndexExists extends BIF {

	/**
	 * Constructor
	 */
	public ArrayIndexExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( true, "any", Key.index )
		};
	}

	/**
	 * Returns whether there exists an item in the array at the selected index.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be searched.
	 *
	 * @argument.index The index to check.
	 */
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		int		index		= IntegerCaster.cast( arguments.get( Key.index ) ) - 1;

		return index >= 0 && index < actualArray.size() && actualArray.get( index ) != null;
	}

}
