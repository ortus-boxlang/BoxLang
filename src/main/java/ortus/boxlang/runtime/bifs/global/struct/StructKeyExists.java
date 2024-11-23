/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructKeyExists extends BIF {

	/**
	 * Constructor
	 */
	public StructKeyExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( true, "any", Key.key )
		};
	}

	/**
	 * Tests whether a key exists in a struct and returns a boolean value
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to test
	 *
	 * @argument.key The key within the struct to test for existence
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		keyKey	= Key.of( arguments.get( Key.key ) );
		IStruct	struct	= arguments.getAsStruct( Key.struct );

		// First check if the key exists in the struct. This is here to filter out keys which are accessible, but
		// not actually returned by the keySet() method, such as the `1` key in an arguments scope. You can access it, but it's not really there as a key.
		if ( !struct.containsKey( keyKey ) ) {
			return false;
		}

		// If the key exists, then we need to check if the value is defined based on our current null settings.
		Object result = struct.getRaw( keyKey );
		return context.isDefined( result );
	}

}
