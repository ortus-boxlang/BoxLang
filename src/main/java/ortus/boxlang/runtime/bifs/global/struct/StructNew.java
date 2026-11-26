
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

package ortus.boxlang.runtime.bifs.global.struct;

import java.util.Comparator;
import java.util.HashMap;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.FunctionCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.StructUtil;
import ortus.boxlang.runtime.util.LocalizationUtil;

@BoxBIF( description = "Create a new struct" )

public class StructNew extends BIF {

	/**
	 * Constructor
	 */
	public StructNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.type, "default" ),
		    new Argument( false, "any", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "any", Key.localeSensitive, false ),
		    new Argument( false, "function:Comparator", Key.callback ),
		};
	}

	/**
	 * Creates a new, empty struct of the specified type. The available types are:
	 * <ul>
	 * <li>default - an unordered, case-insensitive struct</li>
	 * <li>casesensitive - an unordered, case-sensitive struct</li>
	 * <li>ordered - maintains insertion order (linked), case-insensitive</li>
	 * <li>ordered-casesensitive - maintains insertion order, case-sensitive</li>
	 * <li>soft - values are softly referenced</li>
	 * <li>weak - values are weakly referenced</li>
	 * <li>sorted - a sorted struct</li>
	 * </ul>
	 * The enum names of the struct types (e.g. LINKED) are also accepted. If a sortType (or a comparator callback) is provided, a sorted struct is
	 * created regardless of the type. The sortType must be one of text, textNoCase or numeric, combined with the sortOrder (asc or desc).
	 * Using a noCase sortType with an ordered-casesensitive type throws a BoxRuntimeException. An unknown type also throws a BoxRuntimeException.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The struct type. Defaults to default.
	 *
	 * @argument.sortType An optional sort type: text, textNoCase or numeric. May also be a comparator function, in which case it is treated as the callback.
	 *
	 * @argument.sortOrder The sort order applicable to the sortType argument: asc (default) or desc.
	 *
	 * @argument.localeSensitive Whether text sorting should use the locale rules of the current context. Defaults to false.
	 *
	 * @argument.callback An optional comparator function (or Java Comparator) used to sort the keys. It receives two key names and must return an integer.
	 *
	 * @return A new, empty struct.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {

		IStruct.TYPES			type						= IStruct.TYPES.fromString( arguments.getAsString( Key.type ) );
		CastAttempt<Function>	typeCastToFunctionAttempt	= FunctionCaster.attempt( arguments.get( Key.sortType ), "Comparator" );
		if ( typeCastToFunctionAttempt.wasSuccessful() ) {
			arguments.put( Key.callback, typeCastToFunctionAttempt.get() );
			arguments.put( Key.sortType, null );
		}

		Comparator<Key>					comparator			= null;

		String							sort				= arguments.getAsString( Key.sortType );

		HashMap<Key, Comparator<Key>>	commonComparators	= arguments.getAsBoolean( Key.localeSensitive )
		    ? StructUtil.getCommonKeyComparators( LocalizationUtil.parseLocaleFromContext( context, arguments ) )
		    : StructUtil.getCommonKeyComparators();

		// With Sorting
		if ( sort != null ) {
			Key sortKey = Key.of( sort + arguments.getAsString( Key.sortOrder ) );

			if ( sort.toLowerCase().contains( "nocase" ) && type.equals( IStruct.TYPES.LINKED_CASE_SENSITIVE ) ) {
				throw new BoxRuntimeException(
				    String.format( "Invalid sort type [%s]. A case-sensitive struct can not be ordered without case consideration.", sort )
				);
			}
			type		= IStruct.TYPES.SORTED;
			comparator	= commonComparators.get( sortKey );
		}
		// With Comparator
		else if ( arguments.getAsFunction( Key.callback ) != null ) {
			// TODO: This doesn't quite match ACF's method signature.
			// We will need to investigates other types of maps that can use a `K,V,KV` BiFunction as a comparator
			type		= IStruct.TYPES.SORTED;
			comparator	= ( a, b ) -> ( Integer ) context.invokeFunction(
			    arguments.getAsFunction( Key.callback ),
			    new Object[] { a.getName(), b.getName() }
			);
		}

		return comparator == null
		    ? new Struct( type )
		    : new Struct( comparator );
	}

}
