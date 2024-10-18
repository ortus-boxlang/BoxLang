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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF

public class FileMove extends BIF {

	/**
	 * Constructor
	 */
	public FileMove() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "string", Key.destination )
		};
	}

	/**
	 * Moves file from source to destination
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The source file path.
	 *
	 * @argument.destination The destination file path.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	sourcePath		= FileSystemUtil.expandPath( context, arguments.getAsString( Key.source ) ).absolutePath().toString();
		String	destinationPath	= FileSystemUtil.expandPath( context, arguments.getAsString( Key.destination ) ).absolutePath().toString();
		// Make sure there is no attempt to move a file in to disallowed ( e.g. executable ) type
		if ( !runtime.getConfiguration().security.isFileOperationAllowed( destinationPath ) ) {
			throw new BoxRuntimeException( "The destination path contains an extension disallowed by the runtime security settings." );
		}
		FileSystemUtil.move( sourcePath, destinationPath );
		return null;
	}

}
