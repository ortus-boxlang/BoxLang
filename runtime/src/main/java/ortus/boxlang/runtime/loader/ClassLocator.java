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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
public class ClassLocator extends ClassLoader {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The internal type of a BoxLang class
	 */
	public static final int							TYPE_BX			= 1;
	/**
	 * The internal type of a Java class
	 */
	public static final int							TYPE_JAVA		= 2;
	/**
	 * The class extension to use for loading classes
	 */
	public static final String						CLASS_EXTENSION	= ".class";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private String									classDirectory;

	/**
	 * Singleton instance
	 */
	private static ClassLocator						instance;

	/**
	 * The cache of resolved classes
	 */
	private ConcurrentMap<String, ClassLocation>	resolverCache	= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Static constructor
	 */
	private ClassLocator( String classDirectory ) {
		super( getSystemClassLoader() );
		this.classDirectory = classDirectory;
	}

	/**
	 * Get the singleton instance
	 *
	 * @param classDirectory The class directory for generated bx classes
	 *
	 * @return ClassLocator
	 */
	public static synchronized ClassLocator getInstance( String classDirectory ) {
		if ( instance == null ) {
			instance = new ClassLocator( classDirectory );
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return the classDirectory
	 */
	public String getClassDirectory() {
		return classDirectory;
	}

	/**
	 * Get the cache of resolved classes
	 *
	 * @return The cache of resolved classes
	 */
	public ConcurrentMap<String, ClassLocation> getResolverCache() {
		return resolverCache;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolver Utilities
	 * --------------------------------------------------------------------------
	 */

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
	 * @param name The fully qualified path of the class to remove
	 *
	 * @return The class locator instance
	 */
	public ClassLocator removeClass( String name ) {
		resolverCache.remove( name );
		return instance;
	}

	/**
	 * Get the class record from the resolver cache
	 *
	 * @param name The fully qualified path of the class to get
	 *
	 * @return An optional containing the class record if found, empty otherwise
	 */
	public Optional<ClassLocation> getClass( String name ) {
		return Optional.of( resolverCache.get( name ) );
	}

	/**
	 * Verifies if the passed path key is in the resolver cache
	 *
	 * @param name The fully qualified path of the class to verify
	 *
	 * @return True if the key is in the resolver cache, false otherwise
	 */
	boolean hasClass( String name ) {
		return resolverCache.containsKey( name );
	}

	/**
	 * Get all the class paths keys in the resolver cache
	 *
	 * @return The keys in the resolver cache
	 */
	public Set<String> classSet() {
		return resolverCache.keySet();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Class Loader Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Load a class using this class loader
	 *
	 * @param name The fully qualified path of the class to resolve
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The class requested to be loaded
	 */
	@Override
	public Class loadClass( String name ) throws ClassNotFoundException {
		return resolve( name ).clazz();
	}

	/**
	 * This method is in charge of resolving the class location in the system and generating a cacheable {@link ClassLocation} record
	 *
	 * @param name The fully qualified path of the class to resolve
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The resolved class location
	 */
	public ClassLocation resolve( String name ) throws ClassNotFoundException {
		// Verify if the class record is already in the cache
		Optional<ClassLocation> classLocation = getClass( name );

		// If the class is already in the cache, return it
		if ( classLocation.isPresent() ) {
			return classLocation.get();
		}

		// Verify if the class is in the declared class mappings path

		// Verify if the class in in the app path

		// Verify if the class is a Java class

		// Ask the system to load it
		Class clazz = super.loadClass( name );

		throw new ClassNotFoundException( String.format( "The requested class [%s] was not found anywhere", name ) );
	}

	/**
	 * Get the system class loader
	 *
	 * @return The system class loader
	 */
	public static ClassLoader getSystemClassLoader() {
		return ClassLoader.getSystemClassLoader();
	}

	/**
	 * Load a class from the declared class mappings path
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The loaded class
	 *
	 * @throws IOException
	 */
	private byte[] loadClassBytes( String name ) throws IOException {
		Path fullPath = Paths.get( this.classDirectory, name.replace( '.', '/' ) + CLASS_EXTENSION );
		return Files.readAllBytes( fullPath );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Record Definitions
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This record represents a class location in the application
	 *
	 * @param name         The fully qualified path to the class, e.g. mapping.models.User, path.models.User, java.util.Date
	 * @param resolvedPath The resolved path to the class
	 * @param type         The type of class it is: 1. Box class (this.BX_TYPE), 2. Java class (this.JAVA_TYPE)
	 * @param clazz        The class object that represents the loaded class
	 * @param module       The module the class belongs to, null if none
	 */
	public record ClassLocation(
	        String name,
	        String resolvedPath,
	        int type,
	        Class<?> clazz,
	        String module ) {

		Boolean isFromModule() {
			return module != null;
		}
	}

}
