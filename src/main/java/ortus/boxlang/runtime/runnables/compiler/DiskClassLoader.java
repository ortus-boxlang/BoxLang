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
package ortus.boxlang.runtime.runnables.compiler;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.tools.JavaFileObject;

import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * Dynamic in Memory classloader
 */
public class DiskClassLoader extends URLClassLoader {

	private Path				diskStore;
	private JavaMemoryManager	manager;

	public DiskClassLoader( URL[] urls, ClassLoader parent, Path diskStore, JavaMemoryManager manager ) {
		super( urls, parent );
		this.manager	= manager;
		this.diskStore	= diskStore;
		diskStore.toFile().mkdirs();
	}

	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException {
		Path diskPath = generateDiskPath( name );
		if ( hasClass( diskPath ) ) {
			// Read file as byte array
			byte[] bytes;
			try {
				bytes = Files.readAllBytes( generateDiskPath( name ) );
			} catch ( IOException e ) {
				throw new ClassNotFoundException( "Unable to read class file from disk", e );
			}

			// once read from disk, cache in memory as well
			JavaFileObject jfo = manager.getJavaFileForOutput( null, name, JavaFileObject.Kind.CLASS, null );
			try {
				jfo.openOutputStream().write( bytes );
			} catch ( IOException e ) {
				throw new ClassNotFoundException( "Unable to write class file to memory", e );
			}

			return defineClass( name, bytes, 0, bytes.length );
		} else {
			return super.findClass( name );
		}
	}

	public boolean hasClass( String name ) {
		return generateDiskPath( name ).toFile().exists();
	}

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

	private boolean hasClass( Path name ) {
		return name.toFile().exists();
	}

	private Path generateDiskPath( String name ) {
		return Paths.get( diskStore.toString(), name.replace( ".", File.separator ) + ".class" );
	}

	public void writeToDisk( String name, byte[] bytes ) {
		Path diskPath = generateDiskPath( name );
		diskPath.toFile().getParentFile().mkdirs();
		try {
			Files.write( generateDiskPath( name ), bytes );
		} catch ( IOException e ) {
			throw new ApplicationException( "Unable to write class file to disk", e );
		}
	}

	/*
	 * Required for Java Agents when this classloader is used as the system classloader
	 */
	@SuppressWarnings( "unused" )
	private void appendToClassPathForInstrumentation( String jarfile ) throws IOException {
		addURL( Paths.get( jarfile ).toRealPath().toUri().toURL() );
	}
}
