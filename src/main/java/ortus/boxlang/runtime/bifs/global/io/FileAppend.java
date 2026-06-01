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

import java.nio.charset.Charset;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxFile;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Append content to a file" )

public class FileAppend extends BIF {

	/**
	 * Constructor
	 */
	public FileAppend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "boxfile", Key.file ),
		    new Argument( true, "any", Key.data ),
		    new Argument( false, "string", Key.charset, "utf-8" ),
		    new Argument( false, "boolean", Key.addnewline, false )
		};
	}

	/**
	 * Appends new contents to a file starting at the last character in the file
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.file The file object or string file path
	 *
	 * @argument.data The data to append
	 *
	 * @argument.charset [utf-8] the default charset to open the file for writing
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxFile	file	= arguments.getAsBoxFile( Key.file ).openAs( BoxFile.Mode.APPEND, arguments.getAsString( Key.charset ), false );
		Object	data	= arguments.get( Key.data );
		String	content	= null;
		if ( data instanceof byte[] dataBytes ) {
			content = new String( dataBytes, Charset.forName( arguments.getAsString( Key.charset ) ) );
			return null;
		} else {
			content = StringCaster.cast( data );
		}

		if ( BooleanCaster.cast( arguments.get( Key.addnewline ) ) ) {
			content += FileSystemUtil.LINE_SEPARATOR;
		}

		file.append( content );

		if ( file.implicitlyCast ) {
			file.close();
		}
		return null;
	}

}
