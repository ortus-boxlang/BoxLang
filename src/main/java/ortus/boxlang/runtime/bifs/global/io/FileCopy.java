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

public class FileCopy extends BIF {

	/**
	 * Constructor
	 */
	public FileCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.source ),
		    new Argument( true, Argument.STRING, Key.destination ),
		    new Argument( false, Argument.BOOLEAN, Key.createPath, true )
		};
	}

	/**
	 * Copies a file from one location to another. The destionation can be a file or a directory.
	 * If the destination is a directory, the source file name will be appended to the destination.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source file
	 *
	 * @argument.destination A destionation file or directory, if it's a directory, the suorce file name will be appended
	 *
	 * @argument.createPath [ true ] whether to create any nested paths required to the new file
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Set and expand
		String	sourceString		= FileSystemUtil.expandPath( context, arguments.getAsString( Key.source ) ).absolutePath().toString();
		String	destinationString	= FileSystemUtil.expandPath( context, arguments.getAsString( Key.destination ) ).absolutePath().toString();
		Path	sourcePath			= Path.of( sourceString );
		Path	destinationPath		= Path.of( destinationString );

		// Verify if the destination path is a directory, and if it is, append the file name
		// A convenience method to copy a file to a directory easily
		if ( Files.isDirectory( destinationPath ) ) {
			String fileName = sourcePath.getFileName().toString();
			destinationPath		= destinationPath.resolve( fileName );
			destinationString	= destinationPath.toString();
		}

		// Create the destination directory if it doesn't exist
		Path	destinationDirectory	= destinationPath.getParent();
		Boolean	createPaths				= arguments.getAsBoolean( Key.createPath );

		// Verify the destionation directory exists, else create it if requested
		if ( createPaths && !Files.exists( destinationDirectory ) ) {
			try {
				Files.createDirectories( destinationDirectory );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		// Make sure there is no attempt to move a file in to disallowed ( e.g. executable ) type
		if ( !runtime.getConfiguration().security.isFileOperationAllowed( destinationString ) ) {
			throw new BoxRuntimeException( "The destination path contains an extension disallowed by the runtime security settings." );
		}

		try {
			Files.copy( sourcePath, destinationPath );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return null;

	}

}
