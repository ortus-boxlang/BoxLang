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

@BoxComponent
public class Throw extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Throw() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.message, "any" ),
		    new Attribute( Key.type, "String" ),
		    new Attribute( Key.detail, "String" ),
		    new Attribute( Key.errorcode, "String" ),
		    new Attribute( Key.extendedinfo, "any" ),
		    new Attribute( Key.object, "Throwable" )
		};
	}

	/**
	 * Throws a developer-specified exception, which can be caught with a catch block.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 *
	 * @attribute.message Message that describes exception event
	 * 
	 * @attribute.type The type of the exception
	 * 
	 * @attribute.detail Description of the event
	 * 
	 * @attribute.errorcode A custom error code that you supply
	 * 
	 * @attribute.extendedinfo Additional custom error data that you supply
	 * 
	 * @attribute.object An instance of an exception object. If there is no message provided, this object will be thrown directly. If there is a message, a
	 *                   CustomException will be thrown and this object will be used as the cause.
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		runtime.getFunctionService().getGlobalFunction( Key._throw ).invoke( context, attributes, false, Key._throw );
		return DEFAULT_RETURN;
	}
}
