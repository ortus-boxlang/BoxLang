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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

@BoxBIF
public class ObjectLoad extends BIF {

	/**
	 * Constructor
	 */
	public ObjectLoad() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.input )
		};
	}

	/**
	 * Loads an object serialized in a binary form from a file or as binary input
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.input The binary representation of the object to load, or the file path to load the object from.
	 *
	 * @return The object that was loaded
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object target = arguments.get( Key.input );
		if ( target == null ) {
			throw new BoxRuntimeException( "The target object to load is null" );
		}

		// If it's a string, then it's a file path, load the binary data from the file
		if ( target instanceof String castedPath ) {
			target = context.invokeFunction( Key.of( "FileReadBinary" ), new Object[] { castedPath } );
		}

		// Check if the target is a byte array
		if ( target instanceof byte[] ) {
			// Load the object from the byte array
			return ObjectMarshaller.deserialize( context, ( byte[] ) target );
		}

		// Else throw an exception
		throw new BoxRuntimeException( "The target object to load is not a byte array or a file path" );
	}
}
