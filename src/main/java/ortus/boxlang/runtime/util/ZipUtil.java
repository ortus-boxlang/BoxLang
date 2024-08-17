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
package ortus.boxlang.runtime.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * This class provides zip utilities for the BoxLang runtime
 */
public class ZipUtil {

	// Create enum of valid compression methods: zip, gzip, tar, etc.
	public enum COMPRESSION_FORMAT {
		ZIP,
		GZIP
	}

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( ZipUtil.class );

	/**
	 * --------------------------------------------------------------------------
	 * Comrpession Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * A compression method that compresses a file or folder into a zip file according to the specified format
	 *
	 * @param format            The compression format to use
	 * @param source            The absolute file or folder to compress
	 * @param destination       The absolute destination of the compressed file, we will add the extension based on the format
	 * @param includeBaseFolder Whether to include the base folder in the compressed file, default is true
	 * @param overwrite         Whether to overwrite the destination file if it already exists, default is false
	 */
	public static String compress( COMPRESSION_FORMAT format, String source, String destination, Boolean includeBaseFolder, Boolean overwrite ) {
		switch ( format ) {
			case ZIP :
				return compressZip( source, destination, includeBaseFolder, overwrite );
			case GZIP :
				return compressGzip( source, destination, includeBaseFolder, overwrite );
			default :
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]" );
		}
	}

	/**
	 * Compression method that compresses a file or folder into a zip file and returns the absolute path of the compressed file
	 *
	 * @param source            The absolute file or folder to compress
	 * @param destination       The absolute destination of the compressed file, we will add the extension based on the format
	 * @param includeBaseFolder Whether to include the base folder in the compressed file
	 * @param overwrite         Whether to overwrite the destination file if it already exists, default is false
	 *
	 * @return The absolute path of the compressed file
	 */
	public static String compressZip( String source, String destination, Boolean includeBaseFolder, Boolean overwrite ) {
		final Path	sourceFile		= ensurePath( source );
		final Path	destinationFile	= toPathWithExtension( destination, ".zip" );

		// Verify destination does not exist
		if ( destinationFile.toFile().exists() && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}

		// Compress the source to the destination
		try ( java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream( new java.io.FileOutputStream( destinationFile.toFile() ) ) ) {
			// Is the source a directory?
			if ( Files.isDirectory( sourceFile ) ) {
				Path basePath = ( includeBaseFolder ? sourceFile.getParent() : sourceFile ).normalize();
				Files.walkFileTree( sourceFile, new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
						Path targetFile = basePath.relativize( file.normalize() );  // Normalize the file path
						zipOutputStream.putNextEntry( new ZipEntry( targetFile.toString().replace( "\\", "/" ) ) );
						Files.copy( file, zipOutputStream );
						zipOutputStream.closeEntry();
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
						if ( dir.equals( sourceFile ) && !includeBaseFolder ) {
							return FileVisitResult.CONTINUE;
						}
						Path targetDir = basePath.relativize( dir.normalize() );  // Normalize the directory path
						zipOutputStream.putNextEntry( new ZipEntry( targetDir.toString().replace( "\\", "/" ) + "/" ) );
						zipOutputStream.closeEntry();
						return FileVisitResult.CONTINUE;
					}
				} );
			}
			// We have a file
			else {
				zipOutputStream.putNextEntry( new ZipEntry( sourceFile.getFileName().toString() ) );
				Files.copy( sourceFile, zipOutputStream );
				zipOutputStream.closeEntry();
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error compressing file or folder: [" + source + "] to destination: [" + destination + "]", e );
		}

		return destinationFile.toString();
	}

	/**
	 * Gzip compression method that compresses a file or folder into a gzip file and returns the absolute path of the compressed file
	 *
	 * Note: Gzip does not support compressing directories, so we will compress the files within the directory to the gzip file
	 *
	 * @param source            The absolute file or folder to compress
	 * @param destination       The absolute destination of the compressed file, we will add the extension based on the format
	 * @param includeBaseFolder Whether to include the base folder in the compressed file
	 * @param overwrite         Whether to overwrite the destination file if it already exists, default is false
	 *
	 * @return The absolute path of the compressed file
	 */
	public static String compressGzip( String source, String destination, Boolean includeBaseFolder, Boolean overwrite ) {
		final Path	sourceFile		= ensurePath( source ).normalize();
		final Path	destinationFile	= toPathWithExtension( destination, ".gz" );

		// Verify destination does not exist
		if ( Files.exists( destinationFile ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}

		// Compress the source to the destination
		try ( GZIPOutputStream gzipOutputStream = new GZIPOutputStream( Files.newOutputStream( destinationFile ) ) ) {
			// Is the source a directory?
			if ( Files.isDirectory( sourceFile ) ) {
				Path basePath = ( includeBaseFolder ? sourceFile.getParent() : sourceFile ).normalize();
				Files.walkFileTree( sourceFile, new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
						Files.copy( file, gzipOutputStream );
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
						if ( dir.equals( sourceFile ) && !includeBaseFolder ) {
							return FileVisitResult.CONTINUE;
						}
						// Note: Directories themselves are not directly compressed in Gzip, so this can be used for logging or skipped entirely.
						return FileVisitResult.CONTINUE;
					}
				} );
			} else {
				Files.copy( sourceFile, gzipOutputStream );
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error compressing file or folder: [" + source + "] to destination: [" + destination + "]", e );
		}

		return destinationFile.toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Extraction Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Extracts a compressed file to a destination folder
	 *
	 * @param format      The compression format to use: zip, gzip, etc.
	 * @param source      The absolute path of the compressed file
	 * @param destination The absolute destination folder to extract the compressed file
	 * @param overwrite   Whether to overwrite the destination file if it already exists, default is false
	 * @param recurse     Whether to recurse into subdirectories, default is true
	 * @param filter      A regex or BoxLang function or Java Predicate to apply as a filter to the extraction
	 * @param entryPaths  The specific entry paths to extract from the zip file
	 * @param context     The BoxLang context
	 */
	public static void extract(
	    COMPRESSION_FORMAT format,
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    Object filter,
	    Array entryPaths,
	    IBoxContext context ) {
		switch ( format ) {
			case ZIP :
				extractZip( source, destination, overwrite, recurse, filter, entryPaths, context );
				break;
			case GZIP :
				extractGZip( source, destination, overwrite );
				break;
			default :
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]" );
		}
	}

	/**
	 * Extracts a zip file to a destination folder.
	 * <p>
	 * The {@code filter} argument is used to filter the files to extract. It can be:
	 * <p>
	 * A regex string: {@code ".*\\.txt"}
	 * <p>
	 * A BoxLang function: {@code (path) => path.endsWith(".txt")}
	 * - The function should return {@code true} to extract the entry and {@code false} to skip it.
	 * - The function should take a single argument which is the entry path
	 * - A IBoxContext object is mandatory for BoxLang functions
	 * <p>
	 * A Java Predicate: {@code (entry) -> entry.getName().endsWith(".txt")}
	 * - The predicate should return {@code true} to extract the entry and {@code false} to skip it.
	 * - The predicate should take a single argument which is the {@code ZipEntry} object
	 * <p>
	 * The {@code entryPaths} argument is used to extract specific entries from the zip file.
	 * <p>
	 * The {@code recurse} argument is used to extract the files recursively. The default is {@code true}.
	 * <p>
	 *
	 * @param source      The absolute path of the compressed file
	 * @param destination The absolute destination folder to extract the compressed file
	 * @param overwrite   Whether to overwrite the destination file if it already exists, default is false
	 * @param recurse     Whether to recurse into subdirectories, default is true
	 * @param filter      A regex or BoxLang function or Java Predicate to apply as a filter to the extraction
	 * @param entryPaths  The specific entry paths to extract from the zip file
	 *
	 * @throws BoxRuntimeException If an error occurs during extraction
	 */
	public static void extractZip(
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    Object filter,
	    Array entryPaths,
	    IBoxContext context ) {
		Path	sourceFile		= ensurePath( source );
		Path	destinationPath	= Paths.get( destination ).normalize().toAbsolutePath();

		// Verify destination exists, if it is a file then throw an error, we can only extract to a directory
		// Else create the destination directory if it does not exist
		try {
			if ( Files.exists( destinationPath ) ) {
				if ( !Files.isDirectory( destinationPath ) ) {
					throw new BoxRuntimeException( "Destination is not a directory: [" + destination + "]" );
				}
			} else {
				Files.createDirectories( destinationPath );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to create or verify destination directory: [" + destination + "]", e );
		}

		// Extract the source to the destination
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( sourceFile.toFile() ) ) {
			zipFile.stream()
			    // Apply filters for extraction
			    .filter( entry -> {
				    if ( filter != null ) {
					    // String regex filters
					    if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
						    return Pattern.compile( castedFilter ).matcher( entry.getName() ).matches();
					    }

					    // BoxLang function filters
					    if ( filter instanceof Function filterFunction ) {
						    return BooleanCaster.cast( context.invokeFunction( filterFunction, new Object[] { entry.getName() } ) );
					    }

					    // Java Predicate filters
					    if ( filter instanceof java.util.function.Predicate<?> ) {
						    @SuppressWarnings( "unchecked" )
						    java.util.function.Predicate<ZipEntry> predicate = ( java.util.function.Predicate<ZipEntry> ) filter;
						    return predicate.test( entry );
					    }
				    }
				    return true;
			    } )
			    // Apply entry paths filter
			    .filter( entry -> {
				    if ( entryPaths != null && !entryPaths.isEmpty() ) {
					    return entryPaths.contains( entry.getName() );
				    }
				    return true;
			    } )
			    // Recursion Filter
			    .filter( entry -> {
				    if ( !recurse && entry.getName().contains( File.separator ) && entry.getName().split( File.separator ).length > 1 ) {
					    return false;
				    }
				    return true;
			    } )
			    .forEach( entry -> {
				    // Create target path and prevent Zip Slip attacks
				    Path targetPath = destinationPath.resolve( entry.getName() ).normalize();
				    if ( !targetPath.startsWith( destinationPath ) ) {
					    logger.warn( "Zip Slip attack detected for entry [{}], skipping extraction", entry.getName() );
					    return;
				    }

				    // Check if we should overwrite or if file already exists
				    if ( Files.exists( targetPath ) && !overwrite ) {
					    logger.debug( "Destination file already exists: [{}] skipping extraction", targetPath );
					    return;
				    }

				    // Create parent directories if they do not exist
				    try {
					    // If the entry is a directory, create the directory only if it does not exist
					    if ( entry.isDirectory() ) {
						    if ( !Files.exists( targetPath ) ) {
							    Files.createDirectories( targetPath );
						    }
					    } else {
						    // Ensure parent directories exist for files
						    if ( !Files.exists( targetPath.getParent() ) ) {
							    Files.createDirectories( targetPath.getParent() );
						    }

						    // Extract the entry
						    try ( java.io.InputStream inputStream = zipFile.getInputStream( entry ) ) {
							    Files.copy( inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING );
						    } catch ( IOException e ) {
							    throw new BoxRuntimeException(
							        "Error extracting entry: [" + entry.getName() + "] from zip file: [" + source + "] to destination: [" + destination + "]",
							        e );
						    }
					    }
				    } catch ( IOException e ) {
					    throw new BoxRuntimeException( "Error creating directory or file: [" + targetPath + "]", e );
				    }
			    } );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error extracting zip file: [" + source + "] to destination: [" + destination + "]", e );
		}
	}

	/**
	 * Extracts a gzip file to a destination folder.
	 *
	 * Note: Gzip does not support compressing directories, so we will compress the files within the directory to the gzip file
	 *
	 * @param source      The absolute path of the compressed file
	 * @param destination The absolute destination folder to extract the compressed file
	 * @param overwrite   Whether to overwrite the destination file if it already exists, default is false
	 */
	public static void extractGZip( String source, String destination, Boolean overwrite ) {
		Path	sourceFile				= ensurePath( source );
		Path	destinationDirectory	= Paths.get( destination ).normalize().toAbsolutePath();

		// Verify destination
		try {
			if ( Files.exists( destinationDirectory ) ) {
				if ( !Files.isDirectory( destinationDirectory ) ) {
					throw new BoxRuntimeException( "Destination is not a directory: [" + destination + "]" );
				}
			} else {
				Files.createDirectories( destinationDirectory );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to create or verify destination directory: [" + destination + "]", e );
		}

		// Extract the GZIP file to the destination
		Path targetPath = destinationDirectory.resolve( sourceFile.getFileName().toString().replace( ".gz", "" ) ).normalize();

		// Check if we should overwrite or if file already exists
		if ( Files.exists( targetPath ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + targetPath + "] and overwrite is not allowed." );
		}

		// Create parent directories if they do not exist
		try {
			if ( !Files.exists( targetPath.getParent() ) ) {
				Files.createDirectories( targetPath.getParent() );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to create parent directories for: [" + targetPath + "]", e );
		}

		// Extract the file
		try ( GZIPInputStream gzipInputStream = new GZIPInputStream( new FileInputStream( sourceFile.toFile() ) );
		    OutputStream outputStream = new FileOutputStream( targetPath.toFile() ) ) {
			byte[]	buffer	= new byte[ 1024 ];
			int		len;
			while ( ( len = gzipInputStream.read( buffer ) ) > 0 ) {
				outputStream.write( buffer, 0, len );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting GZIP file: [" + source + "] to destination: [" + destination + "]", e );
		}
	}

	/**
	 * List the entries in a zip file into an array of structures of information about the entries.
	 * <p>
	 * The filter can be a regex string, BoxLang function or Java Predicate.
	 * <p>
	 * A regex string: {@code ".*\\.txt"}
	 * <p>
	 * A BoxLang function: {@code (path) => path.endsWith(".txt")}
	 * - The function should return {@code true} to list the entry and {@code false} to skip it.
	 * - The function should take a single argument which is the entry path
	 * - A IBoxContext object is mandatory for BoxLang functions
	 * <p>
	 * A Java Predicate: {@code (entry) -> entry.getName().endsWith(".txt")}
	 * - The predicate should return {@code true} to list the entry and {@code false} to skip it.
	 * - The predicate should take a single argument which is the {@code ZipEntry} object
	 * <p>
	 *
	 * The structure should contain the following:
	 * - fullpath: The full path of the entry: e.g. "folder1/folder2/file.txt"
	 * - name: The file name of the entry: e.g. "file.txt"
	 * - directory: The directory containing the entry: e.g. "folder1/folder2"
	 * - size: The size of the entry in bytes
	 * - compressedSize: The compressed size of the entry in bytes
	 * - type: The type of the entry: file or directory
	 * - dateLastModified: The date the entry was last modified
	 * - crc: The CRC checksum of the entry
	 * - comment: The comment of the entry
	 * - isEncrypted: Whether the entry is encrypted
	 * - isCompressed: Whether the entry is compressed
	 * - isDirectory: Whether the entry is a directory
	 *
	 * @param source  The absolute path of the zip file
	 * @param filter  A regex or BoxLang function or Java Predicate to apply as a filter to the extraction.
	 * @param recurse Whether to recurse into subdirectories, default is true.
	 * @param context The BoxLang context
	 *
	 * @return An array of structures containing information about the entries in the zip file
	 */
	@SuppressWarnings( "unchecked" )
	public static Array listEntries( String source, Object filter, Boolean recurse, IBoxContext context ) {
		// List the entries in the zip file
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( source ) ) {
			return zipFile.stream()
			    // Apply filters
			    .filter( entry -> {
				    // Apply regex filter if present
				    if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
					    return Pattern.compile( castedFilter ).matcher( entry.getName() ).matches();
				    }

				    // Apply BoxLang function filter if present
				    if ( filter instanceof Function filterFunction ) {
					    return BooleanCaster.cast( context.invokeFunction( filterFunction, new Object[] { entry.getName() } ) );
				    }

				    // Apply Java Predicate filter if present
				    if ( filter instanceof java.util.function.Predicate<?> ) {
					    java.util.function.Predicate<ZipEntry> predicate = ( java.util.function.Predicate<ZipEntry> ) filter;
					    return predicate.test( entry );
				    }

				    // Skip entries that are inside subdirectories
				    if ( !recurse && entry.getName().contains( File.separator ) && entry.getName().split( File.separator ).length > 1 ) {
					    return false;
				    }

				    return true;
			    } )
			    // Map it to a structure
			    .map( entry -> Struct.of(
			        "comment", entry.getComment(),
			        "compressedSize", entry.getCompressedSize(),
			        "crc", entry.getCrc(),
			        "creationTime", ( entry.getCreationTime() == null ) ? "" : entry.getCreationTime().toString(),
			        "lastAccessTime", ( entry.getLastAccessTime() == null ) ? "" : entry.getLastAccessTime().toString(),
			        "lastModifiedTime", ( entry.getLastModifiedTime() == null ) ? "" : entry.getLastModifiedTime().toString(),
			        "dateLastModified", new DateTime( entry.getTimeLocal() ),
			        "directory", StringUtils.substringBeforeLast( entry.getName(), File.separator ),
			        "fullpath", entry.getName(),
			        "isDirectory", entry.isDirectory(),
			        "name", StringUtils.substringAfterLast( entry.getName(), File.separator ),
			        "size", entry.getSize(),
			        "type", entry.isDirectory() ? "directory" : "file"
			    ) )
			    // Collect the results
			    .collect( BLCollector.toArray() );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error listing entries in zip file: [" + source + "]", e );
		}
	}

	/**
	 * List the entries into a flat array of paths in a zip file
	 *
	 * @param source  The absolute path of the zip file
	 * @param filter  The regex file-filter to apply to the extraction. This can be used to extract only files that match the filter
	 * @param recurse Whether to recurse into subdirectories, default is true
	 *
	 * @return An array of structures containing information about the entries in the zip file
	 */
	public static Array listEntriesFlat( String source, Object filter, Boolean recurse, IBoxContext context ) {
		return listEntries( source, filter, recurse, context )
		    .stream()
		    .map( entry -> ( ( IStruct ) entry ).getAsString( Key.of( "fullpath" ) ) )
		    .collect( BLCollector.toArray() );
	}

	/**
	 * Delete entries from a zip file based on a filter which can be:
	 * <p>
	 * A regex string: {@code ".*\\.txt"}
	 * <p>
	 * A BoxLang function: {@code (path) => path.endsWith(".txt")}
	 * - The function should return {@code false} to keep the entry and {@code true} to delete it.
	 * - The function should take a single argument which is the entry path
	 * - A IBoxContext object is mandatory for BoxLang functions
	 * <p>
	 * A Java Predicate: {@code (entry) -> entry.getName().endsWith(".txt")}
	 * - The predicate should return {@code false} to keep the entry and {@code true} to delete it.
	 * - The predicate should take a single argument which is the {@code ZipEntry} object
	 * <p>
	 *
	 * <pre>
	 * // String regex filter
	 * ZipUtil.deleteEntries( "path/to/zipfile.zip", ".*\\.txt", null );
	 * // BoxLang function filter
	 * ZipUtil.deleteEntries( "path/to/zipfile.zip", (path) => path.endsWith(".txt"), context );
	 * // Java Predicate filter
	 * ZipUtil.deleteEntries( "path/to/zipfile.zip", (entry) -> entry.getName().endsWith(".txt"), null );
	 * </pre>
	 *
	 *
	 * @param source  The absolute path of the zip file
	 * @param filter  The filter to apply to the entries: string regex, BoxLang function or Java Predicate
	 * @param context The BoxLang context if using BoxLang functions
	 */
	public static void deleteEntries( String source, Object filter, IBoxContext context ) {
		Path	sourceFile	= ensurePath( source );
		// Create a temporary file to store the updated zip file
		Path	tempFile;
		try {
			tempFile = Files.createTempFile( "ziputil_", ".zip" );
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to create a temporary file for repackaging", e );
		}

		// Delete specified entries and repackage the zip file
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( sourceFile.toFile() );
		    java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream( Files.newOutputStream( tempFile ) ) ) {
			zipFile.stream()
			    // Filter removes files to delete
			    .filter( entry -> {
				    // If the regex matches then that means we are deleting the entry, so we return false
				    if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
					    return !Pattern.compile( castedFilter ).matcher( entry.getName() ).matches();
				    }

				    // Apply BoxLang function filter if present
				    if ( filter instanceof Function filterFunction ) {
					    return !BooleanCaster.cast( context.invokeFunction( filterFunction, new Object[] { entry.getName() } ) );
				    }

				    // Apply Java Predicate filter if present
				    if ( filter instanceof java.util.function.Predicate<?> ) {
					    @SuppressWarnings( "unchecked" )
					    java.util.function.Predicate<ZipEntry> predicate = ( java.util.function.Predicate<ZipEntry> ) filter;
					    return !predicate.test( entry );
				    }
				    // Return it, to add to the new zip file
				    return true;
			    } )
			    // Copy the entries to the new zip file
			    .forEach( entry -> {
				    try {
					    // Copy the entry to the new zip file
					    zipOutputStream.putNextEntry( new ZipEntry( entry.getName() ) );
					    try ( java.io.InputStream inputStream = zipFile.getInputStream( entry ) ) {
						    inputStream.transferTo( zipOutputStream );
					    }
					    zipOutputStream.closeEntry();
				    } catch ( IOException e ) {
					    throw new BoxRuntimeException( "Error while repackaging zip file", e );
				    }
			    } );

		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error processing zip file for deletion", e );
		}

		// Replace the original file with the updated one
		try {
			Files.move( tempFile, sourceFile, StandardCopyOption.REPLACE_EXISTING );
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to replace the original zip file with the updated one", e );
		}
	}

	/**
	 * This method reads an entry from a zip file and returns the content as a string using the specified charset
	 *
	 * @param source    The absolute path of the zip file
	 * @param entryPath The path of the entry to read
	 * @param charset   The charset to use for reading the entry
	 *
	 * @throws BoxRuntimeException If the entry is not found in the zip file
	 *
	 * @return The content of the entry as a string
	 */
	public static String readEntry( String source, String entryPath, String charset ) {
		Path	sourceFile	= ensurePath( source );
		String	entryContent;
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( sourceFile.toFile() ) ) {
			java.util.zip.ZipEntry entry = zipFile.getEntry( entryPath );
			if ( entry == null ) {
				throw new BoxRuntimeException( "Entry not found in zip file: [" + entryPath + "]" );
			}
			try ( java.io.InputStream inputStream = zipFile.getInputStream( entry ) ) {
				entryContent = new String( inputStream.readAllBytes(), charset );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading entry: [" + entryPath + "] from zip file: [" + source + "]", e );
		}
		return entryContent;
	}

	/**
	 * This method reads an entry from a zip file and returns the content as a string using the default charset
	 *
	 * @param source    The absolute path of the zip file
	 * @param entryPath The path of the entry to read
	 *
	 * @return The content of the entry as a string
	 */
	public static String readEntry( String source, String entryPath ) {
		return readEntry( source, entryPath, java.nio.charset.Charset.defaultCharset().name() );
	}

	/**
	 * This method reads an entry from a zip file and returns the content as a byte array
	 *
	 * @param source    The absolute path of the zip file
	 * @param entryPath The path of the entry to read
	 *
	 * @return The byte array content of the entry
	 */
	public static byte[] readBinaryEntry( String source, String entryPath ) {
		Path	sourceFile	= ensurePath( source );
		byte[]	entryContent;
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( sourceFile.toFile() ) ) {
			java.util.zip.ZipEntry entry = zipFile.getEntry( entryPath );
			if ( entry == null ) {
				throw new BoxRuntimeException( "Entry not found in zip file: [" + entryPath + "]" );
			}
			try ( java.io.InputStream inputStream = zipFile.getInputStream( entry ) ) {
				entryContent = inputStream.readAllBytes();
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error reading entry: [" + entryPath + "] from zip file: [" + source + "]", e );
		}
		return entryContent;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Ensures the source path exists and normalizes it
	 *
	 * @param path The string path to verify
	 *
	 * @return The path object
	 */
	private static Path ensurePath( String target ) {
		// Verify source exists
		Path sourcePath = Paths.get( target ).normalize().toAbsolutePath();
		if ( !sourcePath.toFile().exists() ) {
			throw new BoxRuntimeException( "Source file or folder does not exist: [" + target + "]" );
		}
		return sourcePath;
	}

	/**
	 * Adds the format extension to the destination path and normalizes it
	 *
	 * @param destination The destination path
	 * @param extension   The extension to add
	 *
	 * @return The normalized path with the extension
	 */
	private static Path toPathWithExtension( String destination, String extension ) {
		Path destinationFile = Paths.get( destination ).normalize().toAbsolutePath();
		// Add extension if not present
		if ( !destinationFile.toString().toLowerCase().endsWith( extension ) ) {
			destinationFile = Paths.get( destinationFile.toString() + extension );
		}
		return destinationFile;
	}

	/**
	 * Verifies if a file is a zip file or not
	 *
	 * @param filepath The file path to verify
	 *
	 * @return True if the file is a zip file, false otherwise
	 */
	public static Boolean isZipFile( String filepath ) {
		Path path = Paths.get( filepath ).toAbsolutePath();
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( path.toFile() ) ) {
			return true;
		} catch ( Exception e ) {
			return false;
		}
	}

}
