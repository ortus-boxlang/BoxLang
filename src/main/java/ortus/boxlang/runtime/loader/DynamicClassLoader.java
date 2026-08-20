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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.lang.ref.Cleaner;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.global.type.NullValue;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.interop.MethodRecord;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ClassLoaderUtil;

public class DynamicClassLoader extends URLClassLoader implements IModuleClassLoader {

	/**
	 * The name of the class loader as a {@link Key}
	 */
	private Key												nameAsKey;

	/**
	 * The parent class loader
	 */
	private ClassLoader										parent				= null;

	/**
	 * Track if the class loader is closed for better debugging. We can remove this later if we don't need it, but it's useful for now
	 */
	private boolean											closed				= false;

	/**
	 * The stack trace of the thread that closed this class loader
	 */
	private String											closedStack			= "";

	/**
	 * The cache of loaded classes
	 */
	private final ConcurrentHashMap<String, Class<?>>		loadedClasses		= new ConcurrentHashMap<>();

	/**
	 * The cache of unfound classes, for performance reasons
	 */
	private final ConcurrentHashMap<String, Class<?>>		unfoundClasses		= new ConcurrentHashMap<>();

	/**
	 * This caches the method handles for the class so we don't have to look them up every time. This is used by the DynamicInteropService, but stored here
	 * so when a DCL is GC'd the cache goes with it.
	 */
	private final ConcurrentHashMap<String, MethodRecord>	methodHandleCache	= new ConcurrentHashMap<>( 32 );

	/**
	 * Logger. Lazy init to avoid deadlocks on runtime startup
	 */
	private static Logger									logger				= null;

	/**
	 * The BoxLang Runtime instance
	 */
	private static final BoxRuntime							runtime				= BoxRuntime.getInstance();

	/**
	 * Runtime special prefixes Set that MUST come from the parent class loader
	 * THIS IS SPECIAL CASE FOR LOGGING FRAMEWORKS WHERE THIRD PARTY JARS MAY BE LOADED AND DELEGATED TO THE PARENT
	 */
	private static final Set<String>						PARENT_CLASSES		= Set.of(
	    "ch.qos.logback",
	    "org.slf4j"
	);

	private String											URLHash;

	/**
	 * Global Cleaner instance for all DynamicClassLoaders.
	 * Used to reliably close unreferenced class loaders that were not explicitly closed.
	 */
	private static final Cleaner							cleaner				= Cleaner.create();

	/**
	 * List of temporary files created by this class loader for JAR copying.
	 * These files will be deleted when the class loader is closed.
	 */
	private final List<File>								tempFiles;

	/**
	 * Unique identifier for this class loader instance, used in temp file naming.
	 */
	private final String									classLoaderId;

	/**
	 * Cleanable registration for this class loader.
	 * When the CL becomes phantom-reachable (eligible for GC without explicit close),
	 * the registered cleanup action will close it. The action captures only the state
	 * it needs (temp files list) via a static inner class, NOT a reference to this CL,
	 * so the CL can still be GC'd.
	 */
	private final Cleaner.Cleanable							cleanable;

	/**
	 * A cleaning action that closes a DynamicClassLoader without holding a strong reference to it.
	 * Used by {@link Cleaner} so the CL can still become phantom-reachable.
	 * <p>
	 * This static inner class is th ekey to making the Cleaner pattern work: it captures
	 * the CL's identity (for temp file naming) and its temp files list, but does NOT
	 * hold a reference to the DynamicClassLoader itself. Otherwise the CL would never
	 * become phantom-reachable and the Cleaner would never fire.
	 */
	private static class CloseAction implements Runnable {

		private final List<File> tempFiles;

		CloseAction( List<File> tempFiles ) {
			this.tempFiles = tempFiles;
		}

		@Override
		public void run() {
			deleteTempFiles( this.tempFiles );
		}

