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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Dynamic in Memory classloader
 */
public class DiskClassLoader extends URLClassLoader {

	private JavaMemoryManager	manager;
	private Path				diskStore;

	public DiskClassLoader( URL[] urls, ClassLoader parent, Path diskStore ) {
		super( urls, parent );
		this.diskStore = diskStore;

	}

	@Override
	protected Class<?> findClass( String name ) throws ClassNotFoundException {

		Map<String, JavaClassByteCode> compiledClasses = manager
		    .getBytesMap();

		if ( compiledClasses.containsKey( name ) ) {
			byte[] bytes = compiledClasses.get( name )
			    .getBytes();
			return defineClass( name, bytes, 0, bytes.length );
		} else {
			return super.findClass( name );
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
