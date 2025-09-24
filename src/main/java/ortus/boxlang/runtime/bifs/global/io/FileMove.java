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

import java.nio.file.Files;
import java.nio.file.Path;

import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Move a file to a new location" )

public class FileMove extends BIF {

	/**
	 * Constructor
	 */
	public FileMove() {
		super();

		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.source ),
		    new Argument( true, Argument.STRING, Key.destination ),
		    new Argument( true, Argument.BOOLEAN, Key.overwrite, true ),
		    new Argument( false, Argument.STRING, Key.accept )
		};
	}

	/**
	 * Moves file from source to destination. The destination can be a file or a directory. If the destination is a directory, the
	 * file will be moved to that directory with the same name as the source file.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source file path.
	 *
	 * @argument.destination The destination file path or directory path.
	 * 
	 * @argument.overwrite Whether to overwrite the destination file if it exists. Defaults to true.
	 * 
	 * @argument.accept A comma separated list of file extensions to accept - which will override runtime security settings
	 * 
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
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

		// Make sure there is no attempt to move a file in to disallowed ( e.g. executable ) type
		if ( !FileSystemUtil.isFileOperationAllowed( context, destinationString, arguments.getAsString( Key.accept ) ) ) {
			throw new BoxRuntimeException( "The destination path contains an extension disallowed by the runtime security settings." );
		}

		// Move it
		FileSystemUtil.move( sourceString, destinationString, true, arguments.getAsBoolean( Key.overwrite ) );
		return null;
	}

}
