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

public class DirectoryCreate extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryCreate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( false, "boolean", Key.createPath, true ),
		    new Argument( false, "boolean", Key.ignoreExists, false ),
		    new Argument( false, "string", Key.mode )
		};
	}

	/**
	 * Creates a directory
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The directory path to create
	 *
	 * @argument.createPath [true] Whether to create all paths necessary to create the directory path
	 *
	 * @argument.ignoreExists [false] Whether to ignore if a directory already exists
	 *
	 * @argument.mode When provided will attempt to set the posix permissions on the directory
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	targetDirectory	= arguments.getAsString( Key.path );
		Boolean	createPath		= arguments.getAsBoolean( Key.createPath );
		Boolean	ignoreExists	= arguments.getAsBoolean( Key.ignoreExists );
		String	mode			= arguments.getAsString( Key.mode );
		if ( !ignoreExists && FileSystemUtil.exists( targetDirectory ) ) {
			throw new BoxRuntimeException( "The directory [" + Path.of( targetDirectory ).toAbsolutePath().toString()
			    + "] already exists. Set the boolean argument ignoreExists to true to prevent this error" );
		}
		FileSystemUtil.createDirectory( targetDirectory, createPath, mode );
		return null;
	}

}
