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
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Write content to a file" )

public class FileWrite extends BIF {

	/**
	 * Constructor
	 */
	public FileWrite() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "any", Key.data ),
		    new Argument( false, "string", Key.charset, FileSystemUtil.DEFAULT_CHARSET.name() ),
		    new Argument( false, "boolean", Key.createPath, false )
		};
	}

	/**
	 * Writes a string or binary data to a file. With a path, the file is created, or overwritten if it exists. With an open file object from
	 * `fileOpen()`, the data is written through the file's stream, respecting its mode and position; the file object is not closed. Returns
	 * null.
	 *
	 * @argument.file A file path (string, Path or File) to create or overwrite, or an open file object to write through.
	 *
	 * @argument.data The string or binary (byte array) content to write. Other values are cast to a string.
	 *
	 * @argument.charset Optional. The charset used to write string data. Defaults to the runtime default (utf-8). Only applies to path-based
	 *                   writes.
	 *
	 * @argument.createPath Optional. Whether to create any missing parent directories. Defaults to false, in which case a missing directory
	 *                      throws an exception. Only applies to path-based writes.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Always null.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile	file		= arguments.getAsBoxFile( Key.file );
		Object	fileContent	= arguments.get( Key.data );

		// Explicit file object — write through the open stream
		if ( !file.implicitlyCast ) {
			file.write( fileContent );
			return null;
		}

		// Implicit (path) — create/overwrite the file on disk
		String	filePath	= file.filepath;
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
