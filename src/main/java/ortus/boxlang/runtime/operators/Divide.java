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
package ortus.boxlang.runtime.operators;

import java.math.BigDecimal;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.MathUtil;

/**
 * Performs Math Divide
 * {@code a = 10 / 2}
 */
public class Divide implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The the result
	 */
	public static BigDecimal invoke( Object left, Object right ) {
		BigDecimal dRight = BigDecimalCaster.cast( right );
		if ( dRight.doubleValue() == 0 ) {
			throw new BoxRuntimeException( "You cannot divide by zero." );
		}
		return BigDecimalCaster.cast( left ).divide( dRight, MathUtil.getMathContext() );
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key
	 *
	 * @return The result
	 */
	public static BigDecimal invoke( IBoxContext context, Object target, Key name, Object right ) {
		BigDecimal result = invoke( Referencer.get( context, target, name, false ), right );
		Referencer.set( context, target, name, result );
		return result;
	}

}
