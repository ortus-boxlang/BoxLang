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

@BoxBIF( description = "Retain only the elements of a Set that are also in the given collection." )
@BoxMember( type = BoxLangType.SET )
public class SetRetainAll extends BIF {

	public SetRetainAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.MODIFIABLE_SET, Key.set ),
		    new Argument( true, Argument.ANY, Key.values )
		};
	}

	/**
	 * @argument.set The set to filter in place.
	 *
	 * @argument.values The collection whose membership is the keep-mask.
	 *
	 * @return The set (for chaining).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet	set	= arguments.getAsSet( Key.set );
		BoxSet	src	= SetCaster.castLoose( arguments.get( Key.values ) );
		set.retainAll( src );
		return set;
	}

}
