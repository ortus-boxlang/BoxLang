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
 * Represents an execution context. May be subclassed for more specific contexts such as servlet.
 * Each thread/request has a new execution context and may share the same BoxRuntime instance.
 *
 * This is the core execution context that is used by the runtime to execute a template or class via the CLI
 */
public class ExecutionContext {

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
	 * A template has a variables scope
	 */
	private IScope	variablesScope;

	/**
	 * The template that this execution context is bound to
	 */
	private String	templatePath	= null;

	// Also, should variables, this, local, arguments live here, or in the associated page or component they belong to, which in turn, gets associated
	// here?
	// Should the non-web context have even server, session, or application, or would a pure boxlang context only know about local, arguments, variables,
	// and this?
	// Decisions, decisions...

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
	public ExecutionContext( String templatePath ) {
		this.templatePath = templatePath;
	}

	/**
	 * Creates a new execution context with no bounded template
	 */
	public ExecutionContext() {
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set the template path of execution
	 *
	 * @param templatePath The template that this execution context is bound to
	 *
	 * @return ExecutionContext
	 */
	public ExecutionContext setTemplatePath( String templatePath ) {
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
	 * Get the variables scope of the template
	 *
	 * @return The variables scope of the template
	 */
	public IScope getVariablesScope() {
		return this.variablesScope;
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
