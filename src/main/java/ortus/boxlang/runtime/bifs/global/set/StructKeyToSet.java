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
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF( description = "Build a Set containing the keys of a Struct (as Strings)." )
@BoxMember( type = BoxLangType.STRUCT, name = "keyToSet" )
@BoxMember( type = BoxLangType.MODIFIABLE_STRUCT, name = "keyToSet" )
public class StructKeyToSet extends BIF {

	public StructKeyToSet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRUCT, Key.struct ),
		    new Argument( false, Argument.STRING, Key.type, "default" )
		};
	}

	/**
	 * @argument.struct The struct whose keys should populate the set.
	 *
	 * @argument.type The backing variant — "default" (hash), "linked" (preserves struct
	 *                iteration order), or "sorted" (alphabetical).
	 *
	 * @return A new {@link BoxSet} of the struct's keys (rendered via {@code Key.getName()}).
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct		struct	= arguments.getAsStruct( Key.struct );
		BoxSet.Type	type	= BoxSet.parseType( arguments.getAsString( Key.type ) );
		BoxSet		out		= new BoxSet( type );
		struct.keySet().forEach( k -> out.add( k.getName() ) );
		return out;
	}

}
