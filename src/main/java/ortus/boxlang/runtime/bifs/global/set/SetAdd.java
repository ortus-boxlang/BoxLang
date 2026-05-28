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

@BoxBIF( description = "Add an element to a Set. Duplicates are ignored. Returns the Set for chaining." )
@BoxMember( type = BoxLangType.SET )
@BoxMember( type = BoxLangType.SET, name = "append" )
public class SetAdd extends BIF {

	public SetAdd() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.MODIFIABLE_SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.value )
		};
	}

	/**
	 * @argument.set The set to add to.
	 *
	 * @argument.value The value to add. Duplicates are silently ignored.
	 *
	 * @return The set (for chaining).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet set = arguments.getAsSet( Key.set );
		set.add( arguments.get( Key.value ) );
		return set;
	}

}
