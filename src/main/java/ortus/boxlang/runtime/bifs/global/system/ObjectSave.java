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
public class ObjectSave extends BIF {

	/**
	 * Constructor
	 */
	public ObjectSave() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.object ),
		    new Argument( false, "string", Key.file )
		};
	}

	/**
	 * Serialize an object to file or convert it to binary format using Java Serialization
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.object The object to serialize
	 *
	 * @argument.file The file path to save the serialized object to, if not provided, the binary representation of the object is returned
	 *
	 * @return The binary representation of the object regardless of whether it was saved to a file or not
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object target = arguments.get( Key.object );
		if ( target == null ) {
			throw new BoxRuntimeException( "The object to serialize is null" );
		}

		// Serialize the object
		byte[]	serializedData	= ObjectMarshaller.serialize( context, target );

		// If a file path was provided, save the serialized object to the file
		Object	filePath		= arguments.get( Key.file );
		if ( filePath != null ) {
			context.invokeFunction( Key.of( "FileWrite" ), new Object[] { ( String ) filePath, serializedData } );
		}

		// Return the binary representation of the object
		return serializedData;
	}
}
