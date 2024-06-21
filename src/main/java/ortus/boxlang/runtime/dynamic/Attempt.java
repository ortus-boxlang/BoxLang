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

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.NoElementException;

/**
 * This class is a fluent class inspired by Java optionals. It allows for a more
 * fluent way to handle
 * truthy and falsey values in BoxLang with functional aspects.
 *
 * You can also seed it with a Function (closure or lambda) that will be
 * executed when the value is requested.
 * This gives you a delayed attempt.
 */
public class Attempt {

	/**
	 * |--------------------------------------------------------------------------
	 * | Private properties
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * The target value to evaluate
	 * This can be a truthy or falsey value or a Function instance to execute, or a
	 * IClassRunnable instance
	 */
	private Object				value;

	/**
	 * The context that requested the attempt
	 */
	private IBoxContext			context;

	/**
	 * Register validation function
	 */
	private Predicate<Object>	validationFunction;

	/**
	 * Validation Record
	 */
	private ValidationRecord	validationRecord;

	/**
	 * |--------------------------------------------------------------------------
	 * | Constructors
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Constructor for an empty attempt
	 */
	public Attempt() {
		this.value		= null;
		this.context	= null;
	}

	/**
	 * Constructor for an attempt with the incoming value
	 */
	public Attempt( Object value ) {
		this.value		= value;
		this.context	= null;
	}

