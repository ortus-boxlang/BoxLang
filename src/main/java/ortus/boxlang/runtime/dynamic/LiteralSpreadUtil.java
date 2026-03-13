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

	private enum AmbiguousSpreadType {
		ARRAY,
		STRUCT
	}

	/**
	 * spread.
	 */
	public static SpreadValue spread( Object value ) {
		return new SpreadValue( value );
	}

	/**
	 * array.
	 */
	public static Array array( Object... values ) {
		values = normalizeVarargs( values );
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

	/**
	 * Resolve ambiguous spread-only bracket literals such as <code>[ ...value ]</code>.
	 * <p>
	 * If all spread sources are structs, this returns an ordered struct.
	 * If all spread sources are arrays (or array-castable), this returns an array.
	 * Mixing array and struct spread sources is rejected.
	 */
	public static Object arrayOrOrderedStruct( Object... values ) {
		values = normalizeVarargs( values );
		if ( values.length == 0 ) {
			return array( values );
		}

		AmbiguousSpreadType spreadType = null;
		for ( Object value : values ) {
			if ( ! ( value instanceof SpreadValue spreadValue ) ) {
				return array( values );
			}

			AmbiguousSpreadType valueType = detectAmbiguousSpreadType( spreadValue.getValue() );
			if ( spreadType == null ) {
				spreadType = valueType;
			} else if ( spreadType != valueType ) {
				throw new BoxRuntimeException(
				    "Cannot mix array and struct spread values in an ambiguous bracket literal. Use explicit keyed struct member syntax to force an ordered struct literal." );
			}
		}

		if ( spreadType == AmbiguousSpreadType.STRUCT ) {
			IStruct result = new Struct( IStruct.TYPES.LINKED );
			for ( Object value : values ) {
				appendStructSpread( result, ( ( SpreadValue ) value ).getValue() );
			}
			return result;
		}

		return array( values );
	}

	/**
	 * struct.
	 */
	public static IStruct struct( IStruct.TYPES type, Object... values ) {
		values = normalizeVarargs( values );
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

	/**
	 * appendArraySpread.
	 */
	private static void appendArraySpread( Array target, Object spreadValue ) {
		CastAttempt<Array> casted = ArrayCaster.attempt( spreadValue );
		if ( !casted.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    "Cannot spread value of type [" + describeType( spreadValue ) + "] into an array literal." );
		}
		target.addAll( casted.get() );
	}

	/**
	 * appendStructSpread.
	 */
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

	/**
	 * detectAmbiguousSpreadType.
	 */
	private static AmbiguousSpreadType detectAmbiguousSpreadType( Object spreadValue ) {
		if ( spreadValue instanceof IStruct ) {
			return AmbiguousSpreadType.STRUCT;
		}

		CastAttempt<Array> casted = ArrayCaster.attempt( spreadValue );
		if ( casted.wasSuccessful() ) {
			return AmbiguousSpreadType.ARRAY;
		}

		throw new BoxRuntimeException(
		    "Cannot spread value of type [" + describeType( spreadValue ) + "] into an ambiguous bracket literal." );
	}

	/**
	 * describeType.
	 */
	private static String describeType( Object value ) {
		return value == null ? "null" : value.getClass().getName();
	}

	/**
	 * Java varargs calls like {@code fn(null)} can arrive as a null varargs array.
	 * Treat that shape as one explicit null argument instead of crashing.
	 */
	private static Object[] normalizeVarargs( Object[] values ) {
		return values == null ? new Object[] { null } : values;
	}

	public static final class SpreadValue {

		private final Object value;

		private SpreadValue( Object value ) {
			this.value = value;
		}

		/**
		 * @return wrapped spread value.
		 */
		public Object getValue() {
			return value;
		}
	}
}
