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
 * Custom ClassLoader implementation for BoxLang that provides disk-based class caching and dynamic bytecode manipulation.
 *
 * <p>
 * This ClassLoader extends URLClassLoader to provide the following key functionalities:
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 * <li><strong>Disk-based Caching:</strong> Compiled classes are cached to disk for faster subsequent loads</li>
 * <li><strong>Memory-safe Class Cache:</strong> Uses WeakReferences to avoid memory leaks while providing fast access</li>
 * <li><strong>Dynamic Bytecode Manipulation:</strong> Uses ASM to rename classes and update references at runtime</li>
 * <li><strong>Pre-compiled Class Support:</strong> Can load and patch pre-compiled BoxLang classes</li>
 * <li><strong>JIT Compilation Integration:</strong> Works with the BoxPiler for just-in-time compilation</li>
 * </ul>
 *
 * <h3>Architecture:</h3>
 * <p>
 * The class loader maintains a disk store organized by class pool name, where each pool represents
 * a logical grouping of related classes (e.g., classes from the same source file or module).
 * </p>
 *
 * <h3>Bytecode Manipulation:</h3>
 * <p>
 * When loading pre-compiled classes, this loader can dynamically rename classes and update their
 * internal references using ASM. This allows BoxLang to load the same source class multiple times
 * with different FQNs (Fully Qualified Names) without conflicts.
 * </p>
 *
 * <h3>Thread Safety:</h3>
 * <p>
 * The class implements custom locking strategies to minimize contention while ensuring thread safety.
 * Read operations on the cache are lock-free, while writes use synchronized blocks.
 * </p>
 *
 * @see ortus.boxlang.compiler.IBoxpiler
 * @see ortus.boxlang.compiler.ClassInfo
 */
public class DiskClassLoader extends URLClassLoader {

	/**
	 * The location of the disk store where compiled classes are cached.
	 * This path is resolved relative to the base disk store and includes the class pool prefix.
	 */
	private Path																diskStore;

	/**
	 * The BoxLang compiler interface used for just-in-time compilation of source files.
	 * This is used when a class needs to be compiled from source before loading.
	 */
	IBoxpiler																	boxPiler;

	/**
	 * The logical name of the class pool this loader is responsible for.
	 * Class pools group related classes together (e.g., all classes from the same source file).
	 */
	String																		classPoolName;

	/**
	 * The class pool name sanitized for use as a directory name.
	 * All special characters are replaced with underscores to ensure filesystem compatibility.
	 */
	String																		classPoolDiskPrefix;

	/**
	 * Memory-safe cache for loaded classes using WeakReferences to prevent memory leaks.
	 *
	 * <p>
	 * This cache serves multiple purposes:
	 * </p>
	 * <ul>
	 * <li>Avoids the synchronized block in ClassLoader.loadClass() for already-loaded classes</li>
	 * <li>Provides O(1) lookup time for frequently accessed classes</li>
	 * <li>Automatically handles memory pressure via WeakReferences</li>
	 * </ul>
	 *
	 * <p>
	 * <strong>Concurrency Strategy:</strong> This map is NOT thread-safe by design to avoid
	 * read locking. Modifications are synchronized, but reads are lock-free. This means
	 * a read might occur during a write operation, but this is acceptable for performance.
	 * </p>
	 *
	 * <p>
	 * If concurrency issues arise, consider switching to ConcurrentHashMap, but this will
	 * introduce read locking overhead.
	 * </p>
	 */
	private final java.util.Map<String, java.lang.ref.WeakReference<Class<?>>>	loadedClasses	= new java.util.HashMap<>();

