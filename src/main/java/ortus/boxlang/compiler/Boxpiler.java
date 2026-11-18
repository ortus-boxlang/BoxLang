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
package ortus.boxlang.compiler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.FRTransService;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * The `Boxpiler` class serves as an abstract base implementation of the `IBoxpiler` interface,
 * providing core functionality for compiling BoxLang source code into Java classes. It handles
 * parsing, validation, and compilation of BoxLang scripts, statements, templates, and classes.
 *
 * This class manages class pools for compiled classes, supports caching mechanisms, and ensures
 * that generated classes are stored in a specified directory. It also integrates with the
 * `BoxRuntime` environment for logging, configuration, and transaction management.
 *
 * Key features include:
 * - Parsing BoxLang source code into Abstract Syntax Tree (AST) nodes.
 * - Compiling BoxLang source code into Java classes for execution.
 * - Managing class pools for efficient reuse of compiled classes.
 * - Supporting debug mode to clean up generated class directories on startup.
 * - Handling interface proxies for dynamic runtime behavior.
 * - Providing source map information for debugging and error reporting.
 *
 * This class is designed to be extended by concrete implementations that provide additional
 * functionality or customization for specific use cases.
 */
public abstract class Boxpiler implements IBoxpiler {

	/**
	 * Logger Instance
	 */
	protected static final Logger					logger			= BoxRuntime.getInstance().getLoggingService().getLogger( Boxpiler.class.getSimpleName() );

	/**
	 * Keeps track of the classes we've compiled
	 */
	protected Map<String, Map<String, ClassInfo>>	classPools		= new HashMap<>();

	/**
	 * The transaction service used to track subtransactions
	 */
	protected FRTransService						frTransService	= FRTransService.getInstance( true );

	/**
	 * The disk class util
	 */
	protected DiskClassUtil							diskClassUtil;

	/**
	 * The directory where the generated classes are stored
	 */
	protected Path									classGenerationDirectory;

	/**
	 * The BoxRuntime instance
	 */
	protected BoxRuntime							runtime			= BoxRuntime.getInstance();

	/**
	 * The constructor for the Boxpiler class. It initializes the class generation directory and
	 * sets up the disk class utility. If in debug mode, it cleans out the class generation
	 * directory on startup.
	 */
	public Boxpiler() {
		this.classGenerationDirectory	= Paths.get( this.runtime.getConfiguration().classGenerationDirectory );
		this.diskClassUtil				= new DiskClassUtil( classGenerationDirectory );

		// Create the class generation directory if it doesn't exist
		this.classGenerationDirectory.toFile().mkdirs();

		// If we are in debug mode, let's clean out the class generation directory
		if ( this.runtime.getConfiguration().clearClassFilesOnStartup ) {
			clearClassFiles();
		}
	}

