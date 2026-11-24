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
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Delete a directory and its contents" )

public class DirectoryDelete extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( true, "boolean", Key.recursive, false ),
		    new Argument( false, "boolean", Key.followLinks, false )
		};
	}

	/**
	 * Deletes a directory
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path the path of the directory to delete
	 *
	 * @argument.recursive [default:false] whether to recursively delete the directory.
	 *                     If falls and the directory is not empty, with throw a runtime exception
	 *
	 * @argument.followLinks [default:false] whether to follow symbolic links found inside the directory when deleting recursively. When false, links are
	 *                       removed as links and the folders they point to are never touched. When true, the contents of those folders are deleted too,
	 *                       except for a link that leads back into the directory being deleted or to a folder above it, which is removed as a link.
	 *                       A symbolic link given as the path only has the link removed, whatever the value of followLinks.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String directoryPath = arguments.getAsString( Key.path );
		if ( !FileSystemUtil.exists( directoryPath ) ) {
			directoryPath = FileSystemUtil.expandPath( context, directoryPath ).absolutePath().toString();
		}
		FileSystemUtil.deleteDirectory( directoryPath, arguments.getAsBoolean( Key.recursive ), arguments.getAsBoolean( Key.followLinks ) );
		return null;
	}

}
