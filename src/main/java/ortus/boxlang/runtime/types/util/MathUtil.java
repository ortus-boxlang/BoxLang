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
package ortus.boxlang.runtime.types.util;

import java.math.MathContext;
import java.math.RoundingMode;

import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

public class MathUtil {

	/**
	 * The default MathContext to use for all BigDecimal operations
	 */
	public static MathContext defaultMathContext = MathContext.DECIMAL128;

	/**
	 * Returns the default MathContext
	 *
	 * @return The default MathContext
	 */
	public static MathContext getMathContext() {
		return defaultMathContext;
	}

	/**
	 * Sets the default MathContext
	 *
	 * @param mathContext The MathContext to set
	 */
	public static void setMathContext( MathContext mathContext ) {
		defaultMathContext = mathContext;
	}

	/**
	 * Set precision of the current math context
	 * 
	 * @param precision The precision to set
	 * 
	 * @return The new MathContext
	 */
	public static MathContext setPrecision( int precision ) {
		defaultMathContext = new MathContext( precision, defaultMathContext.getRoundingMode() );
		return defaultMathContext;
	}

	/**
	 * Set rounding mode of the current math context
	 * 
	 * @param roundingMode The rounding mode to set as a string
	 * 
	 * @return The new MathContext
	 */
	public static MathContext setRoundingMode( String roundingMode ) {
		defaultMathContext = new MathContext( defaultMathContext.getPrecision(), RoundingMode.valueOf( roundingMode.trim().toUpperCase() ) );
		return defaultMathContext;
	}

	/**
	 * Set rounding mode of the current math context
	 * 
	 * @param roundingMode The rounding mode to set
	 * 
	 * @return The new MathContext
	 */
	public static MathContext setRoundingMode( RoundingMode roundingMode ) {
		defaultMathContext = new MathContext( defaultMathContext.getPrecision(), roundingMode );
		return defaultMathContext;
	}

	/**
	 * Sets the default MathContext based on human name as a string
	 *
	 * @param mathContext The name of the MathContext to set
	 */
	public static void setMathContext( String mathContext ) {
		switch ( mathContext.toUpperCase().trim() ) {
			case "DECIMAL32" :
				defaultMathContext = MathContext.DECIMAL32;
				break;
			case "DECIMAL64" :
				defaultMathContext = MathContext.DECIMAL64;
				break;
			case "DECIMAL128" :
				defaultMathContext = MathContext.DECIMAL128;
				break;
			default :
				throw new BoxValidationException( mathContext + " is not a valid MathContext" );
		}
	}

}
