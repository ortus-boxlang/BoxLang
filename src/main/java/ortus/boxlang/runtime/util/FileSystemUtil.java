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
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
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

	public static final Boolean	isWindows			= SystemUtils.IS_OS_WINDOWS;

	/**
	 * The OS line separator
	 */
	private static final String	lineSeparator		= System.getProperty( "line.separator" );

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
		Path	path	= null;
		Boolean	isURL	= false;
		if ( filePath.substring( 0, 4 ).toLowerCase().equals( "http" ) ) {
			isURL = true;
		} else {
			path = Path.of( filePath );
		}
		Charset cs = Charset.forName( DEFAULT_CHARSET );
		if ( charset != null ) {
			cs = Charset.forName( charset );
		}
		if ( isURL ) {
			try {
				URL fileURL = new URL( filePath );
				if ( isBinaryFile( filePath ) ) {
					return IOUtils.toByteArray( fileURL.openStream() );
				} else {
					InputStreamReader	inputReader	= new InputStreamReader( fileURL.openStream() );
					BufferedReader		reader		= new BufferedReader( inputReader );
					return ( String ) reader.lines().parallel().collect( Collectors.joining( lineSeparator ) );
				}
			} catch ( MalformedURLException e ) {
				throw new BoxRuntimeException( "The url [" + filePath + "] could not be parsed.  The reason was:" + e.getMessage() + "(" + e.getCause() + ")" );
			}

		} else {
			return isBinaryFile( filePath )
			    ? Files.readAllBytes( path )
			    : ( bufferSize == null
			        ? Files.readString( path, cs )
			        : new BufferedReader( Files.newBufferedReader( path, cs ), bufferSize ).lines().parallel().collect( Collectors.joining( lineSeparator ) ) );
		}
	}

	/**
	 * Returns the contents of a file with the defaults
	 *
	 * @param filePath
	 */
	public static Object read( String filePath ) throws IOException {
		return read( filePath, null, null );
	}

	/**
	 * Creates a directory from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or absolute.
	 *
	 * @throws IOException
	 */
	public static void createDirectory( String directoryPath ) throws IOException {
		Files.createDirectories( Path.of( directoryPath ) );
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

	public static Stream<Path> listDirectory( String path, Boolean recurse, String filter, String sort, String type ) {
		final PathMatcher	pathMatcher		= FileSystems.getDefault().getPathMatcher( "glob:" + filter );
		String[]			sortElements	= sort.split( ( "\\s+" ) );
		String				sortField		= sortElements[ 0 ];
		String				sortDirection	= sortElements.length > 1 ? sortElements[ 1 ].toLowerCase() : "asc";
		Comparator<Path>	pathSort		= null;
		Stream<Path>		directoryStream	= null;

		switch ( sortField ) {
			case "size" :
				pathSort = ( final Path a, final Path b ) -> {
					try {
						return sortDirection.equals( "desc" )
						    ? ( int ) Long.compareUnsigned( Files.size( b ), Files.size( a ) )
						    : ( int ) Long.compareUnsigned( Files.size( a ), Files.size( b ) );
					} catch ( IOException e ) {
						return ( int ) Long.compareUnsigned( 0l, 0l );
					}

				};
			case "directory" :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.getParent().toString().compareTo( a.getParent().toString() )
				    : a.getParent().toString().compareTo( b.getParent().toString() );
			case "namenocase" :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.toString().toLowerCase().compareTo( a.toString().toLowerCase() )
				    : a.toString().toLowerCase().compareTo( b.toString().toLowerCase() );
			default :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.toString().compareTo( a.toString() )
				    : a.toString().compareTo( b.toString() );

		}

		try {
			if ( recurse ) {
				directoryStream = Files.walk( Path.of( path ) ).parallel();
			} else {
				directoryStream = Files.walk( Path.of( path ), 1 ).parallel();
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return filter.length() > 1
		    ? directoryStream.filter( item -> matchesType( item, type ) && pathMatcher.matches( item.getFileName() ) ).sorted( pathSort )
		    : directoryStream.filter( item -> matchesType( item, type ) ).sorted( pathSort );
	}

	private static Boolean matchesType( Path item, String type ) {
		switch ( type ) {
			case "directory" :
				return Files.isDirectory( item );
			case "file" :
				return !Files.isDirectory( item );
			default :
				return true;
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
		write( filePath, contents.getBytes( Charset.forName( charset ) ), false );
	}

	/**
	 * Writes a file given a string and specified charset with ensured directory
	 *
	 * @param filePath
	 * @param contents
	 * @param charset
	 * @param ensureDirectory boolean
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, String contents, String charset, Boolean ensureDirectory ) throws IOException {
		write( filePath, contents.getBytes( Charset.forName( charset ) ), ensureDirectory );
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
			Files.createDirectories( fileTarget.getParent() );
		}

		Files.write( fileTarget, contents, StandardOpenOption.CREATE );

	}

	public static void move( String source, String destination ) {
		Path	start	= Path.of( source );
		Path	end		= Path.of( destination );
		try {
			Files.move( start, end );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	public static void copyDirectory( String source, String destination, Boolean recurse, String filter, Boolean createPaths ) {
		Path	start	= Path.of( source );
		Path	end		= Path.of( destination );
		if ( createPaths && !Files.exists( end ) ) {
			try {
				Files.createDirectories( end );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
		if ( Files.isDirectory( start ) ) {
			listDirectory( source, recurse, filter, "name", recurse ? "all" : "file" ).forEachOrdered( path -> {
				Path	targetPath		= Path.of( path.toString().replace( source, destination ) );
				Path	targetParent	= targetPath.getParent();
				if ( recurse && !Files.exists( targetParent ) ) {
					try {
						Files.createDirectories( targetParent );
					} catch ( IOException e ) {
						throw new BoxIOException( e );
					}

				}
				try {
					// our stream is in parallel async so if this is a directory it may already have been created
					if ( !Files.isDirectory( targetPath ) || ( Files.isDirectory( targetPath ) && !Files.exists( targetPath ) ) ) {
						Files.copy( path, targetPath );
					}
				} catch ( IOException e ) {
					throw new BoxIOException( e );
				}
			} );
		} else {
			try {
				Files.copy( start, end );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

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
		String mimeType = null;
		if ( filePath.substring( 0, 4 ).toLowerCase().equals( "http" ) ) {
			mimeType = Files.probeContentType( Paths.get( new URL( filePath ).getFile() ).getFileName() );
			// if we can't determin a mimetype from a path we assume the file is text ( e.g. a friendly URL )
			if ( mimeType == null ) {
				return false;
			}
		} else {
			mimeType = Files.probeContentType( Paths.get( filePath ).getFileName() );
		}
		Object[] mimeParts = mimeType.split( "/" );

		return !TEXT_MIME_PREFIXES.contains( ( String ) mimeParts[ 0 ] ) && !TEXT_MIME_PREFIXES.contains( ( String ) mimeParts[ mimeParts.length - 1 ] );
	}

	/**
	 * gets the posix permission of a file or directory
	 *
	 * @param filePath the file path string
	 *
	 * @return the permission set
	 *
	 * @throws IOException
	 */
	public static Set<PosixFilePermission> getPosixPermissions( String filePath ) throws IOException {
		Path path = Path.of( filePath );
		if ( !isPosixCompliant( path ) ) {
			throw new BoxRuntimeException( "The underlying file system for path  [" + filePath + "] is not posix compliant." );
		} else {
			return Files.getPosixFilePermissions( Path.of( filePath ) );
		}
	}

	/**
	 * Tests whether the file system requested by the path is posix compliant
	 *
	 * @param filePath the file path string
	 *
	 * @return A boolean as to whether the the file system is compliant
	 */
	public static Boolean isPosixCompliant( Object filePath ) {
		Path path = null;
		if ( filePath instanceof String ) {
			path = Path.of( ( String ) filePath );
		} else {
			path = ( Path ) filePath;
		}
		return path.getFileSystem().supportedFileAttributeViews().contains( "posix" );
	}

	/**
	 * Sets the posix permissions on a file
	 *
	 * @param filePath the file path string or File instance
	 * @param mode     the cast three-digit string noting the permissions
	 */
	public static void setPosixPermissions( Object filePath, String mode ) {
		Path path = null;
		if ( filePath instanceof String ) {
			path = Path.of( ( String ) filePath );
		} else {
			path = ( Path ) filePath;
		}
		if ( !isPosixCompliant( path ) ) {
			throw new BoxRuntimeException( "The underlying file system for path  [" + filePath + "] is not posix compliant." );
		} else if ( mode.length() != 3 ) {
			throw new BoxRuntimeException( "The file or directory mode [" + mode + "] is not a valid permission set." );
		} else {
			try {
				// try an integer cast to make sure it's a valid directive set
				try {
					IntegerCaster.cast( mode );
				} catch ( Exception e ) {
					throw new BoxRuntimeException( "The file or directory mode [" + mode + "] is not a valid permission set." );
				}
				Files.setPosixFilePermissions( path, integerToPosixPermissions( mode ) );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
	}

	/**
	 * Utility method to parse numeric permission mode in to a usable permission set
	 *
	 * @param mode The numeric string representation of the mode ( e.g. 755, 644, etc )
	 *
	 * @return The PosixFilePermission set
	 */
	private static Set<PosixFilePermission> integerToPosixPermissions( String mode ) {
		final char[]	directiveSet	= mode.toCharArray();
		final char[]	attributeSet	= { '-', '-', '-', '-', '-', '-', '-', '-', '-' };
		for ( int i = directiveSet.length - 1; i >= 0; i-- ) {
			int n = directiveSet[ i ] - '0';
			if ( i == directiveSet.length - 1 ) {
				if ( ( n & 1 ) != 0 )
					attributeSet[ 8 ] = 'x';
				if ( ( n & 2 ) != 0 )
					attributeSet[ 7 ] = 'w';
				if ( ( n & 4 ) != 0 )
					attributeSet[ 6 ] = 'r';
			} else if ( i == directiveSet.length - 2 ) {
				if ( ( n & 1 ) != 0 )
					attributeSet[ 5 ] = 'x';
				if ( ( n & 2 ) != 0 )
					attributeSet[ 4 ] = 'w';
				if ( ( n & 4 ) != 0 )
					attributeSet[ 3 ] = 'r';
			} else if ( i == directiveSet.length - 3 ) {
				if ( ( n & 1 ) != 0 )
					attributeSet[ 2 ] = 'x';
				if ( ( n & 2 ) != 0 )
					attributeSet[ 1 ] = 'w';
				if ( ( n & 4 ) != 0 )
					attributeSet[ 0 ] = 'r';
			}
		}
		String attributes = new String( attributeSet );
		return PosixFilePermissions.fromString( attributes );
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
