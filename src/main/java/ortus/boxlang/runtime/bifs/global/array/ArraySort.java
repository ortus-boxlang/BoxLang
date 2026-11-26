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

import java.util.Locale;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.util.ListUtil;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Sort the elements of an array" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArraySort extends BIF {

	/**
	 * Constructor
	 */
	public ArraySort() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "assignablearray", Key.array ),
		    new Argument( false, "any", Key.sortType, "textnocase" ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "boolean", Key.localeSensitive, false ),
		    new Argument( false, "function:Comparator", Key.callback )
		};
	}

	/**
	 * Sorts the elements of an array in place and returns the same array.
	 * The sort can be controlled by a type and order, or by a custom comparator function passed either as the callback argument or as the
	 * sortType argument.
	 * The comparator receives two elements and returns a negative number, zero, or a positive number. When a comparator is used, sortOrder is
	 * ignored.
	 * Also available as the <code>sort()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to sort.
	 *
	 * @argument.sortType The type of sort: "text" (case sensitive), "numeric", or "textnocase" (default). A comparator function may also be
	 *                    passed here.
	 *
	 * @argument.sortOrder The sort order: "asc" (default) or "desc". Any other value throws an exception.
	 *
	 * @argument.localeSensitive If true, text sorting uses the locale collation rules of the current context. Defaults to false.
	 *
	 * @argument.callback A comparator function used to sort the array. It receives two elements and returns a negative number, zero, or a
	 *                    positive number.
	 *
	 * @return The same array, sorted.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		array		= arguments.getAsArray( Key.array );
		Function	callback	= arguments.getAsFunction( Key.callback );
		Object		sortType	= arguments.get( Key.sortType );
		String		sortOrder	= arguments.getAsString( Key.sortOrder );

		if ( sortType != null ) {
			CastAttempt<Function> funcAttempt = FunctionCaster.attempt( sortType, "Comparator" );
			if ( funcAttempt.wasSuccessful() ) {
				callback = funcAttempt.get();
			}
		}

		Array result = null;

		if ( callback != null ) {
			result = ListUtil.sort(
			    array,
			    callback,
			    context
			);
		} else {
			Locale requestedLocale = null;
			if ( arguments.getAsBoolean( Key.localeSensitive ) ) {
				requestedLocale = LocalizationUtil.parseLocaleFromContext( context, arguments );
			}

			result = ListUtil.sort(
			    array,
			    StringCaster.cast( sortType ),
			    sortOrder,
			    requestedLocale
			);
		}

		return result;
	}

}