	/**
	 * Wipe all compiled class files from the class generation directory. Any cached
	 * bytecode on disk is removed so the next compile happens fresh.
	 */
	public void clearClassFiles() {
		if ( Files.exists( this.classGenerationDirectory ) ) {
			try {
				logger.debug( "Cleaning out class generation directory: " + classGenerationDirectory );
				FileUtils.cleanDirectory( classGenerationDirectory.toFile() );
			} catch ( IOException e ) {
				// A file may be locked by another process (e.g. a class writer writing a
				// .tmp) — best-effort clean; never fail compilation over it.
				logger.warn( "Failed to clean class generation directory: " + e.getMessage() );
			}
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a class pool by name
	 *
	 * @param classPoolName The name of the class pool
	 *
	 * @return The class pool
	 */
	public Map<String, ClassInfo> getClassPool( String classPoolName ) {
		var result = classPools.get( classPoolName );
		if ( result != null ) {
			return result;
		}
		synchronized ( classPools ) {
			result = classPools.get( classPoolName );
			if ( result != null ) {
				return result;
			}
			var newOne = new HashMap<String, ClassInfo>();
			classPools.put( classPoolName, newOne );
			return newOne;
		}
	}

	/**
	 * Ensure the class info exists in the pool without locking
	 *
	 * @param classPool The class pool to check
	 * @param classInfo The class info to ensure
	 * 
	 * @return The ensured class info.
	 */
	protected ClassInfo ensureClassInfo( Map<String, ClassInfo> classPool, ClassInfo classInfo ) {
		String		name	= classInfo.fqn().toString();
		ClassInfo	ensuredClassInfo;
		if ( ( ensuredClassInfo = classPool.get( name ) ) != null ) {
			return ensuredClassInfo;
		}
		synchronized ( classPool ) {
			if ( ( ensuredClassInfo = classPool.get( name ) ) != null ) {
				return ensuredClassInfo;
			}
			classPool.put( name, classInfo );
			return classInfo;
		}
	}

	/**
	 * Get all class pools
	 *
	 * @return A map of class pools
	 */
	public Map<String, Map<String, ClassInfo>> getClassPools() {
		return classPools;
	}

	/**
	 * Clear page pools
	 */
	public void clearPagePool() {
		var pools = getClassPools();
		synchronized ( pools ) {
			pools.forEach( ( k, v ) -> {
				synchronized ( v ) {
					v.forEach( ( k2, v2 ) -> {
						v2.clearCacheClass();
					} );
					v.clear();
				}
			} );
		}
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param source The source to parse.
	 * @param type   The BoxSourceType of the source.
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parse( String source, BoxSourceType type, Boolean classOrInterface ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL Source Parse", type.name() );
		Parser			parser	= new Parser();
		try {
			return parser.parse( source, type, classOrInterface );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Error compiling source", e );
		} finally {
			frTransService.endTransaction( trans );
		}
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parseOrFail( File file ) {
		return validateParse( parse( file ), file.toString() );
	}

	/**
	 * Parse a file on disk into BoxLang AST nodes. This method will NOT throw an exception if the parse fails.
	 *
	 * @param file The file to parse
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parse( File file ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL File Parse", file.toString() );
		Parser			parser	= new Parser();
		try {
			return parser.parse( file );
		} finally {
			frTransService.endTransaction( trans );
		}
	}

	/**
	 * Parse source text into BoxLang AST nodes. This method will throw an exception if the parse fails.
	 *
	 * @param source The source to parse.
	 * @param type   The BoxSourceType of the source.
	 *
	 * @return The parsed AST nodes and any issues if encountered while parsing.
	 */
	@Override
	public ParsingResult parseOrFail( String source, BoxSourceType type, Boolean classOrInterface ) {
		return validateParse( parse( source, type, classOrInterface ), "ad-hoc source" );
	}

	/**
	 * Validate a parsing result and throw an exception if the parse failed.
	 *
	 * @param result The parsing result to validate
	 *
	 * @return The parsing result if the parse was successful
	 */
	@Override
	public ParsingResult validateParse( ParsingResult result, String source ) {
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), source );
		}
		return result;
	}

	/**
	 * Compile a single BoxLang statement into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileStatement( String source, BoxSourceType type ) {
		ClassInfo	classInfo	= ClassInfo.forStatement( source, type, this );
		var			classPool	= getClassPool( classInfo.classPoolName() );
		classInfo = ensureClassInfo( classPool, classInfo );

		return classInfo.getDiskClass();

	}

	/**
	 * Compile a BoxLang script into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 * @param type   The type of BoxLang source code
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileScript( String source, BoxSourceType type ) {
		ClassInfo	classInfo	= ClassInfo.forScript( source, type, this );
		var			classPool	= getClassPool( classInfo.classPoolName() );
		classInfo = ensureClassInfo( classPool, classInfo );
		return classInfo.getDiskClass();
	}

	/**
	 * Compile a BoxLang template (file on disk) into a Java class
	 *
	 * @param resolvedFilePath The BoxLang source code as a Path on disk
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileTemplate( ResolvedFilePath resolvedFilePath ) {
		ClassInfo	classInfo			= ClassInfo.forTemplate( resolvedFilePath, Parser.detectFile( resolvedFilePath.absolutePath().toFile(), true ), this );
		var			classPool			= getClassPool( classInfo.classPoolName() );
		ClassInfo	ensuredClassInfo	= ensureClassInfo( classPool, classInfo );
		// If the new class is newer than the one on disk, recompile it
		long		lastModified		= ensuredClassInfo.lastModified();
		long		lastModified2		= classInfo.lastModified();
		// This needs to be tested at decision time since the setting may have changed in the runtime since the compiler was created
		Boolean		trustedCache		= runtime.getConfiguration().trustedCache;
		if ( ( lastModified > 0 ) && ( lastModified2 > 0 ) && !trustedCache && ( lastModified != lastModified2 ) ) {
			// Double check lock using the class name as the lockn. This ensures only one thread recompiles a class at a time
			String internedFQN = classInfo.fqn().toString().intern();
			synchronized ( internedFQN ) {

				lastModified	= ensuredClassInfo.lastModified();
				lastModified2	= classInfo.lastModified();
				if ( ( lastModified > 0 ) && ( lastModified2 > 0 ) && !trustedCache && ( lastModified != lastModified2 ) ) {
					// Close the stale loader before recompiling. Match the prior behavior exactly: close
					// without nulling the discarded ClassInfo's reference (it is replaced in the pool
					// below). Guard for resolve-only loaders (e.g. Android's) which are not Closeable.
					ClassLoader staleLoader = ensuredClassInfo.getClassLoader();
					if ( staleLoader instanceof Closeable closeable ) {
						try {
							closeable.close();
						} catch ( IOException e ) {
							e.printStackTrace();
						}
					}
					try {
						// Mark the class info instance as not ready to use yet
						classInfo.startCompiling();
						classPool.put( classInfo.fqn().toString(), classInfo );
						compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );
					} finally {
						// It's ready now
						classInfo.doneCompiling();
					}
				} else {
					classInfo = ensuredClassInfo;
				}
			}
		} else {
			classInfo = ensuredClassInfo;
		}
		// This will block if the class info is still being compiled
		return classInfo.getDiskClass();
	}

	/**
	 * Compile a BoxLang Class from source into a Java class
	 *
	 * @param source The BoxLang source code as a string
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileClass( String source, BoxSourceType type ) {
		ClassInfo	classInfo	= ClassInfo.forClass( source, type, this );
		var			classPool	= getClassPool( classInfo.classPoolName() );
		classInfo = ensureClassInfo( classPool, classInfo );

		return classInfo.getDiskClass();
	}

	/**
	 * Compile a BoxLang Class from a file into a Java class
	 *
	 * @param resolvedFilePath The BoxLang source code as a Path on disk
	 *
	 * @return The loaded class
	 */
	@Override
	public Class<IBoxRunnable> compileClass( ResolvedFilePath resolvedFilePath ) {
		// Try hard to avoid creating a new ClassInfo instance by finding an existing one first
		ClassInfo	classInfo	= findClassInfo( resolvedFilePath );
		ClassInfo	ensuredClassInfo;
		long		lastModifiedCurrent;

		// Ok, we give up. Now we create a full ClassInfo instance.
		if ( classInfo == null ) {
			classInfo			= ClassInfo.forClass( resolvedFilePath, Parser.detectFile( resolvedFilePath.absolutePath().toFile(), true ), this );
			// This date will be fresh since we just created the ClassInfo.
			lastModifiedCurrent	= classInfo.lastModified();
			ensuredClassInfo	= ensureClassInfo( getClassPool( classInfo.classPoolName() ), classInfo );
		} else {
			// The classInfo may be cached, but get a fresh modified date
			lastModifiedCurrent	= classInfo.getFreshLastModified();
			ensuredClassInfo	= classInfo;
		}

		// If the new class is newer than the one on disk, recompile it
		@SuppressWarnings( "null" )
		long					lastModifiedPrevious	= ensuredClassInfo.lastModified();
		Map<String, ClassInfo>	classPool				= getClassPool( classInfo.classPoolName() );
		// This needs to be tested at decision time since the setting may have changed in the runtime since the compiler was created
		Boolean					trustedCache			= runtime.getConfiguration().trustedCache;
		if ( ( lastModifiedPrevious > 0 ) && ( lastModifiedCurrent > 0 ) && !trustedCache && ( lastModifiedPrevious != lastModifiedCurrent ) ) {
			// Double check lock using the class name as the lock. This ensures only one thread recompiles a class at a time
			String internedFQN = classInfo.fqn().toString().intern();
			synchronized ( internedFQN ) {

				lastModifiedPrevious	= ensuredClassInfo.lastModified();
				lastModifiedCurrent		= classInfo.getFreshLastModified();
				if ( ( lastModifiedPrevious > 0 ) && ( lastModifiedCurrent > 0 ) && !trustedCache && ( lastModifiedPrevious != lastModifiedCurrent ) ) {
					ensuredClassInfo.shutdownClassLoader();
					// Get a fresh class info since we may have had a used one from above with an outdated modified date and class loader
					classInfo = ClassInfo.forClass( resolvedFilePath, Parser.detectFile( resolvedFilePath.absolutePath().toFile(), true ), this );
					try {
						// Mark the class info instance as not ready to use yet
						classInfo.startCompiling();
						classPool.put( classInfo.fqn().toString(), classInfo );
						compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );
					} finally {
						// It's ready now
						classInfo.doneCompiling();
					}
				} else {
					classInfo = ensuredClassInfo;
				}
			}
		} else {
			classInfo = ensuredClassInfo;
		}
		// This will block if the class info is still being compiled
		return classInfo.getDiskClass();
	}

