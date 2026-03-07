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
package ortus.boxlang.runtime.dynamic;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Runtime helper for literal spread support in array and struct literals.
 */
public class LiteralSpreadUtil {

	public static SpreadValue spread( Object value ) {
		return new SpreadValue( value );
	}

	public static Array array( Object... values ) {
		Array result = new Array();
		for ( Object value : values ) {
			if ( value instanceof SpreadValue spreadValue ) {
				appendArraySpread( result, spreadValue.getValue() );
			} else {
				result.add( value );
			}
		}
		return result;
	}

	public static IStruct struct( IStruct.TYPES type, Object... values ) {
		IStruct result = new Struct( type );
		for ( int i = 0; i < values.length; ) {
			Object current = values[ i ];
			if ( current instanceof SpreadValue spreadValue ) {
				appendStructSpread( result, spreadValue.getValue() );
				i++;
				continue;
			}

			if ( i + 1 >= values.length ) {
				throw new BoxRuntimeException( "Invalid struct literal data while processing spread values." );
			}
			result.put( KeyCaster.cast( current ), values[ i + 1 ] );
			i += 2;
		}
		return result;
	}

	private static void appendArraySpread( Array target, Object spreadValue ) {
		CastAttempt<Array> casted = ArrayCaster.attempt( spreadValue );
		if ( !casted.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    "Cannot spread value of type [" + describeType( spreadValue ) + "] into an array literal." );
		}
		target.addAll( casted.get() );
	}

	private static void appendStructSpread( IStruct target, Object spreadValue ) {
		if ( spreadValue instanceof IStruct spreadStruct ) {
			spreadStruct.forEach( target::put );
			return;
		}

		CastAttempt<Array> casted = ArrayCaster.attempt( spreadValue );
		if ( !casted.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    "Cannot spread value of type [" + describeType( spreadValue ) + "] into a struct literal." );
		}

		Array spreadArray = casted.get();
		for ( int i = 1; i <= spreadArray.size(); i++ ) {
			target.put( Key.of( i ), spreadArray.getAt( i ) );
		}
	}

	private static String describeType( Object value ) {
		return value == null ? "null" : value.getClass().getName();
	}

	public static final class SpreadValue {

		private final Object value;

		private SpreadValue( Object value ) {
			this.value = value;
		}

		public Object getValue() {
			return value;
		}
	}
}
