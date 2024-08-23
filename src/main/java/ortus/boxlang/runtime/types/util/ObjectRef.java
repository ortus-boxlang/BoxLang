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
 * I am a simple wrapper for a value which allows it to be passed by reference into a method, possibly modified in that method, and then the
 * modified value accessed without needing to return it from the method.
 * 
 */
public class ObjectRef {

	/**
	 * The value being wrapped
	 */
	private Object value;

	/**
	 * Constructor
	 * 
	 * @param value The value to wrap
	 */
	public ObjectRef( Object value ) {
		this.value = value;
	}

	/**
	 * Factory method to create a new ObjectRef
	 * 
	 * @param value The value to wrap
	 * 
	 * @return A new ObjectRef wrapping the value
	 */
	public static ObjectRef of( Object value ) {
		return new ObjectRef( value );
	}

	/**
	 * Get the value
	 */
	public Object get() {
		return value;
	}

	/**
	 * Set the value
	 * 
	 * @param value The new value
	 */
	public void set( Object value ) {
		this.value = value;
	}

	/**
	 * Helper method for bytecode that needs to represent a literal value as an expression
	 * which Java will accept as a statement.
	 * 
	 * @param o The value to return
	 * 
	 * @return The value
	 */
	public static Object echoValue( Object o ) {
		return o;
	}

}
