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
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayGetMetadata extends BIF {

	/**
	 * Constructor
	 */
	public ArrayGetMetadata() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "array", Key.array )
		};
	}

	/**
	 * Gets metadata for items of an array and indicates the array type.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to be inserted into
	 */
	public Struct invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Array actualArray = arguments.getAsArray( Key.array );
		Struct meta = new Struct();

		// this value never seems to change
		meta.put( Key.datatype, "any" );

		// these values are determined by how the array is created when using ArrayNew
		meta.put( Key.type, "unsynchronized" );
		meta.put( Key.dimensions, 1 );

		return meta;
	}

}
