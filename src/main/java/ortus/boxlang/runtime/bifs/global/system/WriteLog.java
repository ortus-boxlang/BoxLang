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
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interceptors.Logging;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class WriteLog extends BIF {

	/**
	 * Constructor
	 */
	public WriteLog() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.text ),
		    new Argument( false, "string", Key.type, Logging.DEFAULT_LOG_LEVEL ),
		    new Argument( false, "boolean", Key.application, true ),
		    new Argument( false, "string", Key.file ),
		    new Argument( false, "string", Key.log, Logging.DEFAULT_LOG_TYPE ),
		};
	}

	/**
	 * Writes a log message out
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.text The text of the log message
	 *
	 * @argument.type The log level ( debug, info, warn, error )
	 *
	 * @argument.application If true, it logs the application name alongside the message. Default is true.
	 *
	 * @argument.file A custom log file to write to
	 *
	 * @argument.log If a custom file is not specified the log category to write to
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the application name
		ApplicationBoxContext appContext = context.getApplicationContext();
		// Set the application name if not null
		if ( appContext != null ) {
			arguments.put( Key.application, appContext.getApplication().getName() );
		}
		// Announce the log message
		interceptorService.announce( BoxEvent.LOG_MESSAGE, arguments );
		return null;
	}
}
