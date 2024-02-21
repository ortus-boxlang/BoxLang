/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.operators.InstanceOf;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class IsInstanceOf extends BIF {

	/**
	 * Constructor
	 */
	public IsInstanceOf() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( true, "string", Key.typename )
		};
	}

	/**
	 * Determines whether an object is an instance of a ColdFusion interface or component, or of a Java class.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.object The CFC instance or Java object that you are testing
	 * 
	 * @argument.typename The name of the interface, component, or Java class of which the object might be an instance
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return InstanceOf.invoke( context, arguments.get( Key.object ), arguments.getAsString( Key.typename ) );
	}
}
