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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

// I don't think this should allow a body, but Lucee supports this.
@BoxComponent( allowsBody = true )
public class Execute extends Component {

	private final Key	outputFileKey		= Key.of( "outputFile" );
	private final Key	errorFileKey		= Key.of( "errorFile" );
	private final Key	errorVariableKey	= Key.of( "errorVariable" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Execute() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.variable, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.of( "name" ), "string", Set.of( Validator.REQUIRED ) ),
		    new Attribute( Key.arguments, "any" ),
		    new Attribute( Key.timeout, "long" ),
		    new Attribute( Key.terminateOnTimeout, "boolean", false ),
		    new Attribute( Key.directory, "string" ),
		    new Attribute( outputFileKey, "string" ),
		    new Attribute( errorFileKey, "string" ),
		    new Attribute( errorVariableKey, "string" )
		};
	}

	/**
	 * Component variation of Execute function
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.variable The variable name to produce
	 *
	 * @attribute.errorVariable Optional variable to produce for error output
	 *
	 * @attribute.name The process name or binary path ( e.g. bash or /bin/sh )
	 *
	 * @attribute.arguments The process arguments ( e.g. for `java --version` this would be `--version` )
	 *
	 * @attribute.timeout The timeout to wait for the command, in seconds ( default unlimited )
	 *
	 * @attribute.terminateOnTimeout Whether to terminate the process/command if a timeout is reached
	 *
	 * @attribute.directory A working directory to execute the command from
	 *
	 * @attribute.ouptutFile An optional file path to write the command output to
	 *
	 * @attribute.errorFile An optional file path to write errors to
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		if ( attributes.containsKey( outputFileKey ) ) {
			attributes.put( Key.output, attributes.get( outputFileKey ) );
		}
		if ( attributes.containsKey( errorFileKey ) ) {
			attributes.put( Key.error, attributes.get( errorFileKey ) );
		}

		IStruct response = StructCaster
		    .cast( runtime.getFunctionService().getGlobalFunction( Key.systemExecute ).invoke( context, attributes, false, Key.execute ) );

		// Set the result(s) back into the page
		ExpressionInterpreter.setVariable( context, attributes.getAsString( Key.variable ), response.getAsString( Key.output ) );

		if ( attributes.containsKey( errorFileKey ) ) {
			ExpressionInterpreter.setVariable( context, attributes.getAsString( errorFileKey ), response.getAsString( Key.error ) );
		}

		return DEFAULT_RETURN;
	}
}
