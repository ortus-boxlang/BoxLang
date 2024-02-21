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

@BoxBIF

public class FileCopy extends BIF {

	/**
	 * Constructor
	 */
	public FileCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "boolean", Key.createPath, true )
		};
	}

	/**
	 * Copies a file from one location to another
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source file
	 *
	 * @argument.destination The destination file
	 *
	 * @argument.createPath [ true ] whether to create any nested paths required to the new file
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Path	sourcePath				= Path.of( arguments.getAsString( Key.source ) );
		Path	destinationPath			= Path.of( arguments.getAsString( Key.destination ) );
		Path	destinationDirectory	= destinationPath.getParent();
		Boolean	createPaths				= arguments.getAsBoolean( Key.createPath );

		if ( createPaths && !Files.exists( destinationDirectory ) ) {
			try {
				Files.createDirectories( destinationDirectory );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		try {
			Files.copy( sourcePath, destinationPath );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return null;

	}

}
