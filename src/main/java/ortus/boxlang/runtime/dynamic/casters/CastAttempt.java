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

import ortus.boxlang.runtime.dynamic.Attempt;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

/**
 * A container object which may or may not contain the result of a cast attempt
 * This allows you to attempt a cast, getting the casted value if it was successfull, but not
 * throwing an exception if it fails. This prevents you from having to run the casting logic twice
 * to check if a value is castable and then actually cast it. Instead, you can combine those
 * steps into one and still do something else if the casting was not possible.
 */
public final class CastAttempt<T> extends Attempt<T> {

	private static final CastAttempt<?> EMPTY = new CastAttempt<>();

	/**
	 * |--------------------------------------------------------------------------
	 * | Constructors
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Constructs an empty instance.
	 */
	private CastAttempt() {
		super();
	}

	/**
	 * Constructs an instance with the value present.
	 *
	 * @param value the non-null value to be present
	 */
	private CastAttempt( T value ) {
		super( Objects.requireNonNull( value ) );
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Static Builders
	 * |--------------------------------------------------------------------------
	 */

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
	 * Create an empty attempt
	 *
	 * @return An empty attempt
	 */
	@SuppressWarnings( "unchecked" )
	public static <T> CastAttempt<T> empty() {
		return ( CastAttempt<T> ) EMPTY;
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Overrides
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * If a value is present in this {@code CastAttempt}, returns the value,
	 * otherwise throws BoxCastException
	 *
	 * @return the non-null value held by this {@code CastAttempt}
	 *
	 * @throws BoxCastException if there is no value present
	 */
	@Override
	public T get() {
		if ( isPresent() ) {
			return this.value;
		}
		throw new BoxCastException( "The cast was not successful.  You cannot get the value." );
	}

	/**
	 * Verifies if the attempt is empty
	 *
	 * @return True if the attempt is empty, false otherwise
	 */
	@Override
	public boolean isPresent() {
		return this.value != null;
	}

}
