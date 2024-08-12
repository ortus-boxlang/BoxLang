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
import java.math.MathContext;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.MathUtil;

@BoxBIF
@BoxMember( type = BoxLangType.NUMERIC )
public class Cos extends BIF {

	private static final BigDecimal	MINUS_ONE	= BigDecimal.valueOf( -1 );
	private static final BigDecimal	THRESHOLD	= new BigDecimal( "1E-10" );
	private static final BigDecimal	TWO_PI		= BigDecimal.valueOf( 2 * StrictMath.PI );

	/**
	 * Constructor
	 */
	public Cos() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.number )
		};
	}

	/**
	 * Returns the cosine of an angle entered in radians
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.number The number to calculate the cosine of (in radians).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Number num = arguments.getAsNumber( Key.number );
		if ( num instanceof BigDecimal bd ) {
			return cos( bd, MathUtil.getMathContext() );
		}
		return StrictMath.cos( num.doubleValue() );
	}

	/**
	 * Calculate the cosine of a BigDecimal. There were no algroithms found that could calculate the cosine of a BigDecimal
	 * without being slow or returning incorrect results.
	 * 
	 * @param x  The number to calculate the cosine of
	 * @param mc The MathContext to use
	 * 
	 * @return The cosine of the number
	 */
	public static BigDecimal cos( BigDecimal x, MathContext mc ) {
		return new BigDecimal( StrictMath.cos( x.doubleValue() ) );
	}
}
