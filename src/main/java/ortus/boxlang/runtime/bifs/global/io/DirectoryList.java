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
import java.nio.file.Paths;
import java.util.stream.Stream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.util.FileSystemUtil;

@BoxBIF
public class DirectoryList extends BIF {

	/**
	 * Constructor
	 */
	public DirectoryList() {
		super();
		// path=string, recurse=boolean, listInfo=string, filter=any, sort=string, type=string
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.path ),
		    new Argument( true, "boolean", Key.recurse, false ),
		    new Argument( false, "string", Key.listInfo, "path" ),
		    new Argument( false, "any", Key.filter ),
		    new Argument( false, "string", Key.sort, "name" ),
		    new Argument( false, "string", Key.type, "all" )
		};
	}

	/**
	 * List the contents of a directory. Returns either an array, or a query depending on the {@code listInfo} argument.
	 * <p>
	 * The {@code listInfo} argument can be one of the following:
	 * <ul>
	 * <li>{@code name} - Returns an array of the names of the items in the directory.</li>
	 * <li>{@code path} - Returns an array of the absolute paths of the items in the directory.</li>
	 * <li>{@code query} - Returns a query of the items in the directory containing the following fields:
	 * <ul>
	 * <li>{@code attributes} - The attributes of the item (R, W, X, H).</li>
	 * <li>{@code dateLastModified} - The date the item was last modified.</li>
	 * <li>{@code directory} - The directory containing the item.</li>
	 * <li>{@code mode} - The mode of the item.</li>
	 * <li>{@code name} - The name of the item.</li>
	 * <li>{@code size} - The size of the item in bytes.</li>
	 * <li>{@code type} - The type of the item (either "Dir" or "File").</li>
	 * </ul>
	 * </li>
	 * </ul>
	 * <p>
	 * The {@code filter} argument can be the following:
	 * <ul>
	 * <li>
	 * A closure/lambda that takes a single argument (the path of the item) and returns a boolean. True to return it, false otherwise.
	 *
	 * <pre>
	 * DirectoryList( path: "/path/to/dir", filter: path -> path.endsWith(".txt") )
	 * </pre>
	 *
	 * </li>
	 * <li>
	 * A string that is a glob pattern: E.g. "*.txt" to only return files with the .txt extension. Or you can use the {@code |} pipe to separate multiple patterns: E.g. "*.txt|*.csv" to return files with either the .txt or .csv extension.
	 * </li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The absolute path to the directory to list.
	 *
	 * @argument.recurse Whether to recurse into subdirectories or not. The default is false.
	 *
	 * @argument.listInfo The type of information to return. Valid values are "name", "path", and "query". The default is "path".
	 *
	 * @argument.filter A filter to apply to the listing. This can be a function that takes a single argument (the path of the item) and returns a boolean or a string that is a glob pattern. The default is no filter.
	 *
	 * @argument.sort The sort order of the listing. Valid values are "name", "size", "date", and "type". The default is "name".You can also use <code>asc</code> or <code>desc</code> to specify the sort order. E.g. <code>sort: "name desc"</code>.
	 *
	 * @argument.type The type of items to list. Valid values are "all", "file", and "dir". Default is "all".
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	returnType		= arguments.getAsString( Key.listInfo ).toLowerCase();
		String	directoryPath	= arguments.getAsString( Key.path );
		if ( !FileSystemUtil.exists( directoryPath ) ) {
			directoryPath = FileSystemUtil.expandPath( context, directoryPath ).absolutePath().toString();
		}

		if ( arguments.get( Key.filter ) instanceof Function ) {
			arguments.put( Key.filter, createPredicate( context, ( Function ) arguments.get( Key.filter ) ) );
		}

		Stream<Path> listing = FileSystemUtil.listDirectory(
		    directoryPath,
		    arguments.getAsBoolean( Key.recurse ),
		    arguments.get( Key.filter ),
		    arguments.getAsString( Key.sort ),
		    arguments.getAsString( Key.type )
		);

		return switch ( returnType ) {
			case "name" -> listingToNames( listing );
			case "query" -> listingToQuery( listing );
			case "querynames" -> listingToQueryNames( listing, Paths.get( directoryPath ) );
			default -> listingToPaths( listing );
		};

	}

	public Array listingToPaths( Stream<Path> listing ) {
		return ArrayCaster.cast( listing.map( item -> item.toAbsolutePath().toString() ).toArray() );
	}

	public Array listingToNames( Stream<Path> listing ) {
		return ArrayCaster.cast( listing.map( item -> item.getFileName().toString() ).toArray() );
	}

	public Query listingToQuery( Stream<Path> listing ) {
		Query listingQuery = new Query();
		listingQuery
		    .addColumn( Key._NAME, QueryColumnType.VARCHAR )
		    .addColumn( Key.size, QueryColumnType.BIGINT )
		    .addColumn( Key.type, QueryColumnType.VARCHAR )
		    .addColumn( Key.dateLastModified, QueryColumnType.TIMESTAMP )
		    .addColumn( Key.attributes, QueryColumnType.VARCHAR )
		    .addColumn( Key.mode, QueryColumnType.VARCHAR )
		    .addColumn( Key.directory, QueryColumnType.VARCHAR );

		listing.forEachOrdered( item -> {
			try {
				listingQuery.addRow(
				    new Object[] {
				        item.getFileName().toString(),
				        Files.isDirectory( item ) ? 0L : Files.size( item ),
				        Files.isDirectory( item ) ? "Dir" : "File",
				        new DateTime( Files.getLastModifiedTime( item ).toInstant() ),
				        getAttributes( item ),
				        "",
				        item.getParent().toAbsolutePath().toString()
				    }
				);
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} );

		return listingQuery;
	}

	public Query listingToQueryNames( Stream<Path> listing, Path basePath ) {
		Query listingQuery = new Query();
		listingQuery
		    .addColumn( Key._NAME, QueryColumnType.VARCHAR );

		listing.forEachOrdered( item -> {
			listingQuery.addRow(
			    new Object[] {
			        basePath.relativize( item ).toString()
			    }
			);
		} );

		return listingQuery;
	}

	private static String getAttributes( Path file ) {
		String attributes = "";

		if ( Files.isReadable( file ) ) {
			attributes += "R";
		}
		if ( Files.isWritable( file ) ) {
			attributes += "W";
		}
		if ( Files.isExecutable( file ) ) {
			attributes += "X";
		}
		try {
			if ( Files.isHidden( file ) ) {
				attributes += "H";
			}
		} catch ( IOException e ) {
			// if we have an exception testing if the file is hidden it is not permissible so clear the attributes
			attributes = "";
		}

		return attributes;
	}

	private java.util.function.Predicate<Path> createPredicate( IBoxContext context, Function closure ) {
		return path -> BooleanCaster.cast( context.invokeFunction( closure, new Object[] { path.toString() } ) );
	}

}
