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

/**
 * Represents a UDF. A UDF is specifically a function that is defined with the "function name()" syntax.
 * UDFs have names, access, hints, etc which closures do not have.
 */
public abstract class UDF extends Function {

	/**
	 * Constructor
	 */
	protected UDF() {
		super();
	}

	/**
	 * Constructor that sets output
	 */
	protected UDF( boolean defaultOutput ) {
		super( defaultOutput );
	}

}
