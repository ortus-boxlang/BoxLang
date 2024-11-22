
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

package ortus.boxlang.runtime.components.system;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interceptors.Logging;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent
public class Log extends Component {

	/**
	 * Constructor
	 */
	public Log() {
		super();
		// Uncomment and define declare argument to this Component
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.text, "string" ),
		    new Attribute( Key.file, "string" ),
		    new Attribute( Key.log, "string", Logging.DEFAULT_LOG_TYPE ),
		    new Attribute( Key.type, "string", Logging.DEFAULT_LOG_LEVEL ),
		    new Attribute( Key.application, "boolean", true )
		};
	}

	/**
	 * Logs information to the specified log file
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.text The text to log
	 *
	 * @attribute.file An optional explicit file to log to
	 *
	 * @attribute.log The log category to write to
	 *
	 *
	 * @argument.application If true, it logs the application name alongside the message. Default is true.
	 *
	 * @attribute.type The log level of the entry. One of "Information", "Warning", "Error", "Debug", "Trace"
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// Get the application name
		ApplicationBoxContext appContext = context.getApplicationContext();
		// Set the application name if not null
		if ( appContext != null ) {
			attributes.put( Key.application, appContext.getApplication().getName() );
		}
		// Announce the log message
		interceptorService.announce( BoxEvent.LOG_MESSAGE, attributes );

		return DEFAULT_RETURN;
	}

}
