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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF

public class FileAppend extends BIF {

	/**
	 * Constructor
	 */
	public FileAppend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "any", Key.data ),
		    new Argument( false, "string", Key.charset, "utf-8" )
		};
	}

	/**
	 * Appends new contents to a file starting at the last character in the file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The file object or string file path
	 *
	 * @argument.data The data to append
	 *
	 * @argument.charset [utf-8] the default charset to open the file for writing
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		File file = null;
		if ( arguments.get( Key.file ) instanceof File ) {
			file = ( File ) arguments.get( Key.file );
		} else if ( arguments.get( Key.file ) instanceof String ) {
			file = new File( arguments.getAsString( Key.file ), "append", arguments.getAsString( Key.charset ), false );
		} else {
			throw new BoxRuntimeException( "The file argumennt [" + arguments.getAsString( Key.file ) + "] is not an open file stream or string path." );
		}

		file.append( arguments.getAsString( Key.data ) );
		// For strings file args we need to close the buffer
		if ( arguments.get( Key.file ) instanceof String ) {
			file.close();
		}
		return null;
	}

}
