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

/**
 * This class represents a BoxLang fully qualified name (FQN) for a class or package.
 * It handles all the edge cases of dealing with file paths and package names.
 */
public class BoxFQN extends FQN {

	/**
	 * Construct an FQN that uses the root path to generate a relative path based on filePath.
	 *
	 * For example, given root = "c:\foo\bar" and filePath = "c:\foo\bar\test\Car.bx" this will create an FQN
	 * like "test.Car".
	 *
	 * @param root     The root path to use to generate the relative path.
	 * @param filePath The file path to generate the FQN from.
	 */
	public BoxFQN( Path root, Path filePath ) {
		super( root, filePath );
	}

	private BoxFQN( String[] parts ) {
		super( parts );
	}

	/**
	 * Construct an FQN from a Path.
	 *
	 * @param path The path to generate the FQN from.
	 */
	public BoxFQN( Path path ) {
		super( path );
	}

	/**
	 * Construct an FQN from a string.
	 *
	 * @param path The string to generate the FQN from.
	 */
	public BoxFQN( String path ) {
		super( path );
	}

	/**
	 * Construct an FQN from a prefix and an existing FQN.
	 *
	 * @param prefix The prefix to add to the FQN.
	 * @param fqn    The existing FQN to add the prefix to.
	 */
	public BoxFQN( String prefix, FQN fqn ) {
		super( prefix, fqn );
	}

	/**
	 * Construct an FQN from a prefix and a string.
	 *
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The string to generate the FQN from.
	 */
	public BoxFQN( String prefix, String path ) {
		super( prefix, path );
	}

	/**
	 * Construct an FQN from a prefix and a Path.
	 *
	 * @param prefix The prefix to add to the FQN.
	 * @param path   The path to generate the FQN from.
	 */
	public BoxFQN( String prefix, Path path ) {
		super( prefix, path );
	}

	/**
	 * Get only the package as an FQN.
	 *
	 * @return String
	 */
	public BoxFQN getPackage() {
		if ( parts.length > 1 ) {
			return new BoxFQN( Arrays.copyOfRange( parts, 0, parts.length - 1 ) );
		} else {
			return new BoxFQN( new String[] {} );
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
		fqn = normalizeDots( fqn );

		if ( fqn.isEmpty() ) {
			return new String[] {};
		}

		// parse fqn into array of parts
		return Arrays.stream( fqn.split( "\\." ) )
		    .toArray( String[]::new );
	}

	/**
	 * Clean the file name by removing the extension.
	 *
	 * @param fileName The file name to clean.
	 *
	 * @return The cleaned file name.
	 */
	protected String cleanFileName( String fileName ) {
		int dotIndex = fileName.lastIndexOf( '.' );
		if ( dotIndex > 0 ) {
			fileName = fileName.substring( 0, dotIndex );
		}
		return fileName;
	}

	/**
	 * Factory method to create a new BoxFQN instance from a prefix and a string.
	 *
	 * @return A new BoxFQN instance.
	 */
	public static BoxFQN of( String prefix, String path ) {
		return new BoxFQN( prefix, path );
	}

	/**
	 * Factory method to create a new BoxFQN instance from a prefix and a path.
	 *
	 * @return A new BoxFQN instance.
	 */
	public static BoxFQN of( String prefix, Path path ) {
		return new BoxFQN( prefix, path );
	}

	/**
	 * Factory method to create a new BoxFQN instance from a string.
	 *
	 * @return A new BoxFQN instance.
	 */
	public static BoxFQN of( String path ) {
		return new BoxFQN( path );
	}

	/**
	 * Factory method to create a new BoxFQN instance from a path.
	 *
	 * @return A new BoxFQN instance.
	 */
	public static BoxFQN of( Path path ) {
		return new BoxFQN( path );
	}

	/**
	 * Create a new BoxFQN instance from the current BoxFQN with the prefix appended.
	 *
	 * @return A new BoxFQN instance.
	 */
	public BoxFQN appendPrefix( String prefix ) {
		return new BoxFQN( prefix, this );
	}
}
