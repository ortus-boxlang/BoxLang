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
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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
	private static final Logger logger = LoggerFactory.getLogger( ClassDiscovery.class );

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
	 * Find all classes annotated with the given annotation(s) in the given directory
	 *
	 * @param startDir    The directory to start searching in
	 * @param annotations The annotation classes to search for (varargs)
	 *
	 * @return A stream of classes
	 */
	@SafeVarargs
	public static Stream<Class<?>> findAnnotatedClasses( String startDir, Class<? extends Annotation>... annotations ) {
		List<Class<?>> classes = new ArrayList<>();
		try {
			ClassLoader			classLoader	= ClassDiscovery.class.getClassLoader();
			Enumeration<URL>	resources	= classLoader.getResources( startDir );
			while ( resources.hasMoreElements() ) {
				URL resource = resources.nextElement();
				// Jar Loading
				if ( resource.getProtocol().equals( "jar" ) ) {
					classes.addAll(
					    findClassesInJar( resource, startDir, classLoader, annotations )
					);
				}
				// Normal directory loading
				else {
					classes.addAll(
					    findClassesInDirectory(
					        resource.getFile(),
					        startDir.replace( '/', '.' ),
					        classLoader,
					        annotations
					    )
					);
				}
			}
		} catch ( Exception e ) {
			logger.error( "Exception finding annotated classes in path [{}]", startDir, e );
		}
		return classes.stream();
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
	private static String[] findClassNames( File directory, String packageName, Boolean recursive ) {
		return Arrays.stream( directory.listFiles() )
		    .parallel()
		    .flatMap( file -> {
			    if ( file.isDirectory() ) {
				    if ( recursive ) {
					    return Arrays.stream( findClassNames( file, packageName + "." + file.getName(), recursive ) );
				    }
				    return Stream.empty();
			    } else {
				    return file.getName().endsWith( ".class" )
				        ? Stream.of( packageName + "." + file.getName().replace( ".class", "" ) )
				        : Stream.empty();
			    }
		    } )
		    .toArray( String[]::new );
	}

	/**
	 * Find all classes annotated with {@code BoxBIF} or {@code BoxMember} in the given directory
	 *
	 * @param jarURL      The URL to the jar file
	 * @param startDir    The directory to start searching in
	 * @param classLoader The classloader to use
	 *
	 * @return A list of classes
	 *
	 * @throws IOException
	 */
	private static List<Class<?>> findClassesInJar(
	    URL jarURL,
	    String startDir,
	    ClassLoader classLoader,
	    Class<? extends Annotation>[] annotations ) throws IOException {

		List<Class<?>>			classes	= new ArrayList<>();
		String					jarPath	= jarURL.getPath().substring( 5, jarURL.getPath().indexOf( "!" ) );
		JarFile					jar		= new JarFile( URLDecoder.decode( jarPath, "UTF-8" ) );
		Enumeration<JarEntry>	entries	= jar.entries();

		while ( entries.hasMoreElements() ) {
			JarEntry	entry	= entries.nextElement();
			String		name	= entry.getName();

			if ( name.startsWith( startDir ) && name.endsWith( ".class" ) ) {
				String className = name.replace( '/', '.' ).substring( 0, name.length() - 6 );
				try {
					Class<?> clazz = Class.forName( className, false, classLoader );
					if ( isAnnotated( clazz, annotations ) ) {
						classes.add( clazz );
					}
				} catch ( ClassNotFoundException e ) {
					logger.error( "Class not found: {}", className, e );
				}
			}
		}
		jar.close();
		return classes;
	}

	/**
	 * Find all classes annotated with {@code BoxBIF} or {@code BoxMember} in the given directory
	 *
	 * @param directory   The directory to start searching in
	 * @param packageName The package name for classes found inside the base directory
	 * @param classLoader The classloader to use
	 *
	 * @return A list of classes
	 */
	private static List<Class<?>> findClassesInDirectory(
	    String directory,
	    String packageName,
	    ClassLoader classLoader,
	    Class<? extends Annotation>[] annotations ) {
		Path			filePath	= Path.of( directory ).normalize();

		// Ensure threadsafe list
		List<Class<?>>	classes		= Collections.synchronizedList( new ArrayList<>() );

		try {
			Files.walk( filePath, 1 ).parallel().forEach(
			    path -> {
				    String name = path.getFileName().toString();
				    // recursion
				    if ( !path.equals( filePath ) && Files.isDirectory( path ) ) {
					    classes.addAll( findClassesInDirectory( path.toAbsolutePath().toString(), packageName + "." + name, classLoader, annotations ) );
				    }
				    // Class file found
				    else if ( name.endsWith( ".class" ) ) {
					    String className = packageName + '.' + name.substring( 0, name.length() - 6 );
					    try {
						    Class<?> clazz = Class.forName( className, false, classLoader );
						    if ( isAnnotated( clazz, annotations ) ) {
							    classes.add( clazz );
						    }
					    } catch ( ClassNotFoundException e ) {
						    logger.error( "Class not found: {}", className, e );
					    }
				    }
			    }
			);
		} catch ( IOException e ) {
			throw new BoxRuntimeException(
			    String.format(
			        "The directory [%s] could not be opened as a file strem",
			        directory
			    )
			);
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
	private static boolean isAnnotated( Class<?> clazz, Class<? extends Annotation>[] annotations ) {
		return Arrays.stream( annotations ).anyMatch( clazz::isAnnotationPresent );
	}

}
