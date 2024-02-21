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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "compare" )
public class Compare extends BIF {

	/**
	 * Constructor
	 */
	public Compare() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.string1 ),
		    new Argument( true, "any", Key.string2 ),
		};
	}

	/**
	 *
	 * Performs a case-sensitive comparison of two strings.
	 * -1, if string1 is less than string2
	 * 0, if string1 is equal to string2
	 * 1, if string1 is greater than string2
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string1 The first string to compare
	 *
	 * @argument.string2 The second string to compare
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Integer comparison = ortus.boxlang.runtime.operators.Compare.invoke( arguments.get( Key.string1 ), arguments.get( Key.string2 ), true );
		return comparison < 0 ? -1
		    : comparison > 0 ? 1
		        : 0;
	}

}
