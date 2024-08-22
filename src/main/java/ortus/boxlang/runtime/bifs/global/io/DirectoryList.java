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
import java.util.stream.Stream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
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
		    // TODO: This can be a function that returns a boolean
		    new Argument( false, "string", Key.filter, "" ),
		    new Argument( false, "string", Key.sort, "name" ),
		    new Argument( false, "string", Key.type, "all" )
		};
	}

	/**
	 * List the contents of a directory. Returns either an array, or a query.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.path The path to the directory to list.
	 *
	 * @argument.recurse Whether to recurse into subdirectories.
	 *
	 * @argument.listInfo The type of information to return. Valid values are "path", "name", and "query".
	 *
	 * @argument.filter A filter to apply to the listing.
	 *
	 * @argument.type The type of items to list. Valid values are "all", "file", and "dir".
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			returnType	= arguments.getAsString( Key.listInfo ).toLowerCase();
		String directoryPath =  arguments.getAsString( Key.path );
		if( !FileSystemUtil.exists( directoryPath ) ) {
			directoryPath = FileSystemUtil.expandPath( context, directoryPath ).absolutePath().toString();
		}
		Stream<Path>	listing		= FileSystemUtil.listDirectory(
		    directoryPath,
		    arguments.getAsBoolean( Key.recurse ),
		    arguments.getAsString( Key.filter ),
		    arguments.getAsString( Key.sort ),
		    arguments.getAsString( Key.type )
		);

		return switch ( returnType ) {
			case "name" -> listingToNames( listing );
			case "query" -> listingToQuery( listing );
			default -> listingToPaths( listing );
		};

	}

	public Array listingToPaths( Stream<Path> listing ) {
		return ArrayCaster.cast( listing.map( item -> ( String ) item.toAbsolutePath().toString() ).toArray() );
	}

	public Array listingToNames( Stream<Path> listing ) {
		return ArrayCaster.cast( listing.map( item -> ( String ) item.getFileName().toString() ).toArray() );
	}

	public Query listingToQuery( Stream<Path> listing ) {
		Query listingQuery = new Query();
		listingQuery
		    .addColumn( Key.of( "name" ), QueryColumnType.VARCHAR )
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

}
