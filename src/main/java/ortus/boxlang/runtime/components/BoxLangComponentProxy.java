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
package ortus.boxlang.runtime.components;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This is a Component that is used to proxy from a BoxLang Class to a Java method
 */
public class BoxLangComponentProxy extends Component {

	/**
	 * The target BoxLang BIF class we proxy to
	 */
	private IClassRunnable target;

	/**
	 * Constructor
	 */
	public BoxLangComponentProxy( IClassRunnable target ) {
		super();
		this.target = target;
	}

	/**
	 * Constructor
	 */
	public BoxLangComponentProxy() {
	}

	/**
	 * Get the target BoxLang BIF class we proxy to
	 *
	 * @return the target
	 */
	public IClassRunnable getTarget() {
		return target;
	}

	/**
	 * Set the target BoxLang BIF class we proxy to
	 *
	 * @param target the target to set
	 */
	public BoxLangComponentProxy setTarget( IClassRunnable target ) {
		this.target = target;
		return this;
	}

	@Override
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {

		// System.out.println( "BoxLangComponent.invoke() called" );
		// System.out.println( "Attributes " + attributes.toString() );

		// Prepare component arguments
		ArgumentsScope arguments = new ArgumentsScope();
		arguments.put( Key.context, context );
		arguments.put( Key.attributes, attributes );
		arguments.put( Key.body, body );
		arguments.put( Key.executionState, executionState );

		// Execute
		var bodyResult = this.target.dereferenceAndInvoke( context, Key.invoke, arguments, false );
		return ( bodyResult == null ) ? Component.DEFAULT_RETURN : ( BodyResult ) bodyResult;
	}

}
