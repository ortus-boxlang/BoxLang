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

@BoxBIF( description = "Create a new directory" )

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
	 * Creates a directory, including any missing parent directories by default. Throws an exception if the directory already exists, unless
	 * `ignoreExists` is true. Returns null.
	 *
	 * @argument.path The directory path to create. It can be absolute or relative to the application.
	 *
	 * @argument.createPath Optional. Whether to create all missing parent directories. Defaults to true. When false, a missing parent causes an
	 *                      exception.
	 *
	 * @argument.ignoreExists Optional. Whether to silently ignore a directory which already exists. Defaults to false.
	 *
	 * @argument.mode Optional. A POSIX permission string, such as `755`, to set on the new directory. Ignored if not provided.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Always null.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	targetDirectory	= FileSystemUtil.expandPath( context, arguments.getAsString( Key.path ) ).absolutePath().toString();
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
