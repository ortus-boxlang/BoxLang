/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

@BoxBIF( description = "Create a symbolic link" )
public class FileCreateSymlink extends BIF {

	/**
	 * Constructor
	 */
	public FileCreateSymlink() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.link ),
		    new Argument( true, Argument.STRING, Key.target ),
		    new Argument( false, Argument.BOOLEAN, Key.overwrite, false )
		};
	}

	/**
	 * Creates a symbolic link at the given path pointing to the given target. Throws an exception if the link cannot be created, for example when the
	 * operating system does not allow the current user to create symbolic links.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.link The path of the link to create. Missing parent directories are created.
	 *
	 * @argument.target The path the link points to. It is stored exactly as given, so a relative target is relative to the folder holding the link.
	 *
	 * @argument.overwrite [false] Whether to replace an existing file or link at the link path. Directories are never replaced.
	 *
	 * @return The absolute path of the created link
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String linkPath = FileSystemUtil.expandPath( context, arguments.getAsString( Key.link ) ).absolutePath().toString();
		// The link name is the file that is created, so it is held to the same rules as the destination of fileCopy and fileMove
		if ( !FileSystemUtil.isFileOperationAllowed( context, linkPath ) ) {
			throw new BoxRuntimeException( "The link path contains an extension disallowed by the runtime security settings." );
		}
		return FileSystemUtil.createSymlink( linkPath, arguments.getAsString( Key.target ), arguments.getAsBoolean( Key.overwrite ) );
	}

}
