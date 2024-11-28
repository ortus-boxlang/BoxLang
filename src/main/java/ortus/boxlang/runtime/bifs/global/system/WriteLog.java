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
import ortus.boxlang.runtime.logging.LoggingService;
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
		    new Argument( false, "string", Key.type, LoggingService.DEFAULT_LOG_LEVEL ),
		    new Argument( false, "boolean", Key.application, true ),
		    new Argument( false, "string", Key.log )
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
	 * @argument.type The log level of the entry. One of "Information", "Warning", "Error", "Debug", "Trace"
	 *
	 * @argument.application If true, it logs the application name alongside the message. Default is true.
	 *
	 * @argument.log The destination logger to use. If not passed, we use the default logger (runtime.log).
	 *               If the logger is a file appender and it doesn't exist it will create it for you.
	 *               If the value is an absolue path, it will create a file appender for you at that location.
	 *
	 * @argument.file (COMPAT ONLY) Do not use anymore, use log instead. If defined, we will use this instead of log.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the application name
		ApplicationBoxContext appContext = context.getApplicationContext();
		// Set the application name if not null and the application argument is true
		if ( appContext != null && arguments.getAsBoolean( Key.application ) ) {
			arguments.put( Key.applicationName, appContext.getApplication().getName() );
		}
		// Announce the log message
		interceptorService.announce( BoxEvent.LOG_MESSAGE, arguments );
		return null;
	}
}
