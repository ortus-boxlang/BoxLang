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

import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Read the target of a symbolic link" )
public class FileReadSymlink extends BIF {

	/**
	 * Constructor
	 */
	public FileReadSymlink() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.path ),
		    new Argument( false, Argument.BOOLEAN, Key.resolve, false )
		};
	}

	/**
	 * Returns the target a symbolic link points to. Throws an exception if the path is not a symbolic link.
	 *
	 * @argument.path The symbolic link to read.
	 *
	 * @argument.resolve Optional. Defaults to false, which returns the target exactly as stored in the link. When true, it is returned as a
	 *                   normalized absolute path.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The target of the link as a string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String path = arguments.getAsString( Key.path );
		if ( !FileSystemUtil.isSymlink( Path.of( path ) ) ) {
			path = FileSystemUtil.expandPath( context, path ).absolutePath().toString();
		}
		return FileSystemUtil.readSymlink( Path.of( path ), arguments.getAsBoolean( Key.resolve ) );
	}

}
