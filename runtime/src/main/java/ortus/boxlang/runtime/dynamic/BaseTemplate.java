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

import java.time.LocalDateTime;

import ortus.boxlang.runtime.context.IBoxContext;

/// import ortus.boxlang.runtime.core.Derefrencer;

public class BaseTemplate {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	public String			name;
	public String			extension;
	public String			path;
	public LocalDateTime	lastModified;
	public LocalDateTime	compiledOn;

	// public ??? ast;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invoke the template
	 *
	 * @param context The context to invoke the template with
	 */
	public void invoke( IBoxContext context ) {
		throw new UnsupportedOperationException( "This method must be overridden as a static method." );
	}
}
