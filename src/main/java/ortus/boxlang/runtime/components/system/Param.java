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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

@BoxComponent
public class Param extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Param() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.REQUIRED ) ),
		    new Attribute( Key.type, "string" ),
		    new Attribute( Key._DEFAULT, "any" ),
		    new Attribute( Key.max, "numeric" ),
		    new Attribute( Key.min, "numeric" ),
		    new Attribute( Key.pattern, "string" )
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
	 * @attribute.name The name of the parameter
	 *
	 * @attribute.type The data type of the parameter
	 *
	 * @attribute.default The default value of the parameter
	 *
	 * @attribute.max The maximum value of the parameter
	 *
	 * @attribute.min The minimum value of the parameter
	 *
	 * @attribute.pattern The pattern of the parameter
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	varName			= attributes.getAsString( Key._NAME );
		Object	defaultValue	= attributes.get( Key._DEFAULT );
		Object	existingValue	= ExpressionInterpreter.getVariable( context, varName, defaultValue != null );
		if ( existingValue == null && defaultValue != null ) {
			existingValue = defaultValue;
		}

		// TODO: Enforce validation here
		// BL types can be passed to GenericCaster
		// Other type delegate to isValid()

		ExpressionInterpreter.setVariable( context, varName, existingValue );
		return DEFAULT_RETURN;
	}
}
