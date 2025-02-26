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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.ListUtil;

public final class FileSystemUtil {

	/**
	 * Flag for whether FS is case sensitive or not to short circuit case insensitive path resolution
	 */
	private static boolean					isCaseSensitiveFS		= caseSensitivityCheck();

	/**
	 * The default charset for file operations in BoxLang
	 */
	public static final Charset				DEFAULT_CHARSET			= StandardCharsets.UTF_8;

	/**
	 * MimeType suffixes which denote files which should be treated as text - e.g.
	 * application/json, application/xml, etc
	 */
	public static final ArrayList<String>	TEXT_MIME_SUFFIXES		= new ArrayList<String>() {

																		{
																			add( "json" );
																			add( "xml" );
																			add( "javascript" );
																			add( "plain" );
																			add( "pkcs" );
																			add( "x-x509" );
																			add( "x-pem" );
																			add( "x-x509" );
																		}
																	};

	/**
	 * MimeType prefixes which denote text files - e.g. text/plain, text/x-yaml
	 */
	public static final ArrayList<String>	TEXT_MIME_PREFIXES		= new ArrayList<String>() {

																		{
																			add( "text" );
																		}
																	};

	/**
	 * Octal representations for Posix strings to octals
	 * Thanks to
	 * http://www.java2s.com/example/java-utility-method/posix/tooctalfilemode-set-posixfilepermission-permissions-64fb4.html
	 */
	private static final int				OWNER_READ_FILEMODE		= 0400;
	private static final int				OWNER_WRITE_FILEMODE	= 0200;
	private static final int				OWNER_EXEC_FILEMODE		= 0100;
	private static final int				GROUP_READ_FILEMODE		= 0040;
	private static final int				GROUP_WRITE_FILEMODE	= 0020;
	private static final int				GROUP_EXEC_FILEMODE		= 0010;
	private static final int				OTHERS_READ_FILEMODE	= 0004;
	private static final int				OTHERS_WRITE_FILEMODE	= 0002;
	private static final int				OTHERS_EXEC_FILEMODE	= 0001;

	/**
	 * The Necessary constants for the file mode
	 */
	public static final boolean				IS_WINDOWS				= SystemUtils.IS_OS_WINDOWS;

	/**
	 * The OS line separator
	 */
	public static final String				LINE_SEPARATOR			= System.getProperty( "line.separator" );

	/**
	 * A starting file slash prefix
	 */
	public static final String				SLASH_PREFIX			= "/";

