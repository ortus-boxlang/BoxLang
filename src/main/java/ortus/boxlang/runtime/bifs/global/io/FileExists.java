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

public class FileExists extends BIF {

	/**
	 * Constructor
	 */
	public FileExists() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "boolean", Key.allowRealPath, true )
		};
	}

	/**
	 * Determines whether a file exists
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The file path
	 *
	 * @arguments.allowRealPath Whether to allow an absolute path as the path argument
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	filePath		= arguments.getAsString( Key.source );
		Boolean	allowRealPath	= arguments.getAsBoolean( Key.allowRealPath );
		try {
			Path finalPath;
			if ( !allowRealPath && Path.of( filePath ).isAbsolute() ) {
				throw new BoxRuntimeException(
				    "The file or path argument [" + filePath + "] is an absolute path. This is disallowed when the allowRealPath argument is set to false."
				);
			} else if ( !FileSystemUtil.exists( filePath ) ) {
				finalPath = FileSystemUtil.expandPath( context, filePath ).absolutePath();
			} else {
				finalPath = Path.of( filePath );
			}

			return ( Boolean ) finalPath.toFile().exists() && !Files.isDirectory( finalPath );
		} catch ( java.nio.file.InvalidPathException e ) {
			// We ignore invalid paths
			return false;
		}
	}

}
