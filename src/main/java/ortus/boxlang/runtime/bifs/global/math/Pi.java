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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.util.MathUtil;

@BoxBIF
public class Pi extends BIF {

	/**
	 * 
	 * Returns the mathmatical constant Pi accurate to 15 digits
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 */
	public BigDecimal _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return new BigDecimal( "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679",
		    MathUtil.getMathContext() );
	}
}
