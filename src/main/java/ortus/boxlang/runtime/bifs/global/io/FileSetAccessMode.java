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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Set access permissions for a file" )
@BoxMember( type = BoxLangType.FILE )

public class FileSetAccessMode extends BIF {

	/**
	 * Constructor
	 */
	public FileSetAccessMode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "string", Key.mode )
		};
	}

	/**
	 * Sets the POSIX permissions on a file or directory using a mode such as `755` or `644`. This has no effect on operating systems which do
	 * not support POSIX permissions. Also available as the member function `setAccessMode()` on file objects.
	 *
	 * @argument.file The file path or file object.
	 *
	 * @argument.mode The three-digit permission designation, for example `644`.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Null when a path was given, or the file object when a file object was given.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile file = arguments.getAsBoxFile( Key.file );
		FileSystemUtil.setPosixPermissions( file.filepath, arguments.getAsString( Key.mode ) );
		return file.implicitlyCast ? null : file;
	}

}
