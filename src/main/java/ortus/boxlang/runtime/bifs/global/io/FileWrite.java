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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class FileWrite extends BIF {

	/**
	 * Constructor
	 */
	public FileWrite() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file ),
		    new Argument( true, "any", Key.data ),
		    new Argument( false, "string", Key.charset, "utf-8" ),
		    new Argument( false, "boolean", Key.createPath, false )
		};
	}

	/**
	 * Writes the contents of a string or binary data to a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The string path of the file - either root relative or absolute
	 *
	 * @argument.data The string or binary byte array of the file content
	 *
	 * @arguments.charset The charset encoding ( ignored for binary data )
	 *
	 * @aguments.createPath [false] ( Boxlang only ) When true will ensure all directories to file destination are created
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	filePath	= arguments.getAsString( Key.file );
		Object	fileContent	= arguments.get( Key.data );
		String	charset		= arguments.getAsString( Key.charset );
		Boolean	createPath	= arguments.getAsBoolean( Key.createPath );

		if ( fileContent instanceof String ) {
			FileSystemUtil.write( filePath, arguments.getAsString( Key.data ), charset, createPath );
		} else if ( fileContent instanceof byte[] barr ) {
			FileSystemUtil.write( filePath, barr, createPath );
		} else {
			FileSystemUtil.write( filePath, StringCaster.cast( arguments.get( Key.data ) ), charset, createPath );
		}

		return null;

	}

}
