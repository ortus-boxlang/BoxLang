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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.XMLCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.XML;

@BoxBIF( description = "Check if value is an XML node" )
public class IsXMLNode extends BIF {

	/**
	 * Constructor
	 */
	public IsXMLNode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value ),
		};
	}

	/**
	 * Determines whether a value is any XML node (document, element, attribute, etc) or can be parsed into one. Well-formed XML strings return
	 * true.
	 *
	 * @argument.value The value to test.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return True if the value is, or can be cast to, an XML node; false otherwise.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		CastAttempt<XML> castAttempt = XMLCaster.attempt( arguments.get( Key.value ) );
		return castAttempt.wasSuccessful();
	}

}