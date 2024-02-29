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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent
public class Setting extends Component {

	public Setting() {
		super();
		declaredAttributes = new Attribute[] {
		    // TODO: TRANSPILE enablecfoutputonly into enableOutputOnly!
		    new Attribute( Key.enableOutputOnly, "boolean" ),
		    new Attribute( Key.showDebugOutput, "boolean" ),
		    new Attribute( Key.requestTimeout, "long" )

		};
	}

	/**
	 * Tests for a parameter's existence, tests its data type, and, if a default value is not assigned, optionally provides one.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		Boolean				showDebugOutput		= attributes.getAsBoolean( Key.showDebugOutput );
		Boolean				enableOutputOnly	= attributes.getAsBoolean( Key.enableOutputOnly );
		Long				requestTimeout		= attributes.getAsLong( Key.requestTimeout );

		RequestBoxContext	requestContext		= context.getParentOfType( RequestBoxContext.class );

		if ( enableOutputOnly != null ) {
			// This will change the setting for the request of the request
			requestContext.setEnforceExplicitOutput( enableOutputOnly );
		}
		if ( requestTimeout != null ) {
			// This will change the setting for the request of the request
			requestContext.setRequestTimeout( requestTimeout );
		}

		// TODO: Implemet showDebugOutput. It feels like this should be specific to a web server, but since the setting component is core, maybe we add it to
		// the core

		return DEFAULT_RETURN;
	}
}
