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

import java.util.Random;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class Rand extends BIF {

	static Random rand = new Random();

	/**
	 * Constructor
	 */
	public Rand() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.algorithm )
		};
	}

	/**
	 * 
	 * Return a random double between 0 and 1
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.algorithm The algorithm to use to generate the random number.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		if ( arguments.get( Key.algorithm ) != null ) {
			return _invoke( arguments.getAsString( Key.algorithm ) );
		}
		return _invoke();
	}

	public static double _invoke() {
		return rand.nextDouble();
	}

	public double _invoke( String algorithm ) {
		throw new BoxRuntimeException( "The algorithm argument has not yet been implemented" );
	}

}
