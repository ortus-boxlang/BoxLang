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
package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@BoxBIF
public class PrecisionEvaluate extends BIF {

	/**
	 * Constructor
	 */
	public PrecisionEvaluate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.expressions )
		};
	}

	/**
	 * Evaluates one or more string expressions using BigDecimal precision arithmetic.
	 * If the results ends in an infinitely repeating decimal value only the first 20 digits of the decimal
	 * portion will be used. BigDecimal precision results only work with addition, subtraction,
	 * multiplication and division. The use of ^, MOD, % or \ arithmetic operators will result in
	 * normal integer precision.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.expressions Expressions to evaluate
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	expressions	= arguments.getAsString( Key.expressions );
		String	regex		= "^[0-9+\\-*/^%\\\\()\\s]*(MOD\\s*)?[0-9+\\-*/^%\\\\()\\s]*$";
		Pattern	pattern		= Pattern.compile( regex );
		Matcher	matcher		= pattern.matcher( expressions );
		// make sure we are maths before we execute to stop any bad actors
		if ( matcher.matches() ) {
			Double		results			= ( double ) runtime.executeStatement( expressions, context );
			BigDecimal	finalResults	= BigDecimal.valueOf( results );
			return finalResults;
		} else {
			throw new BoxRuntimeException( "The expressions provided are not valid" );
		}
	}
}