	/**
	 * Constructs a new DiskClassLoader with the specified configuration.
	 *
	 * <p>
	 * This constructor sets up the disk-based class loading infrastructure:
	 * </p>
	 * <ul>
	 * <li>Initializes the disk store directory structure</li>
	 * <li>Sanitizes the class pool name for filesystem compatibility</li>
	 * <li>Sets up the parent delegation chain</li>
	 * </ul>
	 *
	 * @param urls          The URLs from which to load classes and resources (passed to URLClassLoader)
	 * @param parent        The parent ClassLoader for delegation
	 * @param diskStore     The base directory where compiled classes will be cached
	 * @param boxpiler      The BoxLang compiler interface for JIT compilation
	 * @param classPoolName The logical name for this class pool (used for organization and caching)
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
	 * Loads a class by name with optimized caching to avoid ClassLoader synchronization overhead.
	 *
	 * <p>
	 * This method implements a three-tier loading strategy:
	 * </p>
	 * <ol>
	 * <li><strong>Fast Path:</strong> Check the WeakReference cache (lock-free read)</li>
	 * <li><strong>Double-Check:</strong> Synchronize and check cache again (handles race conditions)</li>
	 * <li><strong>Delegate:</strong> Fall back to parent ClassLoader.loadClass() and cache the result</li>
	 * </ol>
	 *
	 * <p>
	 * <strong>Performance Note:</strong> This approach eliminates the synchronized block overhead
	 * of the standard ClassLoader.loadClass() method for already-loaded classes, providing
	 * near-zero overhead for cache hits.
	 * </p>
	 *
	 * <p>
	 * <strong>Memory Management:</strong> Uses WeakReferences so classes can be garbage collected
	 * when no longer referenced, preventing memory leaks in long-running applications.
	 * </p>
	 *
	 * @param name The fully qualified name of the class to load
	 * 
	 * @return The loaded Class object
	 * 
	 * @throws ClassNotFoundException If the class cannot be found by this loader or its parents
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
	 * Finds and loads a class from disk, with just-in-time compilation support.
	 *
	 * <p>
	 * This method implements the core class loading logic:
	 * </p>
	 * <ol>
	 * <li><strong>Compilation Check:</strong> Determines if the class needs (re)compilation</li>
	 * <li><strong>JIT Compilation:</strong> Compiles source to bytecode if needed</li>
	 * <li><strong>Disk Loading:</strong> Loads compiled bytecode from disk cache</li>
	 * <li><strong>Pre-compiled Support:</strong> Handles classes already loaded via defineClasses()</li>
	 * </ol>
	 *
	 * <p>
	 * <strong>Compilation Logic:</strong>
	 * </p>
	 * <ul>
	 * <li>Inner classes are never compiled directly (they're compiled with their outer class)</li>
	 * <li>Classes are recompiled if source is newer than cached bytecode</li>
	 * <li>Missing cache files trigger compilation</li>
	 * </ul>
	 *
	 * <p>
	 * <strong>Pre-compiled Classes:</strong> Some classes may be loaded directly into the
	 * ClassLoader via defineClasses() without disk caching. These are handled by delegating
	 * back to loadClass().
	 * </p>
	 *
	 * <p>
	 * <strong>Thread Safety:</strong> This method is synchronized to prevent concurrent
	 * compilation of the same class, which could lead to file system conflicts.
	 * </p>
	 *
	 * @param name The fully qualified name of the class to find
	 * 
	 * @return The loaded Class object
	 * 
	 * @throws ClassNotFoundException If the class cannot be found or compiled
	 *
	 * @see #needsCompile(ClassInfo, Path, String, String)
	 * @see #defineClasses(String, File, ClassInfo)
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
	 * Determines whether a class needs to be compiled based on various factors.
	 *
	 * <p>
	 * This method implements intelligent compilation logic to avoid unnecessary work:
	 * </p>
	 *
	 * <h4>Decision Matrix:</h4>
	 * <table border="1">
	 * <tr>
	 * <th>Condition</th>
	 * <th>Result</th>
	 * <th>Reason</th>
	 * </tr>
	 * <tr>
	 * <td>Inner class (name != baseName)</td>
	 * <td>false</td>
	 * <td>Compiled with outer class</td>
	 * </tr>
	 * <tr>
	 * <td>No disk cache exists</td>
	 * <td>true</td>
	 * <td>First-time compilation needed</td>
	 * </tr>
	 * <tr>
	 * <td>Source newer than cache</td>
	 * <td>true</td>
	 * <td>Recompilation needed</td>
	 * </tr>
	 * <tr>
	 * <td>Cache exists and up-to-date</td>
	 * <td>false</td>
	 * <td>Use cached version</td>
	 * </tr>
	 * </table>
	 *
	 * <p>
	 * <strong>Timestamp Comparison:</strong> Uses lastModified timestamps to detect when
	 * source files have been updated since the last compilation. A mismatch triggers recompilation.
	 * </p>
	 *
	 * <p>
	 * <strong>Inner Class Optimization:</strong> Inner classes are never compiled individually
	 * since they're always compiled together with their enclosing class.
	 * </p>
	 *
	 * <p>
	 * <strong>Future Enhancement:</strong> The ClassInfo object should include compilation
	 * flags to better track compilation state and handle pre-compiled classes that were
	 * loaded directly into memory.
	 * </p>
	 *
	 * @param classInfo The metadata about the class (may be null for unknown classes)
	 * @param diskPath  The expected path to the compiled class file on disk
	 * @param name      The fully qualified class name being requested
	 * @param baseName  The base name without inner class suffixes
	 *
	 * @return true if the class needs to be compiled, false if cached version can be used
	 *
	 * @see ClassInfo#lastModified()
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
	 * Checks if a compiled class file exists on disk for the given class name.
	 *
	 * <p>
	 * This is a simple existence check that converts the class name to a file path
	 * and verifies the file exists in the disk cache.
	 * </p>
	 *
	 * @param name The fully qualified class name to check
	 * 
	 * @return true if a compiled class file exists on disk, false otherwise
	 *
	 * @see #generateDiskPath(String)
	 */
	public boolean hasClass( String name ) {
		return generateDiskPath( name ).toFile().exists();
	}

