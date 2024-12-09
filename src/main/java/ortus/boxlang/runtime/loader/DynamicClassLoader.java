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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.bifs.global.type.NullValue;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
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
	 * Track if the class loader is closed for better debugging. We can remove this later if we don't need it, but it's useful for now
	 */
	private boolean										closed			= false;

	/**
	 * The stack trace of the thread that closed this class loader
	 */
	private String										closedStack		= "";

	/**
	 * The cache of loaded classes
	 */
	private final ConcurrentHashMap<String, Class<?>>	loadedClasses	= new ConcurrentHashMap<>();

	/**
	 * The cache of unfound classes, for performance reasons
	 */
	private final ConcurrentHashMap<String, Class<?>>	unfoundClasses	= new ConcurrentHashMap<>();

	/**
	 * Logger. Lazy init to avoid deadlocks on runtime startup
	 */
	private static Logger								logger			= null;

	/**
	 * Runtime special prefixes Set that MUST come from the parent class loader
	 * THIS IS SPECIAL CASE FOR LOGGING FRAMEWORKS WHERE THIRD PARTY JARS MAY BE LOADED AND DELEGATED TO THE PARENT
	 */
	private static final Set<String>					PARENT_CLASSES	= Set.of(
	    "ch.qos.logback",
	    "org.slf4j"
	);

	/**
	 * Construct the class loader
	 *
	 * @param name            The unique name of the class loader
	 * @param url             A single URL to load from
	 * @param parent          The parent class loader to delegate to
	 * @param loadParentFirst Whether to load the parent class loader or not, default is to create a boundary.
	 */
	public DynamicClassLoader( Key name, URL url, ClassLoader parent, Boolean loadParentFirst ) {
		this( name, new URL[] { url }, parent, loadParentFirst );
	}

	/**
	 * Construct the class loader
	 * <p>
	 * Please note the {@code loadParentFirst} setting. By default we create a virtual boundary
	 * between classloaders and do not load the parent class loader into the root ClassLoader, only
	 * into this class loader for hierarchical purposes. If this setting is set to true, then the
	 * parent class loader will be loaded into the root class loader first and lookups go to the parent first.
	 * <p>
	 * This can be desired on certain ocassions, but for modular separation, this is disabled by default.
	 *
	 * @param name            The unique name of the class loader
	 * @param urls            The URLs to load from
	 * @param parent          The parent class loader to delegate to
	 * @param loadParentFirst Whether to load the parent class loader or not, default is to create a boundary.
	 */
	public DynamicClassLoader( Key name, URL[] urls, ClassLoader parent, Boolean loadParentFirst ) {
		// We do not seed the parent class loader because we want to control the class loading
		// And when to null out the parent to create separate class loading environments
		super( name.getName(), urls, loadParentFirst ? parent : null );
		Objects.requireNonNull( parent, "Parent class loader cannot be null" );
		this.parent		= parent;
		this.nameAsKey	= name;
	}

	/**
	 * Construct the class loader
	 *
	 * @param name   The unique name of the class loader
	 * @param parent The parent class loader to delegate to
	 */
	public DynamicClassLoader( Key name, ClassLoader parent ) {
		this( name, new URL[ 0 ], parent, false );
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
		Logger logger = getLogger();
		if ( closed ) {
			throw new BoxRuntimeException(
			    "Class loader [" + nameAsKey.getName() + "] is closed, but you are trying to use it still! Closed by this thread: \n\n" + closedStack );
		}

		// Default it to false
		if ( safe == null ) {
			safe = false;
		}

		logger.trace( "[{}] Discovering class: [{}]", this.nameAsKey.getName(), className );

		// 1. Check the loaded cache first and return if found
		Class<?> cachedClass = this.loadedClasses.get( className );
		if ( cachedClass != null ) {
			logger.trace( "[{}].[{}] : Class found in cache", this.nameAsKey.getName(), className );
			return cachedClass;
		}

		// 2. Check the unfound cache, and if already there, return just null or throw an exception depending on the safe flag
		if ( this.unfoundClasses.containsKey( className ) ) {
			logger.trace( "[{}].[{}] : Class not found in cache, but already in unfound cache", this.nameAsKey.getName(), className );
			if ( safe ) {
				return null;
			}
			throw new ClassNotFoundException( String.format( "Class [%s] not found in class loader [%s]", className, this.nameAsKey.getName() ) );
		}

		// 2.5. Special case for Logback/SL4j so we are guaranteed to use the same interfaces as the BoxLang Runtime.
		if ( this.parent != null && PARENT_CLASSES.stream().anyMatch( className::startsWith ) ) {
			// logger.trace( "[{}].[{}] : Class is a special parent class, delegating to parent", this.nameAsKey.getName(), className );
			return getDynamicParent().loadClass( className );
		}

		// 3. Attempt to load from JARs/classes in the seeded URLs
		try {
			cachedClass = super.findClass( className );
			logger.trace( "[{}].[{}] : Class found locally", this.nameAsKey.getName(), className );
		} catch ( ClassNotFoundException e ) {

			// 3. If not found in JARs, delegate to parent class loader
			try {
				logger.trace( "[{}].[{}] : Class not found locally, trying the parent...", this.nameAsKey.getName(), className );
				cachedClass = getDynamicParent().loadClass( className );
				logger.trace( "[{}].[{}] : Class found in parent", this.nameAsKey.getName(), className );
			} catch ( ClassNotFoundException parentException ) {
				// Add to the unfound cache
				this.unfoundClasses.put( className, NullValue.class );
				// If not safe, throw the exception
				if ( !safe ) {
					throw new ClassNotFoundException( String.format( "Class [%s] not found in class loader [%s]", className, this.nameAsKey.getName() ) );
				}
				logger.trace( "[{}].[{}] : Class not found in parent", this.nameAsKey.getName(), className );
			}

		}

		// 4. Put the loaded class in the cache if found
		if ( cachedClass != null ) {
			this.loadedClasses.put( className, cachedClass );
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
	 * Add a URL to the class loader
	 *
	 * @param url The URL to add
	 *
	 * @see URLClassLoader#addURL(URL)
	 */
	@Override
	public void addURL( URL url ) {
		super.addURL( url );
	}

	/**
	 * Add an array of URLs to the class loader
	 *
	 * @param urls The URLs to add
	 *
	 * @see URLClassLoader#addURL(URL)
	 */
	public void addURLs( URL[] urls ) {
		for ( URL url : urls ) {
			addURL( url );
		}
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
	 *
	 * @return The size of the cache
	 */
	public int getCacheSize() {
		return this.loadedClasses.size();
	}

	/**
	 * Get the cache of unfound classes
	 *
	 * @return The cache of unfound classes
	 */
	public Map<String, Class<?>> getUnfoundClasses() {
		return this.unfoundClasses;
	}

	/**
	 * Get the set of keys in the unfound class cache
	 *
	 * @return The keys in the unfound class cache
	 */
	public Set<String> getUnfoundClassesKeys() {
		return this.unfoundClasses.keySet();
	}

	/**
	 * How many unfound classes we have found
	 *
	 * @return The size of the unfound classes
	 */
	public int getUnfoundClassesSize() {
		return this.unfoundClasses.size();
	}

	/**
	 * Clear the unfound class cache
	 */
	public void clearUnfoundClasses() {
		this.unfoundClasses.clear();
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
		closed = true;
		StringWriter	sw	= new StringWriter();
		PrintWriter		pw	= new PrintWriter( sw );
		new Exception().printStackTrace( pw );
		closedStack = sw.toString();

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
			return Stream.concat(
			    Stream.of( targetPath ), // Include the directory itself
			    fileStream
			        .parallel()
			        .filter( path -> path.toString().endsWith( ".jar" ) || path.toString().endsWith( ".class" ) )
			)
			    .map( path -> {
				    try {
					    // Convert Path to URL using toUri() and toURL()
					    return path.toUri().toURL();
				    } catch ( IOException e ) {
					    throw new UncheckedIOException( e );
				    }
			    } )
			    .toArray( URL[]::new );
		} catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	/**
	 * Goes through an array of path locations and inflates them into an array of URLs
	 * of all the JARs and classes in the paths
	 *
	 * @param paths An array of paths' to inflate
	 *
	 * @return The URLs of jars and classes
	 */
	public static URL[] inflateClassPaths( Array paths ) {
		// Conver it to a list of jar/class paths
		return paths.stream()
		    .map( path -> {
			    try {
				    Path targetPath = Paths.get( ( String ) path );
				    // If this is a directory, then get all the JARs and classes in the directory as well as the dir itself
				    // else if it's a jar/class file then just return the URL
				    if ( Files.isDirectory( targetPath ) ) {
					    return getJarURLs( targetPath );
				    } else {
					    return new URL[] { targetPath.toUri().toURL() };
				    }
			    } catch ( IOException e ) {
				    throw new BoxIOException( path + " is not a valid path", e );
			    }
		    } )
		    .flatMap( Arrays::stream )
		    .distinct()
		    // .peek( url -> getLogger().debug( "Inflated URL: [{}]", url ) )
		    .toArray( URL[]::new );
	}

	private static Logger getLogger() {
		if ( logger == null ) {
			synchronized ( DynamicClassLoader.class ) {
				if ( logger == null ) {
					logger = LoggerFactory.getLogger( DynamicClassLoader.class );
				}
			}
		}
		return logger;

	}

}
