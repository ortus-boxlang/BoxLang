package ortus.boxlang.runtime.util;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.*;

/**
 * Provides a fluent API for performing ZIP operations within BoxLang.
 *
 * <p>
 * This utility supports:
 * <ul>
 * <li>Compressing files or directories into a ZIP archive</li>
 * <li>Extracting ZIP archives into directories</li>
 * <li>Listing contents of a ZIP archive without extraction</li>
 * </ul>
 *
 * <p>
 * Example usage:
 * 
 * <pre>
 * new ZipFile()
 *     .source( "/path/to/folder" )
 *     .to( "/path/to/archive.zip" )
 *     .compress();
 * </pre>
 */
public class ZipFile {

	/** The source file or directory path */
	private String	source;

	/** The destination file or directory path */
	private String	destination;

	/**
	 * Default constructor.
	 */
	public ZipFile() {
	}

	/**
	 * Sets the source file or directory for the ZIP operation.
	 *
	 * @param path Absolute or relative path to source file/directory
	 * 
	 * @return This ZipFile instance for method chaining
	 */
	public ZipFile source( String path ) {
		this.source = path;
		return this;
	}

	/**
	 * Sets the destination file or directory for the ZIP operation.
	 *
	 * @param path Absolute or relative path to destination
	 * 
	 * @return This ZipFile instance for method chaining
	 */
	public ZipFile to( String path ) {
		this.destination = path;
		return this;
	}

	/**
	 * Compresses the source file or directory into a ZIP archive.
	 *
	 * <p>
	 * If the source is a directory, it recursively walks the directory
	 * and preserves relative paths inside the ZIP file.
	 * </p>
	 *
	 * @throws BoxRuntimeException if compression fails
	 */
	public void compress() {

		validateSourceAndDestination();

		Path	sourcePath	= Paths.get( source );       // Convert source string to Path
		Path	zipPath		= Paths.get( destination );     // Convert destination to Path

		if ( !Files.exists( sourcePath ) ) {
			throw new BoxRuntimeException( "Source path does not exist: " + source );
		}

		try ( ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( zipPath.toFile() ) ) ) {

			if ( Files.isDirectory( sourcePath ) ) {

				// Walk directory tree and process files only
				try ( var paths = Files.walk( sourcePath ) ) {

					paths
					    .filter( path -> !Files.isDirectory( path ) )
					    .forEach( path -> {

						    // Create relative entry name inside ZIP
						    ZipEntry zipEntry = new ZipEntry( sourcePath.relativize( path ).toString() );

						    try {
							    zos.putNextEntry( zipEntry );     // Start ZIP entry
							    Files.copy( path, zos );          // Copy file contents
							    zos.closeEntry();                 // Close entry
						    } catch ( IOException e ) {
							    throw new BoxRuntimeException(
							        "Error compressing file: " + path, e
							    );
						    }
					    } );
				}

			} else {

				// Single file compression
				ZipEntry zipEntry = new ZipEntry( sourcePath.getFileName().toString() );

				zos.putNextEntry( zipEntry );
				Files.copy( sourcePath, zos );
				zos.closeEntry();
			}

		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Compression failed", e );
		}
	}

	/**
	 * Extracts a ZIP archive into the destination directory.
	 *
	 * <p>
	 * Includes protection against ZIP Slip vulnerability.
	 * </p>
	 *
	 * @throws BoxRuntimeException if extraction fails
	 */
	public void extract() {

		validateSourceAndDestination();

		Path	zipPath	= Paths.get( source );
		Path	destDir	= Paths.get( destination );

		try ( ZipInputStream zis = new ZipInputStream( new FileInputStream( zipPath.toFile() ) ) ) {

			ZipEntry entry;

			while ( ( entry = zis.getNextEntry() ) != null ) {

				Path newFile = destDir.resolve( entry.getName() ).normalize();

				// Prevent ZIP Slip vulnerability
				if ( !newFile.startsWith( destDir ) ) {
					throw new BoxRuntimeException(
					    "Invalid ZIP entry (possible ZIP Slip attack): "
					        + entry.getName()
					);
				}

				if ( entry.isDirectory() ) {

					Files.createDirectories( newFile );

				} else {

					Files.createDirectories( newFile.getParent() );

					try ( FileOutputStream fos = new FileOutputStream( newFile.toFile() ) ) {

						zis.transferTo( fos );
					}
				}

				zis.closeEntry();
			}

		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Extraction failed", e );
		}
	}

	/**
	 * Lists the contents of a ZIP archive without extracting it.
	 *
	 * @return List of entry names inside the ZIP archive
	 * 
	 * @throws BoxRuntimeException if listing fails
	 */
	public List<String> list() {

		if ( source == null ) {
			throw new BoxRuntimeException( "Source zip file not set" );
		}

		List<String> entries = new ArrayList<>();

		try ( java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile( source ) ) {

			zipFile.stream()
			    .forEach( entry -> entries.add( entry.getName() ) );

		} catch ( IOException e ) {
			throw new BoxRuntimeException(
			    "Failed to list zip contents", e
			);
		}

		return entries;
	}

	/**
	 * Validates that both source and destination have been set.
	 *
	 * @throws BoxRuntimeException if either value is missing
	 */
	private void validateSourceAndDestination() {

		if ( source == null || destination == null ) {
			throw new BoxRuntimeException(
			    "Source and destination must be specified"
			);
		}
	}
}
