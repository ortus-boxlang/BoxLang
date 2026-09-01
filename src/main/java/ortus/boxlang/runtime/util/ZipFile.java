package ortus.boxlang.runtime.util;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

import java.util.List;

/**
 * Provides a fluent API for performing ZIP operations within BoxLang.
 *
 * <p>
 * This class is a thin fluent delegator — all actual ZIP logic lives in
 * {@link ZipUtil}. This follows the same pattern as {@link PropertyFile},
 * which delegates to its underlying utility.
 * </p>
 *
 * <p>
 * Supported operations:
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
	 * Constructor with source path.
	 *
	 * @param source Absolute or relative path to source file/directory
	 */
	public ZipFile( String source ) {
		this.source = source;
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
	 * Gets the source path.
	 *
	 * @return The source path
	 */
	public String getSource() {
		return this.source;
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
	 * Gets the destination path.
	 *
	 * @return The destination path
	 */
	public String getDestination() {
		return this.destination;
	}

	/**
	 * Compresses the source file or directory into a ZIP archive
	 * at the destination path.
	 *
	 * <p>
	 * Delegates to {@link ZipUtil#compress(String, String)}.
	 * </p>
	 *
	 * @return This ZipFile instance for method chaining
	 *
	 * @throws BoxRuntimeException if source or destination is not set,
	 *                             or if compression fails
	 */
	public ZipFile compress() {
		validateSourceAndDestination();
		ZipUtil.compress( this.source, this.destination );
		return this;
	}

	/**
	 * Extracts the source ZIP archive into the destination directory.
	 *
	 * <p>
	 * Delegates to {@link ZipUtil#extract(String, String)}.
	 * </p>
	 *
	 * @return This ZipFile instance for method chaining
	 *
	 * @throws BoxRuntimeException if source or destination is not set,
	 *                             or if extraction fails
	 */
	public ZipFile extract() {
		validateSourceAndDestination();
		ZipUtil.extract( this.source, this.destination );
		return this;
	}

	/**
	 * Lists the contents of the source ZIP archive without extracting it.
	 *
	 * <p>
	 * Delegates to {@link ZipUtil#list(String)}.
	 * </p>
	 *
	 * @return List of entry names inside the ZIP archive
	 *
	 * @throws BoxRuntimeException if source is not set, or if listing fails
	 */
	public List<String> list() {
		if ( this.source == null ) {
			throw new BoxRuntimeException( "Source zip file not set" );
		}
		return ZipUtil.list( this.source );
	}

	/**
	 * Validates that both source and destination have been set.
	 *
	 * @throws BoxRuntimeException if either value is missing
	 */
	private void validateSourceAndDestination() {
		if ( this.source == null || this.destination == null ) {
			throw new BoxRuntimeException(
			    "Source and destination must be specified"
			);
		}
	}
}
