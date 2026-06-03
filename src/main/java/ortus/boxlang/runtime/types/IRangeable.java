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

/**
 * Interface that allows any class to participate in iterable ranges.
 * Implement this to enable {@code myObj..otherObj} syntax with full iteration support.
 * <p>
 * Implementors should also implement {@code Comparable<T>} for natural ordering,
 * but it is not enforced here to avoid erasure conflicts with types that already
 * implement Comparable with a different type parameter (e.g., ChronoZonedDateTime).
 *
 * @param <T> the implementing type (self-referential bound)
 */
public interface IRangeable<T> {

	/**
	 * Produce the next value in a range given the current step.
	 *
	 * @param step the step amount (positive for forward progression)
	 *
	 * @return the next value
	 */
	T rangeAdvance( Number step );

	/**
	 * Compare this value to another for ordering within a range.
	 * Semantics are the same as {@link Comparable#compareTo(Object)}.
	 *
	 * @param other the other value to compare to
	 *
	 * @return negative if this &lt; other, zero if equal, positive if this &gt; other
	 */
	int rangeCompare( T other );

	/**
	 * Attempt to coerce an arbitrary value into this range's element type.
	 * Used by {@code Range.contains(Object)} to allow custom types to define
	 * their own casting logic for containment checks.
	 * <p>
	 * The default implementation accepts values that are already the correct type.
	 * Override to provide richer coercion (e.g., parsing strings into dates).
	 *
	 * @param value the value to coerce
	 *
	 * @return the coerced value, or null if incompatible
	 */
	@SuppressWarnings( "unchecked" )
	default T rangeCoerce( Object value ) {
		if ( this.getClass().isInstance( value ) ) {
			return ( T ) value;
		}
		return null;
	}

	/**
	 * Convert an amount and unit into a raw numeric step value that
	 * {@link #rangeAdvance(Number)} understands.
	 * <p>
	 * Override this to support unit-based stepping (e.g., hours, minutes) on your type.
	 * The default implementation throws, indicating unit-based stepping is not supported.
	 *
	 * @param amount the number of units per step
	 * @param unit   the unit name (interpretation is type-specific)
	 *
	 * @return a numeric step value suitable for {@link #rangeAdvance(Number)}
	 */
	default Number rangeStepFromUnit( Number amount, String unit ) {
		throw new ortus.boxlang.runtime.types.exceptions.BoxRuntimeException(
		    String.format( "Unit-based stepping is not supported for type [%s].", this.getClass().getSimpleName() )
		);
	}

	/**
	 * Return a custom stepper function for a calendar-based unit (e.g., months, years)
	 * that cannot be represented as a fixed numeric step.
	 * <p>
	 * If this method returns a non-null BiFunction, {@code Range.step(amount, unit)} will
	 * install it as the range's stepper directly, bypassing {@link #rangeStepFromUnit(Number, String)}.
	 * <p>
	 * The default implementation returns null, indicating the unit should use the numeric path.
	 *
	 * @param unit the unit name (interpretation is type-specific)
	 *
	 * @return a stepper BiFunction, or null to use the numeric step path
	 */
	default BiFunction<T, Number, T> rangeUnitStepper( String unit ) {
		return null;
	}
}
