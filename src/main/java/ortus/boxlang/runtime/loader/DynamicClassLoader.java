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
package ortus.boxlang.runtime.loader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class DynamicClassLoader extends URLClassLoader {

	/**
	 * The name of the class loader as a {@link Key}
	 */
	private Key											nameAsKey;

	/**
	 * The parent class loader
	 */
	private ClassLoader									parent			= null;

	/**
	 * The cache of loaded classes
	 */
	private final ConcurrentHashMap<String, Class<?>>	loadedClasses	= new ConcurrentHashMap<>();

	/**
	 * Logger
	 */
	private static final Logger							logger			= LoggerFactory.getLogger( DynamicClassLoader.class );

	/**
	 * Construct the class loader
	 *
	 * @param name   The unique name of the class loader
	 * @param urls   The URLs to load from
	 * @param parent The parent class loader to delegate to
	 */
	public DynamicClassLoader( Key name, URL[] urls, ClassLoader parent ) {
		// We do not seed the parent class loader because we want to control the class loading
		// And when to null out the parent to create separate class loading environments
		super( name.getName(), urls, null );
		this.parent		= parent;
		this.nameAsKey	= name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolving Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name of the class loader as a {@Link Key}
	 *
	 * @return The name of the class loader
	 */
	public Key getNameAsKey() {
		return this.nameAsKey;
	}

	/**
	 * Find a class in the class loader or delegate to the parent. If not found, then throw an exception
	 *
	 * @param className The name of the class to find
	 */
	@Override
	public Class<?> findClass( String className ) throws ClassNotFoundException {
		return findClass( className, false );
	}

	/**
	 * Find a class in the class loader or delegate to the parent
	 *
	 * @param className The name of the class to find
	 * @param safe      Whether to throw an exception if the class is not found
	 */
	public Class<?> findClass( String className, Boolean safe ) throws ClassNotFoundException {
		// Default it to false
		if ( safe == null ) {
			safe = false;
		}

		logger.atDebug().log( "[{}] Discovering class: [{}]", this.nameAsKey.getName(), className );

		// 1. Check cache first and return if found
		Class<?> cachedClass = loadedClasses.get( className );
		if ( cachedClass != null ) {
			logger.atDebug().log( "[{}].[{}] : Class found in cache", this.nameAsKey.getName(), className );
			return cachedClass;
		}

		// 2. Attempt to load from JARs/classes in the seeded URLs
		try {
			cachedClass = super.findClass( className );
			logger.atDebug().log( "[{}].[{}] : Class found locally", this.nameAsKey.getName(), className );
		} catch ( ClassNotFoundException e ) {
			// 3. If not found in JARs, delegate to parent class loader
			logger.atDebug().log( "[{}].[{}] : Class not found locally, trying the parent...", this.nameAsKey.getName(), className );
			try {
				cachedClass = getDynamicParent().loadClass( className );
				logger.atDebug().log( "[{}].[{}] : Class found in parent", this.nameAsKey.getName(), className );
			} catch ( ClassNotFoundException e1 ) {
				if ( safe ) {
					throw new ClassNotFoundException( String.format( "Class [%s] not found in class loader [%s]", className, this.nameAsKey.getName() ) );
				}
				logger.atDebug().log( "[{}].[{}] : Class not found in parent", this.nameAsKey.getName(), className );
			}
		}

		// 4. Put the loaded class in the cache if found
		if ( cachedClass != null ) {
			loadedClasses.put( className, cachedClass );
		}

		return cachedClass;
	}

	/**
	 * Get the parent class loader
	 *
	 * @return The parent class loader
	 */
	public ClassLoader getDynamicParent() {
		return this.parent;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Class Cache Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Clear The cache of loaded classes
	 */
	public void clearCache() {
		this.loadedClasses.clear();
	}

	/**
	 * Is the cache empty or not
	 */
	public boolean isCacheEmpty() {
		return this.loadedClasses.isEmpty();
	}

	/**
	 * Verifies if the passed class name is in the cache
	 *
	 * @param className The name of the class to check
	 *
	 * @return True if the class is in the cache, false otherwise
	 */
	public boolean isClassInCache( String className ) {
		return this.loadedClasses.containsKey( className );
	}

	/**
	 * Get all the class paths keys in the resolver cache
	 *
	 * @return The keys in the resolver cache
	 */
	public Set<String> getCacheKeys() {
		return this.loadedClasses.keySet();
	}

	/**
	 * Size of the cache
	 */
	public int getCacheSize() {
		return this.loadedClasses.size();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life-Cycle Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Close the class loader
	 *
	 * @throws IOException
	 */
	@Override
	public void close() throws IOException {
		// Clear the cache
		clearCache();
		// Null out the parent
		this.parent = null;
		// Close the class loader
		super.close();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static method that takes in a String path and returns an array
	 * of URLs of all the JARs/clases in the path
	 *
	 * @param targetPath The path to search for JARs
	 *
	 * @return An array of URLs of all the JARs in the path
	 */
	public static URL[] getJarURLs( String targetPath ) throws IOException {
		return getJarURLs( Paths.get( targetPath ) );
	}

	/**
	 * Static method that takes in a path and returns an array
	 * of URLs of all the JARs in the path
	 *
	 * @param targetPath The path to search for JARs
	 *
	 * @return An array of URLs of all the JARs in the path
	 */
	public static URL[] getJarURLs( Path targetPath ) throws IOException {
		// Ensure the path is a directory and that it exists
		if ( Files.exists( targetPath ) && !Files.isDirectory( targetPath ) ) {
			throw new BoxRuntimeException(
			    String.format( "The requested path [%s] to discover jar's and classes must be a valid directory", targetPath )
			);
		}

		// Stream all files recursively, filtering for .jar and .class files
		try ( Stream<Path> fileStream = Files.walk( targetPath ) ) {
			return fileStream
			    .parallel()
			    .filter( path -> path.toString().endsWith( ".jar" ) || path.toString().endsWith( ".class" ) )
			    .map( path -> {
				    try {
					    // Convert Path to URL using toUri() and toURL()
					    return path.toUri().toURL();
				    } catch ( IOException e ) {
					    throw new UncheckedIOException( e );
				    }
			    } )
			    .toArray( URL[]::new );
		}
	}

}
