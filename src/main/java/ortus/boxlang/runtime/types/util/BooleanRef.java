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
package ortus.boxlang.runtime.types.util;

/**
 * I am a simple wrapper for a Boolean value which allows it to be passed by reference into a method, possibly modified in that method, and then the
 * modified Boolean value accessed without needing to return it from the method.
 * 
 */
public class BooleanRef {

	/**
	 * The Boolean value being wrapped
	 */
	private Boolean value;

	/**
	 * Constructor
	 * 
	 * @param value The Boolean value to wrap
	 */
	public BooleanRef( Boolean value ) {
		this.value = value;
	}

	/**
	 * Factory method to create a new BooleanRef
	 * 
	 * @param value The Boolean value to wrap
	 * 
	 * @return A new BooleanRef wrapping the Boolean value
	 */
	public static BooleanRef of( Boolean value ) {
		return new BooleanRef( value );
	}

	/**
	 * Get the Boolean value
	 */
	public Boolean get() {
		return value;
	}

	/**
	 * Set the value
	 * 
	 * @param value The new value
	 */
	public void set( Boolean value ) {
		this.value = value;
	}

}
