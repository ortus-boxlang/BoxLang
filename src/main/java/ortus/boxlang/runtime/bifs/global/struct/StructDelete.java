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

@BoxBIF( description = "Delete a key from a struct" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructDelete extends BIF {

	/**
	 * Constructor
	 */
	public StructDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiableStruct", Key.struct ),
		    new Argument( true, "any", Key.key )
		};
	}

	/**
	 * Deletes a key from a struct, in place. The key is converted to a BoxLang Key, so the match is case-insensitive for default structs.
	 * Deleting a key that does not exist is not an error.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to remove the key from. Must be modifiable.
	 *
	 * @argument.key The key to delete.
	 *
	 * @return The same struct, after the deletion.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	target	= arguments.getAsStruct( Key.struct );
		Key		key		= Key.of( arguments.get( Key.key ) );
		target.remove( key );
		return target;
	}

}
