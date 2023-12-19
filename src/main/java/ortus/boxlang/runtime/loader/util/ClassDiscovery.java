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
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

/**
 * The {@code ClassDiscovery} class is used to discover classes in a given package at runtime
 *
 * - {@code getClassFilesAsStream} will return a stream of fully qualified class names
 * - {@code getClassFiles} will return an array of fully qualified class names
 * - {@code loadClassFiles} will return an array of loaded classes
 */
public class ClassDiscovery {

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
				    // TODO: use logger for this, or remove
				    e.printStackTrace();
				    return null;
			    }
		    } )
		    .toArray( Class<?>[]::new );
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

}