	/**
	 * Constructor for a delayed attempt using a function and the executing context
	 *
	 * @param context The context that requested the attempt using a Function
	 * @param value   The function to register for delayed execution
	 */
	public Attempt( IBoxContext context, Object value ) {
		this.value		= value;
		this.context	= context;
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Static Builders
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Create an attempt from a delayed attempt using a function and the executing
	 * context
	 *
	 * @param context The context that requested the attempt using a Function
	 * @param value   The value of the attempt, truthy or falsey
	 *
	 * @return Attempt
	 */
	public static Attempt ofFunction( IBoxContext context, Object value ) {
		return new Attempt( context, value );
	}

	/**
	 * Create an attempt from a value
	 *
	 * @param value The value of the attempt, truthy or falsey
	 *
	 * @return Attempt
	 */
	public static Attempt of( Object value ) {
		return new Attempt( value );
	}

	/**
	 * Create an empty attempt
	 *
	 * @return An empty attempt
	 */
	public static Attempt empty() {
		return new Attempt();
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Fluent methods
	 * |--------------------------------------------------------------------------
	 */

	private record ValidationRecord( String type, Double min, Double max, String pattern ) {
	}

	public Attempt toMatchRegex( String pattern ) {
		this.validationRecord = new ValidationRecord( "regex", null, null, pattern );
		return this;
	}

	public Attempt toBeBetween( Double min, Double max ) {
		this.validationRecord = new ValidationRecord( "range", min, max, null );
		return this;
	}

	public Attempt toBeValidType( String type ) {
		this.validationRecord = new ValidationRecord( type, null, null, null );
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
	public Attempt toBeValid( Predicate<Object> predicate ) {
		this.validationFunction = predicate;
		return this;
	}

	/**
	 * Verifies if the attempt is valid or not
	 *
	 * @return True if the attempt is valid
	 */
	public boolean isValid() {
		// Verify if present first
		if ( isEmpty() ) {
			return false;
		}

		if ( this.validationFunction != null ) {
			return this.validationFunction.test( this.value );
		}

		return false;
	}

	/**
	 * Checks if the context is set or not
	 *
	 * @return True if the context is set
	 */
	public boolean hasContext() {
		return this.context != null;
	}

	/**
	 * Set the context of execution. This could be needed
	 * depending on the execution context of the attempt
	 *
	 * @param context The context to set
	 *
	 * @return The attempt
	 */
	public Attempt setContext( IBoxContext context ) {
		this.context = context;
		return this;
	}

	/**
	 * Checks if the value is a BoxLang Function
	 *
	 * @return True if the value is a BoxLang Function
	 */
	public boolean isFunction() {
		return this.value instanceof Function;
	}

	/**
	 * Get the value of the attempt
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt
	 */
	public Object get() {
		if ( isPresent() ) {
			return this.value;
		}
		throw new NoElementException( "Attempt is empty" );
	}

	/**
	 * Verifies if the attempt is empty or not using the following rules:
	 * - If the value is a function, execute it and set the value to the result
	 * - If the value is null, it is empty
	 * - If the value is a truthy/falsey value, evaluate it
	 */
	public boolean isEmpty() {
		return !isPresent();
	}

	/**
	 * Verifies if the attempt is empty or not using the following rules:
	 * - If the value is a function, execute it and set the value to the result
	 * - If the value is null, it is empty
	 * - If the value is a truthy/falsey value, evaluate it
	 */
	public boolean isPresent() {
		// Check if the value is a function, if so, execute it
		// and set the value to the result, this is a delayed execution
		if ( this.value instanceof Function castedFunction ) {
			this.value = this.context.invokeFunction( castedFunction );
		}

		// If null, we have our answer already
		if ( this.value == null ) {
			return false;
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
	public Attempt ifPresent( Consumer<Object> action ) {
		if ( this.isPresent() ) {
			action.accept( this.value );
		}
		return this;
	}

	/**
	 * If a value is present, performs the given action with the value, otherwise
	 * performs the given empty-based action.
	 */
	public Attempt ifPresentOrElse( Consumer<Object> action, Runnable emptyAction ) {
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
	public Attempt ifEmpty( Runnable consumer ) {
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
	public Attempt or( Supplier<Attempt> supplier ) {
		if ( this.isEmpty() ) {
			return supplier.get();
		}
		return this;
	}

	/**
	 * If a value is present, returns the value, otherwise returns the other passed
	 * value
	 *
	 * @param other The value to return if the attempt is empty
	 *
	 * @return The value of the attempt or the value passed in
	 */
	public Object orElse( Object other ) {
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
	public Object getOrDefault( Object other ) {
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
	public Object orElseGet( Supplier<Object> supplier ) {
		if ( this.isEmpty() ) {
			return supplier.get();
		}
		return this.value;
	}

	/**
	 * Map the attempt to a new value with a supplier
	 *
	 * @param mapper The mapper to map the attempt to
	 *
	 * @return The new attempt
	 */
	public Attempt map( java.util.function.UnaryOperator<Object> mapper ) {
		if ( this.isEmpty() ) {
			return hasContext() ? new Attempt( this.context, null ) : new Attempt();
		}

		return new Attempt( this.context, mapper.apply( this.value ) );
	}

	/**
	 * If a value is present, returns the value, otherwise throws a
	 * NoElementException
	 *
	 * @throws NoElementException If the attempt is empty
	 *
	 * @return The value of the attempt if present
	 */
	public Object orThrow() {
		return orThrow( "Attempt is empty" );
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
	public Object orThrow( String message ) {
		if ( this.isEmpty() ) {
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
	public Object orThrow( Exception throwable ) {
		if ( this.isEmpty() ) {
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
	public Stream<Object> stream() {
		return isEmpty() ? Stream.empty() : Stream.of( this.value );
	}

	/**
	 * Returns the string representation of the value
	 *
	 * @return The string representation of the value if any, else empty string
	 */
	public String toString() {
		return isEmpty() ? "Attempt.empty" : "Attempt[" + this.value.toString() + "]";
	}

	/**
	 * Hash code of the value
	 */
	@Override
	public int hashCode() {
		return isEmpty() ? 0 : this.value.hashCode();
	}

	/**
	 * Equals method
	 */
	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}

		if ( obj instanceof Attempt castedAttempt ) {
			// If both are empty, they are equal
			if ( this.isEmpty() && castedAttempt.isEmpty() ) {
				return true;
			}

			// If one is empty and the other is not, they are not equal
			if ( this.isEmpty() || castedAttempt.isEmpty() ) {
				return false;
			}

			// If both are not empty, compare the values
			return get().equals( castedAttempt.get() );
		}

		// If we are empty and the incoming object is null, they are equal
		if ( this.isEmpty() && obj == null ) {
			return true;
		}

		// If we are not empty and the incoming object is null, they are not equal
		if ( this.isEmpty() && obj != null ) {
			return false;
		}

		// Else evaluate this.value to incoming object
		return get().equals( obj );
	}

	/**
	 * If a value is present, and the value matches the given predicate, returns an
	 * Optional describing the value, otherwise returns an empty Optional.
	 *
	 * @param predicate The predicate to test the value
	 *
	 * @return The attempt if the predicate is true, else an empty attempt
	 */
	public Attempt filter( Predicate<Object> predicate ) {
		if ( this.isEmpty() ) {
			return hasContext() ? new Attempt( this.context, null ) : new Attempt();
		}

		if ( predicate.test( this.value ) ) {
			return this;
		}

		return hasContext() ? new Attempt( this.context, null ) : new Attempt();
	}

}
