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
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * Disk Class Loader for our Class Infos
 */
public class DiskClassLoader extends URLClassLoader {

	/**
	 * The location of the disk store
	 */
	private Path																diskStore;

	/**
	 * The boxpiler
	 */
	IBoxpiler																	boxPiler;

	/**
	 * The class pool name
	 */
	String																		classPoolName;

	/**
	 * The class pool name but with all special characters replaced so it can be used as a valid folder name
	 */
	String																		classPoolDiskPrefix;

	/**
	 * Memory-safe cache for loaded classes
	 * Trying to avoid calling super.loadClass() here as the JDK ClassLoader.loadClass() method has a mandatory synchronized block which single threads access to loading a class, even if it's
	 * already compiled and already been loaded before. That is dumb, so we'll keep our own cache here once we've loaded it.
	 * I'm not using a conncurrent Map because I want to have NO LOCKING ON READS. I'm supplying my own concurrency below any time this map is modified.
	 * This means a read may happen while the map is having a put() operation performed. I THINK this is ok. If we see errors, then switch to a concurrent class.
	 */
	private final java.util.Map<String, java.lang.ref.WeakReference<Class<?>>>	loadedClasses	= new java.util.HashMap<>();

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
		this.classPoolDiskPrefix	= RegexBuilder.of( classPoolName, RegexBuilder.NON_ALPHANUMERIC ).replaceAllAndGet( "_" );
		this.diskStore				= diskStore.resolve( classPoolDiskPrefix );

