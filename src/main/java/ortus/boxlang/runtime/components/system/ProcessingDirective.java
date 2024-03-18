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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent( allowsBody = true )
public class ProcessingDirective extends Component {

	public ProcessingDirective() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.pageEncoding, "string" ),
		    new Attribute( Key.suppressWhiteSpace, "boolean" )

		};
	}

	/**
	 * This tag does nothing for now. Still evaluating if we need it
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * @Argument.pageEncoding The page encoding to use
	 * 
	 * @Argument.suppressWhiteSpace Suppress white space in the output
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// TODO: I don't think page encoding is needed and even Adobe says that.
		// It's really not clear exactly what the suppressWhiteSpace attribtue does. Perhaps it is the same as Lucee's "smart" whitespace management?

		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		return DEFAULT_RETURN;
	}
}
