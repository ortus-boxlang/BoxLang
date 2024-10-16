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
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

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
	 * The class pool name
	 */
	String			classPoolName;

	/**
	 * The class pool name but with all special characters replaced so it can be used as a valid folder name
	 */
	String			classPoolDiskPrefix;

	/**
	 * Constructor
	 *
	 * @param urls      classpath
	 * @param parent    parent classloader
	 * @param diskStore disk store location path
	 * @param boxpiler  Boxpiler
	 */
	public DiskClassLoader( URL[] urls, ClassLoader parent, Path diskStore, IBoxpiler boxpiler, String classPoolName ) {
		super( urls, parent );
		this.boxPiler				= boxpiler;
		this.classPoolName			= classPoolName;
		this.classPoolDiskPrefix	= classPoolName.replaceAll( "[^a-zA-Z0-9]", "_" );
		this.diskStore				= diskStore.resolve( classPoolDiskPrefix );

		// Init disk store
		diskStore.toFile().mkdirs();
	}

	/**
	 * Find class on disk, if not found, delegate to parent
	 * TODO: If we move to sharing classloaders across files with trusted cache, make the locking more granular instead of synchronizing the entire method
	 *
	 * @param name class name
	 */
	@Override
	protected synchronized Class<?> findClass( String name ) throws ClassNotFoundException {
		Path		diskPath	= generateDiskPath( name );
		// JIT compile
		String		baseName	= IBoxpiler.getBaseFQN( name );
		ClassInfo	classInfo	= boxPiler.getClassPool( classPoolName ).get( baseName );
		// Do we need to compile the class?
		// Pre-compiled source files will follow this path, but will be discovered as already compiled when we try to parse them
		if ( needsCompile( classInfo, diskPath, name, baseName ) ) {
			// After this call, the class files will exist on disk, or will have been side-loaded into this classloader via the
			// defineClass method (for pre-compiled source files)
			boxPiler.compileClassInfo( classPoolName, baseName );
		}

		// If there is no class file on disk, then we assume pre-compiled class bytes were already side loaded in, so we just get them
		// TODO: Change this to use the same flag discussed in the needsCompile method
		if ( !diskPath.toFile().exists() ) {
			return loadClass( name );
		}

		// In all other scenarios, the BoxPiler should have compiled the class and written it to disk
		byte[] bytes;
		try {
			bytes = Files.readAllBytes( diskPath );
		} catch ( IOException e ) {
			throw new ClassNotFoundException( "Unable to read class file from disk", e );
		}
		return defineClass( name, bytes, 0, bytes.length );
	}

	/**
	 * Abstract out the logic for determining if a class needs to be compiled
	 * 
	 * @param classInfo the class info, may be null if there is none
	 * @param diskPath  the path to the class file on disk, may not exist
	 * @param name      the fully qualified class name
	 * @param baseName  the base name of the class
	 * 
	 * @return true if the class needs to be compiled
	 */
	private boolean needsCompile( ClassInfo classInfo, Path diskPath, String name, String baseName ) {
		// TODO: need to add some mutable flags to the classInfo object to track if it has been compiled or not
		// and in what manner. For example, precompiled sources read directly into the class loader won't
		// have a class file on disk and we should be able to just skip that check entirely if we know that.
		// Using the existence of the class file on disk is not a good indicator of whether or not the class
		// needs to be compiled or not, as a precompiled source file will not have a class file on disk.

		// If we're loading an inner class, we know the compilation has already happened
		if ( !name.equals( baseName ) ) {
			return false;
		}
		// There is a class file cached on disk
		if ( hasClass( diskPath ) ) {
			// If the class file is older than the source file
			if ( classInfo != null && classInfo.lastModified() > diskPath.toFile().lastModified() ) {
				return true;
			}
			return false;
		}

		// There is no class file cached on disk
		return true;
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

	public void defineClasses( String fqn, File sourceFile ) {
		try {
			byte[]		fileBytes	= Files.readAllBytes( sourceFile.toPath() );
			ByteBuffer	buffer		= ByteBuffer.wrap( fileBytes );
			// remove initial magic number
			buffer.getInt();

			boolean first = true;

			while ( buffer.hasRemaining() ) {
				// Read the length of the class file
				int		length		= buffer.getInt();

				// Read the class file bytes
				byte[]	classBytes	= new byte[ length ];
				buffer.get( classBytes );

				if ( first ) {
					first = false;
					String classNameInFile = new String( classBytes );
					if ( !fqn.equals( classNameInFile ) ) {
						throw new RuntimeException( "The source file " + sourceFile.toPath().toString()
						    + " is pre-compiled bytecode, but its original class name [" + classNameInFile + "] does not match what we expected [" + fqn
						    + "].  Pre-compiled source code must have the same path and name as the original file." );
					}
				} else {
					// String className = getClassName( classBytes );
					// Define the class
					defineClass( null, classBytes, 0, classBytes.length );
				}
			}

		} catch ( IOException e ) {
			throw new RuntimeException( "Failed to read file", e );
		}
	}

	public static String getClassName( byte[] classBytes ) {
		ClassReader	classReader	= new ClassReader( classBytes );
		ClassNode	classNode	= new ClassNode();
		classReader.accept( classNode, 0 );
		return classNode.name.replace( '/', '.' );
	}

}
