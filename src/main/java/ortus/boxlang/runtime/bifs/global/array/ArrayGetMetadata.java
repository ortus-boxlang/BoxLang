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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF( description = "Get metadata information about an array" )
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
	 * Gets metadata about an array. The returned struct contains the keys <code>datatype</code> (always "any"),
	 * <code>type</code> ("synchronized" or "unsynchronized") and <code>dimensions</code> (the number of dimensions of the array).
	 * Also available as the <code>getMetadata()</code> member function on arrays.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to get the metadata of.
	 *
	 * @return A struct with the keys datatype, type and dimensions.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Array	actualArray	= arguments.getAsArray( Key.array );
		IStruct	meta		= new Struct();

		// this value never seems to change
		meta.put( Key.datatype, "any" );

		meta.put( Key.type, actualArray.isSynchronized() ? "synchronized" : "unsynchronized" );
		meta.put( Key.dimensions, actualArray.dimensions );

		return meta;
	}

}