	private static InterceptorService		interceptorService		= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * Returns the contents of a file
	 *
	 * @param filePath
	 * @param charset
	 * @param bufferSize
	 * @param resultsAsString
	 *
	 * @return Object - Strings without a buffersize arg return the contents, with a
	 *         buffersize arg a Buffered reader is returned, binary files return the
	 *         byte array unless `resultsAsString` is true, in which case a string will always be returned
	 *
	 * @throws IOException
	 */
	public static Object read( String filePath, String charset, Integer bufferSize, boolean resultsAsString ) {
		Path	path	= null;
		boolean	isURL	= false;
		if ( filePath.substring( 0, 4 ).equalsIgnoreCase( "http" ) ) {
			isURL = true;
		} else {
			path = Path.of( filePath );
		}

		boolean allowBinary = !resultsAsString;

		try {
			if ( isURL ) {
				try {
					URL fileURL = URI.create( filePath ).toURL();
					if ( allowBinary && isBinaryFile( filePath ) ) {
						return IOUtils.toByteArray( fileURL.openStream() );
					} else {
						return StringCaster.cast( fileURL.openStream(), charset, true );
					}
				} catch ( MalformedURLException e ) {
					throw new BoxRuntimeException(
					    "The url [" + filePath + "] could not be parsed.  The reason was:" + e.getMessage() + "("
					        + e.getCause() + ")" );
				}

			} else {
				if ( allowBinary && isBinaryFile( filePath ) ) {
					return Files.readAllBytes( path );
				} else if ( bufferSize == null ) {
					return StringCaster.cast( Files.newInputStream( path ), charset, true );
				} else {
					// @formatter:off
					try (
					    BOMInputStream inputStream = BOMInputStream.builder()
					        .setPath( path )
					        .setByteOrderMarks( ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_32BE,
					            ByteOrderMark.UTF_32LE )
					        .setInclude( false )
					        .get() 
						) {
							InputStreamReader inputReader = null;
							if ( charset != null ) {
								inputReader = new InputStreamReader( inputStream, charset );
							} else {
								inputReader = new InputStreamReader( inputStream );
							}
							try ( BufferedReader reader = new BufferedReader( inputReader, bufferSize ) ) {
								return reader.lines().collect( Collectors.joining( FileSystemUtil.LINE_SEPARATOR ) );
							}
						}
					// @formatter:on
				}
			}

		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Returns the contents of a file
	 *
	 * @param filePath
	 * @param charset
	 * @param bufferSize
	 *
	 * @return Object - Strings without a buffersize arg return the contents, with a
	 *         buffersize arg a Buffered reader is returned, binary files return the
	 *         byte array
	 *
	 * @throws IOException
	 */
	public static Object read( String filePath, String charset, Integer bufferSize ) {
		return read( filePath, charset, bufferSize, false );
	}

	/**
	 * Returns the contents of a file with the defaults
	 *
	 * @param filePath
	 */
	public static Object read( String filePath ) {
		return read( filePath, null, null );
	}

	/**
	 * Creates a directory from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or
	 *                      absolute.
	 *
	 * @throws IOException
	 */
	public static void createDirectory( String directoryPath ) {
		createDirectory( directoryPath, true, null );
	}

	/**
	 * Creates a directory from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or
	 *                      absolute.
	 *
	 * @throws IOException
	 */
	public static void createDirectory( String directoryPath, Boolean createPath, String mode ) {
		try {
			if ( createPath ) {
				Files.createDirectories( Path.of( directoryPath ) );
			} else {
				Files.createDirectory( Path.of( directoryPath ) );
			}

			if ( mode != null ) {
				FileSystemUtil.setPosixPermissions( directoryPath, mode );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Deletes a file from a string path.
	 *
	 * @param directoryPath the path to create. This can be root-relative or
	 *                      absolute.
	 *
	 * @throws IOException
	 */
	public static void deleteDirectory( String directoryPath, Boolean recursive ) {
		Path targetDirectory = Path.of( directoryPath );

		if ( recursive ) {
			try ( DirectoryStream<Path> stream = Files.newDirectoryStream( targetDirectory ) ) {
				for ( Path entry : stream ) {
					if ( Files.isDirectory( entry ) ) {
						deleteDirectory( entry.toString(), true );
					} else {
						Files.delete( entry );
					}
				}
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

		try {
			Files.delete( targetDirectory );
		} catch ( DirectoryNotEmptyException e ) {
			throw new BoxRuntimeException( "The directory " + directoryPath
			    + " is not empty and may not be deleted without the recursive option." );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Lists the contents of a directory
	 *
	 * @param path    the path to list
	 * @param recurse whether to recurse into subdirectories
	 * @param filter  a glob filter or a closure to filter the results as a Predicate
	 * @param sort    a string containing the sort field and direction
	 * @param type    the type of files to list
	 *
	 * @return a stream of paths
	 */
	@SuppressWarnings( "unchecked" )
	public static Stream<Path> listDirectory( String path, Boolean recurse, Object filter, String sort, String type ) {
		Path targetPath = Path.of( path );

		// If path doesn't exist, return an empty stream
		if ( !Files.exists( targetPath ) ) {
			return Stream.empty();
		}

		// Setup variables
		final String		theType			= type.toLowerCase();
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
						return Long.compareUnsigned( 0l, 0l );
					}
				};
				break;
			case "directory" :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.getParent().toString().compareTo( a.getParent().toString() )
				    : a.getParent().toString().compareTo( b.getParent().toString() );
				break;
			case "namenocase" :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.toString().toLowerCase().compareTo( a.toString().toLowerCase() )
				    : a.toString().toLowerCase().compareTo( b.toString().toLowerCase() );
				break;
			default :
				pathSort = ( final Path a, final Path b ) -> sortDirection.equals( "desc" )
				    ? b.toString().compareTo( a.toString() )
				    : a.toString().compareTo( b.toString() );
				break;
		}

		try {
			if ( recurse ) {
				directoryStream = Files.walk( targetPath ).parallel().filter( filterPath -> !filterPath.equals( targetPath ) );
			} else {
				directoryStream = Files.walk( targetPath, 1 ).parallel().filter( filterPath -> !filterPath.equals( targetPath ) );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		// Apply the type filter
		directoryStream = directoryStream.filter( item -> matchesType( item, theType ) );

		// Is the filter a string or a closure?
		if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
			ArrayList<PathMatcher> pathMatchers = ListUtil
			    .asList( castedFilter, "|" )
			    .stream()
			    .map( filterString -> FileSystems.getDefault().getPathMatcher( "glob:" + filterString ) )
			    .collect( Collectors.toCollection( ArrayList::new ) );
			directoryStream = directoryStream.filter( item -> pathMatchers.stream().anyMatch( pathMatcher -> pathMatcher.matches( item.getFileName() ) ) );
		}
		// Predicate filter
		else if ( filter instanceof java.util.function.Predicate<?> ) {
			directoryStream = directoryStream.filter( ( java.util.function.Predicate<Path> ) filter );
		}

		// Finally, sort the stream
		return directoryStream.sorted( pathSort );
	}

	/**
	 * Matches the type of a file or directory
	 *
	 * @param item the path to match
	 * @param type the type to match
	 *
	 * @return a boolean as to whether the path matches the type
	 */
	private static Boolean matchesType( Path item, String type ) {
		switch ( type ) {
			case "directory" :
			case "dir" :
				return Files.isDirectory( item );
			case "file" :
				return Files.isRegularFile( item );
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
	public static void deleteFile( String filePath ) {
		try {
			Files.delete( Path.of( filePath ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Writes a file given a path and string contents using the default charset
	 *
	 * @param filePath
	 * @param contents
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, String contents ) {
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
	public static void write( String filePath, String contents, String charset ) {
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
	public static void write( String filePath, String contents, String charset, Boolean ensureDirectory ) {
		write( filePath, contents.getBytes( Charset.forName( charset ) ), ensureDirectory );
	}

	/**
	 * Writes a file given a path, byte array, and a boolean as to whether the
	 * directory should be assured before the write
	 *
	 * @param filePath
	 * @param contents
	 * @param ensureDirectory
	 *
	 * @throws IOException
	 */
	public static void write( String filePath, byte[] contents, Boolean ensureDirectory ) {

		Path fileTarget = Path.of( filePath );

		try {

			if ( ensureDirectory && !Files.exists( fileTarget.getParent() ) ) {
				Files.createDirectories( fileTarget.getParent() );
			}

			Files.write( fileTarget, contents, StandardOpenOption.CREATE );

		} catch ( NoSuchFileException e ) {
			throw new BoxRuntimeException(
			    "The file [" + filePath + "] could not be writtent. The directory ["
			        + Path.of( filePath ).getParent().toString() + "] does not exist." );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

	}

	public static void move( String source, String destination ) {
		move( source, destination, true );
	}

	public static void move( String source, String destination, boolean createPath ) {
		Path	start	= Path.of( source );
		Path	end		= Path.of( destination );
		if ( !createPath && !Files.exists( end.getParent() ) ) {
			throw new BoxRuntimeException( "The directory [" + end.toAbsolutePath().toString()
			    + "] cannot be created because the parent directory [" + end.getParent().toAbsolutePath().toString()
			    + "] does not exist.  To prevent this error set the createPath argument to true." );
		} else if ( Files.exists( end ) ) {
			throw new BoxRuntimeException( "The target path of [" + end.toAbsolutePath().toString() + "] already exists" );
		} else {
			try {
				if ( createPath ) {
					Files.createDirectories( end.getParent() );
				}
				Files.move( start, end );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
	}

	public static void copyDirectory(
	    String source,
	    String destination,
	    Boolean recurse,
	    Object filter,
	    Boolean createPaths ) {
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
			Stream<Path> directoryListing = null;
			if ( filter instanceof String filterString ) {
				directoryListing = listDirectory( source, recurse, filterString, "name", recurse ? "all" : "file" );
			} else if ( filter instanceof java.util.function.Predicate<?> filterPredicate ) {
				directoryListing = listDirectory( source, recurse, createPaths ? filterPredicate : null, "name", recurse ? "all" : "file" );
			} else {
				throw new BoxRuntimeException( "The filter argument to the method DirectoryCopy filter must either be a string or function/closure" );
			}
			directoryListing.forEachOrdered( path -> {
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
					// our stream is in parallel async so if this is a directory it may already have
					// been created
					if ( !Files.isDirectory( targetPath )
					    || ( Files.isDirectory( targetPath ) && !Files.exists( targetPath ) ) ) {
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
	 * Is the file path a valid file path. If the test throws an exception, it is
	 * assumed that the file path is invalid.
	 *
	 * @param filePath the file path
	 *
	 * @return a boolean as to whether the file path is valid
	 */
	public static Boolean isValidFilePath( Path filePath ) {
		return Files.exists( filePath );
	}

	/**
	 * Is the file path a valid file path
	 *
	 * @param filePath the file path string
	 *
	 * @return a boolean as to whether the file path is valid
	 */
	public static Boolean isValidFilePath( String filePath ) {
		try {
			return isValidFilePath( Path.of( filePath ) );
		} catch ( InvalidPathException e ) {
			return false;
		}
	}

	/**
	 * Tests whether a file is binary
	 *
	 * @param filePath the file path string
	 *
	 * @return a boolean as to whether the file is binary
	 *
	 * @throws IOException
	 */
	public static Boolean isBinaryFile( String filePath ) {
		return isBinaryMimeType( getMimeType( filePath ) );
	}

	/**
	 * Tests whether a given mime type is a binary mime type
	 * 
	 * @param mimeType
	 * 
	 * @return
	 */
	public static Boolean isBinaryMimeType( String mimeType ) {
		// if we can't determine a mimetype from a path we assume the file is text (
		// e.g. a friendly URL )
		if ( mimeType == null || mimeType.split( "/" ).length == 1 ) {
			return false;
		}

		// if this is a content-type header we need to strip the charset
		if ( mimeType.split( ";" ).length > 1 ) {
			mimeType = mimeType.split( ";" )[ 0 ];
		}

		String[] mimeParts = mimeType.split( "/" );

		return !TEXT_MIME_PREFIXES.contains( mimeParts[ 0 ] )
		    && !TEXT_MIME_SUFFIXES.stream().anyMatch( suffix -> mimeParts[ 1 ].startsWith( StringCaster.cast( suffix ).toLowerCase() ) );
	}

	/**
	 * Gets the MIME type for the file path/file object you have specified.
	 *
	 * @param filePath
	 *
	 * @return
	 */
	public static String getMimeType( String filePath ) {
		try {
			if ( filePath.substring( 0, 4 ).equalsIgnoreCase( "http" ) ) {
				return Files.probeContentType( Paths.get( new URI( filePath ).toURL().getFile() ).getFileName() );
			} else {
				return Files.probeContentType( Paths.get( filePath ).getFileName() );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		} catch ( URISyntaxException e ) {
			throw new BoxRuntimeException( "The provided URL [" + filePath + "] is not a valid. " + e.getMessage() );
		}
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
	public static Set<PosixFilePermission> getPosixPermissions( String filePath ) {
		Path path = Path.of( filePath );
		if ( !isPosixCompliant( path ) ) {
			throw new BoxRuntimeException(
			    "The underlying file system for path  [" + filePath + "] is not posix compliant." );
		} else {
			try {
				return Files.getPosixFilePermissions( Path.of( filePath ) );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
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
			throw new BoxRuntimeException(
			    "The underlying file system for path  [" + filePath + "] is not posix compliant." );
		} else if ( mode.length() != 3 ) {
			throw new BoxRuntimeException( "The file or directory mode [" + mode + "] is not a valid permission set." );
		} else {
			try {
				// try an integer cast to make sure it's a valid directive set
				try {
					IntegerCaster.cast( mode );
				} catch ( Exception e ) {
					throw new BoxRuntimeException(
					    "The file or directory mode [" + mode + "] is not a valid permission set." );
				}
				Files.setPosixFilePermissions( path, octalToPosixPermissions( mode ) );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
	}

	/**
	 * Utility method to parse numeric permission mode in to a usable permission set
	 *
	 * @param mode The numeric string representation of the mode ( e.g. 755, 644,
	 *             etc )
	 *
	 * @return The PosixFilePermission set
	 */
	private static Set<PosixFilePermission> octalToPosixPermissions( String mode ) {
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
	 * Converts a set of {@link PosixFilePermission} to chmod-style octal file mode.
	 *
	 * @param permissions the posix permissions set
	 *
	 * @return the octal integer representing the permission set
	 */
	public static int posixSetToOctal( Set<PosixFilePermission> permissions ) {
		int octal = 0;
		for ( PosixFilePermission permissionBit : permissions ) {
			switch ( permissionBit ) {
				case OWNER_READ :
					octal |= OWNER_READ_FILEMODE;
					break;
				case OWNER_WRITE :
					octal |= OWNER_WRITE_FILEMODE;
					break;
				case OWNER_EXECUTE :
					octal |= OWNER_EXEC_FILEMODE;
					break;
				case GROUP_READ :
					octal |= GROUP_READ_FILEMODE;
					break;
				case GROUP_WRITE :
					octal |= GROUP_WRITE_FILEMODE;
					break;
				case GROUP_EXECUTE :
					octal |= GROUP_EXEC_FILEMODE;
					break;
				case OTHERS_READ :
					octal |= OTHERS_READ_FILEMODE;
					break;
				case OTHERS_WRITE :
					octal |= OTHERS_WRITE_FILEMODE;
					break;
				case OTHERS_EXECUTE :
					octal |= OTHERS_EXEC_FILEMODE;
					break;
			}
		}
		return octal;
	}

	/**
	 * Tests whether a file or directory exists
	 *
	 * @param path Can be a relative or absolute path
	 *
	 * @return
	 */
	public static Boolean exists( String path ) {
		try {
			return Files.exists( Paths.get( path ) );
		} catch ( java.nio.file.InvalidPathException e ) {
			return false;
		}
	}

	/**
	 * Returns a struct of information on a file - supports the FileInfo and
	 * GetFileInfo BIFs
	 *
	 * @param filePath The filepath or File object
	 * @param verbose  Currently returns the GetFileInfo additional information -
	 *                 this should be either deprecated or expanded at a later date
	 *
	 * @return a Struct containing the info of the file or directory
	 */
	public static IStruct info( Object filePath, Boolean verbose ) {
		Path path = null;
		if ( filePath instanceof String ) {
			path = Path.of( ( String ) filePath );
		} else {
			path = ( Path ) filePath;
		}
		IStruct infoStruct = new Struct();
		try {
			infoStruct.put( "name", path.getFileName().toString() );
			infoStruct.put( "path", path.toAbsolutePath().toString() );
			infoStruct.put( "size", Files.isDirectory( path ) ? 0l : Files.size( path ) );
			infoStruct.put( "type", Files.isDirectory( path ) ? "dir" : "file" );
			infoStruct.put( "lastModified",
			    new DateTime( Files.getLastModifiedTime( path ).toInstant() ).setFormat( "MMMM, d, yyyy HH:mm:ss Z" ) );
			if ( verbose == null || !verbose ) {
				// fileInfo method compatibile keys
				infoStruct.put( "attributes", "" );
				infoStruct.put( "mode",
				    isPosixCompliant( path ) ? StringCaster.cast( posixSetToOctal( Files.getPosixFilePermissions( path ) ) )
				        : "" );
				infoStruct.put( "read", Files.isReadable( path ) );
				infoStruct.put( "write", Files.isWritable( path ) );
				infoStruct.put( "execute", Files.isExecutable( path ) );
				infoStruct.put( "checksum", !Files.isDirectory( path ) ? EncryptionUtil.checksum( path, "md5" ) : "" );
			} else {
				// getFileInfo method compatibile keys
				infoStruct.put( "parent", path.getParent().toAbsolutePath().toString() );
				infoStruct.put( "isHidden", Files.isHidden( path ) );
				infoStruct.put( "canRead", Files.isReadable( path ) );
				infoStruct.put( "canWrite", Files.isWritable( path ) );
				infoStruct.put( "canExecute", Files.isExecutable( path ) );
				infoStruct.put( "isArchive", IS_WINDOWS ? Files.getAttribute( path, "dos:archive" ) : false );
				infoStruct.put( "isSystem", IS_WINDOWS ? Files.getAttribute( path, "dos:system" ) : false );
				infoStruct.put( "isAttributesSupported", IS_WINDOWS );
				infoStruct.put( "isModeSupported", isPosixCompliant( path ) );
				infoStruct.put( "isCaseSensitive",
				    !Files.exists( Path.of( path.toAbsolutePath().toString().toUpperCase() ) ) );
			}
			return infoStruct;
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Take input stream, read it, and return byte array
	 *
	 * @param inputStream The input stream to read
	 *
	 * @return The byte array
	 */
	public static byte[] convertInputStreamToByteArray( InputStream inputStream ) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

		try ( byteArrayOutputStream ) {
			byte[]	buffer	= new byte[ 1024 ];
			int		length;
			while ( ( length = inputStream.read( buffer ) ) != -1 ) {
				byteArrayOutputStream.write( buffer, 0, length );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}

		return byteArrayOutputStream.toByteArray();
	}

	/**
	 * Take input stream, read it, and return string
	 *
	 * @param inputStream The input stream to read
	 *
	 * @return The string
	 */
	public static String convertInputStreamToString( InputStream inputStream ) {
		return new String( convertInputStreamToByteArray( inputStream ), DEFAULT_CHARSET );
	}

	/**
	 * Expands a path to an absolute path. If the path is already absolute, it is
	 * returned as is.
	 *
	 * @param context The context in which the BIF is being invoked.
	 * @param path    The path to expand
	 *
	 * @return The expanded path represented in a ResolvedFilePath record
	 */
	public static ResolvedFilePath expandPath( IBoxContext context, String path ) {
		ResolvedFilePath resolvedFilePath = context.findClosestTemplate();
		return expandPath( context, path, resolvedFilePath );
	}

	/**
	 * Expands a path to an absolute path. If the path is already absolute, it is
	 * returned as is.
	 *
	 * @param context  The context in which the BIF is being invoked.
	 * @param path     The path to expand
	 * @param basePath The base path to use for relative paths
	 *
	 * @return The expanded path represented in a ResolvedFilePath record
	 */
	public static ResolvedFilePath expandPath( IBoxContext context, String path, ResolvedFilePath basePath ) {
		// This really isn't a valid path, but ColdBox does this by carelessly appending too many slashes to view paths
		if ( path.startsWith( "//" ) ) {
			// strip one of them off
			path = path.substring( 1 );
		}
		// Change all instances of \ to / to make it Java Standard.
		path = path.replace( "\\", "/" );
		String	originalPath		= path;
		Path	originalPathPath	= null;
		originalPathPath = Path.of( originalPath );

		boolean isAbsolute = originalPathPath.isAbsolute();

		// If the incoming path does NOT start with a /, then we make it relative to the current template (if there is one)
		if ( !isAbsolute && !path.startsWith( SLASH_PREFIX ) ) {
			if ( basePath != null ) {
				Path template = basePath.absolutePath();
				if ( template != null ) {
					return basePath.newFromRelative( path );
				}
			}
			// No template, no problem. Slap a slash on, and we'll match it below
			path = SLASH_PREFIX + path;
		}

		// Let's find the longest mapping that matches the start of the path
		// Mappings are already sorted by length, so we can just take the first one that matches
		final String			finalPath				= path;
		Map.Entry<Key, Object>	matchingMappingEntry	= context.getConfig()
		    .getAsStruct( Key.mappings )
		    .entrySet()
		    .stream()
		    // Don't match the root mapping yet, we'll do that below
		    .filter( entry -> !entry.getKey().getName().equals( "/" ) && StringUtils.startsWithIgnoreCase( finalPath, entry.getKey().getName() ) )
		    .findFirst()
		    .orElse( null );
		if ( matchingMappingEntry != null ) {
			path = path.substring( matchingMappingEntry.getKey().getName().length() );
			String	matchingMapping	= matchingMappingEntry.getValue().toString();
			Path	result			= Path.of( matchingMapping, path ).toAbsolutePath();

			return ResolvedFilePath.of( matchingMappingEntry.getKey().getName(), matchingMapping, Path.of( finalPath ).normalize().toString(),
			    result.normalize() );

		}

		// If C:/foo is absolute, then great, but /foo has to actually exist on disk before I'll take it as really absolute
		if ( isAbsolute && !originalPath.equals( "/" ) ) {
			// detect if *nix OS file system...
			if ( File.separator.equals( "/" ) ) {
				// ... if so the path needs to start with / AND the parent must exist (and the parent can't be /)
				if ( originalPath.startsWith( "/" ) && !originalPathPath.getParent().toString().equals( "/" ) ) {
					String[]	pathParts	= originalPath.substring( 1, originalPath.length() - 1 ).split( "/" );
					// Any part of the provided absolute path exists, then the path is already expanded
					boolean		tldExists	= IntStream.range( 0, pathParts.length ).filter( idx -> pathParts[ idx ].length() > 0 ).mapToObj( idx -> {
												String tld = "";
												for ( int i = 0; i <= idx; i++ ) {
													tld += "/" + pathParts[ i ];
												}
												return tld;
											} ).anyMatch( tld -> Files.exists( Path.of( tld ) ) );
					if ( tldExists ) {
						return ResolvedFilePath.of( originalPathPath );
					}
				}
			} else {
				return ResolvedFilePath.of( originalPathPath );
			}
		}

		IStruct interceptData = Struct.of( Key.path, path, Key.resolvedFilePath, null );
		// An interceptor can populate a ResolvedFilePath instance in the interceptData struct to supply the resolved path
		interceptorService.announce(
		    BoxEvent.ON_MISSING_MAPPING,
		    interceptData
		);
		if ( interceptData.get( Key.resolvedFilePath ) != null ) {
			// if an interceptor puts a bad value here, we'll get a class cast exception. We can validate it, but it should error either way.
			return ( ResolvedFilePath ) interceptData.get( Key.resolvedFilePath );
		}

		// We give up, just assume it uses the root mapping of /
		String	rootMapping	= context.getConfig().getAsStruct( Key.mappings ).getAsString( Key.of( "/" ) );
		Path	result		= Path.of( rootMapping, path ).toAbsolutePath();
		return ResolvedFilePath.of( "/", rootMapping, Path.of( finalPath ).normalize().toString(), result.normalize() );
	}

	/**
	 * Tries to match a given absolute path to mappings in the environment and will return a path that is contracted by the shortest matched mapping. If there are no matches, returns the absolute path it was given.
	 *
	 * @param context The context in which the BIF is being invoked.
	 * @param path    The path to contract
	 *
	 * @return The contracted path represented in a ResolvedFilePath record. (The contracted path will be in the relativePath property)
	 */
	public static ResolvedFilePath contractPath( IBoxContext context, String path ) {
		return contractPath( context, path, null );
	}

	/**
	 * Tries to match a given absolute path to mappings in the environment and will return a path that is contracted by the shortest matched mapping. If there are no matches, returns the absolute path it was given.
	 *
	 * @param context          The context in which the BIF is being invoked.
	 * @param path             The path to contract
	 * @param preferredMapping A mapping to check for a match first
	 *
	 * @return The contracted path represented in a ResolvedFilePath record. (The contracted path will be in the relativePath property)
	 */
	public static ResolvedFilePath contractPath( IBoxContext context, String path, String preferredMapping ) {
		String	finalPath	= Path.of( path ).normalize().toString().replace( "\\", "/" );
		IStruct	mappings	= context.getConfig().getAsStruct( Key.mappings );

		if ( preferredMapping != null && mappings.get( Key.of( preferredMapping ) ) != null ) {
			String mappingDirectory = mappings.getAsString( Key.of( preferredMapping ) );
			mappingDirectory = Path.of( mappingDirectory ).normalize().toString().replace( "\\", "/" );
			if ( File.separator.equals( "/" ) ? finalPath.startsWith( mappingDirectory )
			    : StringUtils.startsWithIgnoreCase( finalPath, mappingDirectory ) ) {
				String contractedPath = finalPath.replace( mappingDirectory, "" );
				if ( !contractedPath.startsWith( "/" ) ) {
					contractedPath = "/" + contractedPath;
				}

				if ( !preferredMapping.equals( "/" ) ) {
					// Add mapping name back to relative path (which is inside of mapping root)
					contractedPath = preferredMapping + contractedPath;
				}

				return ResolvedFilePath.of(
				    preferredMapping,
				    mappingDirectory,
				    contractedPath,
				    Path.of( finalPath )
				);
			}

		}

		Map.Entry<Key, String> matchingMappingEntry = mappings
		    .entrySet()
		    .stream()
		    // negating length comparator to sort in descending order (checking longest match first)
		    .sorted( Comparator.comparingInt( entry -> -entry.getKey().getName().length() ) )
		    .map( entry -> new AbstractMap.SimpleEntry<Key, String>( entry.getKey(),
		        Path.of( entry.getValue().toString() ).normalize().toString().replace( "\\", "/" ) ) )
		    .filter( entry -> {
			    return File.separator.equals( "/" ) ? finalPath.startsWith( entry.getValue() )
			        : StringUtils.startsWithIgnoreCase( finalPath, entry.getValue() );
		    } )
		    .findFirst()
		    .orElse( null );

		if ( matchingMappingEntry != null ) {
			String contractedPath = finalPath.replace( matchingMappingEntry.getValue(), "" );
			if ( !contractedPath.startsWith( "/" ) ) {
				contractedPath = "/" + contractedPath;
			}

			String mappingName = matchingMappingEntry.getKey().getName();
			if ( !mappingName.equals( "/" ) ) {
				// Add mapping name back to relative path (which is inside of mapping root)
				contractedPath = mappingName + contractedPath;
			}

			return ResolvedFilePath.of(
			    matchingMappingEntry.getKey().getName(),
			    matchingMappingEntry.getValue(),
			    contractedPath,
			    Path.of( finalPath )
			);

		}

		return ResolvedFilePath.of(
		    null,
		    null,
		    finalPath,
		    Path.of( finalPath )
		);

	}

	/**
	 * Get the system temp directory
	 *
	 * @return The system temp directory
	 */
	public static String getTempDirectory() {
		return System.getProperty( "java.io.tmpdir" );
	}

	/**
	 * Serializes a target Serializable object to a file destination as binary data.
	 * If the file already exists, it will be overwritten.
	 *
	 * @param target   The target object to serialize
	 * @param filePath The file path to serialize to
	 */
	public static void serializeToFile( Object target, Path filePath ) {
		try ( OutputStream fileStream = Files.newOutputStream( filePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING ) ) {
			try ( ObjectOutputStream objStream = new ObjectOutputStream( fileStream ) ) {
				objStream.writeObject( target );
			} catch ( IOException e ) {
				throw new BoxIOException( String.format(
				    "The target entry [%s] could not be written to the file path [%s]. The message received was: %s",
				    target.getClass().getName(),
				    filePath.toString(),
				    e.getMessage()
				),
				    e
				);
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Deserializes a target Serializable object from a file destination as binary data
	 *
	 * @param filePath The file path to deserialize from
	 *
	 * @return The deserialized object
	 */
	public static Object deserializeFromFile( Path filePath ) {
		try ( InputStream fileStream = Files.newInputStream( filePath ) ) {
			try ( ObjectInputStream objStream = new ObjectInputStream( fileStream ) ) {
				return objStream.readObject();
			} catch ( ClassNotFoundException e ) {
				throw new BoxRuntimeException(
				    "Cannot cast the deserialized object to a known class.",
				    e
				);
			} catch ( IOException e ) {
				throw new BoxIOException( String.format(
				    "The file path [%s] could not be read. The message received was: %s",
				    filePath.toString(),
				    e.getMessage()
				),
				    e
				);
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Performs case insensitive path resolution. This will return the real path, which may be different in case than the incoming path.
	 *
	 * @param path The path to check
	 *
	 * @return The resolved path or null if not found
	 */
	public static Path pathExistsCaseInsensitive( Path path ) {
		Boolean defaultCheck = Files.exists( path );
		if ( defaultCheck ) {
			try {
				return path.toRealPath();
			} catch ( IOException e ) {
				return null;
			}
		}
		if ( isCaseSensitiveFS ) {
			String		realPath		= "";
			String[]	pathSegments	= path.toString().replace( '\\', '/' ).split( "/" );
			if ( pathSegments.length > 0 && pathSegments[ 0 ].contains( ":" ) ) {
				realPath = pathSegments[ 0 ];
			}
			Boolean first = true;
			for ( String thisSegment : pathSegments ) {
				// Skip windows drive letter
				if ( realPath == pathSegments[ 0 ] && pathSegments[ 0 ].contains( ":" ) && first ) {
					first = false;
					continue;
				}
				// Skip empty segments
				if ( thisSegment.length() == 0 ) {
					continue;
				}

				Boolean		found		= false;
				String[]	children	= new File( realPath + "/" ).list();
				// This will happen if we have a matched file in the middle of a path like /foo/index.cfm/bar
				if ( children == null ) {
					return null;
				}
				for ( String thisChild : children ) {
					// We're taking the FIRST MATCH. Buyer beware
					if ( thisSegment.equalsIgnoreCase( thisChild ) ) {
						realPath	+= "/" + thisChild;
						found		= true;
						break;
					}
				}
				// If we made it through the inner loop without a match, we've hit a dead end
				if ( !found ) {
					return null;
				}
			}
			// If we made it through the outer loop, we've found a match
			Path realPathFinal = Paths.get( realPath );
			return realPathFinal;
		}
		return null;
	}

	private static boolean caseSensitivityCheck() {
		try {
			File	currentWorkingDir	= new File( System.getProperty( "user.home" ) );
			File	case1				= new File( currentWorkingDir, "case1" );
			File	case2				= new File( currentWorkingDir, "Case1" );
			case1.createNewFile();
			if ( case2.createNewFile() ) {
				case1.delete();
				case2.delete();
				return true;
			} else {
				case1.delete();
				return false;
			}
		} catch ( Throwable e ) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Creates a URI from a file path string
	 * 
	 * @param input the file path string
	 * 
	 * @return the URI
	 * 
	 * @throws Exception
	 */
	public static URI createFileUri( String input ) {
		try {
			if ( input.startsWith( "/" ) || input.contains( ":" ) ) {
				// Absolute path: ensure "file://" scheme starts with "file:///"
				// Convert backslashes to forward slashes and prepend "file:///"
				if ( input.matches( "^[A-Za-z]:.*" ) ) {
					// Windows absolute path (e.g., C:\path\to\file)
					input = "file:///" + input.replace( "\\", "/" );
				} else {
					// Unix-style absolute path (e.g., /path/to/file)
					input = "file://" + input.replace( "\\", "/" );
				}
				return new URI( input );
			} else {
				// Relative path: just replace backslashes with forward slashes
				return new URI( input.replace( "\\", "/" ) );
			}
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( "The provided file path [" + input + "] is not a valid URI.", e );
		}
	}

	/**
	 * Creates a filter predicate for a directory stream based on the provided closure.
	 *
	 * @param context The context in which the BIF is being invoked.
	 * @param closure The closure to use for filtering.
	 *
	 * @return A predicate that filters paths based on the closure.
	 */
	public static java.util.function.Predicate<Path> createPathFilterPredicate( IBoxContext context, Function closure ) {
		return path -> BooleanCaster.cast( context.invokeFunction( closure, new Object[] { path.toString() } ) );
	}

}