		/**
		 * Best-effort deletion of temporary JAR files. This may fail on Windows
		 * if the JARs are still locked; if so, {@code deleteOnExit()} in the
		 * constructor is the JVM-shutdown fallback.
		 */
		private static void deleteTempFiles( List<File> tempFiles ) {
			if ( tempFiles.isEmpty() ) {
				return;
			}
			for ( File tempFile : tempFiles ) {
				try {
					if ( tempFile.exists() ) {
						tempFile.delete();
					}
				} catch ( Exception e ) {
					// best-effort
				}
			}
		}
	}

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
		super( name.getName(), new URL[ 0 ], loadParentFirst ? parent : null );
		Objects.requireNonNull( parent, "Parent class loader cannot be null" );
		this.parent			= parent;
		this.nameAsKey		= name;
		this.tempFiles		= new ArrayList<>();
		this.classLoaderId	= UUID.randomUUID().toString().replace( "-", "" ).substring( 0, 8 );
		this.URLHash		= ClassLoaderUtil.hashSorted( urls );
		this.cleanable		= cleaner.register( this, new CloseAction( this.tempFiles ) );
		// Process original URLs through temp-copying addURL after super()
		for ( URL url : urls ) {
			addURL( url );
		}
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
	 * Temp File Management
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Copies JAR files from the provided URLs to temporary files to prevent file locking on Windows.
	 * Non-JAR files and non-file URLs are returned as-is.
	 * <p>
	 * Temp file naming strategy:
	 * {@code {original-filename}-{hash-of-path-timestamp}-{runtimeId}-{classLoaderId}.jar}
	 * <p>
	 * This ensures:
	 * - Original filename is preserved for debugging
	 * - Hash includes full path + lastModified timestamp for uniqueness when file changes
	 * - Runtime ID prevents conflicts across multiple BoxLang processes sharing the same temp dir
	 * - ClassLoader ID prevents conflicts when multiple CLs load the same JAR simultaneously
	 *
	 * @param urls          The original URLs to process
	 * @param tempFiles     List to populate with created temp files
	 * @param runtimeId     Unique ID of the runtime process
	 * @param classLoaderId Unique ID of this class loader instance
	 *
	 * @return Array of URLs pointing to temp files (or original URLs if not file-based)
	 */
	private static URL[] copyJarsToTemp( URL[] urls, List<File> tempFiles, String runtimeId, String classLoaderId ) {
		if ( urls == null || urls.length == 0 ) {
			return urls;
		}

		List<URL> resultUrls = new ArrayList<>();

		for ( URL url : urls ) {
			if ( !"file".equals( url.getProtocol() ) ) {
				resultUrls.add( url );
				continue;
			}

			String	path		= url.getPath();
			File	sourceFile	= new File( path );

			if ( !path.toLowerCase().endsWith( ".jar" ) || !sourceFile.exists() ) {
				resultUrls.add( url );
				continue;
			}

			try {
				String	fileName		= sourceFile.getName();
				String	nameWithoutExt	= fileName.endsWith( ".jar" ) ? fileName.substring( 0, fileName.length() - 4 ) : fileName;
				String	pathHash		= ClassLoaderUtil.hashSorted( new Object[] { sourceFile.getAbsolutePath(), sourceFile.lastModified() } );
				String	tempFileName	= String.format( "%s-%s-%s-%s.jar", nameWithoutExt, pathHash.substring( 0, 16 ),
				    runtimeId, classLoaderId );

				Path	tempDir			= Paths.get( System.getProperty( "java.io.tmpdir" ), "boxlang-jars" );
				Files.createDirectories( tempDir );
				Path	tempFilePath	= tempDir.resolve( tempFileName );
				File	tempFile		= tempFilePath.toFile();

				if ( !tempFile.exists() ) {
					copyFile( sourceFile, tempFile );
				}
				tempFile.deleteOnExit();

				tempFiles.add( tempFile );
				resultUrls.add( tempFile.toURI().toURL() );

				if ( logger != null ) {
					logger.debug( "Copied JAR [{}] to temp location [{}]", sourceFile.getAbsolutePath(), tempFile.getAbsolutePath() );
				}
			} catch ( Exception e ) {
				resultUrls.add( url );
				if ( logger != null ) {
					logger.warn( "Failed to copy JAR [{}] to temp location, using original path: {}", sourceFile.getAbsolutePath(),
					    e.getMessage() );
				}
			}
		}

		return resultUrls.toArray( new URL[ 0 ] );
	}

