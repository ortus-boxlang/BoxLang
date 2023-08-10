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
package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.*;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * This context represents the context of a template execution in BoxLang
 */
public class TemplateContext extends BaseContext {

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
	 * The template that this execution context is bound to
	 */
	private String templatePath = null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template
	 *
	 * @param templatePath The template that this execution context is bound to
	 */
	public TemplateContext( String templatePath ) {
		this.templatePath	= templatePath;
		this.name			= "template";
	}

	/**
	 * Creates a new template context with no bounded template
	 */
	public TemplateContext() {
		this.name = "template";
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set the template path of execution
	 *
	 * @param templatePath The template that this execution context is bound to
	 *
	 * @return ExecutionContext
	 */
	public TemplateContext setTemplatePath( String templatePath ) {
		this.templatePath = templatePath;
		return this;
	}

	/**
	 * Get the template path of execution
	 *
	 * @return The template that this execution context is bound to
	 */
	public String getTemplatePath() {
		return this.templatePath;
	}

	/**
	 * Has the execution context been bound to a template?
	 *
	 * @return True if bound, else false
	 */
	public boolean hasTemplatePath() {
		return this.templatePath != null;
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order
	 *
	 * Here is the order for bx templates
	 * (Not all yet implemented and some will be according to platform: WebContext, AndroidContext, IOSContext, etc)
	 *
	 * 1. Query (only in query loops)
	 * 2. Thread
	 * 3. Variables
	 * 4. CGI (should it exist in the core runtime?)
	 * 5. CFFILE
	 * 6. URL (Only for web runtime)
	 * 7. FORM (Only for web runtime)
	 * 8. COOKIE (Only for web runtime)
	 * 9. CLIENT (Only for web runtime)
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 * @throws KeyNotFoundException If the key was not found in any scope
	 */
	@Override
	public Object scopeFind( Key key ) {

		// In query loop?

		// In Thread scope?

		// In Variables scope?
		if ( getVariablesScope().containsKey( key ) ) {
			return getVariablesScope().get( key );
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		        String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

}
