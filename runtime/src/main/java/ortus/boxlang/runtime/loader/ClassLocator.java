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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.ClassInvoker;

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
 *
 * If the class is found, it will be cached for future lookups.
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

	/**
	 * The class directory for generated bx classes
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
	 * Get the singleton instance
	 *
	 * @return ClassLocator
	 */
	public static synchronized ClassLocator getInstance() {
		return getInstance( null );
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
		return Optional.ofNullable( resolverCache.get( name ) );
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
	 * This is the main entry point into the system ClassLocator. It allows you to
	 * search for the incoming class name in the following order:
	 *
	 * 1. Registered modules
	 * 2. System loader
	 *
	 * The return value is an invokable representation of the class using invokeDynamic and our {@link ClassInvoker} class.
	 * Every Java/BoxLang class is represented by a {@link ClassInvoker} instance so you can invoke dynamically.
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 *
	 * @return The invokable representation of the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 */
	public ClassInvoker load( IBoxContext context, String name ) throws ClassNotFoundException {
		return ClassInvoker.of( findClass( name ) );
	}

	/**
	 * Find a class from BoxLang. It will try to locate it in the following order:
	 *
	 * 1. Registered modules
	 * 2. System loader
	 *
	 * This method ONLY returns the class representation, it does not cache it.
	 *
	 * @param name The fully qualified path/name of the class to load
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The class requested to be loaded represented by the incoming name
	 */
	@Override
	public Class<?> findClass( String name ) throws ClassNotFoundException {
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
		// Try to get it from cache
		return getClass( name )
		        // BxClass: if not found, then try to find it in the registered modules
		        .or( () -> findFromModules( name ) )
		        // BxClass : if not found, then try to find it in the local byte code
		        .or( () -> findFromLocal( name ) )
		        // else from the system class loader: it must be a native or class loaded Java class
		        .or( () -> findFromSystem( name ) )
		        // If found, cache it
		        .map( ( target ) -> {
			        resolverCache.put( name, target );
			        return target;
		        } )
		        // If not found, throw an exception
		        .orElseThrow(
		                () -> new ClassNotFoundException(
		                        String.format( "The requested class [%s] has not been located", name )
		                )
		        );
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
	 * Load a class from the registered runtime module class loaders
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( String name ) {
		return Optional.ofNullable( null );
	}

	/**
	 * Load a class from the configured directory byte code
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromLocal( String name ) {
		return Optional.ofNullable( null );
	}

	/**
	 * Load a class from the system class loader
	 *
	 * @param name The fully qualified path of the class to load
	 *
	 * @return The {@link ClassLocation} record wrapped in an optional if found, empty otherwise
	 */
	public Optional<ClassLocation> findFromSystem( String name ) {
		try {
			return Optional.of(
			        new ClassLocation(
			                name, // fully qualified name
			                name, // resolved path, same in java
			                TYPE_JAVA, // type
			                super.loadClass( name ), // talk to the system class loader
			                null // no module
			        )
			);
		} catch ( ClassNotFoundException e ) {
			return Optional.empty();
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * ClassLocation Record
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