	/**
	 * Checks if a compiled class file exists on disk and is up-to-date with the source.
	 *
	 * <p>
	 * This method performs both existence and freshness checks:
	 * </p>
	 * <ol>
	 * <li>Verifies the compiled class file exists on disk</li>
	 * <li>Compares timestamps to ensure the cache is not stale</li>
	 * </ol>
	 *
	 * <p>
	 * <strong>Timestamp Logic:</strong> If lastModified is greater than 0 and doesn't
	 * match the disk file's timestamp, the cache is considered stale.
	 * </p>
	 *
	 * @param name         The fully qualified class name to check
	 * @param lastModified The timestamp of the source file (0 to skip timestamp check)
	 * 
	 * @return true if class exists on disk and is up-to-date, false otherwise
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
	 * Internal helper method to check if a class file exists at the given path.
	 *
	 * <p>
	 * This is a simple wrapper around File.exists() used internally
	 * by the compilation decision logic.
	 * </p>
	 *
	 * @param path The file system path to check
	 * 
	 * @return true if the file exists, false otherwise
	 */
	private boolean hasClass( Path path ) {
		return path.toFile().exists();
	}

	/**
	 * Generates the file system path where a compiled class should be stored.
	 *
	 * <p>
	 * This method converts a fully qualified class name into a file path
	 * within the disk store directory:
	 * </p>
	 *
	 * <p>
	 * <strong>Path Construction:</strong>
	 * </p>
	 * <ul>
	 * <li>Base directory: diskStore (includes class pool prefix)</li>
	 * <li>Package structure: dots replaced with file separators</li>
	 * <li>File extension: .class added</li>
	 * </ul>
	 *
	 * <p>
	 * <strong>Example:</strong><br>
	 * Class: {@code com.example.MyClass}<br>
	 * Result: {@code /path/to/diskStore/com/example/MyClass.class}
	 * </p>
	 *
	 * @param name The fully qualified class name
	 * 
	 * @return The path where the compiled class file should be stored
	 */
	private Path generateDiskPath( String name ) {
		return Paths.get( diskStore.toString(), name.replace( ".", File.separator ) + ".class" );
	}

