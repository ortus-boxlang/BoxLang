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
package ortus.boxlang.runtime;

import ortus.boxlang.runtime.dynamic.ITemplate;
import ortus.boxlang.runtime.dynamic.MockTemplate;

public class BoxPiler {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static BoxPiler instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private BoxPiler() {
		// Any initialization code can be placed here
		getInstance();
	}

	/**
	 * Get an instance of the BoxPiler
	 *
	 * @return The BoxPiler instance
	 */
	public static synchronized BoxPiler getInstance() {
		if ( instance == null ) {
			instance = new BoxPiler();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Parse a template into an executable dynamic ITemplate object
	 *
	 * @param templatePath The path to the template
	 *
	 * @return The parsed template class
	 */
	public static Class<? extends ITemplate> parse( String templatePath ) {

		// TODO: Check if the template is in cache (local or ram)

		// If not, discover it, parse it, ast it and cache it
		// TODO: Parse the template
		// TODO: AST the template
		// TODO: Cache the template

		// Return the template
		return MockTemplate.class;
	}

}
