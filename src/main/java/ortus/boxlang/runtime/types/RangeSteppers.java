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
package ortus.boxlang.runtime.types;

import java.util.function.BiFunction;

import ortus.boxlang.runtime.operators.Plus;

/**
 * Registry of built-in stepper functions for known JDK types that can participate
 * in iterable ranges but don't implement {@link IRangeable}.
 */
public class RangeSteppers {

	/**
	 * Stepper for Number types. Uses the BoxLang Plus operator which handles
	 * all type contagion (Integer, Long, Double, BigDecimal, etc.)
	 */
	public static final BiFunction<Number, Number, Number>			NUMBER_STEPPER		= ( current, step ) -> Plus.invoke( current, step );

	/**
	 * Stepper for Character types. Advances the character by the integer value of the step.
	 */
	public static final BiFunction<Character, Number, Character>	CHARACTER_STEPPER	= ( current, step ) -> ( char ) ( current + step.intValue() );

	/**
	 * Look up a stepper for the given type. Returns null if no built-in stepper is registered.
	 *
	 * @param type the class to find a stepper for
	 *
	 * @return a stepper BiFunction, or null if none is registered
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> BiFunction<T, Number, T> lookup( Class<?> type ) {
		if ( Number.class.isAssignableFrom( type ) ) {
			return ( BiFunction<T, Number, T> ) ( BiFunction<?, ?, ?> ) NUMBER_STEPPER;
		}
		if ( type == Character.class || type == char.class ) {
			return ( BiFunction<T, Number, T> ) ( BiFunction<?, ?, ?> ) CHARACTER_STEPPER;
		}
		return null;
	}
}
