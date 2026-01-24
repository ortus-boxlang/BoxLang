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
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@BoxBIF( description = "Transposes the arrays rows into columns and columns into rows" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayTranspose extends BIF {

	/**
	 * Constructor
	 */
	public ArrayTranspose() {
		super();
		declaredArguments = new Argument[] {};
	}

	/**
	 * Returns a transposed array based on all passed in arrays.
	 *
	 * <pre>
	 * arrayTranspose(
	 *     [ 1, 2, 3 ],
	 *     [ 4, 5, 6 ],
	 *     [ 7, 8, 9 ]
	 * );
	 * // [ [ 1, 4, 7 ], [ 2, 5, 8 ], [ 3, 6, 9 ] ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Collection<Object>	args				= arguments.entrySet().stream()
		    .filter( entry -> entry.getKey() instanceof IntKey )
		    .map( entry -> entry.getValue() )
		    .collect( Collectors.toList() );
		List<Array>			arraysToTranspose	= new LinkedList<>();
		for ( Object o : args ) {
			Objects.requireNonNull( o );
			CastAttempt<Array> attempt = ArrayCaster.attempt( o );
			if ( !attempt.isValid() ) {
				throw new BoxRuntimeException( "All arguments passed in to arrayTranspose must be arrays" );
			}
			arraysToTranspose.add( attempt.get() );
		}

		if ( arraysToTranspose.size() <= 0 ) {
			return Array.of();
		}

		List<Integer> arrayLengths = arraysToTranspose.stream().map( Array::size ).collect( Collectors.toList() );

		// if the lengths are not all the same
		if ( arrayLengths.stream().distinct().limit( 2 ).count() > 1 ) {
			throw new BoxRuntimeException(
			    "All arrays passed in to arrayTranspose must be the same length." +
			        IntStream.range( 0, arrayLengths.size() )
			            .mapToObj( i -> "array" + ( i + 1 ) + ": [" + arrayLengths.get( i ) + "]" )
			            .collect( Collectors.joining( "; " ) )
			);
		}

		if ( arrayLengths.get( 0 ) <= 0 ) {
			return Array.of();
		}

		Array result = Array.of();
		for ( int i = 0; i < arrayLengths.get( 0 ); i++ ) {
			Array row = Array.of();
			for ( int j = 0; j < arraysToTranspose.size(); j++ ) {
				row.add( arraysToTranspose.get( j ).get( i ) );
			}
			result.add( row );
		}
		return result;
	}
}
