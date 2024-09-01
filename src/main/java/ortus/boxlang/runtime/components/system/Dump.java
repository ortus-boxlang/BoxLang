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
public class Dump extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Dump() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.var, "any" ),
		    new Attribute( Key.label, "string", "" ),
		    new Attribute( Key.top, "numeric", 0 ),
		    new Attribute( Key.expand, "boolean", true ),
		    new Attribute( Key.abort, "boolean", false ),
		    new Attribute( Key.output, "string" ),
		    new Attribute( Key.format, "string" )
		};
	}

	/**
	 * Outputs the contents of a variable of any type for debugging purposes.
	 * The variable can be as simple as a string or as complex as a class or struct.
	 *
	 * @attributes.var The variable to dump
	 *
	 * @attributes.label A label to display before the dump
	 *
	 * @attributes.top The number of levels to display
	 *
	 * @attributes.expand Whether to expand the dump
	 * 
	 * @attributes.abort Whether to abort the request after the dump
	 * 
	 * @attributes.output The output format
	 * 
	 * @attributes.format The format of the output
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		runtime
		    .getFunctionService()
		    .getGlobalFunction( Key.dump )
		    .invoke(
		        context,
		        new Object[] {
		            attributes.get( Key.var ),
		            attributes.get( Key.label ),
		            attributes.get( Key.top ),
		            attributes.get( Key.expand ),
		            attributes.get( Key.abort ),
		            attributes.get( Key.output ),
		            attributes.get( Key.format )
		        },
		        false,
		        Key.dump
		    );
		return DEFAULT_RETURN;
	}
}
