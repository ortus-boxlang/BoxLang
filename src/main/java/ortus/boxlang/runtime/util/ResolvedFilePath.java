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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.runtime.types.exceptions.BoxIOException;

/**
 * I represent the a file path that has been resolved to an absolute path.
 * I track additional data such as what mapping was used to resolve the path.
 *
 */
public record ResolvedFilePath( String mappingName, String mappingPath, String relativePath, Path absolutePath ) {

	/**
	 * Factor method to create a new ResolvedFilePath instance.
	 *
	 * @param mappingName  The mapping name used to resolve the path.
	 * @param mappingPath  The mapping path used to resolve the path.
	 * @param relativePath The relative path that was resolved
	 * @param absolutePath The absolute path resolved.
	 *
	 * @return A new ResolvedFilePath instance.
	 */
	public static ResolvedFilePath of( String mappingName, String mappingPath, String relativePath, Path absolutePath ) {
		return new ResolvedFilePath(
		    mappingName,
		    mappingPath,
		    relativePath,
		    absolutePath != null ? makeReal( absolutePath.normalize() ) : null
		);
	}

	/**
	 * Factory method to create a new ResolvedFilePath instance, but using a string for the absolute path.
	 *
	 * @param mappingName  The mapping name used to resolve the path.
	 * @param mappingPath  The mapping path used to resolve the path.
	 * @param relativePath The relative path that was resolved
	 * @param absolutePath The absolute path resolved.
	 *
	 * @return A new ResolvedFilePath instance.
	 */
	public static ResolvedFilePath of( String mappingName, String mappingPath, String relativePath, String absolutePath ) {
		return ResolvedFilePath.of(
		    mappingName,
		    mappingPath,
		    relativePath,
		    Path.of( absolutePath )
		);
	}

	/**
	 * Factory method to create a new ResolvedFilePath instance using only an absolute path.
	 *
	 * @param absolutePath The absolute path resolved.
	 *
	 * @return A new ResolvedFilePath instance.
	 */
	public static ResolvedFilePath of( Path absolutePath ) {
		return new ResolvedFilePath(
		    null,
		    null,
		    absolutePath != null ? absolutePath.normalize().toString() : null,
		    makeReal( absolutePath )
		);
	}

	/**
	 * Factory method to create a new ResolvedFilePath instance using only an absolute path which is a string
	 *
	 * @param absolutePath The absolute path resolved.
	 *
	 * @return A new ResolvedFilePath instance.
	 */
	public static ResolvedFilePath of( String absolutePath ) {
		return ResolvedFilePath.of( Path.of( absolutePath ) );
	}

	/**
	 * Was the path resolved via a mapping?
	 *
	 * @return true if the path was resolved via a mapping.
	 */
	public boolean resolvedViaMapping() {
		return mappingName != null;
	}

	/**
	 * Get the package of the resolved path, but with a prefix prepended in front
	 *
	 * @param prefix The prefix to prepend to the package.
	 *
	 * @return The package of the resolved path with the prefix prepended.
	 */
	public FQN getFQN( String prefix ) {
		return FQN.of( prefix, relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	/**
	 * Get the package of the resolved path.
	 *
	 * @return The package of the resolved path.
	 */
	public FQN getFQN() {
		return FQN.of( relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	/**
	 * Get the Box package of the resolved path.
	 *
	 * @return The package of the resolved path.
	 */
	public BoxFQN getBoxFQN() {
		return BoxFQN.of( relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	/**
	 * Get the Box package of the resolved path, but with a prefix prepended in front
	 *
	 * @param prefix The prefix to prepend to the package.
	 *
	 * @return The package of the resolved path with the prefix prepended.
	 */
	public BoxFQN getBoxFQN( String prefix ) {
		return BoxFQN.of( prefix, relativePath != null ? Path.of( relativePath ) : absolutePath );
	}

	/**
	 * Create a new ResolvedFilePath instance from a path relative to the current path.
	 *
	 * @param relativePath The relative path to create a new ResolvedFilePath instance from.
	 *
	 * @return A new ResolvedFilePath instance.
	 */
	public ResolvedFilePath newFromRelative( String relativePath ) {
		String	newRelativePath;
		Path	absoluteParent	= absolutePath().getParent();
		Path	newAbsolutePath	= absoluteParent.resolve( relativePath );

		if ( absolutePath().toString().equals( relativePath() ) ) {
			newRelativePath = newAbsolutePath.toString();
		} else {
			String	tmpAbsolutePath	= Paths.get( absolutePath().toString() ).toAbsolutePath().normalize().toString();
			String	tmpRelativePath	= Paths.get( relativePath() ).normalize().toString();
			Path	absoluteRoot	= Paths.get( tmpAbsolutePath.replace( tmpRelativePath, "" ) );
			newRelativePath = absoluteRoot.relativize( newAbsolutePath ).normalize().toString();
		}
		return ResolvedFilePath.of(
		    mappingName,
		    mappingPath,
		    newRelativePath,
		    newAbsolutePath
		);
	}

	private static Path makeReal( Path path ) {
		// if exists, make it real
		if ( path != null && path.toFile().exists() ) {
			try {
				return path.toRealPath();
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
		return path;
	}

}
