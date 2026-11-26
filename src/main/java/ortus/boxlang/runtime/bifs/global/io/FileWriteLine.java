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

@BoxBIF( description = "Write a line to a file" )
@BoxMember( type = BoxLangType.FILE )

public class FileWriteLine extends BIF {

	/**
	 * Constructor
	 */
	public FileWriteLine() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "string", Key.data )
		};
	}

	/**
	 * Writes a line of text, followed by a line separator, to the end of a file. The file can be a file object from `fileOpen()`, which is
	 * returned so the call can be chained and which you must close yourself, or a path, in which case the file is opened, appended to and closed
	 * for you. Also available as the member function `writeLine()` on file objects.
	 *
	 * @argument.file An open file object or the path of a file.
	 *
	 * @argument.data The line of text to write.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Null when a path was given, or the file object when a file object was given.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile file = arguments.getAsBoxFile( Key.file ).openAs( BoxFile.Mode.APPEND );
		try {
			file.writeLine( arguments.getAsString( Key.data ) );
			if ( file.implicitlyCast ) {
				return null;
			} else {
				return file;
			}
		} finally {
			if ( file.implicitlyCast ) {
				file.close();
			}
		}
	}

}