		// Init disk store
		diskStore.toFile().mkdirs();
	}

	/**
	 * Load a class by name. The logic for this is all in the super class, but we're overriding the method
	 * so we can provide a layer of caching to avoid the heavy-handed locking in the ClassLoader.loadClass() method.
	 * Our version will require ZERO locking once the class has been loaded once.
	 */
	@Override
	public Class<?> loadClass( String name ) throws ClassNotFoundException {
		// check local cache
		java.lang.ref.WeakReference<Class<?>> ref = loadedClasses.get( name );
		if ( ref != null ) {
			Class<?> clazz = ref.get();
			if ( clazz != null ) {
				return clazz;
			}
		}

		// If not found, delegate to parent
		// This will only get hit the first time through, or if GC reaps the class
		synchronized ( loadedClasses ) {
			// Another thread may have created it. Perform double-check.
			ref = loadedClasses.get( name );
			if ( ref != null ) {
				Class<?> clazz = ref.get();
				if ( clazz != null ) {
					return clazz;
				}
			}
			// Ok, we give up. Load it up.
			var clazz = super.loadClass( name );
			loadedClasses.put( name, new java.lang.ref.WeakReference<>( clazz ) );
			return clazz;
		}
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
		// TODO: need to add some Modifiable flags to the classInfo object to track if it has been compiled or not
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
			if ( classInfo != null && classInfo.lastModified() > 0 && classInfo.lastModified() != diskPath.toFile().lastModified() ) {
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
		if ( lastModified > 0 && lastModified != diskPath.toFile().lastModified() ) {
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

	public void defineClasses( String fqn, File sourceFile, ClassInfo classInfo ) {
		try {
			byte[]		fileBytes	= Files.readAllBytes( sourceFile.toPath() );
			ByteBuffer	buffer		= ByteBuffer.wrap( fileBytes );
			// remove initial magic number
			buffer.getInt();
			// Read the original class name from the file
			int		classNameLength	= buffer.getInt();
			byte[]	classNameBytes	= new byte[ classNameLength ];
			buffer.get( classNameBytes );
			String	originalClassName	= new String( classNameBytes );

			String	newInternalName		= fqn.replace( '.', '/' );
			String	oldInternalName		= originalClassName.replace( '.', '/' );
			boolean	first				= true;

			while ( buffer.hasRemaining() ) {
				// Read the length of the class file
				int		length		= buffer.getInt();

				// Read the class file bytes
				byte[]	classBytes	= new byte[ length ];
				buffer.get( classBytes );

				// Use ASM to rewrite the class name and references
				byte[] newClassBytes = renameClassAndReferences( classBytes, oldInternalName, newInternalName, classInfo, first );

				defineClass( null, newClassBytes, 0, newClassBytes.length );
				first = false;
			}

		} catch ( IOException e ) {
			throw new RuntimeException( "Failed to read file", e );
		}
	}

	/**
	 * Rename a class and all its references in the bytecode
	 * Also updates the box class FQN and path to reflect the current disk path and mapping
	 * 
	 * @param classBytes      the bytecode of the class
	 * @param oldInternalName the old internal name of the class (e.g., "com/example/MyClass")
	 * @param newInternalName the new internal name of the class (e.g., "com/example/NewClass")
	 * @param classInfo       the ClassInfo object containing metadata about the class
	 * @param outerClass      true if this is the outer class, false if it is an inner class
	 * 
	 * @return the modified bytecode with the class name and references updated
	 */
	private byte[] renameClassAndReferences( byte[] classBytes, String oldInternalName, String newInternalName, ClassInfo classInfo, boolean outerClass ) {
		String		boxFQN			= classInfo.boxFqn().toString();
		String		mappingName		= classInfo.resolvedFilePath().mappingName();
		String		mappingPath		= classInfo.resolvedFilePath().mappingPath();
		String		relativePath	= classInfo.resolvedFilePath().relativePath();
		String		absolutePath	= classInfo.resolvedFilePath().absolutePath().toString();

		ClassReader	cr				= new ClassReader( classBytes );
		ClassNode	sourceNode		= new ClassNode();
		cr.accept( sourceNode, 0 );

		// Remap class names into a new ClassNode
		ClassNode	remappedNode	= new ClassNode();
		Remapper	remapper		= new Remapper() {

										@Override
										public String map( String internalName ) {
											if ( internalName.equals( oldInternalName ) || internalName.startsWith( oldInternalName + "$" ) ) {
												return internalName.replace( oldInternalName, newInternalName );
											}
											return internalName;
										}
									};
		// Remap from sourceNode to remappedNode
		sourceNode.accept( new ClassRemapper( remappedNode, remapper ) );

		// Only patch the static initializer for the name and path fields if this is the outer class
		if ( outerClass ) {
			for ( MethodNode mn : remappedNode.methods ) {
				if ( "<clinit>".equals( mn.name ) ) {
					InsnList insns = mn.instructions;
					for ( AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext() ) {
						// Patch Key name assignment
						if ( insn.getOpcode() == Opcodes.LDC
						    && insn.getNext() != null
						    && insn.getNext().getOpcode() == Opcodes.INVOKESTATIC
						    && insn.getNext().getNext() != null
						    && insn.getNext().getNext().getOpcode() == Opcodes.PUTSTATIC ) {

							LdcInsnNode		ldc			= ( LdcInsnNode ) insn;
							MethodInsnNode	keyOf		= ( MethodInsnNode ) insn.getNext();
							FieldInsnNode	putstatic	= ( FieldInsnNode ) insn.getNext().getNext();

							if ( keyOf.owner.equals( "ortus/boxlang/runtime/scopes/Key" )
							    && keyOf.name.equals( "of" )
							    && keyOf.desc.equals( "(Ljava/lang/String;)Lortus/boxlang/runtime/scopes/Key;" )
							    && putstatic.name.equals( "name" )
							    && putstatic.desc.equals( "Lortus/boxlang/runtime/scopes/Key;" ) ) {
								ldc.cst = boxFQN;
							}
						}

						// Patch ResolvedFilePath path assignment (util package version)
						if ( insn.getOpcode() == Opcodes.LDC
						    && insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.LDC
						    && insn.getNext().getNext() != null && insn.getNext().getNext().getOpcode() == Opcodes.LDC
						    && insn.getNext().getNext().getNext() != null && insn.getNext().getNext().getNext().getOpcode() == Opcodes.LDC
						    && insn.getNext().getNext().getNext().getNext() != null
						    && insn.getNext().getNext().getNext().getNext().getOpcode() == Opcodes.INVOKESTATIC
						    && insn.getNext().getNext().getNext().getNext().getNext() != null
						    && insn.getNext().getNext().getNext().getNext().getNext().getOpcode() == Opcodes.PUTSTATIC ) {

							LdcInsnNode		ldc1		= ( LdcInsnNode ) insn;
							LdcInsnNode		ldc2		= ( LdcInsnNode ) insn.getNext();
							LdcInsnNode		ldc3		= ( LdcInsnNode ) insn.getNext().getNext();
							LdcInsnNode		ldc4		= ( LdcInsnNode ) insn.getNext().getNext().getNext();
							MethodInsnNode	ofCall		= ( MethodInsnNode ) insn.getNext().getNext().getNext().getNext();
							FieldInsnNode	putstatic	= ( FieldInsnNode ) insn.getNext().getNext().getNext().getNext().getNext();

							if ( ofCall.owner.equals( "ortus/boxlang/runtime/util/ResolvedFilePath" )
							    && ofCall.name.equals( "of" )
							    && ofCall.desc.equals(
							        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Lortus/boxlang/runtime/util/ResolvedFilePath;" )
							    && putstatic.name.equals( "path" )
							    && putstatic.desc.equals( "Lortus/boxlang/runtime/util/ResolvedFilePath;" ) ) {
								ldc1.cst	= mappingName;
								ldc2.cst	= mappingPath;
								ldc3.cst	= relativePath;
								ldc4.cst	= absolutePath;
							}
						}
					}
				}
			}
		}

		ClassWriter cw = new ClassWriter( 0 );
		remappedNode.accept( cw );
		return cw.toByteArray();
	}

	public static String getClassName( byte[] classBytes ) {
		ClassReader	classReader	= new ClassReader( classBytes );
		ClassNode	classNode	= new ClassNode();
		classReader.accept( classNode, 0 );
		return classNode.name.replace( '/', '.' );
	}

}
