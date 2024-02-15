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
package ortus.boxlang.runtime.loader.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * The {@code ClassDiscovery} class is used to discover classes in a given package at runtime
 *
 * - {@code getClassFilesAsStream} will return a stream of fully qualified class names
 * - {@code getClassFiles} will return an array of fully qualified class names
 * - {@code loadClassFiles} will return an array of loaded classes
 */
public class ClassDiscovery {

	/**
	 * Logger
	 */
	private static final Logger	logger					= LoggerFactory.getLogger( ClassDiscovery.class );

	private static final String	JAR_FILE_EXTENSION		= "jar";
	private static final String	CLASS_FILE_EXTENSION	= ".class";

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @param recursive   Whether to scan recursively or not
	 *
	 * @return The discovered classes as a stream of loadable fully qualified names
	 *
	 * @throws IOException If the classpath cannot be read
	 */
	public static Stream<String> getClassFilesAsStream( String packageName, Boolean recursive ) throws IOException {
		ClassLoader			classLoader	= ClassLoader.getSystemClassLoader();
		String				path		= packageName.replace( '.', '/' );
		Enumeration<URL>	resources	= classLoader.getResources( path );

		return Collections.list( resources )
		    .stream()
		    .map( URL::getFile )
		    .map( File::new )
		    .flatMap( directory -> Stream.of( findClassNames( directory, packageName, recursive ) ) );
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @param recursive   Whether to scan recursively or not
	 *
	 * @return The discovered classes as loadable fully qualified names
	 *
	 * @throws IOException If the classpath cannot be read
	 */
	public static String[] getClassFiles( String packageName, Boolean recursive ) throws IOException {
		return getClassFilesAsStream( packageName, recursive )
		    .toArray( String[]::new );
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 * This method will load the classes into the JVM.
	 *
	 * If a class can't be loaded it will be {@code null} in that position in the array
	 *
	 * @param packageName The base package
	 * @param recursive   Whether to scan recursively or not
	 *
	 * @return The discovered classes as loaded classes
	 *
	 * @throws IOException
	 */
	public static Class<?>[] loadClassFiles( String packageName, Boolean recursive ) throws IOException {
		return getClassFilesAsStream( packageName, recursive )
		    .parallel()
		    .map( className -> {
			    try {
				    return Class.forName( className );
			    } catch ( ClassNotFoundException e ) {
				    e.printStackTrace();
				    return null;
			    }
		    } )
		    .toArray( Class<?>[]::new );
	}

	/**
	 * Find all classes annotated with the given annotation(s) in the given directory and given class loader
	 *
	 * @param startDir     The directory to start searching in
	 * @param targetLoader The classloader to use
	 * @param packageName  The package name for classes found inside the base directory, if null, we auto-detect it
	 * @param annotations  The annotation classes to search for (varargs)
	 *
	 * @return A stream of classes
	 */
	@SafeVarargs
	public static Stream<Class<?>> findAnnotatedClasses(
	    String startDir,
	    ClassLoader targetLoader,
	    String packageName,
	    Class<? extends Annotation>... annotations ) {

		List<Class<?>> classes = new ArrayList<>();

		// Auto-detect package name if not passed.
		if ( packageName == null ) {
			packageName	= startDir.replace( '/', '.' );
			packageName	= packageName.replace( '\\', '.' );
		}

		try {
			Enumeration<URL> resources;

			// URL Class loaders are different, look locally first
			if ( targetLoader instanceof URLClassLoader URLTargetLoader ) {
				resources = URLTargetLoader.findResources( startDir );
			} else {
				resources = targetLoader.getResources( startDir );
			}

			while ( resources.hasMoreElements() ) {
				URL resource = resources.nextElement();

				// Jar Loading
				if ( resource.getProtocol().equals( "jar" ) ) {

					logger.atDebug().log( "FindAnnotatedClasses: Jar file found: {}", resource );

					classes.addAll(
					    findClassesInJar( resource, startDir, targetLoader, annotations )
					);
				}
				// Normal directory loading
				else {
					logger.atDebug().log( "FindAnnotatedClasses: Normal directory found: {}", resource );
					classes.addAll(
					    findClassesInDirectory(
					        new File( resource.getFile() ), // directory
					        packageName, // package name
					        targetLoader, // class loader
					        annotations // annotations
					    )
					);
				}
			}
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Exception finding annotated classes in path " + startDir, e );
			// logger.error( "Exception finding annotated classes in path [{}]", startDir, e );
		}

		return classes.stream();
	}

	/**
	 * Find all classes annotated with the given annotation(s) in the given directory.
	 * We also auto-detect the package name and use the system classloader
	 *
	 * @param startDir    The directory to start searching in
	 * @param annotations The annotation classes to search for (varargs)
	 *
	 * @return A stream of classes
	 */
	@SafeVarargs
	public static Stream<Class<?>> findAnnotatedClasses( String startDir, Class<? extends Annotation>... annotations ) {
		return findAnnotatedClasses( startDir, ClassDiscovery.class.getClassLoader(), null, annotations );
	}

	/**
	 * Find all classes annotated with the given annotation(s) in the given directory
	 *
	 * @param startDir    The directory to start searching in
	 * @param packageName The package name for classes found inside the base directory, if null, we auto-detect it
	 * @param annotations The annotation classes to search for (varargs)
	 *
	 * @return A stream of classes
	 */
	@SafeVarargs
	public static Stream<Class<?>> findAnnotatedClasses( String startDir, String packageName, Class<? extends Annotation>... annotations ) {
		return findAnnotatedClasses( startDir, ClassDiscovery.class.getClassLoader(), packageName, annotations );
	}

	/**
	 * Get a file from the BoxLang jar resources as a {@code File} object
	 *
	 * @param resourceName The path to the resource. Example: {@code "ortus/boxlang/runtime/modules/core"}
	 *
	 * @return The file object for the resource
	 *
	 * @throws URISyntaxException If the resource URL cannot be converted to a URI
	 */
	public static File getFileFromResource( String resourceName ) {
		return getPathFromResource( resourceName ).toFile();
	}

	/**
	 * Get a file from the BoxLang jar resources as a {@code Path} object
	 *
	 * @param resourceName The path to the resource. Example: {@code "ortus/boxlang/runtime/modules/core"}
	 *
	 * @return The path object for the resource
	 *
	 * @throws URISyntaxException If the resource URL cannot be converted to a URI
	 */
	public static Path getPathFromResource( String resourceName ) {
		// Get the URL of the resource
		URL resourceUrl = ClassDiscovery.class.getClassLoader().getResource( resourceName );

		// Check if the resource exists
		if ( resourceUrl == null ) {
			throw new BoxRuntimeException( "Resource not found: " + resourceName );
		}

		// Convert the URI to a File object
		try {
			return Paths.get( resourceUrl.toURI() );
		} catch ( URISyntaxException e ) {
			throw new BoxRuntimeException(
			    String.format( "Cannot build an URI from the discovered resource %s", resourceUrl ),
			    e
			);
		}
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 *
	 * @return The classes as file representations
	 *
	 * @throws ClassNotFoundException
	 */
	public static String[] findClassNames( File directory, String packageName, Boolean recursive ) {
		return Arrays.stream( directory.listFiles() )
		    .parallel()
		    .flatMap( file -> {
			    if ( file.isDirectory() ) {
				    if ( recursive ) {
					    return Arrays.stream( findClassNames( file, packageName + "." + file.getName(), recursive ) );
				    }
				    return Stream.empty();
			    } else {
				    return file.getName().endsWith( CLASS_FILE_EXTENSION )
				        ? Stream.of( packageName + "." + file.getName().replace( CLASS_FILE_EXTENSION, "" ) )
				        : Stream.empty();
			    }
		    } )
		    .toArray( String[]::new );
	}

	/**
	 * Find all classes annotated in the specified jar file and subdirectories.
	 * Using the given classloader, start directory and the given annotations to search for.
	 * If a class cannot be loaded, it is logged and ignored.
	 *
	 * @param jarURL      The URL to the jar file
	 * @param startDir    The directory to start searching in
	 * @param classLoader The classloader to use
	 *
	 * @return A list of classes found in the jar and subdirectories
	 */
	public static List<Class<?>> findClassesInJar(
	    URL jarURL,
	    String startDir,
	    ClassLoader classLoader,
	    Class<? extends Annotation>[] annotations ) {

		List<Class<?>>	classes;
		String			jarPath	= jarURL.getPath().substring( 5, jarURL.getPath().indexOf( "!" ) );

		try ( JarFile jar = new JarFile( URLDecoder.decode( jarPath, "UTF-8" ) ) ) {
			classes = jar.stream()
			    // divide and conquer!
			    .parallel()
			    // Only process entries that start with the startDir and end with .class
			    .filter( entry -> entry.getName().startsWith( startDir ) && entry.getName().endsWith( CLASS_FILE_EXTENSION ) )
			    // Construct the class name from the path
			    .map( entry -> entry.getName().replace( '/', '.' ).substring( 0, entry.getName().length() - 6 ) )
			    // Load it or log it
			    .map( className -> {
				    try {
					    return Class.forName( className, false, classLoader );
				    } catch ( ClassNotFoundException e ) {
					    logger.error( "Class not found: {}", className, e );
					    return null;
				    }
			    } )
			    // Only annotated ones and non null
			    .filter( clazz -> clazz != null && isAnnotated( clazz, annotations ) )
			    // Collect them
			    .collect( Collectors.toList() );

		} catch ( IOException e ) {
			logger.error( "Error while processing JAR file", e );
			throw new BoxRuntimeException( "Error while processing JAR file", e );
		}

		return classes;
	}

	/**
	 * Find all classes annotated in the specified directory and subdirectories.
	 * It will load the classes but not initialize them via the passed classloader and package name
	 * and the given annotations.
	 *
	 * @param directory   The directory to start searching in
	 * @param packageName The package name for classes found inside the base directory
	 * @param classLoader The classloader to use
	 *
	 * @return A list of classes
	 */
	public static List<Class<?>> findClassesInDirectory(
	    File directory,
	    String packageName,
	    ClassLoader classLoader,
	    Class<? extends Annotation>[] annotations ) {

		List<Class<?>>	classes;
		Path			directoryPath	= directory.toPath();

		// System.out.println( "Scanning directory: " + directory );
		// System.out.println( "PackageName: " + packageName );
		try ( Stream<Path> pathStream = Files.walk( directoryPath ) ) {
			classes = pathStream
			    // Run baby run!
			    .parallel()
			    // Only .class files please
			    .filter( path -> path.toString().endsWith( CLASS_FILE_EXTENSION ) )
			    // Replace the base directory path so we only work with packages and use dot notation
			    .map( path -> directoryPath.relativize( path ).toString().replace( File.separator, "." ) )
			    // Construct the class name from the path
			    .map( path -> packageName + "." + StringUtils.replaceIgnoreCase( path, CLASS_FILE_EXTENSION, "" ) )
			    // Load it or throw up
			    .map( className -> {
				    try {
					    return Class.forName( className, false, classLoader );
				    } catch ( ClassNotFoundException e ) {
					    logger.error( "Class not found: {}", className, e );
					    return null;
				    }
			    } )
			    // Only annotated ones
			    .filter( clazz -> clazz != null && isAnnotated( clazz, annotations ) )
			    // Collect them
			    .collect( Collectors.toList() );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Exception finding classes in directory " + directory, e );
		}

		return classes;
	}

	/**
	 * Check if the given class is annotated with any of the given annotations
	 *
	 * @param clazz       The class to check
	 * @param annotations The annotations to check for
	 *
	 * @return
	 */
	public static boolean isAnnotated( Class<?> clazz, Class<? extends Annotation>[] annotations ) {
		return annotations.length > 0 ? Arrays.stream( annotations ).anyMatch( clazz::isAnnotationPresent ) : true;
	}

}
