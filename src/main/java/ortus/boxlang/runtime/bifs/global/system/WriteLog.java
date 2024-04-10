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
import ortus.boxlang.runtime.events.BoxEvent;
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
		    new Argument( false, "string", Key.file ),
		    new Argument( false, "string", Key.log, "Application" ),
		    new Argument( false, "string", Key.type, "Information" ),
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
	 * @argument.file A custom log file to write to
	 * 
	 * @argument.log If a custom file is not specified the log category to write to
	 * 
	 * @argument.type The log level ( debug, info, warn, error )
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		interceptorService.announce( BoxEvent.LOG_MESSAGE, arguments );
		return null;
	}
}