	/**
	 * Copy a file from source to destination
	 *
	 * @param source The source file
	 * @param dest   The destination file
	 *
	 * @throws IOException If an I/O error occurs during copying
	 */
	private static void copyFile( File source, File dest ) throws IOException {
		try ( InputStream is = new java.io.FileInputStream( source );
		    FileOutputStream os = new FileOutputStream( dest ) ) {
			byte[]	buffer	= new byte[ 8192 ];
			int		bytesRead;
			while ( ( bytesRead = is.read( buffer ) ) != -1 ) {
				os.write( buffer, 0, bytesRead );
			}
		}
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
	 * Get the unique identifier for this class loader instance.
	 * Used in temp file naming to distinguish files from different class loaders.
	 *
	 * @return The unique class loader identifier
	 */
	public String getClassLoaderId() {
		return this.classLoaderId;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @return This loader as a {@link ClassLoader}
	 */
	@Override
	public ClassLoader toClassLoader() {
		return this;
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
	 * Find a class in the class loader or delegate to the parent. If not found, then throw an exception
	 *
	 * @param className The name of the class to find
	 */
	public Class<?> findClass( String className, Boolean safe ) throws ClassNotFoundException {
		return findClass( className, false, true );
	}

	/**
	 * Find a class in the class loader or delegate to the parent
	 *
	 * @param className The name of the class to find
	 * @param safe      Whether to throw an exception if the class is not found
	 */
	public Class<?> findClass( String className, Boolean safe, boolean checkParent ) throws ClassNotFoundException {
		Logger logger = getLogger();
		if ( closed ) {
			throw new BoxRuntimeException(
			    "Class loader [" + nameAsKey.getName() + "] is closed, but you are trying to use it still! Closed by this thread: \n\n" + closedStack );
		}

		// Default it to false
		if ( safe == null ) {
			safe = false;
		}

		logger.trace( "[{}] Discovering class: [{}] from thread [{}]", this.nameAsKey.getName(), className, Thread.currentThread().getName() );
		logger.trace( "The context class loader is [{}]", Thread.currentThread().getContextClassLoader().getName() );

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
		// Any other special cases can be added here to the PARENT_CLASSES set
		if ( this.parent != null && PARENT_CLASSES.stream().anyMatch( className::startsWith ) ) {
			logger.trace( "[{}].[{}] : Class is a special parent class, delegating to parent", this.nameAsKey.getName(), className );
			return getDynamicParent().loadClass( className );
		}

		// 3. Attempt to load from JARs/classes in the seeded URLs
		try {
			cachedClass = super.findClass( className );
			logger.trace( "[{}].[{}] : Class found locally from thread [{}] ", this.nameAsKey.getName(), className, Thread.currentThread().getName() );
		} catch ( ClassNotFoundException e ) {
			if ( checkParent ) {
				// 4. If not found in JARs, delegate to parent class loader
				try {
					logger.trace( "[{}].[{}] : Class not found locally, trying the parent...", this.nameAsKey.getName(), className );
					logger.trace( "The context class loader is [{}]", Thread.currentThread().getContextClassLoader().getName() );
					cachedClass = getDynamicParent().loadClass( className );
					logger.trace( "[{}].[{}] : Class found in parent on thread [{}]", this.nameAsKey.getName(), className, Thread.currentThread().getName() );
				} catch ( ClassNotFoundException parentException ) {

					// Only do this if we've bubbled up to the runtime classloader. Otherwise, we'll get stack overflows if this is just a module classloader
					if ( this == runtime.getRuntimeLoader() ) {
						logger.trace( "[{}].[{}] : Class not found in parent, searching the class locator", this.nameAsKey.getName(), className );
						IBoxContext				context	= Optional.ofNullable( ( IBoxContext ) RequestBoxContext.getCurrent() )
						    .orElse( runtime.getRuntimeContext() );
						Optional<ClassLocation>	result	= runtime.getClassLocator().getJavaResolver().findFromAllModules( className, null, context );

						if ( result.isPresent() ) {
							cachedClass = result.get().clazz();
							logger.trace( "[{}].[{}] : Class found in class locator on thread [{}]", this.nameAsKey.getName(), className,
							    Thread.currentThread().getName() );
						}
					}
				}

				if ( cachedClass == null ) {
					logger.trace( "[{}].[{}] : Giving up on class, adding to unfound classes", this.nameAsKey.getName(), className );
					// Add to the unfound cache
					this.unfoundClasses.put( className, NullValue.class );
					// If not safe, throw the exception
					if ( !safe ) {
						throw new ClassNotFoundException( String.format( "Class [%s] not found in class loader [%s]", className, this.nameAsKey.getName() ) );
					}
				}

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
	 * Add a URL to the class loader, automatically copying JARs to temp files
	 * to prevent file locking on Windows.
	 *
	 * @param url The URL to add
	 *
	 * @see URLClassLoader#addURL(URL)
	 */
	@Override
	public void addURL( URL url ) {
		List<File>	newTempFiles	= new ArrayList<>();
		URL[]		processed		= copyJarsToTemp( new URL[] { url }, newTempFiles, runtime.getRuntimeId(), this.classLoaderId );
		for ( URL u : processed ) {
			super.addURL( u );
		}
		this.tempFiles.addAll( newTempFiles );
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
	 * Add a single path to the class loader by converting it to URLs
	 * and adding them to the class loader. This method internally calls
	 * getJarURLs() to discover all JAR files and classes in the path.
	 *
	 * @param path The file system path to add (can be a directory or single file)
	 *
	 * @throws BoxIOException If the path is invalid or cannot be processed
	 */
	public void addPaths( String path ) {
		try {
			URL[] urls = getJarURLs( path );
			addURLs( urls );
		} catch ( IOException e ) {
			throw new BoxIOException( "Failed to add path [" + path + "] to class loader [" + this.nameAsKey.getName() + "]", e );
		} catch ( UncheckedIOException e ) {
			// Convert UncheckedIOException to IOException for BoxIOException constructor
			IOException cause = e.getCause();
			throw new BoxIOException( "Failed to add path [" + path + "] to class loader [" + this.nameAsKey.getName() + "]", cause );
		}
	}

	/**
	 * Add multiple paths to the class loader by converting them to URLs
	 * and adding them to the class loader. This method internally calls
	 * getJarURLs() for each path and then addURLs() to add them.
	 *
	 * @param paths Array of file system paths to add (can be directories or single files)
	 *
	 * @throws BoxIOException If any path is invalid or cannot be processed
	 */
	public void addPaths( Array paths ) {
		URL[] urls = inflateClassPaths( paths );
		addURLs( urls );
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
		this.methodHandleCache.clear();
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
		// Guard against double-close and Cleaner racing with explicit close
		if ( closed ) {
			return;
		}
		closed = true;
		StringWriter	sw	= new StringWriter();
		PrintWriter		pw	= new PrintWriter( sw );
		new Exception().printStackTrace( pw );
		closedStack = sw.toString();

		// Cancel the Cleaner registration so it doesn't try to close us again
		if ( this.cleanable != null ) {
			this.cleanable.clean();
		}

		// Clear the cache
		clearCache();
		// Null out the parent
		this.parent = null;
		// Close the class loader
		super.close();
		// Delete temporary JAR files
		deleteTempFiles();
	}

	/**
	 * Delete all temporary files created by this class loader.
	 * This method is called when the class loader is closed to clean up temp JAR files.
	 */
	private void deleteTempFiles() {
		if ( tempFiles.isEmpty() ) {
			return;
		}

		int	deletedCount	= 0;
		int	failedCount		= 0;

		for ( File tempFile : tempFiles ) {
			try {
				if ( tempFile.exists() && tempFile.delete() ) {
					deletedCount++;
					if ( logger != null ) {
						logger.debug( "Deleted temp JAR file [{}]", tempFile.getAbsolutePath() );
					}
				} else {
					failedCount++;
					if ( logger != null ) {
						logger.warn( "Failed to delete temp JAR file [{}] - may still be locked by another process",
						    tempFile.getAbsolutePath() );
					}
				}
			} catch ( Exception e ) {
				failedCount++;
				if ( logger != null ) {
					logger.warn( "Error deleting temp JAR file [{}]: {}", tempFile.getAbsolutePath(), e.getMessage() );
				}
			}
		}

		if ( logger != null && ( deletedCount > 0 || failedCount > 0 ) ) {
			logger.debug( "Temp file cleanup complete: {} deleted, {} failed for ClassLoader [{}]",
			    deletedCount, failedCount, nameAsKey.getName() );
		}
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
	 * Accepts a folder or a specific jar or class file path
	 *
	 * @param targetPath The path to search for JARs
	 *
	 * @return An array of URLs of all the JARs in the path
	 */
	public static URL[] getJarURLs( Path targetPath ) throws IOException {
		// Ensure the path is a directory and that it exists
		if ( Files.exists( targetPath ) && !Files.isDirectory( targetPath ) ) {
			// If path is already a jar or class file, then return it directly
			if ( targetPath.toString().endsWith( ".jar" ) || targetPath.toString().endsWith( ".class" ) ) {
				return new URL[] { targetPath.toUri().toURL() };
			} else {
				throw new BoxRuntimeException(
				    String.format( "The requested path [%s] to discover jar's and classes must be a valid directory", targetPath )
				);
			}
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
		    // .peek( url -> getLogger().trace( "Inflated URL: [{}]", url ) )
		    .toArray( URL[]::new );
	}

	/**
	 * Lazy init of the logger
	 *
	 * @return The logger
	 */
	private static Logger getLogger() {
		if ( logger == null ) {
			synchronized ( DynamicClassLoader.class ) {
				if ( logger == null ) {
					logger = BoxRuntime.getInstance().getLoggingService().getRuntimeLogger();
				}
			}
		}
		return logger;
	}

	/**
	 * Get the runtime's class loader
	 */
	public static ClassLoader getRuntimeClassLoader() {
		return BoxRuntime.getInstance().getRuntimeLoader();
	}

	/**
	 * Get the thread's context class loader
	 */
	public static ClassLoader getContextClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}

	/**
	 * Get the method handle cache
	 */
	public ConcurrentHashMap<String, MethodRecord> getMethodHandleCache() {
		return methodHandleCache;
	}

	/**
	 * Get URLHash which reprents the unique set of jar/class files loaded in this ClassLoader
	 * This is to be able to tell if another DynamicClassLoader has the same set of jar/class files loaded, even if they were a different version of the jar.
	 */
	public String getURLHash() {
		return URLHash;
	}

}
