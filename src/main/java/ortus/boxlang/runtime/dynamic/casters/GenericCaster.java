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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.DateTime;
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
			type = boxType.name();
		} else {
			type = StringCaster.cast( oType );
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
		String	originalCaseType	= StringCaster.cast( oType );
		String	type				= originalCaseType.toLowerCase();

		if ( type.equals( "null" ) || type.equals( "void" ) ) {
			return null;
		}

		if ( type.equals( "any" ) || type.equals( "object" ) ) {
			return object;
		}

		// Handle arrays like int[], or java.lang.String[]
		if ( type.endsWith( "[]" ) ) {
			// Remove the []
			String newType = type.substring( 0, type.length() - 2 );
			originalCaseType = originalCaseType.substring( 0, originalCaseType.length() - 2 );
			Class<?>	newTypeClass	= getClassFromType( context, newType, originalCaseType, false );
			// Typed as Object instead of Object[] in case we're creating an array of primitives
			Object		result;
			Boolean		convertToArray	= false;

			// If we could not get the class, then we are casting to an array of objects
			if ( newTypeClass == null ) {
				convertToArray	= true;
				newTypeClass	= Object.class;
			}

			if ( object.getClass().isArray() ) {
				// If our incoming object is already an array of the new type, just return it
				if ( object.getClass().getComponentType().equals( newTypeClass ) ) {
					return object;
				}
				result = castNativeArrayToNativeArray( context, object, newType, fail, newTypeClass );
			} else if ( object instanceof List<?> incomingList ) {
				Object[] incomingArray = incomingList.toArray();
				result = castNativeArrayToNativeArray( context, incomingArray, newType, fail, newTypeClass );
			} else {
				throw new BoxCastException(
				    String.format( "You asked for type %s, but input %s cannot be cast to an array.", type,
				        object.getClass().getName() )
				);
			}
			if ( convertToArray ) {
				// unsafe cast to Object[] is OK here because the convertToArray flag will never be true
				// if our target type is an array of primitives, so we know it will have boxed types in it
				return ortus.boxlang.runtime.types.Array.fromArray( ( Object[] ) result );
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
		if ( type.equals( "bit" ) ) {
			return Boolean.TRUE.equals( BooleanCaster.cast( object, fail ) ) ? 1 : 0;
		}
		if ( type.equals( "bigdecimal" ) || type.equals( "decimal" ) ) {
			return BigDecimalCaster.cast( object, fail );
		}
		if ( type.equals( "biginteger" ) ) {
			return BigIntegerCaster.cast( object, fail );
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
		// BL-640 - if we have a DateTime object provided, we use that reference rather than strip the date by using the timecaster
		if ( object instanceof DateTime || type.equals( "datetime" ) || type.equals( "date" ) || type.equals( "timestamp" ) ) {
			return DateTimeCaster.cast( object, fail, context );
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
		if ( type.equals( "collection" ) ) {
			return CollectionCaster.cast( object, fail );
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

		if ( type.equals( "throwable" ) ) {
			return ThrowableCaster.cast( object, fail );
		}

		if ( type.equals( "key" ) ) {
			return KeyCaster.cast( object, fail );
		}

		if ( type.startsWith( "function:" ) && type.length() > 9 ) {
			// strip off class name from "function:com.foo.Bar"
			return FunctionCaster.cast( object, originalCaseType.substring( 9 ), fail );
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

	/**
	 * Cast a native array to a native array
	 * We are accepting Object and returning Object so we can pass arrays of primitives
	 *
	 * @param object       The object to cast
	 * @param newType      The new type
	 * @param fail         True to throw exception when type is invalid
	 * @param newTypeClass The new type class
	 *
	 * @return The casted object
	 */
	private static Object castNativeArrayToNativeArray( IBoxContext context, Object object, String newType, boolean fail, Class<?> newTypeClass ) {
		int		len		= java.lang.reflect.Array.getLength( object );
		Object	result	= java.lang.reflect.Array.newInstance( newTypeClass, len );
		for ( int i = len - 1; i >= 0; i-- ) {
			Object v = GenericCaster.cast( context, java.lang.reflect.Array.get( object, i ), newType, fail );
			java.lang.reflect.Array.set( result, i, v );
		}
		return result;
	}

	/**
	 * Get the class from a type sent in that we can cast
	 *
	 * @param type The type to get the class for
	 *
	 * @return The class instance
	 */
	public static Class<?> getClassFromType( IBoxContext context, String type ) {
		return getClassFromType( context, type, type, true );
	}

	/**
	 * Get the class from a type sent in that we can cast
	 *
	 * @param type             The type to get the class for
	 * @param originalCaseType The original case of the type
	 *
	 * @return The class instance
	 */
	public static Class<?> getClassFromType( IBoxContext context, String type, String originalCaseType ) {
		return getClassFromType( context, type, originalCaseType, true );
	}

	/**
	 * Get the class from a type sent in that we can cast
	 *
	 * @param type             The type to get the class for
	 * @param originalCaseType The original case of the type
	 * @param fail             True to throw exception when type is invalid
	 *
	 * @return The class instance
	 */
	public static Class<?> getClassFromType( IBoxContext context, String type, String originalCaseType, Boolean fail ) {

		// Check for primitive types first
		if ( originalCaseType.equals( "byte" ) ) {
			return byte.class;
		}
		if ( originalCaseType.equals( "char" ) ) {
			return char.class;
		}
		if ( originalCaseType.equals( "short" ) ) {
			return short.class;
		}
		if ( originalCaseType.equals( "int" ) ) {
			return int.class;
		}
		if ( originalCaseType.equals( "long" ) ) {
			return long.class;
		}
		if ( originalCaseType.equals( "float" ) ) {
			return float.class;
		}
		if ( originalCaseType.equals( "double" ) ) {
			return double.class;
		}
		if ( originalCaseType.equals( "boolean" ) ) {
			return boolean.class;
		}

		// Check for boxed types
		if ( type.equals( "bigdecimal" ) || type.equals( "java.math.bigdecimal" ) ) {
			return BigDecimal.class;
		}
		if ( type.equals( "biginteger" ) || type.equals( "java.math.biginteger" ) ) {
			return java.math.BigInteger.class;
		}
		if ( type.equals( "boolean" ) || type.equals( "java.lang.boolean" ) ) {
			return Boolean.class;
		}
		if ( type.equals( "byte" ) || type.equals( "java.lang.byte" ) ) {
			return Byte.class;
		}
		if ( type.equals( "char" ) || type.equals( "java.lang.char" ) ) {
			return Character.class;
		}
		if ( type.equals( "double" ) || type.equals( "java.lang.double" ) ) {
			return Double.class;
		}
		if ( type.equals( "float" ) || type.equals( "java.lang.float" ) ) {
			return Float.class;
		}
		if ( type.equals( "instant" ) || type.equals( "java.time.instant" ) ) {
			return java.time.Instant.class;
		}
		if ( type.equals( "int" ) || type.equals( "integer" ) || type.equals( "java.lang.integer" ) ) {
			return Integer.class;
		}
		if ( type.equals( "LocalDate" ) || type.equals( "java.time.LocalDate" ) ) {
			return java.time.LocalDate.class;
		}
		if ( type.equals( "LocalDateTime" ) || type.equals( "java.time.LocalDateTime" ) ) {
			return java.time.LocalDateTime.class;
		}
		if ( type.equals( "LocalTime" ) || type.equals( "java.time.LocalTime" ) ) {
			return java.time.LocalTime.class;
		}
		if ( type.equals( "long" ) || type.equals( "java.lang.long" ) ) {
			return Long.class;
		}
		if ( type.equals( "short" ) || type.equals( "java.lang.short" ) ) {
			return Short.class;
		}
		if ( type.equals( "string" ) || type.equals( "java.lang.string" ) ) {
			return String.class;
		}
		if ( type.equals( "object" ) || type.equals( "java.lang.object" ) ) {
			return Object.class;
		}

		// If we got here, then we have a full class name like java.lang.String
		// Let's see if we can load it
		Optional<DynamicObject> loadResult = BoxRuntime.getInstance().getClassLocator().safeLoad( context, originalCaseType, "java" );
		if ( loadResult.isPresent() ) {
			return loadResult.get().getTargetClass();
		}

		if ( !fail ) {
			return null;
		}

		throw new BoxCastException(
		    String.format( "Invalid cast type [%s]", originalCaseType )
		);
	}
}
