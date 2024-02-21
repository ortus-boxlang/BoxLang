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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.ClosureBoxContext;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class DebugBoxContexts extends BIF {

	/**
	 * Constructor
	 */
	public DebugBoxContexts() {
		super();
	}

	/**
	 * A debug BIF that dumps out the current context hierarchy
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array		var	= new Array();
		IBoxContext	c	= context;
		while ( c != null ) {
			var.add( 0, generateContextData( c ) );
			c = c.getParent();
		}
		functionService.getGlobalFunction( Key.dump ).invoke( context, new Object[] { var }, false, Key.dump );
		return null;
	}

	private Object generateContextData( IBoxContext c ) {
		var templates = new Array();
		for ( var t : c.getTemplates() ) {
			templates.add( 0, t.toString() );
		}
		return Struct.linkedOf(
		    "name", c.getClass().getSimpleName(),
		    "templates", templates,
		    "function",
		    ( c instanceof FunctionBoxContext fbc ? fbc.getFunction().getName() + "() - " + fbc.getFunction().getClass().getSuperclass().getSimpleName()
		        : "N/A" ),
		    "isInClass", ( c instanceof FunctionBoxContext fbc ? fbc.isInClass() : "N/A" ),
		    "declaringContext", ( c instanceof ClosureBoxContext cbc ? generateContextData( cbc.getFunction().getDeclaringContext() ) : "N/A" )
		);
	}
}
