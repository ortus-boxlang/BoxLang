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

import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.resolvers.BoxResolver;
import ortus.boxlang.runtime.loader.resolvers.IClassResolver;
import ortus.boxlang.runtime.loader.resolvers.JavaResolver;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ClassNotFoundBoxLangException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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

	// Resolver Prefixes
	public static final String							BX_PREFIX			= "bx";
	public static final String							JAVA_PREFIX			= "java";

	/**
	 * The default resolver name
	 */
	public static final String							DEFAULT_RESOLVER	= BX_PREFIX;

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
	private static final List<String>					RESERVED_RESOLVERS	= List.of(
	    BX_PREFIX, JAVA_PREFIX
	);

	/**
	 * The colon delimiter for prefixing
	 */
	private static final String							COLON				= ":";

	/**
	 * The Runtime
	 */
	private BoxRuntime									runtime;

	/**
	 * This locator can track a-la-carte class loaders for dynamic class loading
	 * Requested by createObject() calls.
	 */
	private Map<String, DynamicClassLoader>				classLoaders		= new ConcurrentHashMap<>();

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
	public static ClassLocator getInstance() {
		if ( instance == null ) {
			throw new BoxRuntimeException( "The ClassLocator instance has not been initialized." );
		}
		return instance;
	}

	/**
	 * Get the singleton instance
	 *
	 * @param runtime The current runtime
	 *
	 * @return ClassLocator
	 */
	public static ClassLocator getInstance( BoxRuntime runtime ) {
		if ( instance == null ) {
			synchronized ( ClassLocator.class ) {
				if ( instance == null ) {
					var tmpInstance = new ClassLocator();
					tmpInstance.runtime = runtime;
					// Register core box and java resolvers
					tmpInstance.registerResolver( new BoxResolver( tmpInstance ) );
					tmpInstance.registerResolver( new JavaResolver( tmpInstance ) );
					// wait to set until the end to avoid race conditions
					instance = tmpInstance;
				}
			}
		}
		return instance;

	}

	/**
	 * Get the runtime associated with this locator
	 */
	public BoxRuntime getRuntime() {
		return this.runtime;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers Registration
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Shortcut to get the Java Resolver
	 */
	public JavaResolver getJavaResolver() {
		return ( JavaResolver ) getResolver( JAVA_PREFIX );
	}

	/**
	 * Shortcut to get the Box Resolver
	 */
	public BoxResolver getBoxResolver() {
		return ( BoxResolver ) getResolver( BX_PREFIX );
	}

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
	 */
	public void registerResolver( IClassResolver resolver ) {
		if ( hasResolver( resolver.getPrefix() ) ) {
			throw new BoxRuntimeException( String.format( "The resolver [%s] is already registered", resolver.getPrefix() ) );
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
	 */
	public IClassResolver getResolver( String prefix ) {
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
	 */
	public Boolean removeResolver( String prefix ) {
		prefix = prefix.toLowerCase();

		if ( RESERVED_RESOLVERS.contains( prefix ) ) {
			throw new BoxRuntimeException( String.format( "The resolver [%s] is reserved and cannot be removed", prefix ) );
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
		return this.resolverCache.isEmpty();
	}

	/**
	 * Verify the size of the resolver cache
	 *
	 * @return The size of the resolver cache
	 */
	public int size() {
		return this.resolverCache.size();
	}

	/**
	 * Clear the resolver cache
	 *
	 * @return The class locator instance
	 */
	public ClassLocator clear() {
		this.resolverCache.clear();
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
		return ( this.resolverCache.remove( name ) != null );
	}

	/**
	 * Get the class record from the resolver cache, used internally only
	 *
	 * @param name The fully qualified path of the class to get
	 *
	 * @return An optional containing the class record if found, empty otherwise
	 */
	private Optional<ClassLocation> getClass( String name ) {
		return Optional.ofNullable( this.resolverCache.get( name ) );
	}

	/**
	 * Verifies if the passed path key is in the resolver cache
	 *
	 * @param name The fully qualified path of the class to verify
	 *
	 * @return True if the key is in the resolver cache, false otherwise
	 */
	public boolean hasClass( String name ) {
		return this.resolverCache.containsKey( name );
	}

	/**
	 * Get all the class paths keys in the resolver cache
	 *
	 * @return The keys in the resolver cache
	 */
	public Set<String> classSet() {
		return this.resolverCache.keySet();
	}

	/**
	 * Get a list of all the cached resolver classes
	 *
	 * @return The list of cached classes as a list of key names
	 */
	public List<String> getClassList() {
		return this.resolverCache.keySet().stream().toList();
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
	 */
	public DynamicObject load( IBoxContext context, String name ) {
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
	 */
	public DynamicObject load( IBoxContext context, String name, List<ImportDefinition> imports ) {
		return load( context, name, imports, Struct.EMPTY );
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
	 * @param context    The current context of execution
	 * @param name       The fully qualified path/name of the class to load
	 * @param imports    The list of imports to use when resolving the class
	 * @param properties The properties to use when resolving the class
	 *
	 * @return The invokable representation of the class
	 *
	 */
	public DynamicObject load( IBoxContext context, String name, List<ImportDefinition> imports, IStruct properties ) {
		// If the imports are null, set them to an empty list
		if ( imports == null ) {
			imports = List.of();
		}
		// Check to see if our incoming name has a resolver prefix
		int resolverDelimiterPos = name.indexOf( ":" );
		// If not, use our system lookup order
		if ( resolverDelimiterPos == -1 ) {
			ClassLocation target = resolveFromSystem( context, name, true, imports, properties );
			return ( target == null ) ? null : DynamicObject.of( target.clazz( context ), context );
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
	 *
	 * @return The invokable representation of the class
	 */
	public DynamicObject load( IBoxContext context, String name, String resolverPrefix ) {
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
	 */
	public DynamicObject load(
	    IBoxContext context,
	    String name,
	    String resolverPrefix,
	    Boolean throwException ) {
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
	 */
	public DynamicObject load(
	    IBoxContext context,
	    String name,
	    String resolverPrefix,
	    Boolean throwException,
	    List<ImportDefinition> imports ) {
		return load( context, name, resolverPrefix, throwException, imports, Struct.EMPTY );
	}

	/**
	 * Load a class from a specific resolver
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param resolverPrefix The prefix of the resolver to use
	 * @param throwException If true, it will throw an exception if the class is not found, else it will return null
	 * @param imports        The list of imports to use when resolving the class
	 * @param properties     The properties to use when resolving the class. Boolean useCaching, Boolean externalOnly
	 *
	 * @return The invokable representation of the class
	 *
	 */
	public DynamicObject load(
	    IBoxContext context,
	    String name,
	    String resolverPrefix,
	    Boolean throwException,
	    List<ImportDefinition> imports,
	    IStruct properties ) {

		// If the imports are null, set them to an empty list
		if ( imports == null ) {
			imports = List.of();
		}

		boolean							useCaching	= ( Boolean ) properties.getOrDefault( Key.useCaching, true );

		// Must be final for the lambda to use it
		final List<ImportDefinition>	thisImports	= imports;
		// Unique Cache Key
		String							cacheKey	= useCaching ? new StringBuilder( resolverPrefix )
		    .append( COLON )
		    .append( context.getApplicationName() )
		    .append( COLON )
		    .append( Objects.hash( imports ) )
		    .append( COLON )
		    .append( Objects.hash( context.getConfig().getAsStruct( Key.mappings ) ) )
		    .append( COLON )
		    .append( getTemplatePathPrefix( context ) )
		    .append( COLON )
		    .append( name )
		    .toString() : "";

		// Are we in debug mode, if so disable caching
		if ( !runtime.getConfiguration().classResolverCache ) {
			useCaching = false;
		}
		final boolean			finalUseCaching	= useCaching;

		// Try to resolve it
		Optional<ClassLocation>	resolvedClass	= ( useCaching ? getClass( cacheKey ) : Optional.<ClassLocation>empty() )
		    // Resolve it
		    .or( () -> getResolver( resolverPrefix ).resolve( context, name, thisImports, properties ) )
		    // If found, cache it
		    .map( target -> {
			    if ( finalUseCaching && target.cacheable() ) {
				    this.resolverCache.put( cacheKey, target );
			    }
			    return target;
		    } );

		if ( resolvedClass.isPresent() ) {
			return DynamicObject.of( resolvedClass.get().clazz( context ), context );
		}

		if ( throwException ) {
			throw new ClassNotFoundBoxLangException(
			    String.format( "The requested class [%s] has not been located in the [%s] resolver.", name, resolverPrefix )
			);
		}

		return null;
	}

	/**
	 * Get the template path prefix for the requested resolution. This is to account for the same name of clases
	 * that could potentially be in different packages.
	 *
	 * @param context The current context of execution
	 *
	 * @return The template path prefix
	 */
	private String getTemplatePathPrefix( IBoxContext context ) {
		String				pathPrefix			= "";
		ResolvedFilePath	resolvedFilePath	= context.findClosestTemplate();
		if ( resolvedFilePath != null ) {
			Path parent = resolvedFilePath.absolutePath().getParent();
			if ( parent != null ) {
				pathPrefix = parent.toString();
			}
		}

		return pathPrefix;
	}

	/**
	 * Load a class from a specific array of class paths
	 *
	 * @param context        The current context of execution
	 * @param name           The fully qualified path/name of the class to load
	 * @param classPaths     The array of class paths to use when resolving the class
	 * @param throwException If true, it will throw an exception if the class is not found, else it will return null
	 * @param imports        The list of imports to use when resolving the class
	 *
	 * @return The invokable representation of the class
	 */
	public DynamicObject loadFromClassPaths(
	    IBoxContext context,
	    String name,
	    Array classPaths,
	    Boolean throwException,
	    List<ImportDefinition> imports ) {

		// Get the class paths and expand them
		URL[]				loadPathsUrls	= DynamicClassLoader.inflateClassPaths(
		    classPaths
		        .stream()
		        .map( item -> FileSystemUtil.expandPath( context, ( String ) item ).absolutePath().toString() )
		        .collect( BLCollector.toArray() )
		);
		String				loaderCacheKey	= EncryptionUtil.hash( Arrays.toString( loadPathsUrls ) );
		DynamicClassLoader	classLoader		= this.classLoaders.computeIfAbsent(
		    loaderCacheKey,
		    key -> {
			    // logger.debug( "Application ClassLoader [{}] registered with these paths: [{}]", this.name, Arrays.toString( loadPathsUrls ) );
			    return new DynamicClassLoader(
			        Key.of( loaderCacheKey ),
			        loadPathsUrls,
			        BoxRuntime.getInstance().getRuntimeLoader(),
			        false
			    );
		    } );

		try {
			return DynamicObject.of( classLoader.loadClass( name ), context );
		} catch ( ClassNotFoundException e ) {
			if ( throwException ) {
				throw new ClassNotFoundBoxLangException(
				    String.format( "The requested class [%s] has not been located in the class paths.", name )
				);
			}
			return null;
		}
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
		return safeLoad( context, name, imports, Struct.EMPTY );
	}

	/**
	 * Same as the load method, but it will not throw an exception if the class is not found,
	 * it will return an empty optional instead.
	 *
	 * @param context    The current context of execution
	 * @param name       The fully qualified path/name of the class to load
	 * @param imports    The list of imports to use when resolving the class
	 * @param properties The properties to use when resolving the class
	 *
	 * @return The invokable representation of the class or an empty optional if not found
	 */
	public Optional<DynamicObject> safeLoad( IBoxContext context, String name, List<ImportDefinition> imports, IStruct properties ) {
		ClassLocation location;
		location = resolveFromSystem( context, name, false, imports, properties );
		// If not found, return an empty optional
		return ( location == null )
		    ? Optional.empty()
		    : Optional.of(
		        DynamicObject.of(
		            location.clazz( context ),
		            context
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
		target = load( context, name, resolverPrefix, false, imports );
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
	 * @return The class requested to be loaded represented by the incoming name
	 */
	public Class<?> findClass( IBoxContext context, String name, List<ImportDefinition> imports ) {
		ClassLocation target = resolveFromSystem( context, name, true, imports, Struct.of( Key.useCaching, false ) );
		try {
			return ( target == null )
			    ? super.findClass( name )
			    : target.clazz( context );
		} catch ( ClassNotFoundException e ) {
			throw new ClassNotFoundBoxLangException( String.format( "The requested class [%s] was not found.", name ) );
		}
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
	 * @param useCaching     If true, it will cache the resolved class if allowed, else just does discovery and passthrough
	 *
	 * @return The resolved class location or null if throwException is false and the class is not found
	 */
	private ClassLocation resolveFromSystem(
	    IBoxContext context,
	    String name,
	    Boolean throwException,
	    List<ImportDefinition> imports,
	    IStruct properties ) {

		Boolean	useCaching	= ( Boolean ) properties.getOrDefault( Key.useCaching, true );

		// This builds a unique cache key for the class requested with as much information as possible
		// To guarantee uniqueness
		String	cacheKey	= useCaching ? new StringBuilder( 100 ).append( context.getApplicationName() )
		    .append( Objects.hash( imports ) )
		    .append( Objects.hash( context.getConfig().getAsStruct( Key.mappings ) ) )
		    .append( getTemplatePathPrefix( context ) )
		    .append( name )
		    .toString() : "";

		// No Setting, No Full Caching
		if ( !runtime.getConfiguration().classResolverCache ) {
			useCaching = false;
		}
		final boolean			finalUseCaching	= useCaching;

		// Try to get it from cache
		Optional<ClassLocation>	resolvedClass	= ( useCaching ? getClass( cacheKey ) : Optional.<ClassLocation>empty() )
		    // Is it a BoxClass?
		    .or( () -> getResolver( BX_PREFIX ).resolve( context, name, imports, properties ) )
		    // Is it a JavaClass?
		    .or( () -> getResolver( JAVA_PREFIX ).resolve( context, name, imports, properties ) )
		    // If found, cache it
		    .map( target -> {
			    if ( finalUseCaching && target.cacheable() ) {
				    this.resolverCache.put( cacheKey, target );
			    }
			    return target;
		    } );

		// If we got it, return it
		if ( resolvedClass.isPresent() ) {
			return resolvedClass.get();
		}

		if ( throwException ) {
			throw new ClassNotFoundBoxLangException(
			    String.format( "The requested class [%s] has not been located in any class resolver.", name )
			);
		}

		return null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Class Loader Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get all the class loaders registered
	 *
	 * @return The class loader map
	 */
	public Map<String, DynamicClassLoader> getClassLoaders() {
		return this.classLoaders;
	}

	/**
	 * Verify if the class loader exists by cache key
	 *
	 * @param loaderKey The key of the class loader
	 */
	public boolean hasClassLoader( String loaderKey ) {
		return this.classLoaders.containsKey( loaderKey );
	}

	/**
	 * Get a class loader by cache key
	 *
	 * @param loaderKey The key of the class loader
	 *
	 * @return The class loader
	 */
	public DynamicClassLoader getClassLoader( String loaderKey ) {
		return this.classLoaders.get( loaderKey );
	}

	/**
	 * Count how many class loaders we have loaded
	 */
	public long getClassLoaderCount() {
		return this.classLoaders.size();
	}

	/**
	 * Clear all the class loaders
	 */
	public void clearClassLoaders() {
		this.classLoaders.clear();
	}

}
