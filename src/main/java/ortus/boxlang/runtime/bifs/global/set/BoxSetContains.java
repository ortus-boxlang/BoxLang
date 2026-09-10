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
package ortus.boxlang.runtime.bifs.global.set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.BoxSet;

@BoxBIF( description = "Test whether a Set contains a given value." )
@BoxMember( type = BoxLangType.SET, name = "contains" )
@BoxMember( type = BoxLangType.SET, name = "has" )
public class BoxSetContains extends BIF {

	public BoxSetContains() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.value )
		};
	}

	/**
	 * Test whether a Set contains a given value using BoxLang value equality. Returns true if the value is
	 * present, false otherwise.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to test.
	 *
	 * @argument.value The value to look for.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet set = arguments.getAsSet( Key.set );
		return set.contains( arguments.get( Key.value ) );
	}

}
