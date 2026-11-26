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
import ortus.boxlang.runtime.dynamic.casters.BoxFileCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.types.util.StringUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Read content from a file" )
@BoxBIF( alias = "FileReadBinary" )
public class FileRead extends BIF {

	public static final Key fileReadKey = Key.of( "FileRead" );

	/**
	 * Constructor
	 */
	public FileRead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.filepath ),
		    new Argument( false, "string", Key.charsetOrBufferSize ),
		    new Argument( false, "integer", Key.buffersize )
		};
	}

	/**
	 * Reads the contents of a file and returns it as a string. This BIF is also registered as `FileReadBinary`, which returns the contents as
	 * binary data (a byte array). The file can be a path (string, Path or File), an HTTP URL, or an open file object from `fileOpen()`. With a
	 * path the whole file is read; with an open file object the remaining content from the current position is read, or up to `buffersize`
	 * characters if provided. A file object you pass in is not closed.
	 *
	 * @argument.filepath A file path or URL to read entirely, or an open file object to read the remaining content from.
	 *
	 * @argument.charsetOrBufferSize Optional. Either the charset to use when reading a text file, or, if it is a number, the buffer size.
	 *
	 * @argument.buffersize Optional. The number of bytes or characters to read. Only applies to a file object.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The file contents as a string, or as binary data for `fileReadBinary()`.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	rawFilePath			= arguments.get( Key.filepath );
		Key		bifMethodKey		= arguments.getAsKey( BIF.__functionName );

		// Path-based read — determine the file path string
		String	charsetOrBufferSize	= arguments.getAsString( Key.charsetOrBufferSize );
		String	charset				= null;
		Integer	bufferSize			= arguments.getAsInteger( Key.buffersize );
		BoxFile	boxFile				= null;
		String	filePath			= null;

		if ( rawFilePath instanceof BoxFile boxFileArg ) {
			boxFile = boxFileArg;
		} else {
			if ( rawFilePath instanceof String str && StringUtil.startsWithIgnoreCase( str, "http" ) ) {
				filePath = str;
			} else {
				filePath = BoxFileCaster.cast( context, rawFilePath ).filepath;
			}
		}

		if ( charsetOrBufferSize != null ) {
			if ( bufferSize == null ) {
				CastAttempt<Integer> castAttempt = IntegerCaster.attempt( charsetOrBufferSize );
				if ( castAttempt.wasSuccessful() ) {
					bufferSize = castAttempt.get();
				} else {
					charset = charsetOrBufferSize;
				}
			} else {
				charset = charsetOrBufferSize;
			}
		}

		if ( boxFile != null ) {
			if ( bufferSize != null ) {
				return boxFile.read( bufferSize );
			}
			return boxFile.readAll();
		}

		if ( bifMethodKey.equals( fileReadKey ) ) {
			return FileSystemUtil.readString( filePath, charset );
		} else {
			return FileSystemUtil.readBinary( filePath );
		}

	}

}
