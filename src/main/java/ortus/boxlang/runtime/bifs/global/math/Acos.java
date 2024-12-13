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
package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
public class Acos extends BIF {

	/**
	 * Constructor
	 */
	public Acos() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "double", Key.number )
		};
	}

	/**
	 * Returns the arccosine (inverse cosine) of a number
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the arccosine of
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return _invoke( arguments.getAsDouble( Key.number ) );
	}

	public static double _invoke( double value ) {
		if ( value < -1.0 || value > 1.0 ) {
			throw new BoxRuntimeException( "Input value must be in the range [-1, 1] for ACos function." );
		}
		return StrictMath.acos( value );
	}
}
