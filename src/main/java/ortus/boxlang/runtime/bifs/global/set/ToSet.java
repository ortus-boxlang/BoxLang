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

@BoxBIF( description = "Convert a collection (Array, list-delimited string, Set) into a Set, deduplicating." )
@BoxMember( type = BoxLangType.ARRAY, name = "toSet" )
@BoxMember( type = BoxLangType.MODIFIABLE_ARRAY, name = "toSet" )
@BoxMember( type = BoxLangType.QUERY, name = "toSet" )
public class ToSet extends BIF {

	public ToSet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.value ),
		    new Argument( false, Argument.STRING, Key.type, "default" )
		};
	}

	/**
	 * @argument.value The value to convert to a Set. Accepts {@code Array}, {@code List},
	 *                 {@code Set}, native arrays, {@code QueryColumn}, {@code XML}, or a bounded {@code Range}.
	 *
	 * @argument.type The backing variant — "default" (hash), "linked" (ordered), or "sorted".
	 *
	 * @return A new {@link BoxSet}.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object		value	= arguments.get( Key.value );
		BoxSet.Type	type	= BoxSet.parseType( arguments.getAsString( Key.type ) );
		// Convert the source to a set of DEFAULT variant, then move into the requested variant
		// if different (keeps dedup semantics consistent).
		BoxSet		seed	= SetCaster.castLoose( value );
		if ( seed.getType() == type ) {
			return seed;
		}
		return new BoxSet( type, seed );
	}

}
