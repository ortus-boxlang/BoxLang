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
package ortus.boxlang.runtime.interceptors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BaseInterceptor;
import ortus.boxlang.runtime.events.InterceptionPoint;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * A BoxLang interceptor that provides logging capabilities
 */
public class Logging extends BaseInterceptor {

	private static final String	DEFAULT_LOGGER	= "application";

	private LoggingService		loggingService;
	private BoxRuntime			runtime;

	/**
	 * Constructor
	 *
	 * @param instance The BoxRuntime instance
	 */
	public Logging( BoxRuntime instance ) {
		this.runtime		= instance;
		this.loggingService	= instance.getLoggingService();
	}

	/**
	 * Logs a message to a file or location
	 * <p>
	 * Data should contain the following keys:
	 * <ul>
	 * <li>applicationName: The name of the application requesting the log messasge. Can be empty</li>
	 * <li>text: The text of the log message</li>
	 * <li>type: The severity log level ( fatal, error, info, warn, debug, trace )</li>
	 * <li>log: The logger to log to.</li>
	 * </ul>
	 *
	 * <p>
	 * The <code>log</code> key is a shortcut to a specific log file. Available log files are: Application, Scheduler, etc.
	 * Which is dumb and should be moved to the CFML compatibility module. Leaving until we move it.
	 *
	 * @param data The data to be passed to the interceptor
	 *
	 * @throws IIllegalArgumentException If the log level is not valid
	 */
	@InterceptionPoint
	public void logMessage( IStruct data ) {
		// The incoming data
		String	text			= ( String ) data.getOrDefault( Key.text, "" );
		String	type			= ( String ) data.getOrDefault( Key.type, LoggingService.DEFAULT_LOG_LEVEL );
		String	logger			= ( String ) data.getOrDefault( Key.log, "" );
		String	file			= ( String ) data.getOrDefault( Key.file, "" );
		Object	applicationName	= data.get( Key.applicationName );

		// If the text is empty, then don't log anything
		if ( text.isEmpty() ) {
			return;
		}

		// Defaults
		if ( file == null ) {
			file = "";
		}
		if ( logger == null || logger.isEmpty() ) {
			logger = DEFAULT_LOGGER;
		}

		// COMPAT MODE: if you have a file, we transpile it to the logger
		if ( !file.isEmpty() ) {
			logger = file;
		}

		LoggingService.getInstance().logMessage(
		    text,
		    type,
		    ( applicationName instanceof Key appNameKey ) ? ( appNameKey ).getName() : "",
		    logger
		);
	}

}
