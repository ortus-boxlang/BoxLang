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

package ortus.boxlang.runtime.bifs.global.io;

import java.io.IOException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxBIF( alias = "FileReadBinary" )
public class FileRead extends BIF {

	/**
	 * Constructor
	 */
	public FileRead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.filepath ),
		    new Argument( false, "string", Key.charsetOrBufferSize ),
		    new Argument( false, "string", Key.charset ),
		    new Argument( false, "string", Key.buffersize )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	charsetOrBufferSize	= arguments.getAsString( Key.charsetOrBufferSize );
		String	charset				= arguments.getAsString( Key.charset );
		Integer	bufferSize			= arguments.getAsInteger( Key.buffersize );

		if ( charsetOrBufferSize != null ) {
			if ( IntegerCaster.isInteger( charsetOrBufferSize ) ) {
				bufferSize = IntegerCaster.cast( charsetOrBufferSize );
			} else {
				charset = charsetOrBufferSize;
			}
		}

		try {
			return FileSystemUtil.read( arguments.getAsString( Key.filepath ), charset, bufferSize );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

	}

}
