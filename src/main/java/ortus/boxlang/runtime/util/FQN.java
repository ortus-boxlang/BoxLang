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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.Strings;

/**
 * This class represents a java fully qualified name (FQN) for a class or package.
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
	protected String[]			parts;

	/**
	 * Cached fqn
	 */
	protected String			fqn				= null;

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

	protected FQN( String[] parts ) {
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
		combineParts( parseParts( prefix, true ), fqn.parts );
	}

	/**
	 * Construct an FQN from a prefix and a string.
	 *
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The string to generate the FQN from.
	 */
	public FQN( String prefix, String path ) {
		combineParts( parseParts( prefix, true ), parseParts( path ) );
	}

	/**
	 * Construct an FQN from a prefix and a Path.
	 *
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The path to generate the FQN from.
	 */
	public FQN( String prefix, Path path ) {
		combineParts( parseParts( prefix, true ), parseParts( parseFromFile( path ) ) );
	}

	/**
	 * Construct an FQN from a prefix and a Path.
	 *
	 * @param prefixParts The prefix to add to the FQN.
	 * @param pathParts   The path to generate the FQN from.
	 */
	protected void combineParts( String[] prefixParts, String[] pathParts ) {
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
		if ( fqn == null ) {
			fqn = _toString();
		}
		return fqn;
	}

	/**
	 * Get the FQN as a string. Includes both the name and package.
	 *
	 * @return String
	 */
	private String _toString() {
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

	public String getClassName() {
		if ( parts.length == 0 ) {
			return "";
		}
		return parts[ parts.length - 1 ];
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
	 * Transforms the path into the package name following Java rules.
	 *
	 * @param fqn String to grab the package name for.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	protected String[] parseParts( String fqn ) {
		return parseParts( fqn, false );
	}

	/**
	 * Transforms the path into the package name following Java rules.
	 *
	 * @param fqn        String to grab the package name for.
	 * @param allPackage True to return all parts as package names.
	 *
	 * @return returns the class name according the name conventions Test.ext -
	 *         Test$ext
	 */
	protected String[] parseParts( String fqn, boolean allPackage ) {
		fqn = normalizeDots( fqn );

		// Remove any non alpha-numeric chars.
		// Replace any character not a-z, A-Z, 0-9, $, or . with "__"
		StringBuilder sb = new StringBuilder( fqn.length() + 10 );
		for ( int i = 0; i < fqn.length(); i++ ) {
			char c = fqn.charAt( i );
			if ( ( c >= 'a' && c <= 'z' ) ||
			    ( c >= 'A' && c <= 'Z' ) ||
			    ( c >= '0' && c <= '9' ) ||
			    c == '$' || c == '.' ) {
				sb.append( c );
			} else {
				sb.append( "__" );
			}
		}
		fqn = sb.toString();

		// Short circuit if empty
		if ( fqn.isEmpty() ) {
			return new String[] {};
		}

		if ( allPackage ) {
			fqn = fqn.toLowerCase();
		} else {
			char[]	chars			= fqn.toCharArray();
			int		lastPeriodIndex	= fqn.lastIndexOf( '.' );

			for ( int i = 0; i < chars.length; i++ ) {
				if ( i == lastPeriodIndex + 1 ) {
					// First char after last period - uppercase
					chars[ i ] = Character.toUpperCase( chars[ i ] );
				} else {
					// Everything else - lowercase
					chars[ i ] = Character.toLowerCase( chars[ i ] );
				}
			}

			fqn = new String( chars );
		}

		// parse fqn into array, loop over array and clean/normalize parts
		String[] splitParts = splitOnDot( fqn );
		for ( int i = 0; i < splitParts.length; i++ ) {
			String s = splitParts[ i ];
			// if starts with number, prefix with _
			if ( !s.isEmpty() && Character.isDigit( s.charAt( 0 ) ) ) {
				s = "_" + s;
			}
			if ( RESERVED_WORDS.contains( s ) ) {
				s = "_" + s;
			}
			splitParts[ i ] = s;
		}
		return splitParts;
	}

	/**
	 * Split a string on dots without using regex.
	 * Pre-counts segments to allocate the array exactly once.
	 *
	 * @param str The string to split.
	 *
	 * @return An array of strings split on dots.
	 */
	protected static String[] splitOnDot( String str ) {
		int	len		= str.length();

		// Count dots to determine array size
		int	count	= 1;
		for ( int i = 0; i < len; i++ ) {
			if ( str.charAt( i ) == '.' ) {
				count++;
			}
		}

		String[]	result	= new String[ count ];
		int			idx		= 0;
		int			start	= 0;
		for ( int i = 0; i < len; i++ ) {
			if ( str.charAt( i ) == '.' ) {
				result[ idx++ ]	= str.substring( start, i );
				start			= i + 1;
			}
		}
		result[ idx ] = str.substring( start );
		return result;
	}

	/**
	 * Normalize the dots in a string.
	 * - Remove any double dots.
	 * - Trim trailing period.
	 * - Trim leading period.
	 *
	 * @param fqn The string to normalize.
	 *
	 * @return The normalized string.
	 */
	protected String normalizeDots( String fqn ) {
		// Replace .. with .
		while ( fqn.contains( ".." ) ) {
			fqn = fqn.replace( "..", "." );
		}

		// trim trailing period
		if ( fqn.endsWith( "." ) ) {
			fqn = fqn.substring( 0, fqn.length() - 1 );
		}

		// trim leading period
		if ( fqn.startsWith( "." ) ) {
			fqn = fqn.substring( 1 );
		}
		return fqn;
	}

	/**
	 * Parse the package from a file path.
	 *
	 * @param file The file to parse the package from.
	 *
	 * @return The package name.
	 */
	protected String parseFromFile( Path file ) {
		// Strip extension from file name, if exists
		String fileName = file.getFileName().toString();
		fileName = cleanFileName( fileName );

		String	fqn;
		Path	parent	= file.getParent();
		if ( parent != null ) {
			fqn = parent.resolve( fileName ).toString();
		} else {
			fqn = fileName;
		}

		if ( fqn.startsWith( "/" ) || fqn.startsWith( "\\" ) ) {
			fqn = fqn.substring( 1 );
		}
		// trim trailing \ or /
		if ( fqn.endsWith( "\\" ) || fqn.endsWith( "/" ) ) {
			fqn = fqn.substring( 0, fqn.length() - 1 );
		}

		// Replace all periods with an emtpy string in fqn
		fqn	= Strings.CS.remove( fqn, "." );
		// Take out slashes to . and backslashes to .
		fqn	= Strings.CS.replace( fqn, "/", "." );
		fqn	= Strings.CS.replace( fqn, "\\", "." );
		// Take out colons to _
		fqn	= Strings.CS.replace( fqn, ":", "_" );
		return fqn;
	}

	/**
	 * Clean the file name by replacing periods with $.
	 *
	 * @param fileName The file name to clean.
	 *
	 * @return The cleaned file name.
	 */
	protected String cleanFileName( String fileName ) {
		return fileName.replace( ".", "$" );
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
