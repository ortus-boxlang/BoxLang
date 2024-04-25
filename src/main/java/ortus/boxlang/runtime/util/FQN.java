package ortus.boxlang.runtime.util;

import java.nio.file.Path;
import java.util.Arrays;

public class FQN {

	public final Path	root;
	public final Path	filePath;
	private String[]	parts;

	/**
	 * Construct an FQN that uses the root path to generate a relative path based on filePath.
	 * 
	 * For example, given root = "c:\foo\bar" and filePath = "c:\foo\bar\test\Car.bx" this will create an FQN
	 * like "test.Car".
	 * 
	 * @param root
	 * @param filePath
	 */
	public FQN( Path root, Path filePath ) {
		this.root		= root;
		this.filePath	= filePath;

		this.parts		= root.relativize( filePath )
		    .toString()
		    .replaceAll( "\\.\\.|\\\\|/", "." )
		    .split( "\\." );
	}

	/**
	 * Get the FQN as a string. Includes both the name and package.
	 * 
	 * @return String
	 */
	public String toString() {
		return String.join( ".", Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
	}

	/**
	 * Get only the package name.
	 * 
	 * @return String
	 */
	public String getPackageString() {
		return String.join( ".", Arrays.copyOfRange( parts, 0, parts.length - 2 ) );
	}
}
