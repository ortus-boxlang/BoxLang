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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class FileGetMimeType extends BIF {

	/**
	 * Constructor
	 */
	public FileGetMimeType() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.file ),
		    new Argument( false, "boolean", Key.strict, true )
		};
	}

	/**
	 * Gets the MIME type for the file path/file object you have specified.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The file path or file object to get the MIME type for.
	 *
	 * @argument.strict If true, throws an exception if the file does not exist or is empty. If false, returns "application/octet-stream" for non-existent or empty files.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	filePath	= arguments.getAsString( Key.file );
		Boolean	strict		= arguments.getAsBoolean( Key.strict );

		if ( !filePath.substring( 0, 4 ).equalsIgnoreCase( "http" ) ) {
			filePath = FileSystemUtil.expandPath( context, filePath ).absolutePath().toString();
		} else if ( strict ) {
			throw new BoxRuntimeException(
			    "The file ["
			        + arguments.getAsString( Key.file )
			        + "] is a URL. To retrieve the mimetype of a URL set the strict argument to false."
			);
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
