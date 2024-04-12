/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Disk Class Loader for our Class Infos
 */
public class DiskClassLoader extends URLClassLoader {

	/**
	 * The location of the disk store
	 */
	private Path	diskStore;

	/**
	 * The boxpiler
	 */
	IBoxpiler		boxPiler;

	/**
	 * Constructor
	 *
	 * @param urls      classpath
	 * @param parent    parent classloader
	 * @param diskStore disk store location path
	 * @param boxpiler  Boxpiler
	 */
	public DiskClassLoader( URL[] urls, ClassLoader parent, Path diskStore, IBoxpiler boxpiler ) {
		super( urls, parent );
		this.boxPiler	= boxpiler;
		this.diskStore	= diskStore;
		// Init disk store
		diskStore.toFile().mkdirs();
	}

	/**
	 * Find class on disk, if not found, delegate to parent
	 *
	 * @param name class name
	 */
	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException {
		Path		diskPath	= generateDiskPath( name );
		// JIT compile
		ClassInfo	classInfo	= boxPiler.getClassPool().get( IBoxpiler.getBaseFQN( name ) );
		if ( !hasClass( diskPath ) || ( classInfo != null && ( classInfo.lastModified() > diskPath.toFile().lastModified() ) ) ) {
			// After this call, the class files will exist on disk
			boxPiler.compileClassInfo( name );
		}

		// Read file as byte array
		byte[] bytes;
		try {
			bytes = Files.readAllBytes( diskPath );
		} catch ( IOException e ) {
			throw new ClassNotFoundException( "Unable to read class file from disk", e );
		}

		return defineClass( name, bytes, 0, bytes.length );
	}

	/**
	 * Check if a class exists on disk
	 *
	 * @param name class name
	 *
	 * @return true if class exists on disk
	 */
	public boolean hasClass( String name ) {
		// Don't use disk cache if we are in debug mode
		if ( BoxRuntime.getInstance().inDebugMode() ) {
			return false;
		}
		return generateDiskPath( name ).toFile().exists();
	}

	/**
	 * Check if a class exists on disk and is up to date
	 *
	 * @param name         class name
	 * @param lastModified last modified timestamp of source file
	 *
	 * @return true if class exists on disk and is up to date
	 */
	public boolean hasClass( String name, long lastModified ) {
		Path diskPath = generateDiskPath( name );
		if ( !diskPath.toFile().exists() ) {
			return false;
		}
		// If source file is modified after class file
		if ( lastModified > diskPath.toFile().lastModified() ) {
			return false;
		}

		return true;
	}

	/**
	 * Check if a class exists on disk
	 *
	 * @param name class name
	 *
	 * @return true if class exists on disk
	 */
	private boolean hasClass( Path name ) {
		return name.toFile().exists();
	}

	/**
	 * Generate a disk path for a class name
	 *
	 * @param name class name
	 *
	 * @return path to class file
	 */
	private Path generateDiskPath( String name ) {
		return Paths.get( diskStore.toString(), name.replace( ".", File.separator ) + ".class" );
	}

	/*
	 * Required for Java Agents when this classloader is used as the system classloader
	 */
	@SuppressWarnings( "unused" )
	private void appendToClassPathForInstrumentation( String jarfile ) throws IOException {
		addURL( Paths.get( jarfile ).toRealPath().toUri().toURL() );
	}

}
