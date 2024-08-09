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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.MathUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "InputBaseN" )
public class InputBaseN extends BIF {

	/**
	 * Constructor
	 */
	public InputBaseN() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.string, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( true, Argument.INTEGER, Key.radix, Set.of( Validator.min( 2 ), Validator.max( 36 ) ) )
		};
	}

	/**
	 * Converts a string, using the base specified by radix, to an integer.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to convert to an integer.
	 *
	 * @argument.radix Base of the number represented by string, in the range 2-36.
	 *
	 * @return The integer value of the string.
	 */
	public Number _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	string	= arguments.getAsString( Key.string );
		int		radix	= arguments.getAsInteger( Key.radix );

		// Remove the 0x prefix if it exists.
		if ( string.startsWith( "0x" ) ) {
			string = string.substring( 2, string.length() );
		}

		// Convert the string to BigInteger using the specified radix
		BigInteger bigInt = new BigInteger( string, radix );

		// Convert BigInteger to BigDecimal and return it
		return new BigDecimal( bigInt, MathUtil.getMathContext() );

	}
}
