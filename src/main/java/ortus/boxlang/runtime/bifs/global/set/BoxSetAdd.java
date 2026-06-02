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
@BoxMember( type = BoxLangType.SET, name = "add" )
@BoxMember( type = BoxLangType.SET, name = "append" )
public class BoxSetAdd extends BIF {

	public BoxSetAdd() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.MODIFIABLE_SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.value )
		};
	}

	/**
	 * Add an element to a Set, deduplicating automatically. If the value is already present, the call is a
	 * no-op. The Set is modified in place and returned to support method chaining.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to add the element to.
	 *
	 * @argument.value The value to add. Duplicates are silently ignored.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet set = arguments.getAsSet( Key.set );
		set.add( arguments.get( Key.value ) );
		return set;
	}

}
