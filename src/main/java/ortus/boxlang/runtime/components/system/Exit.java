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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class Exit extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Exit() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.method, "string", "exitTag", Set.of( Validator.valueOneOf( "exitTag", "exitTemplate", "loop" ) ) ),
		};
	}

	/**
	 * This component aborts processing of the currently executing custom tag, exits the page within the currently executing custom tag, or re-executes a section of code within the currently executing custom tag.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.method The method to use for exiting (exitTag, exitTemplate, loop)
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	method	= attributes.getAsString( Key.method ).toLowerCase();

		String	type;
		switch ( method ) {
			case "exittag" :
				type = "exit-tag";
				break;
			case "exittemplate" :
				type = "exit-template";
				break;
			case "loop" :
				type = "exit-loop";
				break;
			default :
				throw new BoxValidationException( "Invalid exit method: " + method );
		}
		context.flushBuffer( false );
		throw new AbortException( type, null );
	}
}
