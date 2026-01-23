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
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

import java.util.function.BiFunction;

@BoxBIF( description = "Groups an array into a struct with the categories defined by the return values of the predicate function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayGroupBy extends BIF {

	/**
	 * Constructor
	 */
	public ArrayGroupBy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, Argument.FUNCTION, Key.callback )
		};
	}

	/**
	 * Returns a struct of keys returned from the predicate function and values of arrays of matching rows.
	 *
	 * <pre>
	 * values = [ 1, 2, 3, 4 ];
	 * values.groupBy( ( value ) => value % 2 ? "odd" : "even" );
	 * // { odd: [ 1, 3 ], even: [ 2, 4 ] }
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to group.
	 * 
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the value, the index, the array. You can alternatively pass a Java Predicate which will only receive the 1st arg.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array								array		= arguments.getAsArray( Key.array );
		Function							callback	= arguments.getAsFunction( Key.callback );

		BiFunction<Object, Integer, Object>	reduction;
		if ( callback.requiresStrictArguments() ) {
			reduction = ( acc, idx ) -> {
				Key		group	= Key.of( context.invokeFunction( callback,
				    new Object[] { array.size() > idx ? array.get( idx ) : null } ) );
				IStruct	s		= ( IStruct ) acc;
				if ( ! ( ( IStruct ) acc ).containsKey( group ) ) {
					s.put( group, Array.of() );
				}
				( ( Array ) s.get( group ) ).add( array.get( idx ) );
				return s;
			};
		} else {
			reduction = ( acc, idx ) -> {
				Key		group	= Key.of( context.invokeFunction( callback,
				    new Object[] { array.size() > idx ? array.get( idx ) : null, idx + 1, array } ) );
				IStruct	s		= ( IStruct ) acc;
				if ( ! ( ( IStruct ) acc ).containsKey( group ) ) {
					s.put( group, Array.of() );
				}
				( ( Array ) s.get( group ) ).add( array.get( idx ) );
				return s;
			};
		}

		return array.intStream()
		    .boxed()
		    .reduce(
		        Struct.of(),
		        reduction,
		        ( acc, intermediate ) -> acc );
	}

}
