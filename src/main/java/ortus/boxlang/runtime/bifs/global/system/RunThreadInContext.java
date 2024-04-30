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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

@BoxBIF
public class RunThreadInContext extends BIF {

	/**
	 * Constructor
	 */
	public RunThreadInContext() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.applicationName ),
		    new Argument( false, Argument.STRING, Key.sessionId ),
		    new Argument( false, Argument.ANY, Key.context ),
		    new Argument( true, Argument.FUNCTION, Key.callback )
		};
	}

	/**
	 * Executes the code in the callback with a speciec parent context so a specific application name
	 * is visible and an optional sessionID, or a specific context can be provided from another request.
	 * If context is provided, application name and sessionID are not allowed.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.applicationName The application name to use run the code in.
	 * 
	 * @argument.sessionId The sessionID to use to run the code in (requires application name).
	 * 
	 * @argument.context The context to use to run the code in. Mututally exclusive with applicationName and sessionId.
	 * 
	 * @argument.callback The function to run in the new context.
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Function	callback		= arguments.getAsFunction( Key.callback );
		String		applicationName	= arguments.getAsString( Key.applicationName );
		String		sessionId		= arguments.getAsString( Key.sessionId );
		Object		objContext		= arguments.get( Key.context );
		IBoxContext	newContext		= null;
		if ( objContext != null ) {
			if ( objContext instanceof IBoxContext ctx ) {
				newContext = ctx;
			} else {
				throw new BoxValidationException( "Context must be instance of IBoxContext." );
			}
			if ( applicationName != null || sessionId != null ) {
				throw new BoxValidationException( "Context and applicationName/sessionId are mutually exclusive." );
			}
		} else if ( applicationName != null ) {
			ScriptingRequestBoxContext scriptingRequestContext = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
			newContext = scriptingRequestContext;
			var appSettings = Struct.of(
			    Key._NAME, applicationName
			);
			if ( sessionId != null ) {
				// This will associate (and possibly create) the session with this sessionID
				scriptingRequestContext.setSessionID( Key.of( sessionId ) );
				appSettings.put( Key.sessionManagement, true );
			}
			// This will associate (and possibly create) the application
			scriptingRequestContext.getApplicationListener().updateSettings( appSettings );
		} else {
			throw new BoxValidationException( "Context or applicationName is required." );
		}

		newContext.invokeFunction( callback );

		return null;
	}
}
