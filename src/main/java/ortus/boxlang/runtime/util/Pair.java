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
package ortus.boxlang.runtime.util;

/**
 * A simple immutable pair class for holding two related objects.
 * <p>
 * This is a utility class used throughout BoxLang for returning or storing two values together.
 * The pair is typed, and both elements can be of any type.
 * </p>
 *
 * @param <T> The type of the first element
 * @param <U> The type of the second element
 */
public class Pair<T, U> {

	private final T	first;
	private final U	second;

	/**
	 * Constructs a new immutable Pair with the given values.
	 *
	 * @param first  The first value
	 * @param second The second value
	 */
	public Pair( T first, U second ) {
		this.first	= first;
		this.second	= second;
	}

	/**
	 * Static factory method to create a new Pair.
	 *
	 * @param <T>    The type of the first element
	 * @param <U>    The type of the second element
	 * @param first  The first value
	 * @param second The second value
	 *
	 * @return A new Pair containing the given values
	 */
	public static <T, U> Pair<T, U> of( T first, U second ) {
		return new Pair<>( first, second );
	}

	/**
	 * Returns the first value in the pair.
	 *
	 * @return The first value
	 */
	public T getFirst() {
		return first;
	}

	/**
	 * Returns the second value in the pair.
	 *
	 * @return The second value
	 */
	public U getSecond() {
		return second;
	}

	/**
	 * Returns a string representation of the pair in the form (first, second).
	 *
	 * @return String representation of the pair
	 */
	@Override
	public String toString() {
		return "(" + first + ", " + second + ")";
	}
}