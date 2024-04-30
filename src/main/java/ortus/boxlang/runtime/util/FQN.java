package ortus.boxlang.runtime.util;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FQN {

	static final Set<String>	RESERVED_WORDS	= new HashSet<>( Arrays.asList( "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
	    "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements",
	    "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static",
	    "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while" ) );

	public Path					root			= null;
	public Path					filePath		= null;
	private String[]			parts;

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

		this.parts		= parseParts( root.relativize( filePath ).toString() );
	}

	public FQN( Path path ) {
		this.parts = parseParts( parsePackageFromFile( path ) );
	}

	public FQN( String path ) {
		this.parts = parseParts( path );
	}

	public FQN( String prefix, FQN fqn ) {
		this.parts		= new String[ fqn.parts.length + 1 ];
		this.parts[ 0 ]	= prefix;
		System.arraycopy( fqn.parts, 0, this.parts, 1, fqn.parts.length );
	}

	public FQN( String prefix, String path ) {
		this.parts		= new String[ parseParts( path ).length + 1 ];
		this.parts[ 0 ]	= prefix;
		System.arraycopy( parseParts( path ), 0, this.parts, 1, parseParts( path ).length );
	}

	public FQN( String prefix, Path path ) {
		this.parts		= new String[ parseParts( parsePackageFromFile( path ) ).length + 1 ];
		this.parts[ 0 ]	= prefix;
		System.arraycopy( parseParts( parsePackageFromFile( path ) ), 0, this.parts, 1, parseParts( parsePackageFromFile( path ) ).length );
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
		return String.join( ".", Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
	}

	/**
	 * Transforms the path into the package name
	 *
	 * @param packg String to grab the package name for.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	static String[] parseParts( String packg ) {
		// Replace .. with .
		packg = packg.replaceAll( "\\.\\.", "." );
		// trim trailing period
		if ( packg.endsWith( "." ) ) {
			packg = packg.substring( 0, packg.length() - 1 );
		}
		// trim leading period
		if ( packg.startsWith( "." ) ) {
			packg = packg.substring( 1 );
		}
		// Remove any non alpha-numeric chars.
		packg = packg.replaceAll( "[^a-zA-Z0-9\\.]", "" );

		if ( packg.isEmpty() ) {
			return new String[] {};
		}
		// parse fqn into list, loop over list and remove any empty strings and turn back into fqn
		return Arrays.stream( packg.split( "\\." ) )
		    .map( s -> s.toLowerCase() )
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

	static String parsePackageFromFile( Path file ) {
		String packg = file.toFile().toString().replace( File.separatorChar + file.toFile().getName(), "" );
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

	public static FQN of( String prefix, String path ) {
		return new FQN( prefix, path );
	}

	public static FQN of( String prefix, Path path ) {
		return new FQN( prefix, path );
	}

	public static FQN of( String path ) {
		return new FQN( path );
	}

	public static FQN of( Path path ) {
		return new FQN( path );
	}

	public FQN appendPrefix( String prefix ) {
		return new FQN( prefix, this );
	}
}
