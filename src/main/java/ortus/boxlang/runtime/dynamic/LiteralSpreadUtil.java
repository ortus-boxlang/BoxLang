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

import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.KeyCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
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
	 * Build a {@link ortus.boxlang.runtime.types.BoxSet} from a literal of the form
	 * {@code set{...}}. Always creates a default (hash-backed) set.
	 *
	 * <p>
	 * Spread expressions inside the literal are expanded element-by-element via
	 * {@link ortus.boxlang.runtime.dynamic.casters.ArrayCaster}, matching the array
	 * literal spread semantics.
	 */
	public static ortus.boxlang.runtime.types.BoxSet set( Object... values ) {
		values = normalizeVarargs( values );
		ortus.boxlang.runtime.types.BoxSet result = new ortus.boxlang.runtime.types.BoxSet();
		for ( Object value : values ) {
			if ( value instanceof SpreadValue spreadValue ) {
				Object spreadSource = spreadValue.getValue();
				if ( spreadSource instanceof java.util.Collection<?> col ) {
					result.addAll( col );
				} else {
					CastAttempt<Array> casted = ArrayCaster.attempt( spreadSource );
					if ( !casted.wasSuccessful() ) {
						throw new BoxRuntimeException(
						    "Cannot spread value of type [" + describeType( spreadSource ) + "] into a set literal." );
					}
					result.addAll( casted.get() );
				}
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
		if ( spreadValue instanceof java.util.Collection<?> col ) {
			target.addAll( col );
			return;
		}
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

		if ( spreadValue instanceof java.util.Collection<?> ) {
			return AmbiguousSpreadType.ARRAY;
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

	/**
	 * Build a positional Object[] from arguments that may contain SpreadValue entries.
	 * Delegates to {@link #array(Object...)} and converts to a native array.
	 */
	public static Object[] positionalArgs( Object... values ) {
		return array( values ).toArray();
	}

	/**
	 * Build a named argument Map from key/value pairs that may contain SpreadValue entries.
	 * Delegates to {@link #struct(IStruct.TYPES, Object...)} with LINKED ordering.
	 */
	@SuppressWarnings( "unchecked" )
	public static Map<Key, Object> namedArgs( Object... values ) {
		return ( Map<Key, Object> ) ( Map<?, ?> ) struct( IStruct.TYPES.LINKED, values ).getWrapped();
	}

	/**
	 * Handle spread-only function arguments where the positional/named determination
	 * must be made at runtime based on the spread values' types.
	 * Delegates to {@link #arrayOrOrderedStruct(Object...)} for type detection.
	 * If the result is a struct, returns its underlying Map; otherwise returns Object[].
	 *
	 * @return Either Object[] (positional) or Map&lt;Key, Object&gt; (named)
	 */
	@SuppressWarnings( "unchecked" )
	public static Object spreadOnlyFunctionArgs( Object... spreadValues ) {
		spreadValues = normalizeVarargs( spreadValues );
		// Wrap each raw value as a SpreadValue so arrayOrOrderedStruct can handle them
		Object[] wrapped = new Object[ spreadValues.length ];
		for ( int i = 0; i < spreadValues.length; i++ ) {
			wrapped[ i ] = spread( spreadValues[ i ] );
		}
		Object result = arrayOrOrderedStruct( wrapped );
		if ( result instanceof IStruct resultStruct ) {
			return ( Map<Key, Object> ) ( Map<?, ?> ) resultStruct.getWrapped();
		}
		return ( ( Array ) result ).toArray();
	}

	/**
	 * Invoke a function with spread-only arguments, dispatching to either positional or named
	 * based on the runtime type of the spread values.
	 */
	@SuppressWarnings( "unchecked" )
	public static Object invokeSpreadOnlyFunction( IBoxContext context, Key name, Object... spreadValues ) {
		Object args = spreadOnlyFunctionArgs( spreadValues );
		if ( args instanceof Object[] positionalArgs ) {
			return context.invokeFunction( name, positionalArgs );
		} else {
			return context.invokeFunction( name, ( Map<Key, Object> ) args );
		}
	}

	/**
	 * Invoke a function (by expression) with spread-only arguments, dispatching to either positional or named
	 * based on the runtime type of the spread values.
	 */
	@SuppressWarnings( "unchecked" )
	public static Object invokeSpreadOnlyFunction( IBoxContext context, Object function, Object... spreadValues ) {
		Object args = spreadOnlyFunctionArgs( spreadValues );
		if ( args instanceof Object[] positionalArgs ) {
			return context.invokeFunction( function, positionalArgs );
		} else {
			return context.invokeFunction( function, ( Map<Key, Object> ) args );
		}
	}

	/**
	 * Invoke a method with spread-only arguments, dispatching to either positional or named
	 * based on the runtime type of the spread values.
	 */
	@SuppressWarnings( "unchecked" )
	public static Object invokeSpreadOnlyMethod( IBoxContext context, Object object, Key methodName, boolean safe, Object... spreadValues ) {
		Object args = spreadOnlyFunctionArgs( spreadValues );
		if ( args instanceof Object[] positionalArgs ) {
			return Referencer.getAndInvoke( context, object, methodName, positionalArgs, safe );
		} else {
			return Referencer.getAndInvoke( context, object, methodName, ( Map<Key, Object> ) args, safe );
		}
	}

	/**
	 * Invoke a constructor with spread-only arguments via DynamicObject.
	 */
	@SuppressWarnings( "unchecked" )
	public static DynamicObject invokeSpreadOnlyConstructor( DynamicObject dynObj, IBoxContext context, Object... spreadValues ) {
		Object args = spreadOnlyFunctionArgs( spreadValues );
		if ( args instanceof Object[] positionalArgs ) {
			return dynObj.invokeConstructor( context, positionalArgs );
		} else {
			return dynObj.invokeConstructor( context, ( Map<Key, Object> ) args );
		}
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
