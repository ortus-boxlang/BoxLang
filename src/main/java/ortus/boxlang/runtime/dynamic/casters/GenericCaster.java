/**
 * [BoxLang]
 *
 * Copytype [2023] [Ortus Solutions, Corp]
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
package ortus.boxlang.runtime.dynamic.casters;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything
 */
public class GenericCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 * If the cast type was "null" or "void" the CastAttempt will contain a NullValue() instance.
	 * If the input value is null and the type is "any", the CastAttempt will contain a NullValue() instance.
	 *
	 * @param object The value to cast
	 * @param oType  The type to cast to
	 * @param strict True to throw exception when casting non-null value to null/void
	 *
	 * @return A CastAttempt, which contains the casted value, if successful
	 */
	public static CastAttempt<Object> attempt( IBoxContext context, Object object, Object oType, boolean strict ) {
		String type;
		if ( oType instanceof BoxLangType boxType ) {
			type = boxType.name().toLowerCase();
		} else {
			type = StringCaster.cast( oType ).toLowerCase();
		}

		// Represent legit null values in a NullValue instance
		if ( type.equalsIgnoreCase( "null" ) || type.equalsIgnoreCase( "void" ) ) {
			if ( strict && object != null ) {
				throw new BoxCastException(
				    String.format( "Cannot cast type [%s] to %s.", object.getClass().getName(), type )
				);
			}
			return CastAttempt.ofNullable( new NullValue() );
		}

		// Represent legit null values in a NullValue instance
		if ( type.equalsIgnoreCase( "any" ) && object == null ) {
			return CastAttempt.ofNullable( new NullValue() );
		}

		return CastAttempt.ofNullable( cast( context, object, type, false ) );
	}

	/**
	 * Tests to see if the value can be cast
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 * If the cast type was "null" or "void" the CastAttempt will contain a NullValue() instance.
	 * If the input value is null and the type is "any", the CastAttempt will contain a NullValue() instance.
	 *
	 * @param object The value to cast
	 * @param oType  The type to cast to
	 *
	 * @return A CastAttempt, which contains the casted value, if successful
	 */
	public static CastAttempt<Object> attempt( IBoxContext context, Object object, Object oType ) {
		return attempt( context, object, oType, false );
	}

	/**
	 * Used to cast anything, throwing exception if we fail
	 *
	 * @param object The value to cast
	 * @param oType  The type to cast to
	 *
	 * @return The value
	 */
	public static Object cast( IBoxContext context, Object object, Object oType ) {
		return cast( context, object, oType, true );
	}

	/**
	 * Used to cast anything. Note, when fail is set to false, it is not possible to differentiate between
	 * a failed cast and a successful cast to type "null" or "void". The same ambiguity exists for an input
	 * of null and a type of "any". For these cases, use the attempt() method and check the optional
	 * for a NullValue() instance.
	 *
	 * @param object The value to cast
	 * @param oType  The type to cast to
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The value, or null when cannot be cast or if the type was "null" or "void"
	 */
	public static Object cast( IBoxContext context, Object object, Object oType, Boolean fail ) {
		String	OriginalCaseType	= StringCaster.cast( oType );
		String	type				= OriginalCaseType.toLowerCase();

		if ( type.equals( "null" ) || type.equals( "void" ) ) {
			return null;
		}

		if ( type.equals( "any" ) ) {
			return object;
		}

		// Handle arrays like int[]
		if ( type.endsWith( "[]" ) ) {

			String		newType			= type.substring( 0, type.length() - 2 );
			Class<?>	newTypeClass	= getClassFromType( newType, false );
			Object[]	result;
			Boolean		convertToArray	= false;
			if ( newTypeClass == null ) {
				convertToArray	= true;
				newTypeClass	= Object.class;
			}

			if ( object.getClass().isArray() ) {
				result = castNativeArrayToNativeArray( context, object, newType, fail, newTypeClass );
			} else if ( object instanceof List<?> l ) {
				result = castListToNativeArray( context, object, newType, fail, newTypeClass );
			} else {
				throw new BoxCastException(
				    String.format( "You asked for type %s, but input %s cannot be cast to an array.", type,
				        object.getClass().getName() )
				);
			}
			if ( convertToArray ) {
				return ortus.boxlang.runtime.types.Array.fromArray( result );
			}
			return result;
		}

		if ( type.equals( "string" ) ) {
			return StringCaster.cast( object, fail );
		}
		if ( type.equals( "double" ) ) {
			return DoubleCaster.cast( object, fail );
		}
		if ( type.equals( "numeric" ) || type.equals( "number" ) ) {
			return NumberCaster.cast( object, fail );
		}
		if ( type.equals( "boolean" ) ) {
			return BooleanCaster.cast( object, fail );
		}
		if ( type.equals( "bigdecimal" ) ) {
			return BigDecimalCaster.cast( object, fail );
		}
		if ( type.equals( "char" ) ) {
			return CharacterCaster.cast( object, fail );
		}
		if ( type.equals( "byte" ) ) {
			return ByteCaster.cast( object, fail );
		}
		if ( type.equals( "int" ) || type.equals( "integer" ) ) {
			return IntegerCaster.cast( object, fail );
		}
		if ( type.equals( "long" ) ) {
			return LongCaster.cast( object, fail );
		}
		if ( type.equals( "short" ) ) {
			return ShortCaster.cast( object, fail );
		}
		if ( type.equals( "float" ) ) {
			return FloatCaster.cast( object, fail );
		}
		if ( type.equals( "array" ) ) {
			return ArrayCaster.cast( object, fail );
		}
		if ( type.equals( "datetime" ) || type.equals( "date" ) ) {
			return DateTimeCaster.cast( object, fail );
		}
		if ( type.equals( "time" ) ) {
			return TimeCaster.cast( object, fail );
		}
		if ( type.equals( "modifiablearray" ) ) {
			return ModifiableArrayCaster.cast( object, fail );
		}
		if ( type.equals( "struct" ) ) {
			return StructCaster.cast( object, fail );
		}
		if ( type.equals( "structloose" ) ) {
			return StructCasterLoose.cast( object, fail );
		}
		if ( type.equals( "modifiablestruct" ) ) {
			return ModifiableStructCaster.cast( object, fail );
		}
		if ( type.equals( "xml" ) ) {
			return XMLCaster.cast( object, fail );
		}

		if ( type.equals( "function" ) ) {
			return FunctionCaster.cast( object, fail );
		}

		if ( type.startsWith( "function:" ) && type.length() > 9 ) {
			// strip off class name from "function:com.foo.Bar"
			return FunctionCaster.cast( object, OriginalCaseType.substring( 9 ), fail );
		}

		if ( type.equals( "query" ) ) {
			// No real "casting" to do, just return it if it is one
			if ( object instanceof Query ) {
				return object;
			}
			if ( fail ) {
				throw new BoxCastException( String.format( "Cannot cast %s, to a Query.", object.getClass().getName() ) );
			} else {
				return null;
			}
		}

		if ( type.equals( "stream" ) ) {
			// No real "casting" to do, just return it if it is one
			if ( object instanceof Stream ) {
				return object;
			}
			if ( object instanceof IntStream is ) {
				return is.boxed();
			}
			if ( object instanceof DoubleStream ds ) {
				return ds.boxed();
			}
			if ( object instanceof LongStream ls ) {
				return ls.boxed();
			}

			if ( fail ) {
				throw new BoxCastException( String.format( "Cannot cast %s, to a Stream.", object.getClass().getName() ) );
			} else {
				return null;
			}
		}

		if ( type.equals( "component" ) || type.equals( "class" ) ) {
			// No real "casting" to do, just return it if it is one
			if ( object instanceof IClassRunnable ) {
				return object;
			}
			if ( fail ) {
				throw new BoxCastException( String.format( "Cannot cast %s, to a %s.", object.getClass().getName(), type ) );
			} else {
				return null;
			}
		}

		// Handle class types. If it is an instance, we pass it
		if ( InstanceOf.invoke( context, object, type ) ) {
			return object;
		}
		if ( fail ) {
			throw new BoxCastException( String.format( "Could not cast object [%s] to type [%s]", object.getClass().getSimpleName(), type ) );
		} else {
			return null;
		}

	}

	private static Object[] castNativeArrayToNativeArray( IBoxContext context, Object object, String newType, boolean fail, Class<?> newTypeClass ) {
		Object[] result = ( Object[] ) java.lang.reflect.Array.newInstance( newTypeClass, Array.getLength( object ) );
		for ( int i = Array.getLength( object ) - 1; i >= 0; i-- ) {
			result[ i ] = GenericCaster.cast( context, Array.get( object, i ), newType, fail );
		}
		return result;
	}

	private static Object[] castListToNativeArray( IBoxContext context, Object object, String newType, boolean fail, Class<?> newTypeClass ) {
		List<?>		l				= ( List<?> ) object;
		Object[]	incomingList	= l.toArray();
		Object[]	result			= ( Object[] ) java.lang.reflect.Array.newInstance( newTypeClass, incomingList.length );
		for ( int i = incomingList.length - 1; i >= 0; i-- ) {
			result[ i ] = GenericCaster.cast( context, incomingList[ i ], newType, fail );
		}
		return result;
	}

	public static Class<?> getClassFromType( String type ) {
		return getClassFromType( type, true );
	}

	public static Class<?> getClassFromType( String type, Boolean fail ) {

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
		if ( type.equals( "object" ) ) {
			return Object.class;
		}
		if ( !fail ) {
			return null;
		}
		throw new BoxCastException(
		    String.format( "Invalid cast type [%s]", type )
		);
	}
}
