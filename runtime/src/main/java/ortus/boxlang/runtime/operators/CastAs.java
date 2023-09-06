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
package ortus.boxlang.runtime.operators;

import java.math.BigDecimal;
import java.util.List;

import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.ByteCaster;
import ortus.boxlang.runtime.dynamic.casters.CharacterCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.FloatCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.ShortCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;

/**
 * Casts input to Java type
 *
 */
public class CastAs implements IOperator {

	/**
	 * @param left  The object to cast
	 * @param right The type to cast to
	 *
	 * @return The result
	 */
	public static Object invoke( Object left, Object right ) {
		String type = StringCaster.cast( right ).toLowerCase();

		if ( type.equals( "null" ) ) {
			return null;
		}

		// Handle arrays like int[]
		if ( type.endsWith( "[]" ) ) {

			Object[] incomingList;

			if ( left.getClass().isArray() ) {
				incomingList = ( Object[] ) left;
			} else if ( left instanceof List ) {
				incomingList = ( ( List<?> ) left ).toArray();
			} else {
				throw new RuntimeException(
				    String.format( "You asked for type %s, but input %s cannot be cast to an array.", type,
				        left.getClass().getName() )
				);
			}

			String		newType	= type.substring( 0, type.length() - 2 );
			Object[]	result	= ( Object[] ) java.lang.reflect.Array.newInstance( getClassFromType( newType ),
			    incomingList.length );

			for ( int i = incomingList.length - 1; i >= 0; i-- ) {
				result[ i ] = CastAs.invoke( incomingList[ i ], newType );
			}
			return result;

		}

		if ( type.equals( "string" ) ) {
			return StringCaster.cast( left );
		}
		if ( type.equals( "double" ) ) {
			return DoubleCaster.cast( left );
		}
		if ( type.equals( "boolean" ) ) {
			return BooleanCaster.cast( left );
		}
		if ( type.equals( "bigdecimal" ) ) {
			return BigDecimalCaster.cast( left );
		}
		if ( type.equals( "char" ) ) {
			return CharacterCaster.cast( left );
		}
		if ( type.equals( "byte" ) ) {
			return ByteCaster.cast( left );
		}
		if ( type.equals( "int" ) ) {
			return IntegerCaster.cast( left );
		}
		if ( type.equals( "long" ) ) {
			return LongCaster.cast( left );
		}
		if ( type.equals( "short" ) ) {
			return ShortCaster.cast( left );
		}
		if ( type.equals( "float" ) ) {
			return FloatCaster.cast( left );
		}

		throw new RuntimeException(
		    String.format( "Invalid cast type [%s]", type )
		);
	}

	public static Class<?> getClassFromType( String type ) {

		if ( type.equals( "string" ) ) {
			return String.class;
		}
		if ( type.equals( "double" ) ) {
			return Double.class;
		}
		if ( type.equals( "boolean" ) ) {
			return Boolean.class;
		}
		if ( type.equals( "bigdecimal" ) ) {
			return BigDecimal.class;
		}
		if ( type.equals( "char" ) ) {
			return Character.class;
		}
		if ( type.equals( "byte" ) ) {
			return Byte.class;
		}
		if ( type.equals( "int" ) ) {
			return Integer.class;
		}
		if ( type.equals( "long" ) ) {
			return Long.class;
		}
		if ( type.equals( "short" ) ) {
			return Short.class;
		}
		if ( type.equals( "float" ) ) {
			return Float.class;
		}
		throw new RuntimeException(
		    String.format( "Invalid cast type [%s]", type )
		);
	}
}
