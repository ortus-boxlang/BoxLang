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
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Convert a collection (Array, list-delimited string, Set) into a Set, deduplicating." )
@BoxMember( type = BoxLangType.ARRAY, name = "toSet" )
@BoxMember( type = BoxLangType.QUERY, name = "toSet" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "listToSet" )
public class ObjectToSet extends BIF {

	public ObjectToSet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.value ),
		    new Argument( false, Argument.STRING, Key.type, "default" ),
		    new Argument( false, Argument.STRING, Key.delimiter, "," )
		};
	}

	/**
	 * Convert a collection into a Set, deduplicating automatically. Accepts an Array, a list-delimited String,
	 * an existing Set, a QueryColumn, an XML node, a bounded Range, or any value castable to a Set. When the
	 * value is already of the requested variant it is returned as-is; otherwise a new Set of the specified
	 * variant is created and populated.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value The value to convert. Accepts Array, Set, list-delimited String, QueryColumn, XML, Range, etc.
	 *
	 * @argument.type The backing variant: "default" / "hash" (HashSet), "linked" / "ordered" (LinkedHashSet),
	 *                or "sorted" / "tree" (TreeSet). Defaults to "default".
	 *
	 * @argument.delimiter When {@code value} is a String, the list delimiter to split on. Defaults to {@code ","}.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object		value		= arguments.get( Key.value );
		BoxSet.Type	type		= BoxSet.parseType( arguments.getAsString( Key.type ) );
		String		delimiter	= arguments.getAsString( Key.delimiter );

		BoxSet		seed;
		if ( value instanceof String s ) {
			// Honor the user-supplied delimiter for String -> Set (SetCaster defaults to ",").
			seed = BoxSet.fromCollection( ListUtil.asList( s, delimiter ) );
		} else {
			seed = SetCaster.castLoose( value );
		}
		if ( seed.getType() == type ) {
			return seed;
		}
		return new BoxSet( type, seed );
	}

}
