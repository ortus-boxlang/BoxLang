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
import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.JavaMethod;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * I handle casting anything to a Function
 */
public class FunctionCaster implements IBoxCaster {

	// These are the methods from the actual Object class, used below to filter.
	static final List<String> objectMethodNames = Arrays.stream( Object.class.getMethods() )
	    .map( Method::getName )
	    .collect( Collectors.toList() );

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
		return attempt( object, null );
	}

	/**
	 * Tests to see if the value can be cast to a Function.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Function
	 *
	 * @return The Function value
	 */
	public static CastAttempt<Function> attempt( Object object, String SAMClass ) {
		return CastAttempt.ofNullable( cast( object, SAMClass, false ) );
	}

	/**
	 * Used to cast anything to a Function, throwing exception if we fail
	 *
	 * @param object   The value to cast to a Function
	 * @param SAMClass The name of the SAM class the object is valid to cast to
	 *
	 * @return The Function value
	 */
	public static Function cast( Object object, String SAMClass ) {
		return cast( object, SAMClass, true );
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
		return cast( object, null, fail );
	}

	/**
	 * Used to cast anything to a Function
	 *
	 * @param object   The value to cast to a Function
	 * @param SAMClass The name of the SAM class the object is valid to cast to
	 * @param fail     True to throw exception when failing.
	 *
	 * @return The Function value, or null when cannot be cast
	 */
	public static Function cast( Object object, String SAMClass, boolean fail ) {
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

		// Only allow abitrary objects to be cast to FI/SAM if the type specified it. i.e.
		// function:Consumer
		// function:Producer
		// function:java.util.function.Predicate
		if ( SAMClass != null && !SAMClass.isBlank() ) {
			String SAMName = getSAMName( object, SAMClass.trim() );
			if ( SAMName != null ) {
				return new JavaMethod( Key.of( SAMName ), DynamicObject.of( object ) );
			}
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
	public static String getSAMName( Object object, String SAMClass ) {
		// Turn shortcuts like producer into java.util.function.Producer
		SAMClass = expandKnownClasses( SAMClass );

		// Lets load up the class and see if it's valid
		// TODO: consider using the class loccater, but we'll need the context passed through for that
		Class<?> clazz;
		try {
			clazz = BoxRuntime.getInstance().getClass().getClassLoader().loadClass( SAMClass );
		} catch ( ClassNotFoundException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		// If the class exists, is an interface, and the object is not assignable from it, then let's see if it's a SAM.
		if ( clazz.isInterface() && !clazz.isAssignableFrom( object.getClass() ) ) {
			return null;
		}
		// Check known FIs in the JDK sinc we already know their abstract method name
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
			case java.util.Comparator ee -> "compare";
			default -> null;
		};

		// if found, return it
		if ( SAMName != null ) {
			return SAMName;
		}

		// For other classes, let's inspect the methods to find the abstract one
		// TODO: cache this based on incoming interface name
		List<Method>	filteredMethods	= Arrays.stream( clazz.getMethods() )
		    .filter( method -> Modifier.isAbstract( method.getModifiers() ) && !Modifier.isStatic( method.getModifiers() ) )
		    .filter( method -> !objectMethodNames.contains( method.getName() ) ||
		        !Arrays.equals( method.getParameterTypes(),
		            Arrays.stream( Object.class.getMethods() )
		                .filter( m -> m.getName().equals( method.getName() ) )
		                .findFirst()
		                .map( Method::getParameterTypes )
		                .orElse( new Class<?>[ 0 ] ) ) )
		    .collect( Collectors.toList() );

		long			count			= filteredMethods.size();

		// Use the filtered list directly if count is exactly one
		if ( count == 1 ) {
			return filteredMethods.get( 0 ).getName();
		}
		return null;

	}

	private static String expandKnownClasses( String SAMClass ) {
		return switch ( SAMClass.toLowerCase() ) {
			case "consumer", "java.util.function.consumer" -> "java.util.function.Consumer";
			case "biconsumer", "java.util.function.biconsumer" -> "java.util.function.BiConsumer";
			case "doubleconsumer", "java.util.function.doubleconsumer" -> "java.util.function.DoubleConsumer";
			case "intconsumer", "java.util.function.intconsumer" -> "java.util.function.IntConsumer";
			case "longconsumer", "java.util.function.longconsumer" -> "java.util.function.LongConsumer";
			case "objdoubleconsumer", "java.util.function.objdoubleconsumer" -> "java.util.function.ObjDoubleConsumer";
			case "objintconsumer", "java.util.function.objintconsumer" -> "java.util.function.ObjIntConsumer";
			case "objlongconsumer", "java.util.function.objlongconsumer" -> "java.util.function.ObjLongConsumer";
			case "doublebinaryoperator", "java.util.function.doublebinaryoperator" -> "java.util.function.DoubleBinaryOperator";
			case "doublefunction", "java.util.function.doublefunction" -> "java.util.function.DoubleFunction";
			case "doubletointfunction", "java.util.function.doubletointfunction" -> "java.util.function.DoubleToIntFunction";
			case "doubletolongfunction", "java.util.function.doubletolongfunction" -> "java.util.function.DoubleToLongFunction";
			case "unaryoperator", "java.util.function.unaryoperator" -> "java.util.function.UnaryOperator";
			case "binaryoperator", "java.util.function.binaryoperator" -> "java.util.function.BinaryOperator";
			case "doubleunaryoperator", "java.util.function.doubleunaryoperator" -> "java.util.function.DoubleUnaryOperator";
			case "function", "java.util.function.function" -> "java.util.function.Function";
			case "intbinaryoperator", "java.util.function.intbinaryoperator" -> "java.util.function.IntBinaryOperator";
			case "intfunction", "java.util.function.intfunction" -> "java.util.function.IntFunction";
			case "inttodoublefunction", "java.util.function.inttodoublefunction" -> "java.util.function.IntToDoubleFunction";
			case "inttolongfunction", "java.util.function.inttolongfunction" -> "java.util.function.IntToLongFunction";
			case "intunaryoperator", "java.util.function.intunaryoperator" -> "java.util.function.IntUnaryOperator";
			case "longbinaryoperator", "java.util.function.longbinaryoperator" -> "java.util.function.LongBinaryOperator";
			case "longfunction", "java.util.function.longfunction" -> "java.util.function.LongFunction";
			case "longtodoublefunction", "java.util.function.longtodoublefunction" -> "java.util.function.LongToDoubleFunction";
			case "longtointfunction", "java.util.function.longtointfunction" -> "java.util.function.LongToIntFunction";
			case "longunaryoperator", "java.util.function.longunaryoperator" -> "java.util.function.LongUnaryOperator";
			case "bifunction", "java.util.function.bifunction" -> "java.util.function.BiFunction";
			case "predicate", "java.util.function.predicate" -> "java.util.function.Predicate";
			case "bipredicate", "java.util.function.bipredicate" -> "java.util.function.BiPredicate";
			case "supplier", "java.util.function.supplier" -> "java.util.function.Supplier";
			case "comparator", "java.util.comparator" -> "java.util.Comparator";
			default -> SAMClass;
		};
	}

}