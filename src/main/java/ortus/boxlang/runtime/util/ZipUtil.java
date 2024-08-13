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
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
	 * @param includeBaseFolder Whether to include the base folder in the compressed file
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

						// Prevent Zip Slip attack
						if ( !targetFile.startsWith( basePath ) ) {
							throw new BoxRuntimeException( "Attempted Zip Slip attack: " + file );
						}

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
						Path targetFile = basePath.relativize( file.normalize() );

						// Prevent Gzip Slip attack (same logic as Zip Slip)
						if ( !targetFile.startsWith( basePath ) ) {
							throw new BoxRuntimeException( "Attempted Gzip Slip attack: " + file );
						}

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
	 * @param filter      The regex file-filter to apply to the extraction. This can be used to extract only files that match the filter
	 * @param entryPaths  The specific entry paths to extract from the zip file
	 */
	public static void extract(
	    COMPRESSION_FORMAT format,
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    String filter,
	    Array entryPaths ) {
		switch ( format ) {
			case ZIP :
				extractZip( source, destination, overwrite, recurse, filter, entryPaths );
				break;
			case GZIP :
				// extractGzip( source, destination, overwrite );
				break;
			default :
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]" );
		}
	}

	/**
	 * Extracts a zip file to a destination folder
	 *
	 * @param source      The absolute path of the compressed file
	 * @param destination The absolute destination folder to extract the compressed file
	 * @param overwrite   Whether to overwrite the destination file if it already exists, default is false
	 * @param recurse     Whether to recurse into subdirectories, default is true
	 * @param filter      The regex file-filter to apply to the extraction. This can be used to extract only files that match the filter
	 * @param entryPaths  The specific entry paths to extract from the zip file
	 *
	 * @throws BoxRuntimeException If an error occurs during extraction
	 */
	public static void extractZip( String source, String destination, Boolean overwrite, Boolean recurse, String filter, Array entryPaths ) {
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

		// Compile the filter pattern if provided
		Pattern filterPattern = filter != null ? Pattern.compile( filter ) : null;

		// Extract the source to the destination
		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( sourceFile.toFile() ) ) {
			zipFile.stream().forEach( entry -> {
				// Apply filter if present
				if ( filterPattern != null && !filterPattern.matcher( entry.getName() ).matches() ) {
					logger.warn( "Filter [{}] does not match entry [{}] skipping extraction", filter, entry.getName() );
					return;
				}

				// Check if we have entry paths
				if ( entryPaths != null && !entryPaths.contains( entry.getName() ) ) {
					logger.warn( "Entry path does not match: [{}], skipping extraction", entry.getName() );
					return;
				}

				// If not recursive, skip entries that are not at the top level
				if ( !recurse && entry.getName().contains( File.separator ) ) {
					logger.warn( "Entry is not at the top level: [{}], skipping extraction", entry.getName() );
					return;
				}

				// Create target path and prevent Zip Slip attacks
				Path targetPath = destinationPath.resolve( entry.getName() ).normalize();
				if ( !targetPath.startsWith( destinationPath ) ) {
					logger.warn( "Zip Slip attack detected for entry [{}], skipping extraction", entry.getName() );
					return;
				}

				// Check if we should overwrite or if file already exists
				if ( Files.exists( targetPath ) && !overwrite ) {
					logger.warn( "Destination file already exists: [{}] skipping extraction", targetPath );
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
							    "Error extracting entry: [" + entry.getName() + "] from zip file: [" + source + "] to destination: [" + destination + "]", e );
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
