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
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.CustomException;

@BoxComponent
public class Abort extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	public Abort() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.showerror, "string" ),
		    new Attribute( Key.type, "string", "request" )
		};
	}

	/**
	 * Tests for a parameter's existence, tests its data type, and, if a default value is not assigned, optionally provides one.
	 *
	 * @param context        The context in which the BIF is being invoked
	 * @param attributes     The attributes to the BIF
	 * @param body           The body of the BIF
	 * @param executionState The execution state of the BIF
	 *
	 * @argument.showerror Whether to show an error
	 *
	 * @argument.type The type of the abort (request or page)
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String		showerror	= attributes.getAsString( Key.showerror );
		String		type		= attributes.getAsString( Key.type );

		Throwable	cause		= null;
		if ( showerror != null && !showerror.isEmpty() ) {
			cause = new CustomException( showerror );
		}
		context.flushBuffer( true );
		throw new AbortException( type, cause );
	}
}
