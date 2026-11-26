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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Insert a key-value pair into a struct" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructInsert extends BIF {

	/**
	 * Constructor
	 */
	public StructInsert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "modifiableStruct", Key.struct ),
		    new Argument( true, "any", Key.key ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "boolean", Key.overwrite, false )
		};
	}

	/**
	 * Inserts a key/value pair into a struct, in place. If the key already exists and overwrite is false, a BoxRuntimeException is thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct. Must be modifiable.
	 *
	 * @argument.key The key to insert.
	 *
	 * @argument.value The value to assign to the key.
	 *
	 * @argument.overwrite Whether to overwrite the value if the key already exists. Defaults to false, in which case an existing key throws an
	 *                     exception.
	 *
	 * @return The same struct, after the insert.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	struct		= arguments.getAsStruct( Key.struct );
		Key		key			= Key.of( arguments.get( Key.key ) );
		Boolean	overwrite	= arguments.getAsBoolean( Key.overwrite );

		if ( overwrite ) {
			struct.put( key, arguments.get( Key.value ) );
		} else if ( !struct.tryPutIfAbsent( key, arguments.get( Key.value ) ) ) {
			// Atomic check and insert, so concurrent callers cannot both pass the existence check
			throw new BoxRuntimeException( String.format( "The key [%s] already exists in the struct", key.getName() ) );
		}

		return struct;
	}

}
