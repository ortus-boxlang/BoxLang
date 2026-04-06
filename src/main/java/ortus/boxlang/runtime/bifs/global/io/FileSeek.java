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
	 * Moves the buffer cursor position forward the number of characters specified by the position argument
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The File instance
	 *
	 * @argument.position The cursor position to move forward in the file
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile file = arguments.getAsBoxFile( Key.file );
		// Seek works on any open seekable file; only open as readbinary if not already open
		if ( file.mode == BoxFile.Mode.NONE ) {
			file.openAs( BoxFile.Mode.READBINARY );
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
