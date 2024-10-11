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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;
import ortus.boxlang.runtime.types.exceptions.NoElementException;
import ortus.boxlang.runtime.util.ValidationUtil;

/**
 * This class is a fluent class inspired by Java optionals. It allows for a more
 * fluent way to handle truthy and falsey values in BoxLang with functional aspects.
 * <p>
 * It is useful when you have a value that could be null or not, and you want to
 * handle it in a more functional way.
 * <p>
 * Attemps are also immutable, so you can chain methods to handle the value in a
 * more functional way, but it never mutates the original value.
 */
public class Attempt<T> {

	/**
	 * |--------------------------------------------------------------------------
	 * | Private properties
	 * |--------------------------------------------------------------------------
	 */

	protected static final Attempt<?>	EMPTY		= new Attempt<>();

	/**
	 * The target value to evaluate
	 * This can be a truthy or falsey value
	 */
	protected final T					value;

	/**
	 * Validation Record
	 */
	protected ValidationRecord			validationRecord;

	/**
	 * Simple eval for the attempt
	 * No truthy or validation checks
	 */
	protected Boolean					simpleEval	= false;

	/**
	 * |--------------------------------------------------------------------------
	 * | Constructors
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Constructor for an empty attempt
	 */
	protected Attempt() {
		this( null );
	}

