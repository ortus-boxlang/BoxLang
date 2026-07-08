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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

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
		Key type;
		if ( oType instanceof BoxLangType boxType ) {
			type = boxType.getKey();
		} else {
			type = KeyCaster.cast( oType );
		}

		return attempt( context, object, type, strict );
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
	 * @param strict True to throw exception when casting non-null value to null/void
	 *
	 * @return A CastAttempt, which contains the casted value, if successful
	 */
	public static CastAttempt<Object> attempt( IBoxContext context, Object object, Key type, boolean strict ) {

		// Represent legit null values in a NullValue instance
		if ( type.equals( Key.nulls ) || type.equals( Key._void ) ) {
			if ( strict && object != null ) {
				throw new BoxCastException(
				    String.format( "Cannot cast type [%s] to %s.", TypeUtil.getObjectName( object ), type )
				);
			}
			return CastAttempt.ofNullable( new NullValue() );
		}

		// Represent legit null values in a NullValue instance
		if ( type.equals( Key._ANY ) && object == null ) {
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
		return cast( false, context, object, oType, true );
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
		return cast( false, context, object, oType, fail );
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
	public static Object cast( boolean allowTruncate, IBoxContext context, Object object, Object oType, Boolean fail ) {
		return cast( allowTruncate, context, object, KeyCaster.cast( oType ), fail );
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
	public static Object cast( boolean allowTruncate, IBoxContext context, Object object, Key type, Boolean fail ) {

		switch ( type.getNameNoCase() ) {
			case "null" :
			case "void" :
				return null;
			case "any" :
			case "object" :
				return object;
			case "string" :
				return StringCaster.cast( object, null, fail );
			case "string_strict" :
				return StringCasterStrict.cast( object, null, fail );
			case "double" :
				return DoubleCaster.cast( object, fail );
			case "numeric" :
			case "number" :
				return NumberCaster.cast( object, fail );
			case "boolean" :
				return BooleanCaster.cast( object, fail );
			case "bit" :
				return Boolean.TRUE.equals( BooleanCaster.cast( object, fail ) ) ? 1 : 0;
			case "bigdecimal" :
			case "decimal" :
				return BigDecimalCaster.cast( object, fail );
			case "biginteger" :
				return BigIntegerCaster.cast( object, fail );
			case "char" :
				return CharacterCaster.cast( object, fail );
			case "byte" :
				return ByteCaster.cast( object, fail );
			case "float" :
				return FloatCaster.cast( object, fail );
			case "array" :
				return ArrayCaster.cast( object, fail );
			case "stringbuilder" :
				return StringBuilderCaster.cast( object, fail );
			case "stringbuilderstrict" :
				return StringBuilderCasterStrict.cast( object, fail );
			case "set" :
				// Strict: only accept actual Sets. Use the explicit `toSet()` member or
				// `setNew(...)` BIF to convert arrays / lists / etc.
				return SetCaster.cast( object, fail );
			case "modifiableset" :
				return ModifiableSetCaster.cast( object, fail );
			case "datetime" :
			case "date" :
			case "timestamp" :
				return DateTimeCaster.cast( object, fail, context );
			case "time" :
				return TimeCaster.cast( object, fail );
			case "modifiablearray" :
				return ModifiableArrayCaster.cast( object, fail );
			case "assignablearray" :
				return AssignableArrayCaster.cast( object, fail );
			case "struct" :
				return StructCaster.cast( object, fail );
			case "collection" :
				return CollectionCaster.cast( object, fail );
			case "structloose" :
				return StructCasterLoose.cast( object, fail );
			case "modifiablestruct" :
				return ModifiableStructCaster.cast( object, fail );
			case "xml" :
				return XMLCaster.cast( object, fail );
			case "function" :
				return FunctionCaster.cast( object, fail );
			case "int" :
			case "integer" :
				return IntegerCaster.cast( allowTruncate, object, fail );
			case "long" :
				return LongCaster.cast( allowTruncate, object, fail );
			case "short" :
				return ShortCaster.cast( allowTruncate, object, fail );
			case "integertruncate" :
				return IntegerCaster.cast( true, object, fail );
			case "longtruncate" :
				return LongCaster.cast( true, object, fail );
			case "shorttruncate" :
				return ShortCaster.cast( true, object, fail );
		}

		// Handle arrays like int[], or java.lang.String[]
		if ( type.getName().endsWith( "[]" ) ) {
			// Remove the []
			Key			newType			= Key.of( type.getName().substring( 0, type.getName().length() - 2 ) );
			Class<?>	newTypeClass	= getClassFromType( context, newType, false );
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
				if ( fail ) {
					throw new BoxCastException(
					    String.format( "You asked for type %s, but input %s cannot be cast to an array.", type.getName(),
					        TypeUtil.getObjectName( object ) )
					);
				} else {
					return null;
				}
			}
			if ( result == null ) {
				// This should only happen if fail is false.
				return null;
			}
			if ( convertToArray ) {
				// unsafe cast to Object[] is OK here because the convertToArray flag will never be true
				// if our target type is an array of primitives, so we know it will have boxed types in it
				return ortus.boxlang.runtime.types.Array.fromArray( ( Object[] ) result );
			}
			return result;
		}

		// We will fall back to an instanceof check below as a last resort if we don't recognize the type being validated
		// but we need a special case here such that if the incoming value is a Box Class instance, we FORCE an instanceof check.
		// This allows Box Class names who just happen to be the same as one of our pre-defined types like "String" or "Email".
		// Only the less-common type names below are allowed to be overriden though. This caster is very "hot" code and the
		// intsanceof check is measurable overhead, even though it's fast.
		boolean isClassRunnable = false;
		if ( object != null && IClassRunnable.class.isAssignableFrom( object.getClass() ) ) {
			isClassRunnable = true;
			if ( type.equals( Key.component ) || type.equals( Key._CLASS ) || type.equals( Key._STRUCT ) || type.equals( Key.structLoose )
			    || type.equals( Key.modifiableStruct ) ) {
				// Any Box Class is also considered of type "component" or "class" or "struct" or "structloose" or "modifiablestruct"
				return object;
			} else if ( InstanceOf.invoke( context, object, type.getName() ) ) {
				return object;
			} else if ( type.equals( Key.collection ) ) {
				// Need a special case for collections, since we'll never hit the generic check below
				return CollectionCaster.cast( object, fail );
			} else if ( fail ) {
				throw new BoxCastException(
				    String.format( "Could not cast object [%s] to type [%s]", TypeUtil.getObjectName( object ), type.getName() ) );
			} else {
				return null;
			}

		}

		switch ( type.getNameNoCase() ) {
			case "component" :
			case "class" :
				// If it was a class, we will have caught it above. Nothing to do now but fail.
				if ( fail ) {
					throwCastException( type, object );
				} else {
					return null;
				}
			case "throwable" :
				return ThrowableCaster.cast( object, fail );
			case "key" :
				return KeyCaster.cast( object, fail );
			case "uuid" :
				return UUIDCaster.cast( object, fail );
			case "guid" :
				return GUIDCaster.cast( object, fail );
			case "variablename" :
				return VariableNameCaster.cast( object, fail );
			case "email" :
				return EmailCaster.cast( object, fail );
			case "binary" :
				return BinaryCaster.cast( object, fail );
			case "query" :
				// No real "casting" to do, just return it if it is one
				if ( object instanceof Query ) {
					return object;
				}
				if ( fail ) {
					throwCastException( type, object );
				} else {
					return null;
				}
			case "file" :
			case "boxfile" :
				return BoxFileCaster.cast( context, object, fail );
			case "stream" :
				if ( !isClassRunnable ) {
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
						throwCastException( type, object );
					} else {
						return null;
					}
				}
				break;
		}

		if ( type.getNameNoCase().startsWith( "function:" ) && type.getName().length() > 9 ) {
			// strip off class name from "function:com.foo.Bar"
			return FunctionCaster.cast( object, type.getName().substring( 9 ), fail );
		}

		// Handle class types. If it is an instance, we pass it
		if ( InstanceOf.invoke( context, object, type.getName() ) ) {
			return object;
		}
		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Could not cast object [%s] to type [%s]", TypeUtil.getObjectName( object ), type.getName() ) );
		} else {
			return null;
		}

	}

	private static void throwCastException( Key type, Object object ) {
		throw new BoxCastException( String.format( "Cannot cast %s, to a %s.", TypeUtil.getObjectName( object ), type.getName() ) );
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
	private static Object castNativeArrayToNativeArray( IBoxContext context, Object object, Key newType, boolean fail, Class<?> newTypeClass ) {
		int		len		= java.lang.reflect.Array.getLength( object );
		Object	result	= java.lang.reflect.Array.newInstance( newTypeClass, len );
		for ( int i = len - 1; i >= 0; i-- ) {
			Object	oldV	= java.lang.reflect.Array.get( object, i );
			// We can't be setting DynamicObjects into primitive arrays, or Java will reject the wrong type
			Object	v		= DynamicObject.unWrap( GenericCaster.cast( context, oldV, newType, fail ) );
			// If the casting failed and we are casting to a primitive or the old value was null, return null because we cannot continue
			// (primitive arrays cannot contain nulls)
			if ( v == null && ( newTypeClass.isPrimitive() || oldV != null ) ) {
				return null;
			}
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
	 *
	 * @deprecated Use {@link #getClassFromType(IBoxContext, Key, Boolean)} instead.
	 */
	@Deprecated
	public static Class<?> getClassFromType( IBoxContext context, String type ) {
		return getClassFromType( context, Key.of( type ), true );
	}

	/**
	 * Get the class from a type sent in that we can cast
	 *
	 * @param type             The type to get the class for
	 * @param originalCaseType The original case of the type
	 *
	 * @return The class instance
	 *
	 * @deprecated Use {@link #getClassFromType(IBoxContext, Key, Boolean)} instead.
	 */
	@Deprecated
	public static Class<?> getClassFromType( IBoxContext context, String type, String originalCaseType ) {
		return getClassFromType( context, Key.of( originalCaseType ), true );
	}

	/**
	 * Get the class from a type sent in that we can cast
	 *
	 * @param type             The type to get the class for
	 * @param originalCaseType The original case of the type
	 * @param fail             True to throw exception when type is invalid
	 *
	 * @return The class instance
	 *
	 * @deprecated Use {@link #getClassFromType(IBoxContext, Key, Boolean)} instead.
	 */
	@Deprecated
	public static Class<?> getClassFromType( IBoxContext context, String type, String originalCaseType, Boolean fail ) {
		return getClassFromType( context, Key.of( originalCaseType ), fail );
	}

	/**
	 * Get the class from a type sent in that we can cast.
	 * Uses Key for case-insensitive type matching.
	 *
	 * @param type The type key to get the class for
	 * @param fail True to throw exception when type is invalid
	 *
	 * @return The class instance
	 */
	public static Class<?> getClassFromType( IBoxContext context, Key type, Boolean fail ) {

		// Check for primitive types first (case-sensitive using original case)
		if ( type.getName().equals( "byte" ) ) {
			return byte.class;
		}
		if ( type.getName().equals( "char" ) ) {
			return char.class;
		}
		if ( type.getName().equals( "short" ) ) {
			return short.class;
		}
		if ( type.getName().equals( "int" ) ) {
			return int.class;
		}
		if ( type.getName().equals( "long" ) ) {
			return long.class;
		}
		if ( type.getName().equals( "float" ) ) {
			return float.class;
		}
		if ( type.getName().equals( "double" ) ) {
			return double.class;
		}
		if ( type.getName().equals( "boolean" ) ) {
			return boolean.class;
		}

		// Check for boxed types (case-insensitive via Key)
		if ( type.equals( Key.bigdecimal ) || type.equals( Key.javaMathBigDecimal ) ) {
			return BigDecimal.class;
		}
		if ( type.equals( Key.biginteger ) || type.equals( Key.javaMathBigInteger ) ) {
			return java.math.BigInteger.class;
		}
		if ( type.equals( Key._BOOLEAN ) || type.equals( Key.javaLangBoolean ) ) {
			return Boolean.class;
		}
		if ( type.equals( Key._byte ) || type.equals( Key.javaLangByte ) ) {
			return Byte.class;
		}
		if ( type.equals( Key._char ) || type.equals( Key.javaLangCharacter ) ) {
			return Character.class;
		}
		if ( type.equals( Key._DOUBLE ) || type.equals( Key.javaLangDouble ) ) {
			return Double.class;
		}
		if ( type.equals( Key._float ) || type.equals( Key.javaLangFloat ) ) {
			return Float.class;
		}
		if ( type.equals( Key.instant ) || type.equals( Key.javaTimeInstant ) ) {
			return java.time.Instant.class;
		}
		if ( type.equals( Key._int ) || type.equals( Key._INTEGER ) || type.equals( Key.javaLangInteger ) ) {
			return Integer.class;
		}
		if ( type.equals( Key.localDate ) || type.equals( Key.javaTimeLocalDate ) ) {
			return java.time.LocalDate.class;
		}
		if ( type.equals( Key.localDateTime ) || type.equals( Key.javaTimeLocalDateTime ) ) {
			return java.time.LocalDateTime.class;
		}
		if ( type.equals( Key.localTime ) || type.equals( Key.javaTimeLocalTime ) ) {
			return java.time.LocalTime.class;
		}
		if ( type.equals( Key._LONG ) || type.equals( Key.javaLangLong ) ) {
			return Long.class;
		}
		if ( type.equals( Key._short ) || type.equals( Key.javaLangShort ) ) {
			return Short.class;
		}
		if ( type.equals( Key._STRING ) || type.equals( Key.javaLangString ) ) {
			return String.class;
		}
		if ( type.equals( Key.object ) || type.equals( Key.javaLangObject ) ) {
			return Object.class;
		}

		// If we got here, then we have a full class name like java.lang.String
		// Let's see if we can load it
		Optional<DynamicObject> loadResult = BoxRuntime.getInstance().getClassLocator().safeLoad( context, type.getName(), "java" );
		if ( loadResult.isPresent() ) {
			return loadResult.get().getTargetClass();
		}

		if ( !fail ) {
			return null;
		}

		throw new BoxCastException(
		    String.format( "Invalid cast type [%s]", type.getName() )
		);
	}
}
