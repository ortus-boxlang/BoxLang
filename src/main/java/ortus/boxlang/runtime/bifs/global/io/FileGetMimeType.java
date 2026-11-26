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
import java.nio.file.Files;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BoxFileCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.StringUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Get the MIME type of a file" )

public class FileGetMimeType extends BIF {

	/**
	 * Constructor
	 */
	public FileGetMimeType() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.file ),
		    new Argument( false, "boolean", Key.strict, true )
		};
	}

	/**
	 * Gets the MIME type of a file, using the file name and contents. If no type can be determined, `application/octet-stream` is returned.
	 *
	 * @argument.file The file path or file object to get the MIME type for. With `strict` set to false this may also be a URL.
	 *
	 * @argument.strict Optional. Defaults to true. When true, an exception is thrown if the file does not exist, is empty, or is a URL. When
	 *                  false, the MIME type is determined from the name alone, even for files which do not exist.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The MIME type as a string, for example `text/plain`.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	rawFile	= arguments.get( Key.file );
		Boolean	strict	= arguments.getAsBoolean( Key.strict );

		// Determine the file path string - HTTP URLs stay as strings, everything else goes through BoxFile
		String	filePath;
		if ( rawFile instanceof String str && StringUtil.startsWithIgnoreCase( str, "http" ) ) {
			if ( strict ) {
				throw new BoxRuntimeException(
				    "The file ["
				        + str
				        + "] is a URL. To retrieve the mimetype of a URL set the strict argument to false."
				);
			}
			filePath = str;
		} else {
			filePath = BoxFileCaster.cast( context, rawFile ).filepath;
		}

		String mimeType = null;

		if ( strict ) {
			Path filePathObject = Path.of( filePath );
			try {
				if ( !Files.exists( filePathObject ) ) {
					throw new BoxRuntimeException(
					    "The file ["
					        + arguments.getAsString( Key.file )
					        + "] does not exist. To retrieve the mimetype of a non-existent file set the strict argument to false."
					);
				} else if ( Files.size( filePathObject ) == 0 ) {
					throw new BoxRuntimeException(
					    "The file ["
					        + arguments.getAsString( Key.file )
					        + "] is empty. To retrieve the mimetype of a empty file set the strict argument to false."
					);
				}
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		mimeType = FileSystemUtil.getMimeType( filePath );
		if ( mimeType == null ) {
			mimeType = "application/octet-stream";
		}

		return mimeType;

	}

}
