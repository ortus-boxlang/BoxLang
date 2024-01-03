/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public final class FileSystemUtil {

	public static final String	DEFAULT_CHARSET		= "UTF-8";

	/**
	 * MimeType suffixes which denote files which should be treated as text - e.g. application/json, application/xml, etc
	 */
	public static final Array	TEXT_MIME_SUFFIXES	= new Array(
	    new Object[] {
	        "json",
	        "xml",
	        "javascript",
	        "plain"
	    }
	);

	/**
	 * MimeType prefixes which denote text files - e.g. text/plain, text/x-yaml
	 */
	public static final Array	TEXT_MIME_PREFIXES	= new Array(
	    new Object[] {
	        "text"
	    }
	);

	/**
	 * Returns the contents of a file
	 *
	 * @param filePath
	 * @param charset
	 * @param bufferSize
	 *
	 * @return Object - Strings without a buffersize arg return the contents, with a buffersize arg a Buffered reader is returned, binary files return the
	 *         byte array
	 *
	 * @throws IOException
	 */
	public static Object read( String filePath, String charset, Integer bufferSize ) throws IOException {
		Path	path	= Paths.get( filePath );
		Charset	cs		= Charset.forName( DEFAULT_CHARSET );
		if ( charset != null ) {
			cs = Charset.forName( charset );
		}
		return isBinaryFile( filePath )
		    ? Files.readAllBytes( path )
		    : ( bufferSize == null
		        ? Files.readString( path, cs )
		        : new BufferedReader( Files.newBufferedReader( path, cs ), bufferSize ).lines().collect( Collectors.joining() ) );
	}

	/**
	 * Creates a directory from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or absolute.
	 *
	 * @throws IOException
	 */
	public static void createDirectory( String directoryPath ) throws IOException {
		Files.createDirectory( Path.of( directoryPath ) );
	}

	/**
	 * Deletes a file from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or absolute.
	 *
	 * @throws IOException
	 */
	public static void deleteDirectory( String directoryPath, Boolean recursive ) throws IOException {
		Path targetDirectory = Path.of( directoryPath );
		if ( recursive ) {
			Iterator<Path> fileIterator = Files.newDirectoryStream( targetDirectory ).iterator();
			while ( fileIterator.hasNext() ) {
				Path filePath = fileIterator.next();
				if ( Files.isDirectory( filePath ) ) {
					deleteDirectory( filePath.toString(), true );
				} else {
					Files.delete( filePath );
				}
			}
		}
		try {
			Files.delete( Path.of( directoryPath ) );
		} catch ( DirectoryNotEmptyException e ) {
			throw new BoxRuntimeException( "The directory " + directoryPath + " is not empty and may not be deleted without the recursive option." );
		}
	}

	/**
	 * Deletes a file from a string path.
	 *
	 * @param filePath the path to create. This can be root-relative or absolute.
	 *
	 * @throws IOException
	 */
	public static void deleteFile( String filePath ) throws IOException {
		Files.delete( Path.of( filePath ) );
	}

	/**
	 * Writes a file given a path and string contents using the default charset
	 *
	 * @param filePath
	 * @param contents
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, String contents ) throws IOException {
		write( filePath, contents.getBytes( DEFAULT_CHARSET ), false );
	}

	/**
	 * Writes a file given a string and specified charset
	 *
	 * @param filePath
	 * @param contents
	 * @param charset
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, String contents, String charset ) throws IOException {
		write( filePath, contents.getBytes( charset ), false );
	}

	/**
	 * Writes a file given a path, byte array, and a boolean as to whether the directory should be assured before the write
	 *
	 * @param filePath
	 * @param contents
	 * @param ensureDirectory
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, byte[] contents, Boolean ensureDirectory ) throws IOException {

		Path fileTarget = Path.of( filePath );

		if ( ensureDirectory && !Files.exists( fileTarget.getParent() ) ) {
			Files.createDirectory( fileTarget.getParent() );
		}

		Files.write( fileTarget, contents, StandardOpenOption.CREATE );

	}

	/**
	 * Tests whether a file is binary
	 *
	 * @param filePath
	 *
	 * @return a boolean as to whether the file is binary
	 *
	 * @throws IOException
	 */
	public static Boolean isBinaryFile( String filePath ) throws IOException {
		String		mimeType	= Files.probeContentType( Paths.get( filePath ) );
		Object[]	mimeParts	= mimeType.split( "/" );

		return !TEXT_MIME_PREFIXES.contains( ( String ) mimeParts[ 0 ] ) && !TEXT_MIME_PREFIXES.contains( ( String ) mimeParts[ mimeParts.length - 1 ] );
	}

	/**
	 * Tests whether a file or directory exists
	 *
	 * @param path Can be a relative or absolute path
	 *
	 * @return
	 */
	public static Boolean exists( String path ) {
		return Files.exists( Paths.get( path ) );
	}

}
