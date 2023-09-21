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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.resolvers.BoxResolver;
import ortus.boxlang.runtime.loader.resolvers.IClassResolver;
import ortus.boxlang.runtime.loader.resolvers.JavaResolver;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * This is a Class Loader is in charge of locating Box classes in the lookup algorithm
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
	public static final int								TYPE_BX				= 1;

	/**
	 * The internal type of a Java class
	 */
	public static final int								TYPE_JAVA			= 2;

	/**
	 * The default resolver name
	 */
	public static final String							DEFAULT_RESOLVER	= "bx";

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Singleton instance
	 */
	private static ClassLocator							instance;

	/**
	 * The cache of resolved classes
	 */
	private ConcurrentMap<String, ClassLocation>		resolverCache		= new ConcurrentHashMap<>();

	/**
	 * The map of class resolvers we track
	 */
	private ConcurrentHashMap<String, IClassResolver>	resolvers			= new ConcurrentHashMap<>();

	/**
	 * The list of reserved resolvers
	 */
	private static final List<String>					RESERVED_RESOLVERS	= List.of( "bx", "java" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private ClassLocator() {
		super( getSystemClassLoader() );
	}

	/**
	 * Get the singleton instance
	 *
	 * @return ClassLocator
	 */
	public static synchronized ClassLocator getInstance() {
		if ( instance == null ) {
			instance = new ClassLocator();
			// Register core box and java resolvers
			instance.registerResolver( BoxResolver.getInstance() );
			instance.registerResolver( JavaResolver.getInstance() );
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers Registration
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the cache of resolved classes
	 *
	 * @return The cache of resolved classes
	 */
	public ConcurrentMap<String, ClassLocation> getResolverCache() {
		return this.resolverCache;
	}

	/**
	 * Register a class resolver
	 *
	 * @param resolver The class resolver to register
	 *
	 * @throws IllegalStateException If the resolver is already registered
	 */
	public void registerResolver( IClassResolver resolver ) throws IllegalStateException {
		if ( hasResolver( resolver.getPrefix() ) ) {
			throw new IllegalStateException( String.format( "The resolver [%s] is already registered", resolver.getPrefix() ) );
		}
		this.resolvers.put( resolver.getPrefix(), resolver );
	}

	/**
	 * Get the registered resolver prefixes
	 *
	 * @return The registered resolver names
	 */
	public List<String> getResolvedPrefixes() {
		return this.resolvers.keySet().stream().toList();
	}

	/**
	 * Get a registered resolver by prefix
	 *
	 * @param prefix The prefix of the resolver
	 *
	 * @return The registered resolver or null if not found
	 *
	 * @throws KeyNotFoundException If the resolver is not found
	 */
	public IClassResolver getResolver( String prefix ) throws KeyNotFoundException {
		IClassResolver target = this.resolvers.get( prefix.toLowerCase() );
		if ( target == null ) {
			throw new KeyNotFoundException(
			    String.format(
			        "The resolver [%s] was not found in the registered resolvers. Valid resolvers are [%s]",
			        prefix,
			        getResolvedPrefixes()
			    ) );
		}
		return target;
	}

	/**
	 * Verify if a resolver is registered
	 *
	 * @param prefix The prefix of the resolver
	 *
	 * @return True if the resolver is registered, false otherwise
	 */
	public Boolean hasResolver( String prefix ) {
		return this.resolvers.containsKey( prefix.toLowerCase() );
	}

	/**
	 * Remove a resolver by prefix
	 *
	 * @param prefix The prefix of the resolver
	 *
	 * @return True if the resolver was removed, false otherwise
	 *
	 * @throws IllegalStateException If the resolver is reserved and cannot be removed
	 */
	public Boolean removeResolver( String prefix ) throws IllegalStateException {
		prefix = prefix.toLowerCase();

		if ( RESERVED_RESOLVERS.contains( prefix ) ) {
			throw new IllegalStateException( String.format( "The resolver [%s] is reserved and cannot be removed", prefix ) );
		}

		return ( this.resolvers.remove( prefix ) != null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolver Cache Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Verifies if the class resolver is empty or not
	 *
	 * @return True if the resolver cache is empty, false otherwise
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
	 * @return True, if it was removed, else if it didn't exist
	 */
	public Boolean clear( String name ) {
		return ( resolverCache.remove( name ) != null );
	}

	/**
	 * Get the class record from the resolver cache, used internally only
	 *
	 * @param name The fully qualified path of the class to get
	 *
	 * @return An optional containing the class record if found, empty otherwise
	 */
	private Optional<ClassLocation> getClass( String name ) {
		return Optional.ofNullable( resolverCache.get( name ) );
	}

	/**
	 * Verifies if the passed path key is in the resolver cache
	 *
	 * @param name The fully qualified path of the class to verify
	 *
	 * @return True if the key is in the resolver cache, false otherwise
	 */
	public boolean hasClass( String name ) {
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
	 * Load a class without a direct resolver. This will require a system resolution
	 * of the class location and will cache the result for future lookups.
	 *
	 * The lookup order is:
	 *
	 * 1. Bx Resolver
	 * 2. Java Resolver
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 *
	 * @return The invokable representation of the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 */
	public DynamicObject load( IBoxContext context, String name ) throws ClassNotFoundException {
		return load( context, name, List.of() );
	}

	/**
	 * Load a class without a direct resolver or with a resovler prefix. If there is not a resolver prefix-- Ex:
	 * java.lang.String
	 * app.models.User
	 *
	 * This will require a system resolution of the class location and will cache the result for future lookups.
	 *
	 * The lookup order is:
	 *
	 * 1. Bx Resolver
	 * 2. Java Resolver
	 *
	 * If there is a resolver prefix, then it will be used directly. Ex:
	 * java:java.lang.String
	 * bx:app.models.User
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 * @param imports The list of imports to use when resolving the class
	 *
	 * @return The invokable representation of the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 */
	public DynamicObject load( IBoxContext context, String name, List<ImportDefinition> imports ) throws ClassNotFoundException {
		// Check to see if our incoming name has a resolver prefix
		int resolverDelimiterPos = name.indexOf( ":" );
		// If not, use our system lookup order
		if ( resolverDelimiterPos == -1 ) {
			ClassLocation target = resolveFromSystem( context, name, true, imports );
			return ( target == null ) ? null : DynamicObject.of( target.clazz() );
		} else {
			// If there is a resolver prefix, carve it off and use it directly/
			String	resolverPrefix	= name.substring( 0, resolverDelimiterPos );
			String	className		= name.substring( resolverDelimiterPos + 1 );
			return load( context, className, resolverPrefix, true, imports );
		}
	}

	/**
	 * Load a class from a specific resolver
	 * This is a convenience method that will throw an exception if the class is not found
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The invokable representation of the class
	 */
	public DynamicObject load( IBoxContext context, String name, String resolverPrefix ) throws ClassNotFoundException {
		return load( context, name, resolverPrefix, true );
	}

	/**
	 * Load a class from a specific resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 * @param throwException If true, it will throw an exception if the class is not found, else it will return null
	 *
	 * @return The invokable representation of the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 */
	public DynamicObject load(
	    IBoxContext context,
	    String name,
	    String resolverPrefix,
	    Boolean throwException )
	    throws ClassNotFoundException {
		return load( context, name, resolverPrefix, throwException, List.of() );
	}

	/**
	 * Load a class from a specific resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 * @param throwException If true, it will throw an exception if the class is not found, else it will return null
	 * @param imports        The list of imports to use when resolving the class
	 *
	 * @return The invokable representation of the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 */
	public DynamicObject load(
	    IBoxContext context,
	    String name,
	    String resolverPrefix,
	    Boolean throwException,
	    List<ImportDefinition> imports )
	    throws ClassNotFoundException {

		// Unique Cache Key
		String					cacheKey		= resolverPrefix + ":" + name;

		// Try to resolve it
		Optional<ClassLocation>	resolvedClass	= getClass( cacheKey )
		    // Resolve it
		    .or( () -> getResolver( resolverPrefix ).resolve( context, name, imports ) )
		    // If found, cache it
		    .map( target -> {
			    resolverCache.put( cacheKey, target );
			    return target;
		    } );

		if ( resolvedClass.isPresent() ) {
			return DynamicObject.of( resolvedClass.get().clazz() );
		}

		if ( throwException ) {
			throw new ClassNotFoundException(
			    String.format( "The requested class [%s] has not been located in an the [%s] resolver.", name, resolverPrefix )
			);
		}

		return null;
	}

	/**
	 * Same as the load method, but it will not throw an exception if the class is not found,
	 * it will return an empty optional instead.
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 *
	 * @return The invokable representation of the class or an empty optional if not found
	 */
	public Optional<DynamicObject> safeLoad( IBoxContext context, String name ) {
		return safeLoad( context, name, List.of() );
	}

	/**
	 * Same as the load method, but it will not throw an exception if the class is not found,
	 * it will return an empty optional instead.
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 * @param imports The list of imports to use when resolving the class
	 *
	 * @return The invokable representation of the class or an empty optional if not found
	 */
	public Optional<DynamicObject> safeLoad( IBoxContext context, String name, List<ImportDefinition> imports ) {
		ClassLocation location;
		try {
			location = resolveFromSystem( context, name, false, imports );
			// This will never get thrown since we're passing throwException=false
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException( e );
		}
		// If not found, return an empty optional
		return ( location == null )
		    ? Optional.empty()
		    : Optional.of(
		        DynamicObject.of(
		            location.clazz()
		        )
		    );
	}

	/**
	 * Load a class from a specific resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 *
	 * @return The invokable representation of the class
	 */
	public Optional<DynamicObject> safeLoad( IBoxContext context, String name, String resolverPrefix ) {
		return safeLoad( context, name, resolverPrefix, List.of() );
	}

	/**
	 * Load a class from a specific resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 * @param imports        The list of imports to use when resolving the class
	 *
	 * @return The invokable representation of the class
	 */
	public Optional<DynamicObject> safeLoad( IBoxContext context, String name, String resolverPrefix,
	    List<ImportDefinition> imports ) {
		DynamicObject target;
		try {
			target = load( context, name, resolverPrefix, false, imports );
			// This will never get thrown since we're passing throwException=false
		} catch ( ClassNotFoundException e ) {
			throw new RuntimeException( e );
		}
		// If not found, return an empty optional
		return ( target == null )
		    ? Optional.empty()
		    : Optional.of(
		        target
		    );
	}

	/**
	 * This method ONLY returns the class representation, it does not cache it.
	 * It is here to support the Java ClassLoader interface.
	 *
	 * @param context The current context of execution
	 * @param name    The fully qualified path/name of the class to load
	 * @param imports The list of imports to use when resolving the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The class requested to be loaded represented by the incoming name
	 */
	public Class<?> findClass( IBoxContext context, String name, List<ImportDefinition> imports ) throws ClassNotFoundException {
		ClassLocation target = resolveFromSystem( context, name, true, imports );
		return ( target == null )
		    ? super.findClass( name )
		    : target.clazz();
	}

	/**
	 * This method is in charge of resolving the class location in the system and
	 * generating a cacheable {@link ClassLocation} record when you don't know the resolver to use.
	 *
	 * This resolver has no prior knowledge of which resolver to use, so it traverses them in order:
	 * 1. Bx Resolver
	 * 2. Java Resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path of the class to resolve
	 * @param throwException If true, it will throw an exception if the class is not found, else it will return null
	 * @param imports        The list of imports to use when resolving the class
	 *
	 * @throws ClassNotFoundException If the class was not found anywhere in the system
	 *
	 * @return The resolved class location or null if throwException is false and the class is not found
	 */
	private ClassLocation resolveFromSystem( IBoxContext context, String name, Boolean throwException,
	    List<ImportDefinition> imports )
	    throws ClassNotFoundException {

		// Try to get it from cache
		Optional<ClassLocation> resolvedClass = getClass( name )
		    // Is it a BoxClass?
		    .or( () -> getResolver( "bx" ).resolve( context, name, imports ) )
		    // Is it a JavaClass?
		    .or( () -> getResolver( "java" ).resolve( context, name, imports ) )
		    // If found, cache it
		    .map( target -> {
			    resolverCache.put( name, target );
			    return target;
		    } );

		if ( resolvedClass.isPresent() ) {
			return resolvedClass.get();
		}

		if ( throwException ) {
			throw new ClassNotFoundException(
			    String.format( "The requested class [%s] has not been located in any class resolver.", name )
			);
		}

		return null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * ClassLocation Record
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This record represents a class location in the application
	 *
	 * @param name        The name of the class
	 * @param path        The fully qualified path to the class
	 * @param packageName The package the class belongs to
	 * @param type        The type of class it is: 1. Box class (this.BX_TYPE), 2. Java class (this.JAVA_TYPE)
	 * @param clazz       The class object that represents the loaded class
	 * @param module      The module the class belongs to, null if none
	 */
	public record ClassLocation(
	    String name,
	    String path,
	    String packageName,
	    int type,
	    Class<?> clazz,
	    String module ) {

		Boolean isFromModule() {
			return module != null;
		}
	}

}
