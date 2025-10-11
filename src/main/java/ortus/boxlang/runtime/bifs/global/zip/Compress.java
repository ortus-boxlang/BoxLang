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
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.ZipUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF( description = "Compress files into an archive" )
public class Compress extends BIF {

	/**
	 * Constructor
	 */
	public Compress() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.format, Set.of( Validator.valueOneOf( "zip", "gzip" ) ) ),
		    new Argument( true, Argument.STRING, Key.source ),
		    new Argument( true, Argument.STRING, Key.destination ),
		    new Argument( false, Argument.BOOLEAN, Key.includeBaseFolder, true ),
		    new Argument( false, Argument.BOOLEAN, Key.overwrite, false ),
		    new Argument( false, Argument.STRING, Key.prefix, Set.of( Validator.NON_EMPTY ) ),
		    new Argument( false, Argument.ANY, Key.filter ),
		    new Argument( false, Argument.BOOLEAN, Key.recurse, true ),
		    new Argument( false, Argument.INTEGER, Key.compressionLevel, ZipUtil.DEFAULT_COMPRESSION_LEVEL, Set.of( Validator.min( 0 ), Validator.max( 9 ) ) )
		};
	}

	/**
	 * Compress the source file or folder to the destination file or folder using
	 * the specified format:
	 * <p>
	 * - zip
	 * - gzip (It will all files separately to the destination folder)
	 * <p>
	 * The {@code includeBaseFolder} argument is used to include the base folder as the root
	 * of the compressed file. The default is {@code true}.
	 * <p>
	 * The {@code overwrite} argument is used to overwrite the destination
	 * file if it already exists, else it will throw an exception. The default is {@code false}.
	 * <p>
	 * <h2>Compression Levels</h2>
	 * The {@code compressionLevel} argument is used to specify the compression level to use for the compression.
	 * The default is {@code 6}, which is a good balance between speed and compression ratio.
	 * The valid range is from {@code 0} (no compression) to {@code 9} (maximum compression).
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
	 * @argument.includeBaseFolder Whether to include the base folder as the root of the compressed file. Default is true.
	 *
	 * @argument.overwrite Whether to overwrite the destination file if it already exists. Default is false.
	 *
	 * @argument.prefix The prefix directory to store the compressed files under. Default is empty.
	 *
	 * @argument.filter A regular expression to filter the files to compress or a function that receives the file name and returns a boolean.
	 *
	 * @argument.recurse Whether to compress the files recursively. Default is true.
	 *
	 * @argument.compressionLevel The compression level to use for the compression. Default is 6, which is a good balance between speed and compression ratio.
	 *
	 * @return The absolute path to the compressed file.
	 */
	public String _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	format				= arguments.getAsString( Key.format );
		String	source				= arguments.getAsString( Key.source );
		String	destination			= arguments.getAsString( Key.destination );
		boolean	includeBaseFolder	= BooleanCaster.cast( arguments.get( Key.includeBaseFolder ) );
		boolean	overwrite			= BooleanCaster.cast( arguments.get( Key.overwrite ) );
		Object	prefix				= arguments.get( Key.prefix );
		Object	filter				= arguments.get( Key.filter );
		Integer	compressionLevel	= arguments.getAsInteger( Key.compressionLevel );

		return ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.valueOf( format.toUpperCase() ),
		    source,
		    destination,
		    includeBaseFolder,
		    overwrite,
		    prefix == null ? "" : StringCaster.cast( prefix ),
		    filter,
		    BooleanCaster.cast( arguments.get( Key.recurse ) ),
		    compressionLevel,
		    context
		);
	}
}
