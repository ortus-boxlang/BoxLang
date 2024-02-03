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

public class StructInsert extends BIF {

	/**
	 * Constructor
	 */
	public StructInsert() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "string", Key.key ),
		    new Argument( true, "any", Key.value ),
		    new Argument( false, "boolean", Key.overwrite, false )
		};
	}

	/**
	 * Inserts a key/value pair in to a struct - with an optional overwrite argument
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct
	 *
	 * @argument.key The struct key
	 *
	 * @argument.value The value to assign for the specified key
	 *
	 * @argument.overwrite Whether to overwrite the existing value if the key exists ( default: false )
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct	struct		= arguments.getAsStruct( Key.struct );
		Key		key			= Key.of( arguments.getAsString( Key.key ) );
		Boolean	overwrite	= arguments.getAsBoolean( Key.overwrite );
		Boolean	isMember	= arguments.getAsBoolean( __isMemberExecution );
		if ( !overwrite && struct.containsKey( key ) ) {
			return isMember
			    ? struct
			    : false;
		}
		struct.put( key, arguments.get( Key.value ) );

		return isMember
		    ? struct
		    : true;
	}

}
