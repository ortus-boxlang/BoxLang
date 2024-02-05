
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
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class StructNew extends BIF {

	private static final HashMap<Key, IStruct.TYPES>	typeMap		= new HashMap<Key, IStruct.TYPES>() {

																		{
																			put( Key.of( "default" ), IStruct.TYPES.DEFAULT );
																			put( Key.of( "ordered" ), IStruct.TYPES.LINKED );
																			put( Key.of( "sorted" ), IStruct.TYPES.SORTED );
																			put( Key.of( "soft" ), IStruct.TYPES.SOFT );
																			put( Key.of( "weak" ), IStruct.TYPES.WEAK );
																			put( Key.of( "casesensitive" ), IStruct.TYPES.CASE_SENSITIVE );
																			put( Key.of( "ordered-casesensitive" ), IStruct.TYPES.LINKED_CASE_SENSITIVE );
																		}
																	};

	private static final HashMap<Key, Comparator<Key>>	comparators	= new HashMap<Key, Comparator<Key>>() {

																		{
																			put( Key.of( "textAsc" ), ( a, b ) -> Compare.invoke( a, b, false ) );
																			put( Key.of( "textDesc" ), ( b, a ) -> Compare.invoke( a, b, false ) );
																			put( Key.of( "numericAsc" ), ( a, b ) -> Compare
																			    .invoke( DoubleCaster.cast( a.getOriginalValue() ),
																			        DoubleCaster.cast( b.getOriginalValue() ) ) );
																			put( Key.of( "numericDesc" ), ( b, a ) -> Compare
																			    .invoke( DoubleCaster.cast( a.getOriginalValue() ),
																			        DoubleCaster.cast( b.getOriginalValue() ) ) );
																		}
																	};

	/**
	 * Constructor
	 */
	public StructNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.type, "default" ),
		    new Argument( false, "string", Key.sortType ),
		    new Argument( false, "string", Key.sortOrder, "asc" ),
		    new Argument( false, "function", Key.callback )
			// TODO: Investigate and implement localeSensitive argument - there's no documentation for ACF on what this does, currently
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key typeKey = Key.of( arguments.getAsString( Key.type ) );
		if ( !typeMap.containsKey( typeKey ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "Could not create a struct with a type of [%s] as it is not a known type.",
			        arguments.getAsString( Key.type )
			    )
			);
		}

		Comparator<Key>	comparator	= null;
		String			sort		= arguments.getAsString( Key.sortType );
		if ( sort != null ) {
			typeKey = Key.of( "sorted" );
			Key sortKey = Key.of( sort + arguments.getAsString( Key.sortOrder ) );
			comparator = comparators.get( sortKey );
		} else if ( arguments.getAsFunction( Key.callback ) != null ) {
			// TODO: This doesn't quite match ACF's method signature.
			// We will need to investigates other types of maps that can use a `K,V,KV` BiFunction as a comparator
			typeKey		= Key.of( "sorted" );
			comparator	= ( a, b ) -> ( Integer ) context.invokeFunction(
			    arguments.getAsFunction( Key.callback ),
			    new Object[] { a, b }
			);
		}

		return comparator == null
		    ? new Struct( typeMap.get( typeKey ) )
		    : new Struct( comparator );
	}

}
