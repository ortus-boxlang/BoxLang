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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.StringUtils;
import org.itadaki.bzip2.BZip2InputStream;
import org.itadaki.bzip2.BZip2OutputStream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.jtar.TarEntry;
import ortus.boxlang.runtime.util.jtar.TarInputStream;
import ortus.boxlang.runtime.util.jtar.TarOutputStream;

/**
 * This class provides zip utilities for the BoxLang runtime
 */
public class ZipUtil {

	// Create enum of valid compression methods: zip, gzip, tar, etc.
	public enum COMPRESSION_FORMAT {
		ZIP,
		GZIP,
		TAR,
		TGZ,
		BZIP,
		TBZ,
		TBZ2
	}

	/**
	 * Resolves an explicit archive format or detects it from a path extension.
	 *
	 * @param format The explicit format, or {@code null}
	 * @param path   The archive path used for extension detection
	 *
	 * @return The resolved compression format
	 *
	 * @throws BoxRuntimeException If the format is unavailable or unsupported
	 */
	public static COMPRESSION_FORMAT detectFormat( String format, String path ) {
		if ( format != null && !format.isBlank() ) {
			String normalizedFormat = format.trim().toLowerCase();
			if ( normalizedFormat.equals( "bz2" ) || normalizedFormat.equals( "bzip2" ) ) {
				return COMPRESSION_FORMAT.BZIP;
			}
			if ( normalizedFormat.equals( "tar.bz" ) ) {
				return COMPRESSION_FORMAT.TBZ;
			}
			if ( normalizedFormat.equals( "tar.gz" ) ) {
				return COMPRESSION_FORMAT.TGZ;
			}
			try {
				return COMPRESSION_FORMAT.valueOf( normalizedFormat.toUpperCase() );
			} catch ( IllegalArgumentException e ) {
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]", e );
			}
		}
		String lowerPath = path == null ? "" : path.trim().toLowerCase();
		if ( lowerPath.endsWith( ".zip" ) ) {
			return COMPRESSION_FORMAT.ZIP;
		}
		if ( lowerPath.endsWith( ".tgz" ) || lowerPath.endsWith( ".tar.gz" ) ) {
			return COMPRESSION_FORMAT.TGZ;
		}
		if ( lowerPath.endsWith( ".tbz" ) || lowerPath.endsWith( ".tbz2" ) || lowerPath.endsWith( ".tar.bz" ) || lowerPath.endsWith( ".tar.bz2" ) ) {
			return COMPRESSION_FORMAT.TBZ;
		}
		if ( lowerPath.endsWith( ".bz2" ) || lowerPath.endsWith( ".bz" ) || lowerPath.endsWith( ".bzip2" ) ) {
			return COMPRESSION_FORMAT.BZIP;
		}
		if ( lowerPath.endsWith( ".gz" ) ) {
			return COMPRESSION_FORMAT.GZIP;
		}
		if ( lowerPath.endsWith( ".tar" ) ) {
			return COMPRESSION_FORMAT.TAR;
		}
		throw new BoxRuntimeException( "Unable to detect compression format from path: [" + path + "]. Specify the format explicitly." );
	}

	/**
	 * Runtime Logger
	 */
	private static final BoxLangLogger	logger						= BoxRuntime.getInstance().getLoggingService().getRuntimeLogger();

	/**
	 * Default compression level
	 */
	public static final int				DEFAULT_COMPRESSION_LEVEL	= 6;

	/**
	 * --------------------------------------------------------------------------
	 * Compression Methods
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
	 * @param prefix            String added as a prefix to the final archive. The string is the name of a subdirectory in which the entries are added to exclusively. Only works for zip archiving.
	 * @param filter            A regex or BoxLang function or Java Predicate to apply as a filter to the extraction
	 * @param recurse           Whether to recurse into subdirectories, default is true
	 * @param compressionLevel  The compression level to use for the compression. Default is 6, which is a good balance between speed and compression ratio.
	 * @param context           The BoxLang context
	 *
	 * @throws BoxRuntimeException If the compression level is invalid or if the destination file already exists and overwrite is false.
	 */
	public static String compress(
	    COMPRESSION_FORMAT format,
	    String source,
	    String destination,
	    Boolean includeBaseFolder,
	    Boolean overwrite,
	    String prefix,
	    Object filter,
	    Boolean recurse,
	    Integer compressionLevel,
	    IBoxContext context ) {

		// Set default compression level if not provided
		int zipCompressionLevel = ( compressionLevel != null ) ? compressionLevel : DEFAULT_COMPRESSION_LEVEL;

		// Validate compression level range
		if ( zipCompressionLevel < Deflater.NO_COMPRESSION || zipCompressionLevel > Deflater.BEST_COMPRESSION ) {
			throw new BoxRuntimeException( "Invalid compression level: [" + zipCompressionLevel + "]. Must be between " +
			    Deflater.NO_COMPRESSION + " (no compression) and " + Deflater.BEST_COMPRESSION + " (maximum compression)" );
		}

		switch ( format ) {
			case ZIP :
				return compressZip( Array.of( source ), destination, includeBaseFolder, overwrite, prefix, filter, recurse, zipCompressionLevel, context );
			case GZIP :
				return compressGzip( source, destination, includeBaseFolder, overwrite, zipCompressionLevel );
			case TAR :
				return compressTar( source, destination, includeBaseFolder, overwrite, filter, recurse, context, false );
			case TGZ :
				return compressTar( source, destination, includeBaseFolder, overwrite, filter, recurse, context, true );
			case BZIP :
				return compressBzip( source, destination, overwrite );
			case TBZ :
			case TBZ2 :
				return compressTarBzip( source, destination, includeBaseFolder, overwrite, filter, recurse, context, format == COMPRESSION_FORMAT.TBZ );
			default :
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]" );
		}
	}

	/**
	 * Compresses a file or directory into a bzip2-compressed TAR archive.
	 *
	 * @param source            The source file or directory
	 * @param destination       The destination archive path
	 * @param includeBaseFolder Whether to include the source directory as the archive root
	 * @param overwrite         Whether to overwrite an existing archive
	 * @param filter            A file-name filter
	 * @param recurse           Whether to include nested directories
	 * @param context           The BoxLang context used by function filters
	 *
	 * @return The absolute destination archive path
	 */
	private static String compressTarBzip(
	    String source,
	    String destination,
	    Boolean includeBaseFolder,
	    Boolean overwrite,
	    Object filter,
	    Boolean recurse,
	    IBoxContext context,
	    Boolean shortExtension ) {
		Path	sourcePath		= ensurePath( source );
		Path	destinationPath	= toPathWithExtension( destination, shortExtension ? ".tbz" : ".tbz2" );
		if ( Files.exists( destinationPath ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}
		try ( BZip2OutputStream bzipOutputStream = new BZip2OutputStream( Files.newOutputStream( destinationPath ) );
		    TarOutputStream tarOutputStream = new TarOutputStream( bzipOutputStream ) ) {
			writeTarTree( sourcePath, destinationPath, includeBaseFolder, filter, recurse, context, tarOutputStream );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compressing TBZ2 file: [" + destination + "]", e );
		}
		return destinationPath.toString();
	}

	/**
	 * Compresses a file or directory as a bzip2 stream.
	 *
	 * @param source      The source file or directory
	 * @param destination The destination bzip2 file
	 * @param overwrite   Whether to overwrite an existing destination
	 *
	 * @return The absolute destination path
	 */
	private static String compressBzip( String source, String destination, Boolean overwrite ) {
		Path	sourcePath		= ensurePath( source );
		Path	destinationPath	= toPathWithExtension( destination, ".bz2" );
		if ( Files.exists( destinationPath ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}
		try ( BZip2OutputStream bzipOutputStream = new BZip2OutputStream( Files.newOutputStream( destinationPath ) ) ) {
			Files.copy( sourcePath, bzipOutputStream );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compressing BZIP file: [" + destination + "]", e );
		}
		return destinationPath.toString();
	}

	/**
	 * Writes all files and directories under a source path into a TAR stream.
	 *
	 * @param sourcePath        The source path
	 * @param destinationPath   The archive path, which is skipped when inside the source
	 * @param includeBaseFolder Whether to include the source directory name
	 * @param filter            The archive entry filter
	 * @param recurse           Whether to recurse into child directories
	 * @param context           The BoxLang context
	 * @param tarOutputStream   The destination TAR stream
	 *
	 * @throws IOException If an entry cannot be written
	 */
	private static void writeTarTree(
	    Path sourcePath,
	    Path destinationPath,
	    Boolean includeBaseFolder,
	    Object filter,
	    Boolean recurse,
	    IBoxContext context,
	    TarOutputStream tarOutputStream ) throws IOException {
		Path basePath = includeBaseFolder ? sourcePath.getParent() : sourcePath;
		Files.walkFileTree( sourcePath, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
				if ( file.toAbsolutePath().normalize().equals( destinationPath ) || !matchesArchiveFilter( basePath.relativize( file ), filter, context ) ) {
					return FileVisitResult.CONTINUE;
				}
				writeTarEntry( tarOutputStream, file, basePath.relativize( file ) );
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult preVisitDirectory( Path directory, BasicFileAttributes attrs ) throws IOException {
				if ( !recurse && !directory.equals( sourcePath ) ) {
					return FileVisitResult.SKIP_SUBTREE;
				}
				if ( !directory.equals( sourcePath ) || includeBaseFolder ) {
					writeTarEntry( tarOutputStream, directory, basePath.relativize( directory ) );
				}
				return FileVisitResult.CONTINUE;
			}
		} );
	}

	/**
	 * Compresses a file or directory into a raw TAR or gzip-compressed TAR archive.
	 *
	 * @param source            The source file or directory
	 * @param destination       The destination archive path
	 * @param includeBaseFolder Whether to include the source directory as the archive root
	 * @param overwrite         Whether to overwrite an existing archive
	 * @param filter            A file-name filter
	 * @param recurse           Whether to include nested directories
	 * @param context           The BoxLang context used by function filters
	 * @param gzip              Whether to gzip-compress the TAR stream
	 *
	 * @return The absolute destination archive path
	 */
	private static String compressTar(
	    String source,
	    String destination,
	    Boolean includeBaseFolder,
	    Boolean overwrite,
	    Object filter,
	    Boolean recurse,
	    IBoxContext context,
	    Boolean gzip ) {
		Path	sourcePath		= ensurePath( source );
		String	extension		= gzip ? ".tgz" : ".tar";
		Path	destinationPath	= toPathWithExtension( destination, extension );
		if ( Files.exists( destinationPath ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}
		try {
			if ( destinationPath.getParent() != null ) {
				Files.createDirectories( destinationPath.getParent() );
			}
			OutputStream	fileOutputStream	= Files.newOutputStream( destinationPath );
			OutputStream	archiveOutputStream	= gzip ? new GZIPOutputStream( fileOutputStream ) : fileOutputStream;
			try ( TarOutputStream tarOutputStream = new TarOutputStream( archiveOutputStream ) ) {
				Path basePath = includeBaseFolder ? sourcePath.getParent() : sourcePath;
				Files.walkFileTree( sourcePath, new SimpleFileVisitor<>() {

					@Override
					public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
						if ( file.toAbsolutePath().normalize().equals( destinationPath )
						    || !matchesArchiveFilter( basePath.relativize( file ), filter, context ) ) {
							return FileVisitResult.CONTINUE;
						}
						writeTarEntry( tarOutputStream, file, basePath.relativize( file ) );
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult preVisitDirectory( Path directory, BasicFileAttributes attrs ) throws IOException {
						if ( !recurse && !directory.equals( sourcePath ) ) {
							return FileVisitResult.SKIP_SUBTREE;
						}
						if ( !directory.equals( sourcePath ) || includeBaseFolder ) {
							writeTarEntry( tarOutputStream, directory, basePath.relativize( directory ) );
						}
						return FileVisitResult.CONTINUE;
					}
				} );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compressing TAR file: [" + destination + "]", e );
		}
		return destinationPath.toString();
	}

	/**
	 * Writes one file or directory to a TAR output stream.
	 *
	 * @param tarOutputStream The TAR output stream
	 * @param file            The source file or directory
	 * @param relativePath    The archive-relative entry path
	 *
	 * @throws IOException If the entry cannot be written
	 */
	private static void writeTarEntry( TarOutputStream tarOutputStream, Path file, Path relativePath ) throws IOException {
		String entryName = relativePath.toString().replace( '\\', '/' );
		if ( Files.isDirectory( file ) && !entryName.endsWith( "/" ) ) {
			entryName += "/";
		}
		TarEntry entry = new TarEntry( file.toFile(), entryName );
		tarOutputStream.putNextEntry( entry );
		if ( Files.isRegularFile( file ) ) {
			Files.copy( file, tarOutputStream );
		}
	}

	/**
	 * Determines whether an archive-relative path passes the configured compression filter.
	 *
	 * @param relativePath The archive-relative path
	 * @param filter       A string or BoxLang function filter
	 * @param context      The BoxLang context used by function filters
	 *
	 * @return {@code true} when the path should be included
	 */
	private static boolean matchesArchiveFilter( Path relativePath, Object filter, IBoxContext context ) {
		if ( filter == null ) {
			return true;
		}
		String entryName = relativePath.toString().replace( '\\', '/' );
		if ( filter instanceof String filterString ) {
			return FileSystemUtil.fileMatchesPattern( filterString, Path.of( entryName ) );
		}
		if ( filter instanceof Function filterFunction ) {
			return BooleanCaster.cast( context.invokeFunction( filterFunction, new Object[] { entryName } ) );
		}
		return true;
	}

	/**
	 * Compression method that compresses a file or folder into a zip file and returns the absolute path of the compressed file
	 *
	 * @param sources           An array of source sets to compress. Each source can be a file path, a directory, or a Struct with content.
	 * @param destination       The absolute destination of the compressed file, we will add the extension based on the format
	 * @param includeBaseFolder Whether to include the base folder in the compressed file
	 * @param overwrite         Whether to overwrite the destination file if it already exists, default is false
	 * @param prefix            String added as a prefix to the final archive. The string is the name of a subdirectory in which the entries are added to exclusively. Only works for zip archiving.
	 * @param filter            A regex or BoxLang function or Java Predicate to apply as a filter to the extraction
	 * @param recurse           Whether to recurse into subdirectories, default is true
	 * @param compressionLevel  The compression level to use for the compression. Default is 6, which is a good balance between speed and compression ratio.
	 * @param context           The BoxLang context
	 *
	 * @throws BoxRuntimeException If the compression level is invalid or if the destination file already exists and overwrite is false.
	 *
	 * @return The absolute path of the compressed file
	 */
	public static String compressZip(
	    Array sources,
	    String destination,
	    Boolean includeBaseFolder,
	    Boolean overwrite,
	    String prefix,
	    Object filter,
	    Boolean recurse,
	    Integer compressionLevel,  // New parameter
	    IBoxContext context ) {

		// Prepare destination paths
		final Path destinationFile = toPathWithExtension( destination, ".zip" ).toAbsolutePath().normalize();

		// Verify destination does not exist
		if ( destinationFile.toFile().exists() && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}

		// Compress the source to the destination
		try ( java.util.zip.ZipOutputStream zipOutputStream = new java.util.zip.ZipOutputStream( new java.io.FileOutputStream( destinationFile.toFile() ) ) ) {

			// Set the compression level on the ZipOutputStream
			zipOutputStream.setLevel( compressionLevel );

			sources.stream().forEach( ( source ) -> {
				Object	itemFilter				= null;
				Path	sourceFile				= null;
				byte[]	sourceBytes				= null;
				String	destinationEntryPath	= null;
				// Calculate the path prefix
				String	pathPrefix				= prefix != null && !prefix.isEmpty() ? prefix + "/" : "";
				if ( source instanceof String stringSource ) {
					sourceFile	= ensurePath( stringSource );
					itemFilter	= filter;
				} else if ( source instanceof IStruct attributes ) {
					String declaredSource = attributes.getAsString( Key.source );
					if ( declaredSource != null && !declaredSource.isEmpty() ) {
						sourceFile	= ensurePath( declaredSource );
						itemFilter	= attributes.get( Key.filter );
					} else if ( attributes.get( Key.entryPath ) != null && !attributes.getAsString( Key.entryPath ).isEmpty() ) {
						// create a temp file with the content
						destinationEntryPath = attributes.getAsString( Key.entryPath );
						if ( attributes.get( Key.prefix ) != null ) {
							pathPrefix = attributes.getAsString( Key.prefix ) + "/";
						}
						Object sourceContent = attributes.get( Key.content );
						if ( sourceContent instanceof byte[] castSourceBytes ) {
							sourceBytes = castSourceBytes;
						} else {
							String charset = StringCaster.cast( attributes.getOrDefault( Key.charset, StandardCharsets.UTF_8.name() ) );
							sourceBytes = StringCaster.cast( sourceContent ).getBytes( Charset.forName( charset ) );
						}
					} else {
						return; // Skip this source if no valid source or content is provided
					}

				}

				// Finalize for visitor
				final String	finalPathPrefix	= pathPrefix;
				final Object	finalItemFilter	= itemFilter;

				try {
					// Is the source a directory
					if ( sourceFile != null && Files.isDirectory( sourceFile ) ) {
						// Finalize for visitor
						final Path	finalSourceFile	= sourceFile;
						Path		basePath		= ( includeBaseFolder ? sourceFile.getParent() : sourceFile ).normalize();
						Files.walkFileTree( sourceFile, new SimpleFileVisitor<>() {

							@Override
							public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
								// Skip the destination zip file if it's in the source directory
								if ( file.toAbsolutePath().normalize().equals( destinationFile ) ) {
									return FileVisitResult.CONTINUE;
								}

								Path	targetFile		= basePath.relativize( file.normalize() );  // Normalize the file path
								String	zipEntryName	= finalPathPrefix + targetFile.toString().replace( "\\", "/" );

								// If a filter is present, apply it
								if ( finalItemFilter != null ) {
									// String regex filter: If there is a match, we add the entry to the zip file, else we skip it
									if ( finalItemFilter instanceof String castedFilter && castedFilter.length() > 1 ) {
										if ( !FileSystemUtil.fileMatchesPattern( castedFilter, Path.of( zipEntryName ) ) ) {
											return FileVisitResult.CONTINUE;
										}
									}

									// BoxLang function filters
									if ( finalItemFilter instanceof Function filterFunction ) {
										if ( !BooleanCaster.cast( context.invokeFunction( filterFunction, new Object[] { zipEntryName } ) ) ) {
											return FileVisitResult.CONTINUE;
										}
									}

									// Java Predicate filters
									if ( finalItemFilter instanceof java.util.function.Predicate<?> ) {
										@SuppressWarnings( "unchecked" )
										java.util.function.Predicate<String> predicate = ( java.util.function.Predicate<String> ) finalItemFilter;
										if ( !predicate.test( zipEntryName ) ) {
											return FileVisitResult.CONTINUE;
										}
									}
								}

								// Add the entry to the zip file
								zipOutputStream.putNextEntry( new ZipEntry( zipEntryName ) );
								Files.copy( file, zipOutputStream );
								zipOutputStream.closeEntry();
								return FileVisitResult.CONTINUE;
							}

							@Override
							public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
								// If the directory is the source directory and we are not including the base folder, we skip it
								if ( dir.equals( finalSourceFile ) && !includeBaseFolder ) {
									return FileVisitResult.CONTINUE;
								}
								// If not recursing, we skip the directory
								if ( !recurse && !dir.equals( finalSourceFile ) ) {
									return FileVisitResult.SKIP_SUBTREE;
								}

								// Calculate the target directory path & Add the directory to the zip file
								Path	targetDir		= basePath.relativize( dir.normalize() );  // Normalize the directory path
								String	zipEntryName	= finalPathPrefix + targetDir.toString().replace( "\\", "/" ) + "/";
								zipOutputStream.putNextEntry( new ZipEntry( zipEntryName ) );
								zipOutputStream.closeEntry();
								return FileVisitResult.CONTINUE;
							}
						} );
					}
					// We have binary content
					else if ( sourceBytes != null ) {
						String zipEntryName = finalPathPrefix + destinationEntryPath;
						zipOutputStream.putNextEntry( new ZipEntry( zipEntryName ) );
						zipOutputStream.write( sourceBytes, 0, sourceBytes.length );
						zipOutputStream.closeEntry();
					}
					// We have a file
					else {
						String zipEntryName = finalPathPrefix + sourceFile.getFileName().toString();
						zipOutputStream.putNextEntry( new ZipEntry( zipEntryName ) );
						Files.copy( sourceFile, zipOutputStream );
						zipOutputStream.closeEntry();
					}

				} catch ( IOException e ) {
					throw new BoxIOException( "Error processing zip source", e );
				}

			} );

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Error compressing to destination: [" + destination + "]", e );
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
	 * @param compressionLevel  The compression level to use for the compression. Default is 6, which is a good balance between speed and compression ratio.
	 *
	 * @return The absolute path of the compressed file
	 */
	public static String compressGzip(
	    String source,
	    String destination,
	    Boolean includeBaseFolder,
	    Boolean overwrite,
	    Integer compressionLevel ) {
		final Path	sourceFile		= ensurePath( source ).normalize();
		final Path	destinationFile	= toPathWithExtension( destination, ".gz" );

		// Verify destination does not exist
		if ( Files.exists( destinationFile ) && !overwrite ) {
			throw new BoxRuntimeException( "Destination file already exists: [" + destination + "]" );
		}

		// Compress the source to the destination
		try ( GZIPOutputStream gzipOutputStream = new GZIPOutputStream( Files.newOutputStream( destinationFile ) ) {

			{
				def.setLevel( compressionLevel );
			}
		} ) {
			// Is the source a directory?
			if ( Files.isDirectory( sourceFile ) ) {
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
			case TAR :
				extractTar( source, destination, overwrite, recurse, filter, entryPaths, false );
				break;
			case TGZ :
				extractTar( source, destination, overwrite, recurse, filter, entryPaths, true );
				break;
			case BZIP :
				extractBzip( source, destination, overwrite );
				break;
			case TBZ :
			case TBZ2 :
				extractTarBzip( source, destination, overwrite, recurse, filter, entryPaths );
				break;
			default :
				throw new BoxRuntimeException( "Unsupported compression format: [" + format + "]" );
		}
	}

	/**
	 * Extracts a bzip2-compressed TAR archive.
	 *
	 * @param source      The source archive path
	 * @param destination The destination directory
	 * @param overwrite   Whether to overwrite existing files
	 * @param recurse     Whether to extract nested entries
	 * @param filter      A string entry-name filter
	 * @param entryPaths  Specific entry paths to extract
	 */
	private static void extractTarBzip(
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    Object filter,
	    Array entryPaths ) {
		try ( BZip2InputStream bzipInputStream = new BZip2InputStream( Files.newInputStream( ensurePath( source ) ), false );
		    TarInputStream tarInputStream = new TarInputStream( bzipInputStream ) ) {
			Path destinationPath = Paths.get( destination ).normalize().toAbsolutePath();
			Files.createDirectories( destinationPath );
			extractTarEntries( tarInputStream, destinationPath, source, destination, overwrite, recurse, filter, entryPaths );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting TBZ2 file: [" + source + "] to destination: [" + destination + "]", e );
		}
	}

	/**
	 * Extracts a raw bzip2 stream to a destination directory.
	 *
	 * @param source      The source bzip2 file
	 * @param destination The destination directory
	 * @param overwrite   Whether to overwrite an existing output file
	 */
	private static void extractBzip( String source, String destination, Boolean overwrite ) {
		Path	sourcePath		= ensurePath( source );
		Path	destinationPath	= Paths.get( destination ).normalize().toAbsolutePath();
		Path	targetPath		= destinationPath.resolve( sourcePath.getFileName().toString().replaceFirst( "(?i)\\.(bzip2|bz2|bz)$", "" ) );
		try {
			Files.createDirectories( destinationPath );
			if ( Files.exists( targetPath ) && !overwrite ) {
				throw new BoxRuntimeException( "Destination file already exists: [" + targetPath + "] and overwrite is not allowed." );
			}
			try ( BZip2InputStream bzipInputStream = new BZip2InputStream( Files.newInputStream( sourcePath ), false ) ) {
				Files.copy( bzipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting BZIP file: [" + source + "] to destination: [" + destination + "]", e );
		}
	}

	/**
	 * Extracts a raw TAR or gzip-compressed TAR archive with path traversal protection.
	 *
	 * @param source      The source archive path
	 * @param destination The destination directory
	 * @param overwrite   Whether to overwrite existing files
	 * @param recurse     Whether to extract nested entries
	 * @param filter      A string entry-name filter
	 * @param entryPaths  Specific entry paths to extract
	 * @param gzip        Whether the TAR stream is gzip-compressed
	 */
	private static void extractTar(
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    Object filter,
	    Array entryPaths,
	    Boolean gzip ) {
		Path	sourcePath		= ensurePath( source );
		Path	destinationPath	= Paths.get( destination ).normalize().toAbsolutePath();
		try {
			Files.createDirectories( destinationPath );
			InputStream	fileInputStream		= Files.newInputStream( sourcePath );
			InputStream	archiveInputStream	= gzip ? new GZIPInputStream( fileInputStream ) : fileInputStream;
			try ( TarInputStream tarInputStream = new TarInputStream( archiveInputStream ) ) {
				extractTarEntries( tarInputStream, destinationPath, source, destination, overwrite, recurse, filter, entryPaths );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting TAR file: [" + source + "] to destination: [" + destination + "]", e );
		}
	}

	/**
	 * Extracts entries from an already decompressed TAR stream.
	 *
	 * @param tarInputStream  The TAR input stream
	 * @param destinationPath The normalized destination directory
	 * @param source          The source archive path for diagnostics
	 * @param destination     The destination path for diagnostics
	 * @param overwrite       Whether to overwrite existing files
	 * @param recurse         Whether to extract nested entries
	 * @param filter          The entry filter
	 * @param entryPaths      Specific entries to extract
	 *
	 * @throws IOException If an archive entry cannot be read or written
	 */
	private static void extractTarEntries(
	    TarInputStream tarInputStream,
	    Path destinationPath,
	    String source,
	    String destination,
	    Boolean overwrite,
	    Boolean recurse,
	    Object filter,
	    Array entryPaths ) throws IOException {
		TarEntry entry;
		while ( ( entry = tarInputStream.getNextEntry() ) != null ) {
			String entryName = entry.getName();
			if ( entryName == null || entryName.isBlank() || ( !recurse && entryName.contains( "/" ) ) ) {
				continue;
			}
			if ( entry.isLink() ) {
				logger.warn( "Skipping unsupported TAR link entry [{}]", entryName );
				continue;
			}
			if ( filter != null && filter instanceof String filterString && !FileSystemUtil.fileMatchesPattern( filterString, Path.of( entryName ) ) ) {
				continue;
			}
			if ( entryPaths != null && !entryPaths.isEmpty() && !entryPaths.contains( entryName ) ) {
				continue;
			}
			Path targetPath = destinationPath.resolve( entryName ).normalize();
			if ( !targetPath.startsWith( destinationPath ) ) {
				logger.warn( "Tar Slip attack detected for entry [{}], skipping extraction", entryName );
				continue;
			}
			if ( Files.exists( targetPath ) && !overwrite ) {
				continue;
			}
			if ( entry.isDirectory() ) {
				Files.createDirectories( targetPath );
			} else {
				if ( targetPath.getParent() != null ) {
					Files.createDirectories( targetPath.getParent() );
				}
				Files.copy( tarInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING );
			}
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
						    return FileSystemUtil.fileMatchesPattern( castedFilter, Path.of( entry.getName() ) );
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
				    if ( !recurse && entry.getName().contains( "/" ) && entry.getName().split( "/" ).length > 1 ) {
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
	 * Extracts the content of a GZIP compressed byte array and returns the decompressed byte array.
	 *
	 * @param content The GZIP compressed byte array
	 * 
	 * @return The decompressed byte array
	 * 
	 * @throws BoxRuntimeException If an error occurs during decompression
	 */
	public static byte[] extractGZipContent( byte[] content ) {
		try ( GZIPInputStream gzipInputStream = new GZIPInputStream( new ByteArrayInputStream( content ) ) ) {
			return extractInputStream( gzipInputStream );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting GZIP content", e );
		}
	}

	/**
	 * Extracts the content of a Deflated compressed byte array and returns the decompressed byte array.
	 *
	 * @param content The Deflated compressed byte array
	 * 
	 * @return The decompressed byte array
	 * 
	 * @throws BoxRuntimeException If an error occurs during decompression
	 */
	public static byte[] inflateDeflatedContent( byte[] content ) {
		try ( InflaterInputStream inflaterInputStream = new InflaterInputStream( new ByteArrayInputStream( content ) ) ) {
			return extractInputStream( inflaterInputStream );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting deflated content", e );
		}
	}

	/**
	 * Extracts the contents of an input stream and returns a byte array
	 * 
	 * @param extractionStream the input stream
	 */
	public static byte[] extractInputStream( InputStream extractionStream ) {
		try ( ByteArrayOutputStream outputStream = new ByteArrayOutputStream() ) {
			byte[]	buffer	= new byte[ 1024 ];
			int		len;
			while ( ( len = extractionStream.read( buffer ) ) > 0 ) {
				outputStream.write( buffer, 0, len );
			}
			return outputStream.toByteArray();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error extracting content from InputStream", e );
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
				    if ( filter != null ) {
					    // Apply regex filter if present
					    if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
						    return FileSystemUtil.fileMatchesPattern( castedFilter, Path.of( entry.getName() ) );
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
				    }
				    return true;
			    } )
			    // Recursion Filter
			    .filter( entry -> {
				    // Skip entries that are inside subdirectories if recurse is false
				    if ( recurse == false && entry.getName().contains( "/" ) && entry.getName().split( "/" ).length > 1 ) {
					    // System.out.println( "Skipping entry: " + entry.getName() );
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
			        "directory", StringUtils.substringBeforeLast( entry.getName(), "/" ),
			        "fullpath", entry.getName(),
			        "isDirectory", entry.isDirectory(),
			        "name", StringUtils.substringAfterLast( entry.getName(), "/" ),
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
	 * @param filter  A regex or BoxLang function or Java Predicate to apply as a filter to the extraction.
	 * @param recurse Whether to recurse into subdirectories, default is true.
	 * @param context The BoxLang context
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
	 * @param source     The absolute path of the zip file
	 * @param filter     The filter to apply to the entries: string regex, BoxLang function or Java Predicate to be deleted
	 * @param entryPaths The specific entry paths to delete from the zip file
	 * @param context    The BoxLang context if using BoxLang functions
	 */
	public static void deleteEntries( String source, Object filter, Array entryPaths, IBoxContext context ) {
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
			    // Filters out the entries to delete
			    .filter( entry -> {
				    if ( filter != null ) {
					    // If the regex matches then that means we are deleting the entry, so we return false
					    if ( filter instanceof String castedFilter && castedFilter.length() > 1 ) {
						    return FileSystemUtil.fileMatchesPattern( castedFilter, Path.of( entry.getName() ) );
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
				    }
				    // Survives execution :!
				    return true;
			    } )
			    // Apply entry paths filter
			    .filter( entry -> {
				    if ( entryPaths != null && !entryPaths.isEmpty() ) {
					    return !entryPaths.contains( entry.getName() );
				    }
				    // Survives execution :!
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
