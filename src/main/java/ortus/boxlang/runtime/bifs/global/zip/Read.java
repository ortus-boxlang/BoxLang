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
package ortus.boxlang.runtime.bifs.global.zip;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.ZipUtil;

@BoxBIF( description = "Read a file from an archive" )
public class Read extends BIF {

	// Constructor
	public Read() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.source ),
		    new Argument( true, Argument.STRING, Key.entryPath ),
		    new Argument( false, Argument.STRING, Key.charset, java.nio.charset.Charset.defaultCharset().name() )
		};
	}

	/**
	 * Reads a file entry from a ZIP archive and returns its contents as a string.
	 * <p>
	 * The {@code source} argument specifies the ZIP file to read.
	 * The {@code entryPath} argument specifies the path of the file inside the archive,
	 * such as {@code docs/readme.txt}.
	 * <p>
	 * The {@code charset} argument specifies the character encoding used to decode
	 * the entry's contents. If omitted, the JVM's default charset is used.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.source The path to the source ZIP file. Required.
	 *
	 * @argument.entryPath The path of the file inside the ZIP archive. Required.
	 *
	 * @argument.charset The character encoding used to decode the entry. Defaults to the JVM's default charset.
	 *
	 * @return The contents of the ZIP entry as a string.
	 */
	public String _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	source	= arguments.getAsString( Key.source );
		String	path	= arguments.getAsString( Key.entryPath );
		String	charSet	= arguments.getAsString( Key.charset );

		return ZipUtil.readEntry( source, path, charSet );
	}
}