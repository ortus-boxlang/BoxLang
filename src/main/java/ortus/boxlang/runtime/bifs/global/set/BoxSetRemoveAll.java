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
import ortus.boxlang.runtime.dynamic.casters.SetCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.BoxSet;

@BoxBIF( description = "Remove every element of a collection from a Set." )
@BoxMember( type = BoxLangType.SET, name = "removeAll" )
public class BoxSetRemoveAll extends BIF {

	public BoxSetRemoveAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.MODIFIABLE_SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.values )
		};
	}

	/**
	 * Remove every element of a collection from a Set, leaving only the elements that are not in the collection.
	 * The Set is modified in place and returned to support method chaining. Elements not present in the Set are
	 * silently skipped.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to remove from.
	 *
	 * @argument.values The collection of values to remove. Accepts an Array, Set, list-delimited String, or any castable collection.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet	set	= arguments.getAsSet( Key.set );
		BoxSet	src	= SetCaster.castLoose( arguments.get( Key.values ) );
		set.removeAll( src );
		return set;
	}

}
