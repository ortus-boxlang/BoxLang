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
package ortus.boxlang.runtime.components.jdbc;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = false )
public class ProcResult extends Component {

	/**
	 * Constructor
	 */
	public ProcResult() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key._name, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.resultSet, "numeric" ),
		    new Attribute( Key.maxRows, "numeric", -1 )
		};

	}

	/**
	 * Register a result set variable for a stored procedure.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 * 
	 * @attribute.name The variable name to place the result set in.
	 * 
	 * @attribute.resultSet The index of the resultset to access. Required if there is more than one ProcResult component.
	 * 
	 * @attribute.maxRows The maximum number of rows to fetch per resultset.
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		IStruct parentState = context.findClosestComponent( Key.storedproc );
		if ( parentState == null ) {
			throw new RuntimeException( "ProcResult must be nested in the body of a StoredProc component" );
		}
		// Set our data into the Query component for it to use
		parentState.getAsArray( Key.procResult ).add( attributes );
		return DEFAULT_RETURN;
	}

}