	/**
	 * Constructor for an attempt with the incoming value
	 * This can be anything, a truthy or falsey or null
	 */
	protected Attempt( T value ) {
		this.value				= value;
		this.validationRecord	= new ValidationRecord();
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Static Builders
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Create an attempt from a value. This can be anything, a truthy or falsey or null
	 *
	 * @param value The value of the attempt, truthy or falsey
	 *
	 * @return a new attempt with the value
	 */
	public static <T> Attempt<T> of( T value ) {
		return new Attempt<>( value );
	}

	/**
	 * Create an empty attempt
	 *
	 * @return An empty attempt
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> Attempt<T> empty() {
		return ( Attempt<T> ) EMPTY;
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Validation methods
	 * |--------------------------------------------------------------------------
	 * You can attach validation rules to the attempt to verify if the value is valid or not
	 * This is useful when you want to validate the value before using it
	 * <p>
	 * The validation rules are:
	 * - Type validation
	 * - Range validation
	 * - Regex pattern validation
	 * - Custom validation function
	 * <p>
	 */

	private record ValidationRecord(
	    String type,
	    Double min,
	    Double max,
	    String pattern,
	    Boolean caseSensitive,
	    Predicate<Object> validationFunction ) {

		/**
		 * Empty validation record
		 */
		public ValidationRecord() {
			this( null, null, null, null, null, null );
		}

		/**
		 * Constructor for validation function only
		 */
		public ValidationRecord( Predicate<Object> validationFunction ) {
			this( null, null, null, null, null, validationFunction );
		}

		/**
		 * Constructor for types
		 */
		public ValidationRecord( String type ) {
			this( type, null, null, null, null, null );
		}

		/**
		 * Constructor for ranges
		 */
		public ValidationRecord( Double min, Double max ) {
			this( null, min, max, null, null, null );
		}

		/**
		 * Constructor for regex patterns
		 */
		public ValidationRecord( String pattern, Boolean caseSensitive ) {
			this( null, null, null, pattern, caseSensitive, null );
		}

		public Boolean isFunction() {
			return this.validationFunction != null;
		}

		public Boolean isPattern() {
			return this.pattern != null;
		}

		public Boolean isRange() {
			return this.min != null && this.max != null;
		}

		public Boolean isType() {
			return this.type != null;
		}
	}

	/**
	 * Validates the attempt to match a regex pattern with case sensitivity
	 * This assumes the value is a string or castable to a string
	 *
	 * @param pattern The pattern to match
	 *
	 * @return The attempt
	 */
	public Attempt<T> toMatchRegex( String pattern ) {
		return toMatchRegex( pattern, true );
	}

	/**
	 * Validates the attempt to match a regex pattern with case sensitivity or not
	 *
	 * @param pattern       The pattern to match
	 * @param caseSensitive True if the match is case sensitive, false otherwise
	 *
	 * @return The attempt
	 */
	public Attempt<T> toMatchRegex( String pattern, Boolean caseSensitive ) {
		Objects.requireNonNull( pattern );
		this.validationRecord = new ValidationRecord( pattern, caseSensitive );
		return this;
	}

	/**
	 * Validates the attempt to be between a range of numbers
	 * This assumes the value is a number or castable to a number
	 * The range is inclusive
	 * If the value is null, it is not valid
	 *
	 * @param min The minimum value
	 * @param max The maximum value
	 *
	 * @return The attempt
	 */
	public Attempt<T> toBeBetween( Double min, Double max ) {
		Objects.requireNonNull( min );
		Objects.requireNonNull( max );
		this.validationRecord = new ValidationRecord( min, max );
		return this;
	}

	/**
	 * Validates the attempt to be a specific BoxLang type that you can
	 * pass to the {@code isValid} function.
	 *
	 * @see ortus.boxlang.runtime.bifs.global.decision.IsValid
	 *
	 * @param type The type to validate
	 *
	 * @return The attempt
	 */
	public Attempt<T> toBeType( String type ) {
		Objects.requireNonNull( type );
		this.validationRecord = new ValidationRecord( type );
		return this;
	}

	/**
	 * Register a validation function to the attempt
	 * This function will be executed when the attempt is evaluated
	 * It must return TRUE for the attempt to be valid
	 *
	 * @param predicate The predicate to register
	 *
	 * @return The attempt
	 */
	public Attempt<T> toSatisfy( Predicate<Object> predicate ) {
		Objects.requireNonNull( predicate );
		this.validationRecord = new ValidationRecord( predicate );
		return this;
	}

	/**
	 * Stores a value to explicitly match against
	 *
	 * @param other The value to match against
	 *
	 * @return The attempt
	 */
	public Attempt<T> toBe( Object other ) {
		return toSatisfy( target -> Objects.equals( target, other ) );
	}

	/**
	 * Verifies if the attempt is valid or not according to the validation rules registered
	 * If the attempt is empty, it is not valid
	 *
	 * @return True if the attempt is valid, false otherwise
	 */
	public boolean isValid() {
		// Verify if present first
		if ( isEmpty() ) {
			return false;
		}

		// Do we have a validation function?
		if ( this.validationRecord.isFunction() ) {
			return this.validationRecord.validationFunction.test( this.value );
		}

		// Is this a range validation
		if ( this.validationRecord.isRange() ) {
			return ValidationUtil.isValidRange( this.value, this.validationRecord.min(), this.validationRecord.min() );
		}

		// Is this a regex validation
		if ( this.validationRecord.isPattern() ) {
			if ( this.validationRecord.caseSensitive() ) {
				return ValidationUtil.isValidMatch( StringCaster.cast( this.value ), this.validationRecord.pattern() );
			} else {
				return ValidationUtil.isValidMatchNoCase( StringCaster.cast( this.value ), this.validationRecord.pattern() );
			}
		}

		// Is this a type validation
		if ( this.validationRecord.isType() ) {
			return ( Boolean ) BoxRuntime.getInstance()
			    .getFunctionService()
			    .getGlobalFunction( Key.isValid )
			    .invoke(
			        BoxRuntime.getInstance().getRuntimeContext(),
			        new Object[] { this.validationRecord.type, this.value }, false, Key.isValid
			    );
		}

		return false;
	}

	/**
	 * If the attempt is valid, run the consumer
	 * This is useful for side effects
	 * If the attempt is empty and invalid, the consumer is not run
	 *
	 * @param action The action to run if the attempt is valid
	 *
	 * @return The attempt
	 */
	public Attempt<T> ifValid( Consumer<? super T> action ) {
		Objects.requireNonNull( action );
		if ( isValid() ) {
			action.accept( this.value );
		}
		return this;
	}

	/**
	 * If the attempt is invalid, run the consumer
	 * This is useful for side effects
	 * If the attempt is empty and invalid, the consumer is not run
	 *
	 * @param action The action to run if the attempt is invalid
	 *
	 * @return The attempt
	 */
	public Attempt<T> ifInvalid( Consumer<? super T> action ) {
		Objects.requireNonNull( action );
		if ( !isValid() ) {
			action.accept( this.value );
		}
		return this;
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Fluent methods
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Get the value of the attempt
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt
	 */
	public T get() {
		if ( isPresent() ) {
			return this.value;
		}
		throw new NoElementException( "Attempt is empty" );
	}

	/**
	 * Alias to get() so it's more functional on what it does
	 *
	 * @return The value of the attempt
	 *
	 * @throws NoElementException If the attempt is empty
	 */
	public T getOrFail() {
		return get();
	}

	/**
	 * Verifies if the attempt is empty or not using the following rules:
	 * - If the value is null, it is empty
	 * - If the value is a truthy/falsey value, evaluate it
	 */
	public boolean isEmpty() {
		return !isPresent();
	}

	/**
	 * Verifies if the value is null or not
	 *
	 * @return True if the attempt is null
	 */
	public boolean isNull() {
		return this.value == null;
	}

	/**
	 * Functional alias to {@link #isEmpty()}
	 *
	 * @return True if the attempt is empty
	 */
	public boolean ifFailed() {
		return isEmpty();
	}

	/**
	 * Return {@code true} if there is a value present, otherwise {@code false}.
	 * This is an alias to {@link #isPresent()} for functional programming
	 *
	 * @return {@code true} if there is a value present, otherwise {@code false}
	 */
	public boolean wasSuccessful() {
		return isPresent();
	}

	/**
	 * Verifies if the attempt is empty or not using the following rules:
	 * - If the value is null, it is empty
	 * - If the value is castable to BoxLang Boolean, evaluate it
	 * - If the value is an object, it is not empty
	 */
	public boolean isPresent() {

		// If null, we have our answer already
		if ( this.value == null ) {
			return false;
		}

		// If we have a simple eval, we are done
		if ( this.simpleEval ) {
			return true;
		}

		// Verify truthy/falsey values
		var castAttempt = BooleanCaster.attempt( this.value );
		if ( castAttempt.wasSuccessful() ) {
			return castAttempt.get();
		}

		// Else we have an object
		return true;
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise
	 * does nothing.
	 *
	 * @param action The action to perform
	 */
	public Attempt<T> ifPresent( Consumer<? super T> action ) {
		Objects.requireNonNull( action );
		if ( this.isPresent() ) {
			action.accept( this.value );
		}
		return this;
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise
	 * does nothing. Alias to {@link #ifPresent}
	 *
	 * @param action The action to perform
	 */
	public Attempt<T> ifSuccessful( Consumer<? super T> action ) {
		return ifPresent( action );
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise
	 * performs the given empty-based action.
	 */
	public Attempt<T> ifPresentOrElse( Consumer<? super T> action, Runnable emptyAction ) {
		Objects.requireNonNull( action );
		Objects.requireNonNull( emptyAction );
		if ( this.isPresent() ) {
			action.accept( this.value );
		} else {
			emptyAction.run();
		}
		return this;
	}

	/**
	 * If the attempt is NOT present, run the consumer
	 *
	 * @param consumer The consumer to run
	 */
	public Attempt<T> ifEmpty( Runnable consumer ) {
		Objects.requireNonNull( consumer );
		if ( this.isEmpty() ) {
			consumer.run();
		}
		return this;
	}

	/**
	 * If a value is present, returns the Attempt,
	 * otherwise returns an Attempt produced by the supplying function.
	 *
	 * @param supplier The supplier to run if the attempt is empty
	 */
	public Attempt<T> or( Supplier<Attempt<T>> supplier ) {
		Objects.requireNonNull( supplier );
		if ( this.isEmpty() ) {
			return supplier.get();
		}
		return this;
	}

	/**
	 * If a value is present, returns the value, otherwise returns the other passed
	 * value passed
	 *
	 * @param other The value to return if the attempt is empty
	 *
	 * @return The value of the attempt or the value passed in
	 */
	public T orElse( T other ) {
		if ( this.isEmpty() ) {
			return other;
		}
		return this.value;
	}

	/**
	 * Alias to `orElse` but more fluent
	 *
	 * @param other The value to return if the attempt is empty
	 *
	 * @return The value of the attempt or the value passed in
	 */
	public T getOrDefault( T other ) {
		return orElse( other );
	}

	/**
	 * If a value is present, returns the value, otherwise returns the result
	 * produced by the supplying function.
	 *
	 * @param supplier The supplier to run if the attempt is empty
	 *
	 * @return The value of the attempt or the value of the supplier
	 */
	public T orElseGet( Supplier<? extends T> supplier ) {
		Objects.requireNonNull( supplier );
		if ( isEmpty() ) {
			return supplier.get();
		}
		return this.value;
	}

	/**
	 * Alias to `orElseGet` but more fluent
	 *
	 * @param other The value to return if the attempt is empty
	 *
	 * @return The value of the attempt or the value passed in
	 */
	public T getOrSupply( Supplier<? extends T> other ) {
		return orElseGet( other );
	}

	/**
	 * Map the attempt to a new value with a supplier
	 *
	 * @param mapper The mapper to map the attempt to
	 *
	 * @return The new attempt
	 */
	public <U> Attempt<U> map( java.util.function.Function<? super T, ? extends U> mapper ) {
		Objects.requireNonNull( mapper );
		if ( isEmpty() ) {
			return empty();
		}

		return of( mapper.apply( this.value ) );
	}

	/**
	 * If a value is present, returns the result of applying the given
	 * {@code Attempt}-bearing mapping function to the value, otherwise returns
	 * an empty {@code Attempt}.
	 *
	 * <p>
	 * This method is similar to {@link #map(Function)}, but the mapping
	 * function is one whose result is already an {@code Attempt}, and if
	 * invoked, {@code flatMap} does not wrap it within an additional
	 * {@code Attempt}.
	 *
	 * @param <U>    The type of value of the {@code Attempt} returned by the
	 *               mapping function
	 * @param mapper the mapping function to apply to a value, if present
	 *
	 * @return the result of applying an {@code Attempt}-bearing mapping
	 *         function to the value of this {@code Attempt}, if a value is
	 *         present, otherwise an empty {@code Attempt}
	 */
	@SuppressWarnings( "unchecked" )
	public <U> Attempt<U> flatMap( java.util.function.Function<? super T, ? extends Attempt<? extends U>> mapper ) {
		Objects.requireNonNull( mapper );
		if ( isEmpty() ) {
			return empty();
		}
		Attempt<U> r = ( Attempt<U> ) mapper.apply( this.value );
		return Objects.requireNonNull( r );
	}

	/**
	 * If a value is present, returns the value, otherwise throws a
	 * NoElementException
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt if present
	 */
	public T orThrow() {
		return orThrow( "Attempt is empty" );
	}

	/**
	 * If a value is present, returns the value, otherwise throws a BoxLang exception with the
	 * provided type and message
	 *
	 * @param type    The type of the exception to throw
	 * @param message The message to display
	 *
	 * @return The value of the attempt if present or throws an exception
	 */
	public T orThrow( String type, String message ) {
		if ( isEmpty() ) {
			orThrow( new CustomException(
			    message,
			    "",
			    "",
			    type,
			    new Struct(),
			    null
			) );
		}
		return this.value;
	}

	/**
	 * If a value is present, returns the value, otherwise throws a
	 * NoElementException with
	 * a custom message
	 *
	 * @param message The message to display
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt if present
	 */
	public T orThrow( String message ) {
		if ( isEmpty() ) {
			throw new NoElementException( message );
		}
		return this.value;
	}

	/**
	 * If a value is present, returns the value, otherwise throws a provided
	 *
	 * @param message   The message to display
	 * @param throwable The exception to throw if the attempt is empty
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt if present
	 */
	public T orThrow( Exception throwable ) {
		if ( isEmpty() ) {
			try {
				throw throwable;
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Can't throw exception", e );
			}
		}
		return this.value;
	}

	/**
	 * If a value is present, returns a sequential Stream containing only that
	 * value, otherwise returns an empty Stream.
	 */
	public Stream<T> stream() {
		return isEmpty() ? Stream.empty() : Stream.of( this.value );
	}

	/**
	 * Returns the string representation of the value
	 *
	 * @return The string representation of the value if any, else empty string
	 */
	public String toString() {
		return isEmpty() ? "Attempt.empty" : String.format( "Attempt[%s]", this.value.toString() );
	}

	/**
	 * Hash code of the value
	 */
	@Override
	public int hashCode() {
		return isEmpty() ? 0 : Objects.hashCode( this.value );
	}

	/**
	 * Equals method
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}

		if ( obj instanceof Attempt<?> castedAttempt ) {
			// If both are empty, they are equal
			if ( isEmpty() && castedAttempt.isEmpty() ) {
				return true;
			}

			// If one is empty and the other is not, they are not equal
			if ( isEmpty() || castedAttempt.isEmpty() ) {
				return false;
			}

			// If both are not empty, compare the values
			return get().equals( castedAttempt.get() );
		}

		// If we are empty and the incoming object is null, they are equal
		if ( isEmpty() && obj == null ) {
			return true;
		}

		// If we are not empty and the incoming object is null, they are not equal
		if ( isEmpty() && obj != null ) {
			return false;
		}

		// Else evaluate this.value to incoming object
		return get().equals( obj );
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns an
	 * Attempt describing the value, otherwise returns an empty Attempt.
	 *
	 * @param predicate The predicate to test the value
	 *
	 * @return The attempt if the predicate is true, else an empty attempt
	 */
	public Attempt<T> filter( Predicate<? super T> predicate ) {
		if ( isEmpty() ) {
			return empty();
		}

		if ( predicate.test( this.value ) ) {
			return this;
		}

		return empty();
	}

	/**
	 * Set the attempt to a simple evaluation
	 *
	 * @param eval True if the attempt is a simple evaluation, false otherwise
	 */
	public Attempt<T> setSimpleEval( Boolean eval ) {
		this.simpleEval = eval;
		return this;
	}

}