	/**
	 * Look for an already compiled ClassInfo for a resolved file path in its mapping's class pool.
	 * <p>
	 * The pool is keyed by the normalized FQN, so a single hash lookup answers this whenever a file is reached the same
	 * way it was compiled. The fallback scan covers entries living under a different key, such as a file that was first
	 * pooled as a template.
	 *
	 * @param resolvedFilePath The resolved file path to find
	 *
	 * @return The pooled ClassInfo, or null if this path has not been compiled yet
	 */
	protected ClassInfo findClassInfo( ResolvedFilePath resolvedFilePath ) {
		// Files without a mapping path are not pooled by path
		if ( resolvedFilePath.mappingPath() == null ) {
			return null;
		}
		Map<String, ClassInfo>	classPool	= getClassPool( resolvedFilePath.mappingPath() );
		// This is the FQN the class was registered under when it was compiled
		ClassInfo				classInfo	= classPool.get( resolvedFilePath.getFQN( "boxgenerated.boxclass" ).toString() );
		if ( classInfo != null ) {
			return classInfo;
		}
		// We're not looping directly over the values() collection as the internal iterator can give ConcurrentModificationExceptions
		// Pre-size with headroom to prevent AIOOBE if another thread adds entries during toArray()
		ClassInfo[] snapshot = classPool.values().toArray( new ClassInfo[ classPool.size() + 50 ] );
		for ( ClassInfo entry : snapshot ) {
			if ( entry == null ) {
				break;
			}
			if ( entry.resolvedFilePath() != null && entry.resolvedFilePath().equals( resolvedFilePath ) ) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public Class<IProxyRunnable> compileInterfaceProxy( IBoxContext context, InterfaceProxyDefinition definition ) {
		ClassInfo	classInfo	= ClassInfo.forInterfaceProxy( definition.name(), definition, this );
		var			classPool	= getClassPool( classInfo.classPoolName() );
		classInfo = ensureClassInfo( classPool, classInfo );

		return classInfo.getDiskClassProxy();

	}

	@Override
	public SourceMap getSourceMapFromFQN( String FQN ) {
		// loop over classPools entry set and find one that has a value with the FQN as the key
		String classPoolName = null;
		for ( Map.Entry<String, Map<String, ClassInfo>> entry : classPools.entrySet() ) {
			if ( entry.getValue().containsKey( FQN ) ) {
				classPoolName = entry.getKey();
				break;
			}
		}
		if ( classPoolName == null ) {
			return null;
		}

		return diskClassUtil.readLineNumbers( classPoolName, IBoxpiler.getBaseFQN( FQN ) );
	}

	@Override
	public List<byte[]> compileTemplateBytes( ResolvedFilePath resolvedFilePath ) {
		Path		path		= resolvedFilePath.absolutePath();
		ClassInfo	classInfo	= null;
		// file extension is .bx or .cfc
		if ( path.toString().endsWith( ".bx" ) || path.toString().endsWith( ".cfc" ) ) {
			classInfo = ClassInfo.forClass( resolvedFilePath, Parser.detectFile( path.toFile() ), this );
		} else {
			classInfo = ClassInfo.forTemplate( resolvedFilePath, Parser.detectFile( path.toFile() ), this );
		}
		var classPool = getClassPool( classInfo.classPoolName() );
		classInfo = ensureClassInfo( classPool, classInfo );
		return compileClassInfo( classInfo.classPoolName(), classInfo.fqn().toString() );
	}

}
