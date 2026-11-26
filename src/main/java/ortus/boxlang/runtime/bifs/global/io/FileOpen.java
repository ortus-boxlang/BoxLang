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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Open a file for reading or writing" )

public class FileOpen extends BIF {

	/**
	 * Constructor
	 */
	public FileOpen() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file ),
		    new Argument( false, "string", Key.mode, "read" ),
		    new Argument( false, "string", Key.charset, "utf-8" ),
		    new Argument( false, "boolean", Key.seekable, null, Set.of() )
		};
	}

	/**
	 * Opens a file for reading or writing and returns a file object for use with BIFs such as `fileReadLine()`, `fileWriteLine()`, `fileIsEOF()`
	 * and `fileClose()`. Always close the file object when finished. Relative paths are resolved against the application.
	 *
	 * @argument.file The path of the file to open.
	 *
	 * @argument.mode Optional. The mode to open the file in: `read` (the default), `readbinary`, `write` or `append`. An invalid mode throws an
	 *                exception.
	 *
	 * @argument.charset Optional. The character set used when reading or writing the file. Defaults to utf-8.
	 *
	 * @argument.seekable Optional. Whether the file can be repositioned with `fileSeek()`. If not provided, files opened in a read mode are
	 *                    seekable and files opened in write or append mode are not.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return A file object.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String filePath = arguments.getAsString( Key.file );
		if ( !FileSystemUtil.exists( filePath ) ) {
			filePath = FileSystemUtil.expandPath( context, filePath ).absolutePath().toString();
		}
		return new BoxFile(
		    filePath,
		    arguments.getAsString( Key.mode ),
		    arguments.getAsString( Key.charset ),
		    arguments.getAsBoolean( Key.seekable )
		);
	}

}
