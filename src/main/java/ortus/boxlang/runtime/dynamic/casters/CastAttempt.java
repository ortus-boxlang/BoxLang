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

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A container object which may or may not contain the result of a cast attempt
 * This allows you to attempt a cast, getting the casted value if it was successfull, but not
 * throwing an exception if it fails. This prevents you from having to run the casting logic twice
 * to check if a value is castable and then actually cast it. Instead, you can combine those
 * steps into one and still do something else if the casting was not possible.
 */
public final class CastAttempt<T> {

	private static final CastAttempt<?>	EMPTY	= new CastAttempt<>();

	/**
	 * If non-null, the value; if null, indicates no value is present
	 */
	private final T						value;

	/**
	 * Constructs an empty instance.
	 */
	private CastAttempt() {
		this.value = null;
	}

	/**
	 * Returns an empty instance. No value is present for this
	 * CastAttempt.
	 *
	 * @param <T> Type of the non-existent value
	 *
	 * @return an empty {@code CastAttempt}
	 */
	public static <T> CastAttempt<T> empty() {
		@SuppressWarnings( "unchecked" )
		CastAttempt<T> t = ( CastAttempt<T> ) EMPTY;
		return t;
	}

	/**
	 * Constructs an instance with the value present.
	 *
	 * @param value the non-null value to be present
	 */
	private CastAttempt( T value ) {
		this.value = Objects.requireNonNull( value );
	}

	/**
	 * Returns an {@code CastAttempt} with the specified present non-null value.
	 *
	 * @param <T>   the class of the value
	 * @param value the value to be present, which must be non-null
	 *
	 * @return an {@code CastAttempt} with the value present
	 */
	public static <T> CastAttempt<T> of( T value ) {
		return new CastAttempt<>( value );
	}

	/**
	 * Returns an {@code CastAttempt} describing the specified value, if non-null,
	 * otherwise returns an empty {@code CastAttempt}.
	 *
	 * @param <T>   the class of the value
	 * @param value the possibly-null value to describe
	 *
	 * @return an {@code CastAttempt} with a present value if the specified value
	 *         is non-null, otherwise an empty {@code CastAttempt}
	 */
	public static <T> CastAttempt<T> ofNullable( T value ) {
		return value == null ? empty() : of( value );
	}

	/**
	 * If a value is present in this {@code CastAttempt}, returns the value,
	 * otherwise throws Exception
	 *
	 * @return the non-null value held by this {@code CastAttempt}
	 *
	 * @see CastAttempt#wasSuccessful()
	 */
	public T get() {
		if ( value == null ) {
			throw new BoxRuntimeException( "The cast was not successful.  You cannot get the value." );
		}
		return value;
	}

	/**
	 * Return {@code true} if there is a value present, otherwise {@code false}.
	 *
	 * @return {@code true} if there is a value present, otherwise {@code false}
	 */
	public boolean wasSuccessful() {
		return value != null;
	}

	/**
	 * If a value is present, invoke the specified consumer with the value,
	 * otherwise do nothing.
	 *
	 * @param consumer block to be executed if a value is present
	 *
	 * @return this {@code CastAttempt}
	 *
	 * @throws NullPointerException if value is present and {@code consumer} is
	 *                              null
	 */
	public CastAttempt<T> ifSuccessful( Consumer<? super T> consumer ) {
		if ( value != null )
			consumer.accept( value );

		return this;
	}

	/**
	 * Return the value if present, otherwise return {@code other}.
	 *
	 * @param other the value to be returned if there is no value present, may
	 *              be null
	 *
	 * @return the value, if present, otherwise {@code other}
	 */
	public T getOrDefault( T other ) {
		return value != null ? value : other;
	}

	/**
	 * Return the value if present, otherwise invoke {@code other} and return
	 * the result of that invocation.
	 *
	 * @param other a {@code Supplier} whose result is returned if no value
	 *              is present
	 *
	 * @return the value if present otherwise the result of {@code other.get()}
	 *
	 * @throws NullPointerException if value is not present and {@code other} is
	 *                              null
	 */
	public T getOrSupply( Supplier<? extends T> other ) {
		return value != null ? value : other.get();
	}

	/**
	 * @return The contained value, if present, otherwise throw an exception
	 */
	public T getOrFail() {
		if ( value != null ) {
			return value;
		} else {
			throw new BoxRuntimeException( "Value could not be cast." );
		}
	}

	/**
	 * Returns a non-empty string representation of this CastAttempt suitable for
	 * debugging. The exact presentation format is unspecified and may vary
	 * between implementations and versions.
	 *
	 * @return the string representation of this instance
	 */
	@Override
	public String toString() {
		return value != null
		    ? String.format( "CastAttempt[%s]", value )
		    : "CastAttempt.empty";
	}
}
