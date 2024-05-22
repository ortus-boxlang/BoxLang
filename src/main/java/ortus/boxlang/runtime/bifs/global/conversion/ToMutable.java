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
package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
@BoxMember( type = BoxLangType.STRUCT )
@BoxMember( type = BoxLangType.QUERY )
public class ToMutable extends BIF {

	/**
	 * Constructor
	 */
	public ToMutable() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.value )
		};
	}

	/**
	 * Convert an array, struct or query to its mutable counterpart.
	 *
	 * @argument.value The array, struct or query to convert.
	 *
	 * @param context   The context in which the BIF is being executed.
	 * @param arguments The arguments passed to the BIF.
	 *
	 * @return The value converted to its mutable counterpart. If we can't change we just return it.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object inputValue = arguments.get( Key.value );

		// Arrays
		if ( inputValue instanceof ImmutableArray castedArray ) {
			return new Array( castedArray );
		}
		// Structs
		else if ( inputValue instanceof ImmutableStruct castedStruct ) {
			return new Struct( castedStruct );
		}
		// Others
		else {
			return inputValue;
		}
	}

}
