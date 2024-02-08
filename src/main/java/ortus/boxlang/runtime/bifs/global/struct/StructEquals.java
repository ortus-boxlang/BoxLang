
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

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructEquals extends BIF {

	/**
	 * Constructor
	 */
	public StructEquals() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct1 ),
		    new Argument( true, "struct", Key.struct2 )
		};
	}

	/**
	 * Tests equality between two structs
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct1 The reference struct
	 * 
	 * @argument.struct2 The struct to test for equality
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return arguments.getAsStruct( Key.struct1 ).equals( arguments.getAsStruct( Key.struct2 ) );
	}

}
