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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

/**
 * This class is in charge of locating Box classes in the lookup algorithm
 * and also Java classes in the classloader paths. The resolution is done once
 * per class path and stored for quicker lookups.
 *
 * Example: apppath.models.User
 * - Verify if the class is in the cache
 * - If not, verify if the class is in the declared class mappings path
 * - If not, verify if the class is in the application path
 * - If not, verify if the class is a Java class
 */
public class ClassLocator {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The internal type of a BoxLang class
	 */
	public static final int TYPE_BX = 1;
	/**
	 * The internal type of a Java class
	 */
	public static final int TYPE_JAVA = 2;

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static final ClassLocator instance = new ClassLocator();

	/**
	 * The cache of resolved classes
	 */
	private ConcurrentHashMap<String, ClassLocation> resolverCache = new ConcurrentHashMap<String, ClassLocation>();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static constructor
	 */
	private ClassLocator() {
		// Initialization code, if needed
	}

	/**
	 * Get the singleton instance
	 *
	 * @return ClassLocator
	 */
	public static ClassLocator getInstance() {
		return instance;
	}

	/**
	 * Get the cache of resolved classes
	 *
	 * @return The cache of resolved classes
	 */
	public ConcurrentHashMap<String, ClassLocation> getResolverCache() {
		return resolverCache;
	}

	/**
	 * Set the cache of resolved classes
	 *
	 * @param resolverCache The cache of resolved classes
	 */
	public ClassLocator setResolverCache( ConcurrentHashMap<String, ClassLocation> resolverCache ) {
		this.resolverCache = resolverCache;
		return this;
	}

	/**
	 * Verifies if the class resolver is empty or not
	 *
	 * @return
	 */
	public Boolean isEmpty() {
		return resolverCache.isEmpty();
	}

	/**
	 * Verify the size of the resolver cache
	 *
	 * @return The size of the resolver cache
	 */
	public int size() {
		return resolverCache.size();
	}

	/**
	 * Clear the resolver cache
	 *
	 * @return The class locator instance
	 */
	public ClassLocator clear() {
		resolverCache.clear();
		return instance;
	}

	/**
	 * Clear a specific key from the resolver cache
	 *
	 * @param key The key to clear
	 *
	 * @return The class locator instance
	 */
	public ClassLocator removeClass( String key ) {
		resolverCache.remove( key );
		return instance;
	}

	/**
	 * Get the class record from the resolver cache
	 *
	 * @param key The key to get
	 *
	 * @return An optional containing the class record if found, empty otherwise
	 */
	public Optional<ClassLocation> getClass( String key ) {
		return Optional.of( resolverCache.get( key ) );
	}

	/**
	 * Resolve a class path to a class location and type
	 *
	 * @param path The class path to resolve
	 *
	 * @return The class location record
	 */
	public ClassLocation resolve( String path ) throws ClassNotFoundException {
		// Verify if the class record is already in the cache
		if ( hasClass( path ) ) {
			return getClass( path ).get();
		}
		// Verify if the class is in the declared class mappings path

		// Verify if the lcass in in the app path

		// Verify if the class is a Java class

		throw new ClassNotFoundException( String.format( "The requested class [%s] was not found anywhere", path ) );
	}

	/**
	 * Verifies if the passed path key is in the resolver cache
	 *
	 * @param key The path key to verify
	 *
	 * @return True if the key is in the resolver cache, false otherwise
	 */
	boolean hasClass( String key ) {
		return resolverCache.containsKey( key );
	}

	/**
	 * Get all the class paths keys in the resolver cache
	 *
	 * @return The keys in the resolver cache
	 */
	public KeySetView<String, ClassLocation> classSet() {
		return resolverCache.keySet();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Record Definitions
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This record represents a class location in the application
	 *
	 * @param path         The instantiation path to the class, e.g. mapping.models.User, path.models.User, java.util.Date
	 * @param resolvedPath The resolved path to the class
	 * @param type         The type of class it is: 1. Box class (this.BX_TYPE), 2. Java class (this.JAVA_TYPE) (TODO: Change to enum or static fields)
	 * @param loader       The classloader that can load the class
	 */
	public record ClassLocation( String path, String resolvedPath, int type, ClassLoader loader ) {
	}

}