	/**
	 * Required method for Java Agent compatibility when this ClassLoader is used as the system ClassLoader.
	 *
	 * <p>
	 * This method allows Java agents to dynamically add JAR files to the classpath
	 * at runtime. It's called via reflection by the Java Agent infrastructure.
	 * </p>
	 *
	 * <p>
	 * <strong>Usage:</strong> Primarily used in development and testing environments
	 * where dynamic classpath modification is needed.
	 * </p>
	 *
	 * @param jarfile The path to the JAR file to add to the classpath
	 * 
	 * @throws IOException If the JAR file cannot be read or added to the classpath
	 */
	@SuppressWarnings( "unused" )
	private void appendToClassPathForInstrumentation( String jarfile ) throws IOException {
		addURL( Paths.get( jarfile ).toRealPath().toUri().toURL() );
	}

	/**
	 * Loads and defines pre-compiled BoxLang classes from a binary file with dynamic class renaming.
	 *
	 * <p>
	 * This method handles the complex process of loading pre-compiled BoxLang classes
	 * that were compiled with a different FQN (Fully Qualified Name) than needed at runtime.
	 * It performs the following operations:
	 * </p>
	 *
	 * <h4>File Format Processing:</h4>
	 * <ol>
	 * <li><strong>Magic Number:</strong> Skips the initial magic number identifier</li>
	 * <li><strong>Original Class Name:</strong> Reads the original FQN from the file header</li>
	 * <li><strong>Class Data:</strong> Processes one or more class bytecode blocks</li>
	 * </ol>
	 *
	 * <h4>Bytecode Manipulation:</h4>
	 * <p>
	 * For each class in the file:
	 * </p>
	 * <ul>
	 * <li>Dynamically renames the class from original FQN to target FQN</li>
	 * <li>Updates all internal references to use the new name</li>
	 * <li>Patches BoxLang-specific metadata (Key names, ResolvedFilePath)</li>
	 * <li>Loads the transformed bytecode into the ClassLoader</li>
	 * </ul>
	 *
	 * <h4>Inner Class Support:</h4>
	 * <p>
	 * The file may contain multiple classes (outer class + inner classes).
	 * The first class is marked as the "outer class" for special metadata patching.
	 * </p>
	 *
	 * <p>
	 * <strong>Use Case:</strong> This enables BoxLang to load the same source class
	 * multiple times with different FQNs, which is essential for template includes
	 * and dynamic class generation.
	 * </p>
	 *
	 * @param fqn        The target fully qualified name for the class
	 * @param sourceFile The binary file containing the pre-compiled class data
	 * @param classInfo  Metadata about the class (used for patching BoxLang-specific fields)
	 *
	 * @throws RuntimeException If the file cannot be read or processed
	 *
	 * @see #renameClassAndReferences(byte[], String, String, ClassInfo, boolean)
	 */
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
	 * Performs sophisticated bytecode manipulation to rename a class and update all its references.
	 *
	 * <p>
	 * This method uses the ASM bytecode manipulation library to perform several complex operations:
	 * </p>
	 *
	 * <h4>Primary Functions:</h4>
	 * <ol>
	 * <li><strong>Class Renaming:</strong> Changes the internal class name throughout the bytecode</li>
	 * <li><strong>Reference Updates:</strong> Updates all references to the old class name</li>
	 * <li><strong>BoxLang Metadata Patching:</strong> Updates BoxLang-specific static fields</li>
	 * </ol>
	 *
	 * <h4>ASM Processing Pipeline:</h4>
	 * <ol>
	 * <li><strong>Parse:</strong> Read bytecode into ClassNode tree structure</li>
	 * <li><strong>Remap:</strong> Use ClassRemapper to rename class and references</li>
	 * <li><strong>Patch:</strong> Scan static initializer for BoxLang metadata to update</li>
	 * <li><strong>Generate:</strong> Write modified tree back to bytecode</li>
	 * </ol>
	 *
	 * <h4>BoxLang-Specific Patching:</h4>
	 * <p>
	 * For outer classes, this method searches the static initializer ({@code <clinit>}) method
	 * and patches specific instruction sequences:
	 * </p>
	 *
	 * <ul>
	 * <li><strong>Key.name Field:</strong> Updates the BoxLang Key name field with the new FQN</li>
	 * <li><strong>ResolvedFilePath.path Field:</strong> Updates the file path metadata with current values</li>
	 * </ul>
	 *
	 * <h4>Instruction Pattern Matching:</h4>
	 * <p>
	 * The method uses sophisticated bytecode pattern matching to identify and patch
	 * specific instruction sequences. It looks for:
	 * </p>
	 *
	 * <ul>
	 * <li>LDC (Load Constant) instructions loading string literals</li>
	 * <li>INVOKESTATIC calls to Key.of() and ResolvedFilePath.of()</li>
	 * <li>PUTSTATIC instructions storing to specific fields</li>
	 * </ul>
	 *
	 * <p>
	 * <strong>Null Safety:</strong> Handles null values in ResolvedFilePath by converting
	 * them to empty strings, as ASM cannot handle null constants.
	 * </p>
	 *
	 * <p>
	 * <strong>Inner Class Handling:</strong> Only patches metadata for outer classes,
	 * as inner classes inherit their metadata from the outer class.
	 * </p>
	 *
	 * @param classBytes      The original bytecode of the class to transform
	 * @param oldInternalName The original internal class name (e.g., "com/example/MyClass")
	 * @param newInternalName The target internal class name (e.g., "com/example/NewClass")
	 * @param classInfo       Metadata about the class containing current path and FQN information
	 * @param outerClass      true if this is the outer class (patches metadata), false for inner classes
	 *
	 * @return The modified bytecode with class name and metadata updated
	 *
	 * @throws RuntimeException If bytecode manipulation fails
	 *
	 * @see org.objectweb.asm.ClassReader
	 * @see org.objectweb.asm.ClassWriter
	 * @see org.objectweb.asm.commons.ClassRemapper
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
								ldc1.cst	= mappingName != null ? mappingName : "";
								ldc2.cst	= mappingPath != null ? mappingPath : "";
								ldc3.cst	= relativePath != null ? relativePath : "";
								ldc4.cst	= absolutePath != null ? absolutePath : "";
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

	/**
	 * Utility method to extract the fully qualified class name from raw bytecode.
	 *
	 * <p>
	 * This method uses ASM to parse the bytecode and extract the internal class name,
	 * then converts it to the standard Java FQN format by replacing forward slashes with dots.
	 * </p>
	 *
	 * <p>
	 * <strong>Use Cases:</strong>
	 * </p>
	 * <ul>
	 * <li>Debugging bytecode processing</li>
	 * <li>Validation during class loading</li>
	 * <li>Tooling that needs to inspect compiled classes</li>
	 * </ul>
	 *
	 * <p>
	 * <strong>Example:</strong><br>
	 * Bytecode internal name: {@code com/example/MyClass}<br>
	 * Returned FQN: {@code com.example.MyClass}
	 * </p>
	 *
	 * @param classBytes The bytecode of the class to inspect
	 * 
	 * @return The fully qualified class name in dot notation
	 *
	 * @throws RuntimeException If the bytecode cannot be parsed
	 */
	public static String getClassName( byte[] classBytes ) {
		ClassReader	classReader	= new ClassReader( classBytes );
		ClassNode	classNode	= new ClassNode();
		classReader.accept( classNode, 0 );
		return classNode.name.replace( '/', '.' );
	}

}
