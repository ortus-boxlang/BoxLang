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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
public class CreateTempDirectory extends BIF {

	/**
	 * Constructor
	 */
	public CreateTempDirectory() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.directory, FileSystemUtil.getTempDirectory() ),
		    new Argument( false, "string", Key.prefix, "" )
		};
	}

	/**
	 * Creates a temporary directory in the specified directory with the specified prefix if passed.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.directory The directory in which to create the temp directory, we default to the system temp directory
	 *
	 * @argument.prefix The prefix string to be used in generating the directory's name; may be empty
	 *
	 * @return The path to the directory as a string
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Path	directory	= Path.of( arguments.getAsString( Key.directory ) ).toAbsolutePath();
		String	prefix		= arguments.getAsString( Key.prefix );

		try {
			return Files.createTempDirectory(
			    directory,
			    prefix.length() > 0 ? prefix : null
			).toFile().getCanonicalPath();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

	}

}
