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
package ortus.boxlang.runtime.dynamic.casters;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.JavaMethod;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a Function
 */
public class FunctionCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Function.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Function
	 *
	 * @return The Function value
	 */
	public static CastAttempt<Function> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Function, throwing exception if we fail
	 *
	 * @param object The value to cast to a Function
	 *
	 * @return The Function value
	 */
	public static Function cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Function
	 *
	 * @param object The value to cast to a Function
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Function value, or null when cannot be cast
	 */
	public static Function cast( Object object, boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Null cannot be cast to a Function" );
			} else {
				return null;
			}
		}

		object = DynamicObject.unWrap( object );

		if ( object instanceof Function fun ) {
			return fun;
		}

		String SAMName = getSAMName( object );
		if ( SAMName != null ) {
			return new JavaMethod( Key.of( SAMName ), DynamicObject.of( object ) );
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Value [%s] cannot be cast to a Function", object.getClass().getName() )
			);
		} else {
			return null;
		}

	}

	/**
	 * I detect if an instance implements a known FI or a SAM and return the name of the method, null if not a SAM.
	 * 
	 * @param object The object to check
	 * 
	 * @return The name of the SAM method, or null if not a SAM
	 */
	@SuppressWarnings( "rawtypes" )
	public static String getSAMName( Object object ) {
		// Check known FIs in the JDK
		String SAMName = switch ( object ) {
			case java.util.function.BiConsumer a -> "accept";
			case java.util.function.Consumer b -> "accept";
			case java.util.function.DoubleConsumer c -> "accept";
			case java.util.function.IntConsumer d -> "accept";
			case java.util.function.LongConsumer e -> "accept";
			case java.util.function.ObjDoubleConsumer f -> "accept";
			case java.util.function.ObjIntConsumer g -> "accept";
			case java.util.function.ObjLongConsumer h -> "accept";
			case java.util.function.DoubleBinaryOperator i -> "applyAsDouble";
			case java.util.function.DoubleFunction j -> "apply";
			case java.util.function.DoubleToIntFunction k -> "applyAsInt";
			case java.util.function.DoubleToLongFunction l -> "applyAsLong";
			case java.util.function.UnaryOperator z -> "apply";
			case java.util.function.BinaryOperator aa -> "apply";
			case java.util.function.DoubleUnaryOperator m -> "applyAsDouble";
			case java.util.function.Function n -> "apply";
			case java.util.function.IntBinaryOperator o -> "applyAsInt";
			case java.util.function.IntFunction p -> "apply";
			case java.util.function.IntToDoubleFunction q -> "applyAsDouble";
			case java.util.function.IntToLongFunction r -> "applyAsLong";
			case java.util.function.IntUnaryOperator s -> "applyAsInt";
			case java.util.function.LongBinaryOperator t -> "applyAsLong";
			case java.util.function.LongFunction u -> "apply";
			case java.util.function.LongToDoubleFunction v -> "applyAsDouble";
			case java.util.function.LongToIntFunction w -> "applyAsInt";
			case java.util.function.LongUnaryOperator x -> "applyAsLong";
			case java.util.function.BiFunction y -> "apply";
			case java.util.function.Predicate bb -> "test";
			case java.util.function.BiPredicate cc -> "test";
			case java.util.function.Supplier dd -> "get";
			default -> null;
		};

		// If not a known FI, check if it is a SAM (implements an interface with a single abstract method)
		if ( SAMName == null ) {
			Class<?>[] interfaces = object.getClass().getInterfaces();

			for ( Class<?> iface : interfaces ) {
				Method[] methods = Arrays.stream( iface.getMethods() )
				    // Note: Interface methods are implicitly abstract if they are not default or static
				    .filter( method -> Modifier.isAbstract( method.getModifiers() ) && !Modifier.isStatic( method.getModifiers() ) )
				    .toArray( Method[]::new );

				if ( methods.length == 1 ) {
					// System.out.println( "Found SAM: " + methods[ 0 ].getName() + " in " + iface.getName() + " for " + object.getClass().getName() );
					SAMName = methods[ 0 ].getName();
					break;
				}
			}
		}

		// Return the SAM method name, or null if not a SAM
		return SAMName;
	}

}
