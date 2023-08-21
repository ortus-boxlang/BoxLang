package ortus.boxlang.runtime.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Stream;

/**
 * The {@code ClassDiscovery} class is used to discover classes in a given package at runtime
 */
public class ClassDiscovery {

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 *
	 * @return The discovered classes as a stream of loadable fully qualified names
	 *
	 * @throws IOException If the classpath cannot be read
	 */
	public static Stream<String> getClassFilesAsStream( String packageName ) throws IOException {
		ClassLoader			classLoader	= ClassLoader.getSystemClassLoader();
		String				path		= packageName.replace( '.', '/' );
		Enumeration<URL>	resources	= classLoader.getResources( path );

		return Collections.list( resources )
		        .stream()
		        .map( URL::getFile )
		        .map( File::new )
		        .flatMap( directory -> Stream.of( findClassNames( directory, packageName ) ) );
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 *
	 * @return The discovered classes as loadable fully qualified names
	 *
	 * @throws IOException If the classpath cannot be read
	 */
	public static String[] getClassFiles( String packageName ) throws IOException {
		return getClassFilesAsStream( packageName )
		        .toArray( String[]::new );
	}

	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 * This method will load the classes into the JVM.
	 *
	 * If a class can't be loaded it will be {@code null} in that position in the array
	 *
	 * @param packageName The base package
	 *
	 * @return The discovered classes as loaded classes
	 *
	 * @throws IOException
	 */
	public static Class<?>[] loadClassFiles( String packageName ) throws IOException {
		return getClassFilesAsStream( packageName )
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
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 *
	 * @return The classes as file representations
	 *
	 * @throws ClassNotFoundException
	 */
	private static String[] findClassNames( File directory, String packageName ) {
		return Arrays.stream( directory.listFiles() )
		        .parallel()
		        .flatMap( file -> {
			        if ( file.isDirectory() ) {
				        return Arrays.stream( findClassNames( file, packageName + "." + file.getName() ) );
			        } else {
				        return file.getName().endsWith( ".class" )
				                ? Stream.of( packageName + "." + file.getName().replace( ".class", "" ) )
				                : Stream.empty();
			        }
		        } )
		        .toArray( String[]::new );
	}

}
