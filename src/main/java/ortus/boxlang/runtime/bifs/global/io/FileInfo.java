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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.File;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
@BoxBIF( alias = "GetFileInfo" )
@BoxMember( type = BoxLangType.FILE )

public class FileInfo extends BIF {

	/**
	 * Constructor
	 */
	public FileInfo() {
		super();
		// Uncomment and define declare argument to this BIF
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.file )
		};
	}

	/**
	 * Returns a struct of file information. Different values are returned for FileInfo and GetFileInfo
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.File The filepath or file object to retrieve info upon
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	file		= arguments.get( Key.file );
		Path	filePath	= null;

		if ( file instanceof File ) {
			File fileObj = ( File ) file;
			filePath = fileObj.getPath();
		} else {
			filePath = Path.of( ( String ) file );
		}

		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		Boolean	verbose			= bifMethodKey.equals( Key.getFileInfo );

		return FileSystemUtil.info( filePath, verbose );

	}

}
