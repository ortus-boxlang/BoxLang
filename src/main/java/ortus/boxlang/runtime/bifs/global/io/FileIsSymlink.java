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
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Determine if a path is a symbolic link" )
public class FileIsSymlink extends BIF {

	/**
	 * Constructor
	 */
	public FileIsSymlink() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.path )
		};
	}

	/**
	 * Determines whether a path is a symbolic link. A link whose target does not exist (a dangling link) is still a link. A path that does not exist
	 * returns false.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The path to test
	 *
	 * @return true if the path is a symbolic link
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String path = arguments.getAsString( Key.path );
		if ( !FileSystemUtil.isSymlink( java.nio.file.Path.of( path ) ) ) {
			// Try again with the path expanded, in case it was relative to the application
			path = FileSystemUtil.expandPath( context, path ).absolutePath().toString();
		}
		return FileSystemUtil.isSymlink( java.nio.file.Path.of( path ) );
	}

}
