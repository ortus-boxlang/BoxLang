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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class DirectoryExists extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( true, "boolean", Key.allowRealPath, true )
		};
	}

	/**
	 * Determines whether a directory exists
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The directory path
	 *
	 * @arguments.allowRealPath Whether to allow an absolute path as the path argument
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	directoryPath	= arguments.getAsString( Key.path );
		Boolean	allowRealPath	= arguments.getAsBoolean( Key.allowRealPath );
		if ( !allowRealPath && Path.of( directoryPath ).isAbsolute() ) {
			throw new BoxRuntimeException(
			    "The file or path argument [" + directoryPath + "] is an absolute path. This is disallowed when the allowRealPath argument is set to false."
			);
		}

		return ( Boolean ) FileSystemUtil.exists( directoryPath ) && Files.isDirectory( Path.of( directoryPath ) );
	}

}
