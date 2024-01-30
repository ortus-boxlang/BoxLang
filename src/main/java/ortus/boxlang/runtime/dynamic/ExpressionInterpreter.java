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

import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.web.scopes.CGIScope;
import ortus.boxlang.web.scopes.CookieScope;
import ortus.boxlang.web.scopes.FormScope;
import ortus.boxlang.web.scopes.URLScope;

/**
 * I handle interpreting expressions
 */
public class ExpressionInterpreter {

	// TODO: This should be dynamic from the runtime based on the registered modules
	private static Set<Key> scopes = Set.of(
	    VariablesScope.name,
	    ArgumentsScope.name,
	    ThisScope.name,
	    RequestScope.name,
	    SessionScope.name,
	    ApplicationScope.name,
	    ServerScope.name,
	    CookieScope.name,
	    FormScope.name,
	    URLScope.name,
	    // ThreadScope.name,
	    // ClientScope.name,
	    CGIScope.name
	);

	/**
	 * Resolve an expression pointing to a varaible in the format of foo, foo.bar, foo.bar.baz, etc.
	 * Only handles dot access at the moment.
	 * 
	 * @param context    The context
	 * @param expression The expression
	 * @param safe       Whether to throw an exception if the variable is not found
	 * 
	 * @return The expression found
	 */
	public static Object getVariable( IBoxContext context, String expression, boolean safe ) {
		if ( expression.isEmpty() || expression.startsWith( "." ) || expression.endsWith( "." ) ) {
			throw new ExpressionException( "Invalid expression", null, expression );
		}
		String[]	parts	= expression.toLowerCase().split( "\\." );
		Object		ref		= null;
		Key			refName	= Key.of( parts[ 0 ] );
		// Find root

		if ( scopes.contains( refName ) ) {
			ref = context.getScopeNearby( refName );
		} else {
			ref = context.scopeFindNearby( refName, ( safe ? context.getDefaultAssignmentScope() : null ) ).value();
			if ( ref == null && !safe ) {
				throw new KeyNotFoundException( "Variable [" + refName + "] not found." );
			}
		}
		// loop over remaining items
		for ( int i = 1; i < parts.length; i++ ) {
			ref = Referencer.get( context, ref, Key.of( parts[ i ] ), safe );
		}
		return ref;
	}
}