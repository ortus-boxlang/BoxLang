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

@BoxBIF( description = "Add every element of a collection to a Set." )
@BoxMember( type = BoxLangType.SET, name = "addAll" )
public class BoxSetAddAll extends BIF {

	public BoxSetAddAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.MODIFIABLE_SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.values )
		};
	}

	/**
	 * Add every element of a collection into a Set, deduplicating automatically. The source collection can be
	 * an Array, another Set, a list-delimited String, or any value castable to a Set. The Set is modified in
	 * place and returned to support method chaining.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.set The set to add to.
	 *
	 * @argument.values The collection of values to add. Accepts an Array, Set, list-delimited String, or any castable collection.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet	set	= arguments.getAsSet( Key.set );
		BoxSet	src	= SetCaster.castLoose( arguments.get( Key.values ) );
		set.addAll( src );
		return set;
	}

}
