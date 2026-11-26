
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

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Update a struct with values from another struct" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructUpdate extends StructInsert {

	/**
	 * Constructor
	 */
	public StructUpdate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiableStruct", Key.struct ),
		    new Argument( true, "any", Key.key ),
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Sets a key/value pair in a struct, replacing any existing value for the key (the key is created if missing). Works like structInsert with
	 * overwrite always true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct. Must be modifiable.
	 *
	 * @argument.key The key to set.
	 *
	 * @argument.value The value to assign to the key.
	 *
	 * @return The same struct, after the update.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put( Key.overwrite, true );
		return super._invoke( context, arguments );
	}

}
