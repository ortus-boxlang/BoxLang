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

	public static final Key stringOnlyBif = Key.of( "FileRead" );

	/**
	 * Constructor
	 */
	public FileRead() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.filepath ),
		    new Argument( false, "string", Key.charsetOrBufferSize ),
		    new Argument( false, "string", Key.charset ),
		    new Argument( false, "string", Key.buffersize )
		};
	}

	/**
	 * Reads the contents of a file and returns it as a string or binary object.
	 * <p>
	 * When called with a <b>file path</b> (string, Path, or File), the entire file is read from disk.
	 * HTTP URLs are also supported as string paths.
	 * When called with an <b>open BoxFile object</b> (from {@code fileOpen()}), the remaining content
	 * is read from the current stream position to EOF. For text mode files, returns a String.
	 * For binary mode files, returns a byte[]. The caller is responsible for closing the file object afterward.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.filepath A file path (string, Path, File, or HTTP URL) to read entirely, or an open BoxFile object to read remaining content from.
	 *
	 * @argument.charsetOrBufferSize Either the charset to use when reading the file, or the buffer size. Only applies to path-based reads.
	 *
	 * @argument.charset The explicit charset to use when reading the file. Only applies to path-based reads.
	 *
	 * @argument.buffersize The explicit buffer size to use when reading the file. Only applies to path-based reads.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	rawFilePath		= arguments.get( Key.filepath );
		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );

		// Explicit file object — read remaining content from open stream
		if ( rawFilePath instanceof BoxFile boxFile && !boxFile.implicitlyCast ) {
			return boxFile.readAll();
		}

		// Path-based read — determine the file path string
		String	charsetOrBufferSize	= arguments.getAsString( Key.charsetOrBufferSize );
		String	charset				= arguments.getAsString( Key.charset );
		Integer	bufferSize			= arguments.getAsInteger( Key.buffersize );

		String	filePath;
		if ( rawFilePath instanceof String str && StringUtil.startsWithIgnoreCase( str, "http" ) ) {
			filePath = str;
		} else {
			filePath = BoxFileCaster.cast( context, rawFilePath ).filepath;
		}

		if ( charsetOrBufferSize != null ) {
			CastAttempt<Integer> castAttempt = IntegerCaster.attempt( charsetOrBufferSize );
			if ( castAttempt.wasSuccessful() ) {
				bufferSize = castAttempt.get();
			} else {
				charset = charsetOrBufferSize;
			}
		}

		return FileSystemUtil.read( filePath, charset, bufferSize, bifMethodKey.equals( stringOnlyBif ) );

	}

}
