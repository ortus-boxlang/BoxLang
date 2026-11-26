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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF( description = "Copy a directory and its contents" )

public class DirectoryCopy extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryCopy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.source ),
		    new Argument( true, "string", Key.destination ),
		    new Argument( false, "boolean", Key.recurse, false ),
		    new Argument( false, "any", Key.filter, "*" ),
		    new Argument( false, "boolean", Key.createPath, true ),
		    new Argument( false, "boolean", Key.overwrite, false ),
		    new Argument( false, "boolean", Key.followLinks, false )
		};
	}

	/**
	 * Copies a directory and its contents to a new location. The source can be absolute or relative to the application, and an existing
	 * destination is merged into. Symbolic links inside the source are copied as links unless `followLinks` is true. Throws an exception if a
	 * file already exists at the destination and `overwrite` is false. Returns null.
	 *
	 * @argument.source The source directory.
	 *
	 * @argument.destination The destination directory.
	 *
	 * @argument.recurse Optional. Whether to copy sub-directories and their contents. Defaults to false, which copies only the files directly in
	 *                   the source.
	 *
	 * @argument.filter Optional. Which items to copy: a glob pattern such as `*.txt` (use `|` to separate multiple patterns), or a
	 *                  closure/lambda which receives the path of each item and returns a boolean. Defaults to `*` (everything).
	 *
	 * @argument.createPath Optional. Whether to create any missing parent directories of the destination. Defaults to true.
	 *
	 * @argument.overwrite Optional. Whether to overwrite existing files at the destination. Defaults to false.
	 *
	 * @argument.followLinks Optional. Whether to follow symbolic links. When false (the default) a link inside the source is copied as a link.
	 *                       When true, the content the link points to is copied instead.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return Always null.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	sourcePath		= arguments.getAsString( Key.source );
		String	destinationPath	= arguments.getAsString( Key.destination );

		if ( !FileSystemUtil.exists( sourcePath ) ) {
			sourcePath = FileSystemUtil.expandPath( context, sourcePath ).absolutePath().toString();
		}

		destinationPath = FileSystemUtil.expandPath( context, destinationPath ).absolutePath().toString();

		if ( arguments.get( Key.filter ) instanceof Function filterFunction ) {
			arguments.put( Key.filter, FileSystemUtil.createPathFilterPredicate( context, filterFunction ) );
		}

		FileSystemUtil.copyDirectory(
		    sourcePath,
		    destinationPath,
		    arguments.getAsBoolean( Key.recurse ),
		    arguments.get( Key.filter ),
		    arguments.getAsBoolean( Key.createPath ),
		    arguments.getAsBoolean( Key.overwrite ),
		    // older version of compat override this BIF and don't have this arg, so treat the value carefully for now
		    BooleanCaster.attempt( arguments.get( Key.followLinks ) ).orElse( false )
		);

		return null;
	}

}
