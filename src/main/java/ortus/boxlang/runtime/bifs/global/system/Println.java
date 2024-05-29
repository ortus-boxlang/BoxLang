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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IType;

@BoxBIF
public class Println extends BIF {

	/**
	 * Constructor
	 */
	public Println() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.message )
		};
	}

	/**
	 * Print a message with line break to the console
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.message The message to print
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object obj = arguments.get( Key.message );

		// If it's a BoxLang type, let's use the string representation
		if ( obj instanceof IType t ) {
			obj = t.asString();
		}

		// If we have a request context, let's use that context's out
		RequestBoxContext rCon = context.getParentOfType( RequestBoxContext.class );
		if ( rCon != null ) {
			rCon.getOut().println( obj );
			return null;
		}

		// Fallback in case we're not in a request
		System.out.println( obj );

		return null;
	}
}
