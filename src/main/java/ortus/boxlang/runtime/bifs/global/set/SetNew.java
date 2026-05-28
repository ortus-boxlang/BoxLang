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

import java.util.Collection;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.SetCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxSet;

@BoxBIF( description = "Create a new Set, optionally seeded from a collection." )
public class SetNew extends BIF {

	/**
	 * Constructor
	 */
	public SetNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.type, "default" ),
		    new Argument( false, Argument.ANY, Key.values ),
		    new Argument( false, Argument.BOOLEAN, Key.caseSensitive, false )
		};
	}

	/**
	 * Create a new Set.
	 *
	 * @argument.type The backing variant — one of "default" (HashSet), "linked" (LinkedHashSet, preserves insertion order),
	 *                or "sorted" (TreeSet, natural ordering). Aliases: "hash" for default, "ordered" for linked, "tree" for sorted.
	 *
	 * @argument.values An optional seed collection (Array, Set, or anything castable to a Set). When provided, its elements
	 *                  are deduplicated into the new set. Pass a single value to seed a one-element set.
	 *
	 * @return A new {@link BoxSet}.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxSet.Type	type			= BoxSet.parseType( arguments.getAsString( Key.type ) );
		Object		seed			= arguments.get( Key.values );
		boolean		caseSensitive	= arguments.getAsBoolean( Key.caseSensitive );
		if ( seed == null ) {
			return new BoxSet( type, true, caseSensitive );
		}
		// Preserve iteration order for ordered seeds (Array/List) by adding directly,
		// rather than routing through a hash-backed SetCaster which would lose order.
		if ( seed instanceof Collection<?> coll ) {
			return new BoxSet( type, coll, caseSensitive );
		}
		var attempt = SetCaster.attemptLoose( seed );
		if ( attempt.wasSuccessful() ) {
			return new BoxSet( type, attempt.get(), caseSensitive );
		}
		BoxSet result = new BoxSet( type, true, caseSensitive );
		result.add( seed );
		return result;
	}

}
