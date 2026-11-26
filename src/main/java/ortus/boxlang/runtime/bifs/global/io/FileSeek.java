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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF( description = "Move the file pointer to a specific position" )
@BoxBIF( alias = "FileSkipBytes" )
@BoxMember( type = BoxLangType.FILE )
@BoxMember( type = BoxLangType.FILE, name = "skipBytes" )

public class FileSeek extends BIF {

	/**
	 * Constructor
	 */
	public FileSeek() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "integer", Key.position )
		};
	}

	/**
	 * Moves the read position of a file object forward. For a file opened as text, the given number of characters are skipped from the current
	 * position. For a binary file, the position is set to the given offset from the start of the file. The file must be a file object from
	 * `fileOpen()` which is seekable. This BIF is also registered as `FileSkipBytes` and as the member functions `seek()` and `skipBytes()` on
	 * file objects. Returns null.
	 *
	 * @argument.file The file object, as created by `fileOpen()`.
	 *
	 * @argument.position The number of characters to skip, or the offset to move to for binary files.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Always null.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile file = arguments.getAsBoxFile( Key.file );
		if ( file.implicitlyCast ) {
			throw new BoxRuntimeException( "fileSeek() requires an existing file object to be passed that was created with fileOpen()." );
		}
		if ( !file.seekable ) {
			throw new BoxRuntimeException( "The provided file does not support seeking." );
		}
		try {
			file.seek( arguments.getAsInteger( Key.position ) );
			return null;
		} finally {
			if ( file.implicitlyCast ) {
				file.close();
			}
		}
	}

}
