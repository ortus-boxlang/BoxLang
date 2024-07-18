
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

@BoxBIF

public class StructNew extends BIF {

	private static final HashMap<Key, IStruct.TYPES> typeMap = new HashMap<>();

	// Available types
	static {
		typeMap.put( Key.of( "casesensitive" ), IStruct.TYPES.CASE_SENSITIVE );
		typeMap.put( Key.of( "default" ), IStruct.TYPES.DEFAULT );
		typeMap.put( Key.of( "ordered-casesensitive" ), IStruct.TYPES.LINKED_CASE_SENSITIVE );
		typeMap.put( Key.of( "ordered" ), IStruct.TYPES.LINKED );
		typeMap.put( Key.of( "soft" ), IStruct.TYPES.SOFT );
		typeMap.put( Key.of( "sorted" ), IStruct.TYPES.SORTED );
		typeMap.put( Key.of( "weak" ), IStruct.TYPES.WEAK );
	}

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
	 * Creates a new struct of the specified type.
	 * The available types are:
	 * - casesensitive
	 * - default
	 * - ordered-casesensitive
	 * - ordered
	 * - soft
	 * - sorted
	 * - weak
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.type The struct type
	 *
	 * @argument.sortType An optional sort type to apply to that type
	 *
	 * @argument.sortOrder The sort order applicable to the sortType argument
	 *
	 * @argument.callback An optional callback to use as the sorting function. You can alternatively pass a Java Comparator.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {

		// Validate Types
		Key typeKey = Key.of( arguments.getAsString( Key.type ) );
		if ( !typeMap.containsKey( typeKey ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "Could not create a struct with a type of [%s] as it is not a known type.",
			        arguments.getAsString( Key.type )
			    )
			);
		}
		CastAttempt<Function> typeCastToFunctionAttempt = FunctionCaster.attempt( arguments.get( Key.sortType ), "Comparator" );
		if ( typeCastToFunctionAttempt.wasSuccessful() ) {
			arguments.put( Key.callback, typeCastToFunctionAttempt.get() );
			arguments.put( Key.sortType, null );
		}

		Comparator<Key>					comparator			= null;

		String							sort				= arguments.getAsString( Key.sortType );

		HashMap<Key, Comparator<Key>>	commonComparators	= arguments.getAsBoolean( Key.localeSensitive )
		    ? StructUtil.getCommonComparators( LocalizationUtil.parseLocaleFromContext( context, arguments ) )
		    : StructUtil.getCommonComparators();

		// With Sorting
		if ( sort != null ) {
			Key initialType = Key.of( arguments.getAsString( Key.type ) );
			typeKey = Key.of( "sorted" );
			Key sortKey = Key.of( sort + arguments.getAsString( Key.sortOrder ) );

			if ( sort.toLowerCase().contains( "nocase" ) && typeMap.get( initialType ).equals( IStruct.TYPES.LINKED_CASE_SENSITIVE ) ) {
				throw new BoxRuntimeException(
				    String.format( "Invalid sort type [%s]. A case-sensitive struct can not be ordered without case consideration.", sort )
				);
			}
			comparator = commonComparators.get( sortKey );
		}
		// With Comparator
		else if ( arguments.getAsFunction( Key.callback ) != null ) {
			// TODO: This doesn't quite match ACF's method signature.
			// We will need to investigates other types of maps that can use a `K,V,KV` BiFunction as a comparator
			typeKey		= Key.of( "sorted" );
			comparator	= ( a, b ) -> ( Integer ) context.invokeFunction(
			    arguments.getAsFunction( Key.callback ),
			    new Object[] { a.getName(), b.getName() }
			);
		}

		return comparator == null
		    ? new Struct( typeMap.get( typeKey ) )
		    : new Struct( comparator );
	}

}
