package ortus.boxlang.runtime.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a fully qualified name (FQN) for a class or package.
 * It handles all the edge cases of dealing with file paths and package names.
 */
public class FQN {

	/**
	 * These words cannot appear in a package name.
	 */
	static final Set<String>	RESERVED_WORDS	= new HashSet<>( Arrays.asList( "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
	    "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements",
	    "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
	    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while" ) );

	/**
	 * An array of strings representing all the pieces of the FQN.
	 */
	private String[]			parts;

	/**
	 * Construct an FQN that uses the root path to generate a relative path based on filePath.
	 * 
	 * For example, given root = "c:\foo\bar" and filePath = "c:\foo\bar\test\Car.bx" this will create an FQN
	 * like "test.Car".
	 * 
	 * @param root     The root path to use to generate the relative path.
	 * @param filePath The file path to generate the FQN from.
	 */
	public FQN( Path root, Path filePath ) {
		root		= root.toAbsolutePath();
		filePath	= filePath.toAbsolutePath();
		if ( !filePath.startsWith( root ) ) {
			throw new IllegalArgumentException( "File path must be a child of the root path." );
		}
		this.parts = parseParts( parseFromFile( root.relativize( filePath ) ) );
	}

	private FQN( String[] parts ) {
		this.parts = parts;
	}

	/**
	 * Construct an FQN from a Path.
	 * 
	 * @param path The path to generate the FQN from.
	 */
	public FQN( Path path ) {
		this.parts = parseParts( parseFromFile( path ) );
	}

	/**
	 * Construct an FQN from a string.
	 * 
	 * @param path The string to generate the FQN from.
	 */
	public FQN( String path ) {
		this.parts = parseParts( path );
	}

	/**
	 * Construct an FQN from a prefix and an existing FQN.
	 * 
	 * @param prefix The prefix to add to the FQN.
	 * @param fqn    The existing FQN to add the prefix to.
	 */
	public FQN( String prefix, FQN fqn ) {
		this.parts		= new String[ fqn.parts.length + 1 ];
		this.parts[ 0 ]	= prefix;
		System.arraycopy( fqn.parts, 0, this.parts, 1, fqn.parts.length );
	}

	/**
	 * Construct an FQN from a prefix and a string.
	 * 
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The string to generate the FQN from.
	 */
	public FQN( String prefix, String path ) {
		this.parts		= new String[ parseParts( path ).length + 1 ];
		this.parts[ 0 ]	= prefix;
		System.arraycopy( parseParts( path ), 0, this.parts, 1, parseParts( path ).length );
	}

	/**
	 * Construct an FQN from a prefix and a Path.
	 * 
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The path to generate the FQN from.
	 */
	public FQN( String prefix, Path path ) {
		var	prefixParts	= parseParts( prefix );
		var	pathParts	= parseParts( parseFromFile( path ) );
		this.parts = new String[ prefixParts.length + pathParts.length ];
		System.arraycopy( prefixParts, 0, this.parts, 0, prefixParts.length );
		System.arraycopy( pathParts, 0, this.parts, prefixParts.length, pathParts.length );
	}

	/**
	 * Get the FQN as a string. Includes both the name and package.
	 * 
	 * @return String
	 */
	public String toString() {
		return String.join( ".", parts );
	}

	/**
	 * Get only the package name.
	 * 
	 * @return String
	 */
	public String getPackageString() {
		return getPackage().toString();
	}

	/**
	 * Get only the package as an FQN.
	 * 
	 * @return String
	 */
	public FQN getPackage() {
		if ( parts.length > 1 ) {
			return new FQN( Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
		} else {
			return new FQN( new String[] {} );
		}
	}

	/**
	 * Transforms the path into the package name
	 *
	 * @param fqn String to grab the package name for.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	static String[] parseParts( String fqn ) {
		// Replace .. with .
		fqn = fqn.replaceAll( "\\.\\.", "." );
		// trim trailing period
		if ( fqn.endsWith( "." ) ) {
			fqn = fqn.substring( 0, fqn.length() - 1 );
		}
		// trim leading period
		if ( fqn.startsWith( "." ) ) {
			fqn = fqn.substring( 1 );
		}
		// Remove any non alpha-numeric chars.
		fqn = fqn.replaceAll( "[^a-zA-Z0-9\\.]", "" );

		if ( fqn.isEmpty() ) {
			return new String[] {};
		}

		// Find the last period in the string
		int lastPeriodIndex = fqn.lastIndexOf( '.' );
		if ( lastPeriodIndex != -1 ) {
			// Lowercase everything up to the last period
			String	beforeLastPeriod	= fqn.substring( 0, lastPeriodIndex ).toLowerCase();
			String	afterLastPeriod		= fqn.substring( lastPeriodIndex + 1 );
			fqn = beforeLastPeriod + "." + afterLastPeriod;
		}

		// parse fqn into list, loop over list and remove any empty strings and turn back into fqn
		return Arrays.stream( fqn.split( "\\." ) )
		    // if starts with number, prefix with _
		    .map( s -> s.matches( "^\\d.*" ) ? "_" + s : s )
		    .map( s -> {
			    if ( RESERVED_WORDS.contains( s ) ) {
				    return "_" + s;
			    }
			    return s;
		    } )
		    .toArray( String[]::new );

	}

	/**
	 * Parse the package from a file path.
	 * 
	 * @param file The file to parse the package from.
	 * 
	 * @return The package name.
	 */
	static String parseFromFile( Path file ) {
		// Strip extension from file name, if exists
		String	fileName	= file.getFileName().toString();
		int		dotIndex	= fileName.lastIndexOf( '.' );
		if ( dotIndex > 0 ) {
			fileName = fileName.substring( 0, dotIndex );
		}
		String	packg;
		Path	parent	= file.getParent();
		if ( parent != null ) {
			packg = parent.resolve( fileName ).toString();
		} else {
			packg = fileName;
		}

		if ( packg.startsWith( "/" ) || packg.startsWith( "\\" ) ) {
			packg = packg.substring( 1 );
		}
		// trim trailing \ or /
		if ( packg.endsWith( "\\" ) || packg.endsWith( "/" ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
		}

		// Take out periods in folder names
		packg	= packg.replaceAll( "\\.", "" );
		// Replace / with .
		packg	= packg.replaceAll( "/", "." );
		// Remove any : from Windows drives
		packg	= packg.replaceAll( ":", "" );
		// Replace \ with .
		packg	= packg.replaceAll( "\\\\", "." );

		return packg;

	}

	/**
	 * Factory method to create a new FQN instance from a prefix and a string.
	 * 
	 * @return A new FQN instance.
	 */
	public static FQN of( String prefix, String path ) {
		return new FQN( prefix, path );
	}

	/**
	 * Factory method to create a new FQN instance from a prefix and a path.
	 * 
	 * @return A new FQN instance.
	 */
	public static FQN of( String prefix, Path path ) {
		return new FQN( prefix, path );
	}

	/**
	 * Factory method to create a new FQN instance from a string.
	 * 
	 * @return A new FQN instance.
	 */
	public static FQN of( String path ) {
		return new FQN( path );
	}

	/**
	 * Factory method to create a new FQN instance from a path.
	 * 
	 * @return A new FQN instance.
	 */
	public static FQN of( Path path ) {
		return new FQN( path );
	}

	/**
	 * Create a new FQN instance from the current FQN with the prefix appended.
	 * 
	 * @return A new FQN instance.
	 */
	public FQN appendPrefix( String prefix ) {
		return new FQN( prefix, this );
	}
}
