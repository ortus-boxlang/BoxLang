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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.util.ZipUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class Extract extends BIF {

	/**
	 * Constructor
	 */
	public Extract() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.format, Set.of( Validator.valueOneOf( "zip", "gzip" ) ) ),
		    new Argument( true, Argument.STRING, Key.source ),
		    new Argument( true, Argument.STRING, Key.destination ),
		    new Argument( false, Argument.BOOLEAN, Key.overwrite, false ),
		    new Argument( false, Argument.BOOLEAN, Key.recurse, true ),
		    new Argument( false, Argument.ANY, Key.filter ),
		    new Argument( false, Argument.ANY, Key.entryPaths ),
		};
	}

	/**
	 * Extract the source file or folder to the destination folder using the specified format:
	 * <p>
	 * - zip
	 * - gzip
	 * <p>
	 * The {@code overwrite} argument is used to overwrite the destination
	 * file if it already exists, else it will throw an exception. The default is {@code false}.
	 * <p>
	 * The {@code recurse} argument is used to extract the files recursively. The default is {@code true}.
	 * <p>
	 * The {@code filter} argument is used to filter the files to extract. It can be:
	 * <p>
	 * - A string with a regular expression to match the file names. Example: ".*\\.txt"
	 * - A Function/Lambda that receives the file name and returns a boolean. Example: (name) => name.endsWith(".txt")
	 * <p>
	 * The {@code entryPaths} argument is used to extract only the files that match the given paths. It can be a string
	 * or an array of strings with the paths to extract. Example: "folder1/file1.txt" or ["folder1/file1.txt", "folder2/file2.txt"]
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.format The format to use for the compression: zip or gzip.
	 *
	 * @argument.source The absolute path to the source file or folder to compress.
	 *
	 * @argument.destination The absolute path with a file name to save as the compressed file. Extension is optional.
	 *
	 * @argument.overwrite Whether to overwrite the destination file if it already exists. Default is false.
	 *
	 * @argument.recurse Whether to extract the files recursively. Default is true.
	 *
	 * @argument.filter A regular expression or a Function/Lambda to filter the files to extract.
	 *
	 * @argument.entryPaths The paths to extract. It can be a string or an array of strings.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	format			= arguments.getAsString( Key.format );
		String	source			= arguments.getAsString( Key.source );
		String	destination		= arguments.getAsString( Key.destination );
		Boolean	overwrite		= BooleanCaster.cast( arguments.get( Key.overwrite ) );
		Boolean	recurse			= BooleanCaster.cast( arguments.get( Key.recurse ) );

		Array	entryPaths		= new Array();
		Object	entryPathsValue	= arguments.get( Key.entryPaths );
		if ( entryPathsValue != null ) {
			if ( entryPathsValue instanceof String ) {
				entryPaths.add( entryPathsValue );
			} else if ( entryPathsValue instanceof Array ) {
				entryPaths = ( Array ) entryPathsValue;
			}
		}

		ZipUtil.extract(
		    ZipUtil.COMPRESSION_FORMAT.valueOf( format.toUpperCase() ),
		    source,
		    destination,
		    overwrite,
		    recurse,
		    arguments.get( Key.filter ),
		    entryPaths,
		    context
		);

		return null;
	}
}
