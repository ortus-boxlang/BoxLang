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
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxMember( type = BoxLangType.FILE )

public class FileSetAccessMode extends BIF {

	/**
	 * Constructor
	 */
	public FileSetAccessMode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file ),
		    new Argument( true, "string", Key.mode )
		};
	}

	/**
	 * Sets the Posix permissions on a file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file A file path or object
	 *
	 * @argument.mode The three-digit permission designations for the file or directory
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	file	= null;
		File	fileObj	= null;
		if ( arguments.get( Key.file ) instanceof File ) {
			fileObj	= ( File ) arguments.get( Key.file );
			file	= fileObj.filepath;
		} else {
			file = FileSystemUtil.expandPath( context, arguments.getAsString( Key.file ) ).absolutePath().toString();
		}

		FileSystemUtil.setPosixPermissions( file, arguments.getAsString( Key.mode ) );

		return fileObj != null ? fileObj : null;
	}

}
