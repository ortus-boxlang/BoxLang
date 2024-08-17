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
import ortus.boxlang.runtime.util.ZipUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
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
		    new Argument( false, Argument.BOOLEAN, Key.overwrite, false )
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
	 */
	public String _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	format				= arguments.getAsString( Key.format );
		String	source				= arguments.getAsString( Key.source );
		String	destination			= arguments.getAsString( Key.destination );
		boolean	includeBaseFolder	= BooleanCaster.cast( arguments.get( Key.includeBaseFolder ) );
		boolean	overwrite			= BooleanCaster.cast( arguments.get( Key.overwrite ) );

		return ZipUtil.compress(
		    ZipUtil.COMPRESSION_FORMAT.valueOf( format.toUpperCase() ),
		    source,
		    destination,
		    includeBaseFolder,
		    overwrite
		);
	}
}
