/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.jdbc;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class PreserveSingleQuotes extends BIF {

	/**
	 * Constructor
	 */
	public PreserveSingleQuotes() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "String", Key.variable )
		};
	}

	/**
	 * Prevents from automatically escaping single quotation mark characters that are contained in a variable.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.variable The expression that needs to have its single quotes preserved.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// This BIF itself doesn't do anything. It's largely a placeholder in the AST for the QueryPreserveSingleQuotesVisitor to look for.
		// The Visitor actually does the work of escaping or preserving single quotes.
		// System.out.println( "PreserveSingleQuotes._invoke " + arguments.get( Key.variable ) );
		return arguments.get( Key.variable );
	}

}
