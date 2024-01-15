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

@BoxBIF
@BoxBIF( alias = "DirectoryRename" )

public class DirectoryMove extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryMove() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.oldPath ),
		    new Argument( true, "string", Key.newPath ),
		    new Argument( false, "boolean", Key.createPath, true )
		};
	}

	/**
	 * Moves a directory from one location to another
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.oldPath The previous directory path
	 *
	 * @argument.newPath The new directory path
	 *
	 * @argument.createPath [true] Whether to create all necessary paths to the new path
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		Path	sourcePath		= Path.of( arguments.getAsString( Key.oldPath ) );
		Path	destinationPath	= Path.of( arguments.getAsString( Key.newPath ) );
		Boolean	createPath		= arguments.getAsBoolean( Key.createPath );

		if ( !createPath && !Files.exists( destinationPath.getParent() ) ) {
			throw new BoxRuntimeException( "The directory [" + destinationPath.toAbsolutePath().toString()
			    + "] cannot be created because the parent directory [" + destinationPath.getParent().toAbsolutePath().toString()
			    + "] does not exist.  To prevent this error set the createPath argument to true." );
		} else if ( Files.exists( destinationPath ) ) {
			throw new BoxRuntimeException( "The target directory [" + destinationPath.toAbsolutePath().toString() + "] already exists" );
		} else {
			try {
				Files.createDirectories( destinationPath.getParent() );
				Files.move( sourcePath, destinationPath );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		return null;
	}

}
