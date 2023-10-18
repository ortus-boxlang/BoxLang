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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;

/**
 * Dynamic in Memory classloader
 */
public class JavaDynamicClassLoader extends URLClassLoader {

	private JavaMemoryManager	manager;
	private DiskClassLoader		diskClassLoader;

	public JavaDynamicClassLoader( URL[] urls, ClassLoader parent, JavaMemoryManager manager, DiskClassLoader diskClassLoader ) {
		super( urls, parent );
		this.manager			= manager;
		this.diskClassLoader	= diskClassLoader;
	}

	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException {

		Map<String, JavaClassByteCode> compiledClasses = manager
		    .getBytesMap();

		if ( compiledClasses.containsKey( name ) ) {
			byte[] bytes = compiledClasses.get( name )
			    .getBytes();

			// Don't use disk cache if we are in debug mode
			if ( !BoxRuntime.getInstance().inDebugMode() ) {
				// Cache on disk
				diskClassLoader.writeToDisk( name, bytes );
			}

			return defineClass( name, bytes, 0, bytes.length );
		} else {
			return super.findClass( name );
		}
	}

	/**
	 * Check if a class exists on disk
	 *
	 * @param name class name
	 *
	 * @return true if class exists on disk
	 */
	public boolean hasClass( String name ) {
		return manager.getBytesMap().containsKey( name );
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
		JavaClassByteCode jcb = manager.getBytesMap().get( name );
		if ( jcb == null ) {
			return false;
		}
		// If source file is modified after class file
		if ( lastModified > jcb.getLastModified() ) {
			return false;
		}

		return true;
	}

	/*
	 * Required for Java Agents when this classloader is used as the system classloader
	 */
	@SuppressWarnings( "unused" )
	private void appendToClassPathForInstrumentation( String jarfile ) throws IOException {
		addURL( Paths.get( jarfile ).toRealPath().toUri().toURL() );
	}
}
