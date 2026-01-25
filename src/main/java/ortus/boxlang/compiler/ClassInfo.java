package ortus.boxlang.compiler;

import java.net.URL;
import java.nio.file.Paths;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.loader.DiskClassLoader;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.BoxFQN;
import ortus.boxlang.runtime.util.FQN;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * A Record that represents the information about a class to be compiled
 */
public record ClassInfo(
    FQN fqn,
    BoxFQN boxFqn,
    String baseclass,
    String returnType,
    BoxSourceType sourceType,
    String source,
    long lastModified,
    DiskClassLoader[] diskClassLoader,
    InterfaceProxyDefinition interfaceProxyDefinition,
    IBoxpiler boxpiler,
    ResolvedFilePath resolvedFilePath,
    int _hashCode,
    boolean[] compiling ) {

	/**
	 * Hash Code
	 */
	public int hashCode() {
		return _hashCode;
	}

	/**
	 * Equals
	 */
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( obj instanceof ClassInfo ) {
			return fqn().toString().equals( ( ( ClassInfo ) obj ).fqn().toString() );
		}
		return false;
	}

	/**
	 * Create a ClassInfo for a BoxLang script from source code.
	 *
	 * @param source     The source code of the script
	 * @param sourceType The type of the source (BoxLang, CFML, etc.)
	 * @param boxpiler   The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the script
	 */
	public static ClassInfo forScript( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		FQN fqn = FQN.of( "boxgenerated.scripts", "Script_" + IBoxpiler.MD5( sourceType.toString() + source ) );
		return new ClassInfo(
		    fqn,
		    BoxFQN.of( "" ),
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Create a ClassInfo for a BoxLang statement from source code.
	 *
	 * @param source     The source code of the statement
	 * @param sourceType The type of the source (BoxLang, CFML, etc.)
	 * @param boxpiler   The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the statement
	 */
	public static ClassInfo forStatement( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		FQN fqn = FQN.of( "boxgenerated.scripts", "Statement_" + IBoxpiler.MD5( sourceType.toString() + source ) );
		return new ClassInfo(
		    fqn,
		    BoxFQN.of( "" ),
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Create a ClassInfo for a BoxLang template from a file on disk.
	 *
	 * @param resolvedFilePath The resolved file path to the template
	 * @param sourceType       The type of the source (BoxLang, CFML, etc.)
	 * @param boxpiler         The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the template
	 */
	public static ClassInfo forTemplate( ResolvedFilePath resolvedFilePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		FQN fqn = resolvedFilePath.getFQN( "boxgenerated.templates" );
		return new ClassInfo(
		    fqn,
		    resolvedFilePath.getBoxFQN( "boxgenerated.templates" ),
		    "BoxTemplate",
		    "void",
		    sourceType,
		    null,
		    isTrustedCache() ? 0 : resolvedFilePath.absolutePath().toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Create a ClassInfo for a BoxLang class from a file on disk.
	 *
	 * @param resolvedFilePath The resolved file path to the class file
	 * @param sourceType       The type of the source (BoxLang, CFML, etc.)
	 * @param boxpiler         The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the class
	 */
	public static ClassInfo forClass( ResolvedFilePath resolvedFilePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		FQN fqn = resolvedFilePath.getFQN( "boxgenerated.boxclass" );
		return new ClassInfo(
		    fqn,
		    resolvedFilePath.getBoxFQN(),
		    null,
		    null,
		    sourceType,
		    null,
		    isTrustedCache() ? 0 : resolvedFilePath.absolutePath().toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Create a ClassInfo for a BoxLang class from source code.
	 *
	 * @param source     The source code of the class
	 * @param sourceType The type of the source (BoxLang, CFML, etc.)
	 * @param boxpiler   The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the class
	 */
	public static ClassInfo forClass( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		FQN fqn = FQN.of( "boxgenerated.boxclass", "Class_" + IBoxpiler.MD5( source ) );
		return new ClassInfo(
		    fqn,
		    BoxFQN.of( "" ),
		    null,
		    null,
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Create a ClassInfo for an interface proxy.
	 *
	 * @param name                     The name of the proxy
	 * @param interfaceProxyDefinition The definition of the interface proxy
	 * @param boxpiler                 The boxpiler instance to use for compilation
	 *
	 * @return A new ClassInfo instance for the interface proxy
	 */
	public static ClassInfo forInterfaceProxy( String name, InterfaceProxyDefinition interfaceProxyDefinition, IBoxpiler boxpiler ) {
		FQN fqn = FQN.of( "boxgenerated.dynamicProxy", "InterfaceProxy_" + name );
		return new ClassInfo(
		    fqn,
		    BoxFQN.of( "" ),
		    null,
		    null,
		    null,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    interfaceProxyDefinition,
		    boxpiler,
		    null,
		    fqn.toString().hashCode(),
		    new boolean[] { false }
		);
	}

	/**
	 * Get a string representation of this ClassInfo.
	 *
	 * @return A string describing this ClassInfo
	 */
	public String toString() {
		if ( resolvedFilePath != null )
			return "Class Info-- sourcePath: [" + resolvedFilePath.absolutePath().toString() + "], fqn: [" + fqn().toString() + "]";
		else if ( sourceType() != null )
			return "Class Info-- type: [" + sourceType() + "], fqn: [" + fqn().toString() + "]";
		else if ( interfaceProxyDefinition != null )
			return "Class Info-- interface proxy: [" + interfaceProxyDefinition.interfaces().toString() + "],  fqn: [" + fqn().toString()
			    + "]";
		else
			return "Class Info-- fqn: [" + fqn().toString() + "]";
	}

	/**
	 * Get or create the DiskClassLoader for this class.
	 * Uses double-checked locking to ensure thread-safe lazy initialization.
	 *
	 * @return The DiskClassLoader for this class
	 */
	public DiskClassLoader getClassLoader() {
		if ( diskClassLoader[ 0 ] != null ) {
			return diskClassLoader[ 0 ];
		}
		synchronized ( this ) {
			if ( diskClassLoader[ 0 ] != null ) {
				return diskClassLoader[ 0 ];
			}
			diskClassLoader[ 0 ] = new DiskClassLoader(
			    new URL[] {},
			    boxpiler().getClass().getClassLoader(),
			    Paths.get( BoxRuntime.getInstance().getConfiguration().classGenerationDirectory ),
			    boxpiler(),
			    classPoolName()
			);
			return diskClassLoader[ 0 ];
		}
	}

	/**
	 * Get a class for a class name from disk.
	 * Will block if the class is currently being compiled.
	 *
	 * @return The loaded class
	 */
	@SuppressWarnings( "unchecked" )
	public Class<IBoxRunnable> getDiskClass() {
		waitWhileCompiling();
		try {
			return ( Class<IBoxRunnable> ) getClassLoader().loadClass( fqn().toString() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn().toString(), e );
		}
	}

	/**
	 * Mark this class as currently being compiled.
	 * Any calls to getDiskClass() or getDiskClassProxy() will block until doneCompiling() is called.
	 */
	public void startCompiling() {
		synchronized ( this ) {
			compiling[ 0 ] = true;
		}
	}

	/**
	 * Mark this class as done compiling.
	 * Wakes up any threads waiting in getDiskClass() or getDiskClassProxy().
	 */
	public void doneCompiling() {
		synchronized ( this ) {
			compiling[ 0 ] = false;
			this.notifyAll();
		}
	}

	/**
	 * Check if currently compiling
	 *
	 * @return true if compiling
	 */
	public boolean isCompiling() {
		return compiling[ 0 ];
	}

	/**
	 * Wait while the class is being compiled.
	 * Uses double-checked locking to avoid synchronization when not compiling.
	 */
	private void waitWhileCompiling() {
		if ( !isCompiling() ) {
			return;
		}
		synchronized ( this ) {
			while ( isCompiling() ) {
				try {
					this.wait();
				} catch ( InterruptedException e ) {
					Thread.currentThread().interrupt();
					throw new BoxRuntimeException( "Interrupted while waiting for class compilation", e );
				}
			}
		}
	}

	/**
	 * Get a proxy class for a class name from disk.
	 * Will block if the class is currently being compiled.
	 *
	 * @return The loaded class
	 */
	@SuppressWarnings( "unchecked" )
	public Class<IProxyRunnable> getDiskClassProxy() {
		waitWhileCompiling();
		try {
			return ( Class<IProxyRunnable> ) getClassLoader().loadClass( fqn().toString() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn().toString(), e );
		}
	}

	/**
	 * Get the package name for this class.
	 *
	 * @return The package name as a string
	 */
	public String packageName() {
		return fqn().getPackageString();
	}

	/**
	 * Get the class name (without package) for this class.
	 *
	 * @return The class name as a string
	 */
	public String className() {
		return fqn().getClassName();
	}

	/**
	 * Check if this ClassInfo represents a BoxLang class (as opposed to a template or script).
	 *
	 * @return true if this is a BoxLang class, false otherwise
	 */
	public Boolean isClass() {
		return packageName().toString().startsWith( "boxgenerated.boxclass" );
	}

	/**
	 * Get the class pool name for this class info
	 * 
	 * @return The class pool name
	 */
	public String classPoolName() {
		if ( resolvedFilePath() != null ) {
			if ( resolvedFilePath().mappingPath() != null ) {
				return resolvedFilePath().mappingPath().toString();
			} else {
				return "__empty_mapping__";
			}
		} else {
			return "__ad_hoc_source__";
		}
	}

	/**
	 * Check if we are using a trusted cache
	 * 
	 * @return true if we are using a trusted cache
	 */
	public static boolean isTrustedCache() {
		return BoxRuntime.getInstance().getConfiguration().trustedCache;
	}

	/**
	 * Get the source type, loading from disk if necessary
	 * 
	 * @return The source type
	 */
	public BoxSourceType sourceType() {
		if ( sourceType != null ) {
			return sourceType;
		}
		// This is loaded on demand for classes which require inspection on disk to decide the type (.cfc)
		return Parser.detectFile( resolvedFilePath.absolutePath().toFile() );
	}

	/**
	 * Clears the in-memory cache of loaded classes.
	 */
	public void clearCacheClass() {
		if ( diskClassLoader[ 0 ] != null ) {
			diskClassLoader[ 0 ].clearClassesCache();
		}
	}
}
