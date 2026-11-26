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
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Convert an array to a delimited list" )
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.ARRAY, name = "join" )
public class ArrayToList extends BIF {

	/**
	 * Constructor
	 */
	public ArrayToList() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array ),
		    new Argument( false, "String", Key.delimiter, ListUtil.DEFAULT_DELIMITER ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * Converts an array to a delimited list string by joining the elements with the delimiter.
	 * Also available as the <code>toList()</code> and <code>join()</code> member functions on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to join together.
	 *
	 * @argument.delimiter The delimiter to put between the elements. Defaults to a comma.
	 *
	 * @argument.initialValue Declared but not used by this function.
	 *
	 * @return The array elements joined into a single string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array actualArray = ArrayCaster.cast( arguments.get( Key.array ) );

		return ListUtil.asString( actualArray, arguments.getAsString( Key.delimiter ) );

	}
}
