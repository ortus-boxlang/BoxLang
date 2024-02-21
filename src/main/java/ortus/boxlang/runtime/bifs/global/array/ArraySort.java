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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.ListUtil;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySort extends BIF {

	/**
	 * Constructor
	 */
	public ArraySort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiablearray", Key.array ),
		    new Argument( false, "any", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "boolean", Key.localeSensitive ),
		    new Argument( false, "any", Key.callback )
		};
	}

	/**
	 * Sorts array elements.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to sort
	 *
	 * @argument.sortType Options are text, numeric, or textnocase
	 *
	 * @argument.sortOrder Options are asc or desc
	 *
	 * @argument.localeSensitive Sort based on local rules
	 *
	 * @argument.callback Function to sort by
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		array		= arguments.getAsArray( Key.array );
		Function	callback	= arguments.getAsFunction( Key.callback );
		Object		sortType	= arguments.get( Key.sortType );
		String		sortOrder	= arguments.getAsString( Key.sortOrder );

		if ( sortType != null && sortType instanceof Function sortFunc ) {
			callback = ( Function ) sortType;
		}

		Array result = null;

		if ( callback != null ) {
			result = ListUtil.sort(
			    array,
			    callback,
			    context
			);
		} else {
			result = ListUtil.sort(
			    array,
			    StringCaster.cast( sortType ),
			    sortOrder,
			    LocalizationUtil.parseLocaleFromContext( context, arguments )
			);
		}

		// TODO: This behavior difference between the member and the BIF is stupid. Let's deprecate the boolean return or just fahgeddaboutit
		return arguments.getAsBoolean( __isMemberExecution )
		    ? result
		    : true;

	}

}
