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
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class InvokeArgument extends Component {

	/**
	 * Constructor
	 */
	public InvokeArgument() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._NAME, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.value, "any" )
		};
	}

	/**
	 * Passes the name and value of an argument to a method. This is used inside of the invoke component.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * @attribute.name The name of the argument
	 * 
	 * @attribute.value The value of the argument
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	name		= attributes.getAsString( Key._NAME );
		Object	value		= attributes.get( Key.value );

		IStruct	parentState	= context.findClosestComponent( Key.invoke );
		if ( parentState == null ) {
			throw new RuntimeException( "invokeArgument must be nested in the body of an invoke component" );
		}
		parentState.getAsStruct( Key.invokeArgs ).put( Key.of( name ), value );

		return DEFAULT_RETURN;
	}

}
