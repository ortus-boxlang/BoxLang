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
package ortus.boxlang.runtime.bifs.global.decision;

import java.util.Arrays;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.UDF;

@BoxBIF
public class IsCustomFunction extends BIF {

	/**
	 * Constructor
	 */
	public IsCustomFunction() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( false, "string", Key.type ),
		};
	}

	/**
	 * Determine whether a given object is a custom function.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.object The value to test for closure-ness.
	 *
	 * @argument.type Check for a specific type of custom function - `UDF`, `Lambda`, or `Closure`.
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String type = arguments.getAsString( Key.type );
		if ( type == null || type.isEmpty() ) {
			return arguments.get( Key.object ) instanceof Function;
		}
		Object value = arguments.get( Key.object );
		switch ( CustomFunctionType.fromString( type ) ) {
			case UDF :
				return value instanceof UDF;
			case LAMBDA :
				return value instanceof Lambda;
			case CLOSURE :
				return value instanceof Closure;
			default :
				return value instanceof Closure;
		}
	}

	enum CustomFunctionType {

		UDF,
		CLOSURE,
		LAMBDA;

		public static CustomFunctionType fromString( String type ) {
			try {
				return CustomFunctionType.valueOf( type.trim().toUpperCase() );
			} catch ( IllegalArgumentException e ) {
				throw new IllegalArgumentException(
				    String.format( "Invalid type [%s], must be one of %s", type, Arrays.toString( CustomFunctionType.values() ) )
				);
			}
		}
	};
}