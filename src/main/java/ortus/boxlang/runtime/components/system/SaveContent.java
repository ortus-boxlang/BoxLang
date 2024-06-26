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

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent( requiresBody = true )
public class SaveContent extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public SaveContent() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.variable, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.trim, "boolean", false ),
		    new Attribute( Key.append, "boolean", false )
		};
	}

	/**
	 * I capture the generated content from the body statements and save it into a variable
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		StringBuffer	buffer		= new StringBuffer();
		BodyResult		bodyResult	= processBody( context, body, buffer );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			// A return statement inside of savecontent discards all output that was built up
			return bodyResult;
		}
		String	content			= buffer.toString();
		String	variableName	= attributes.getAsString( Key.variable );

		boolean	trim			= attributes.getAsBoolean( Key.trim );
		boolean	append			= attributes.getAsBoolean( Key.append );

		// Optionally trim captured content
		if ( trim ) {
			content = content.trim();
		}

		// Lookup existing value and append if it existed
		if ( append ) {
			Object priorContent = ExpressionInterpreter.getVariable( context, variableName, true );
			if ( priorContent != null ) {
				content = StringCaster.cast( priorContent ).concat( content );
			}
		}

		// Set the result back into the page
		ExpressionInterpreter.setVariable( context, variableName, content );
		return DEFAULT_RETURN;
	}
}
