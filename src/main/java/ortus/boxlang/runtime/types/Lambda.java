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

import ortus.boxlang.runtime.scopes.Key;

/**
 * Represents a Lambda, which is a function, but has less data than a UDF and performs NO scope lookups outside of itself.
 * Lambdas aim to be "pure" functions, by
 * - being deterministic (same inputs always produce the same output)
 * - having no side effects (no scope lookups outside of itself)
 * - being immutable (this requires you to pass immutable arguments to the lambda)
 */
public abstract class Lambda extends Function {

	public static final Key defaultName = Key.of( "Lambda" );

	/**
	 * Constructor
	 */
	public Lambda() {
		super();
	}

}
