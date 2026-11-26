
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

@BoxBIF( description = "Find a value in a struct" )
@BoxMember( type = BoxLangType.STRUCT )

public class StructFind extends BIF {

	/**
	 * Constructor
	 */
	public StructFind() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( true, "any", Key.key ),
		    new Argument( false, "any", Key.defaultValue )
		};
	}

	/**
	 * Finds and retrieves the value of a top-level key in a struct. If the key does not exist and no default value is provided, a
	 * BoxRuntimeException is thrown.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to search.
	 *
	 * @argument.key The key to look up.
	 *
	 * @argument.defaultValue An optional value to return if the struct does not contain the key.
	 *
	 * @return The value stored at the key, or the default value if the key is missing.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	struct			= arguments.getAsStruct( Key.struct );
		Key		searchKey		= Key.of( arguments.get( Key.key ) );
		Object	defaultValue	= arguments.get( Key.defaultValue );
		if ( !struct.containsKey( searchKey ) ) {
			if ( defaultValue != null ) {
				return defaultValue;
			} else {
				throw new BoxRuntimeException(
				    String.format(
				        "The key [%s] could not be found in the struct",
				        searchKey.getName()
				    )
				);
			}
		}
		return struct.get( searchKey );
	}

}
