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
import ortus.boxlang.runtime.components.util.LoopUtil;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( requiresBody = true )
public class Output extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Output() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.query, "any" ),
		    new Attribute( Key.group, "string", Set.of( Validator.NON_EMPTY ) ),
		    new Attribute( Key.groupCaseSensitive, "boolean", false ),
		    new Attribute( Key.startRow, "integer", Set.of( Validator.min( 1 ) ) ),
		    new Attribute( Key.maxRows, "integer", Set.of( Validator.min( 0 ) ) ),
		    new Attribute( Key.encodefor, "string" )
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
		// This will allow the current encode for value to be looked up in our context
		String encodeFor = attributes.getAsString( Key.encodefor );
		// If this output component didn't explicitly set an encodeFor, we'll look for the closest parent component that did
		if ( encodeFor == null ) {
			IStruct parent = context.findClosestComponent( Key.output, state -> state.get( Key.encodefor ) != null );
			if ( parent != null ) {
				encodeFor = parent.getAsString( Key.encodefor );
			}
		}
		executionState.put( Key.encodefor, encodeFor );

		Object queryOrName = attributes.get( Key.query );
		// Short circuit if there's no query
		if ( queryOrName == null ) {
			BodyResult bodyResult = processBody( context, body );
			// IF there was a return statement inside our body, we early exit now
			if ( bodyResult.isEarlyExit() ) {
				return bodyResult;
			}
			return DEFAULT_RETURN;
		}

		String	group				= attributes.getAsString( Key.group );
		// TODO: Use this
		Boolean	groupCaseSensitive	= attributes.getAsBoolean( Key.groupCaseSensitive );
		Integer	startRow			= attributes.getAsInteger( Key.startRow );
		Integer	maxRows				= attributes.getAsInteger( Key.maxRows );
		return LoopUtil.processQueryLoop( this, context, body, executionState, queryOrName, group, groupCaseSensitive, startRow, null, maxRows );
	}

}
