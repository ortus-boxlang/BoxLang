package ortus.boxlang.compiler;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.loader.DiskClassLoader;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FQN;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * A Record that represents the information about a class to be compiled
 */
public record ClassInfo(
    FQN packageName,
    String className,
    FQN boxPackageName,
    String baseclass,
    String returnType,
    BoxSourceType sourceType,
    String source,
    Long lastModified,
    DiskClassLoader[] diskClassLoader,
    InterfaceProxyDefinition interfaceProxyDefinition,
    IBoxpiler boxpiler,
    ResolvedFilePath resolvedFilePath ) {

	/**
	 * Hash Code
	 */
	public int hashCode() {
		return FQN().hashCode();
	}

	/**
	 * Equals
	 */
	public boolean equals( Object obj ) {
		if ( obj == null ) {
			return false;
		}
		if ( obj instanceof ClassInfo ) {
			return FQN().equals( ( ( ClassInfo ) obj ).FQN() );
		}
		return false;
	}

	public static ClassInfo forScript( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.scripts" ),
		    "Script_" + IBoxpiler.MD5( sourceType.toString() + source ),
		    FQN.of( "" ),
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null
		);
	}

	public static ClassInfo forStatement( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.scripts" ),
		    "Statement_" + IBoxpiler.MD5( sourceType.toString() + source ),
		    FQN.of( "" ),
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null
		);
	}

	public static ClassInfo forTemplate( ResolvedFilePath resolvedFilePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		File	lcaseFile	= new File( resolvedFilePath.absolutePath().toString().toLowerCase() );
		String	className	= IBoxpiler.getClassName( lcaseFile );
		FQN		packageName	= resolvedFilePath.getPackage( "boxgenerated.templates" );
		return new ClassInfo(
		    packageName,
		    className,
		    packageName,
		    "BoxTemplate",
		    "void",
		    sourceType,
		    null,
		    Math.min( resolvedFilePath.absolutePath().toFile().lastModified(), System.currentTimeMillis() ),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath
		);
	}

	public static ClassInfo forClass( ResolvedFilePath resolvedFilePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		String className = IBoxpiler.getClassName( resolvedFilePath.absolutePath().toFile() );
		return new ClassInfo(
		    resolvedFilePath.getPackage( "boxgenerated.boxclass" ),
		    className,
		    resolvedFilePath.getPackage(),
		    null,
		    null,
		    sourceType,
		    null,
		    Math.min( resolvedFilePath.absolutePath().toFile().lastModified(), System.currentTimeMillis() ),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath
		);
	}

	public static ClassInfo forClass( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.boxclass" ),
		    "Class_" + IBoxpiler.MD5( source ),
		    FQN.of( "" ),
		    null,
		    null,
		    sourceType,
		    source,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    null
		);
	}

	public static ClassInfo forInterfaceProxy( String name, InterfaceProxyDefinition interfaceProxyDefinition, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.dynamicProxy" ),
		    "InterfaceProxy_" + name,
		    FQN.of( "" ),
		    null,
		    null,
		    null,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    interfaceProxyDefinition,
		    boxpiler,
		    null
		);
	}

	public String FQN() {
		return packageName.toString() + "." + className();
	}

	public String toString() {
		if ( resolvedFilePath != null )
			return "Class Info-- sourcePath: [" + resolvedFilePath.absolutePath().toString() + "], packageName: [" + packageName + "], className: [" + className
			    + "]";
		else if ( sourceType != null )
			return "Class Info-- type: [" + sourceType + "], packageName: [" + packageName + "], className: [" + className + "]";
		else if ( interfaceProxyDefinition != null )
			return "Class Info-- interface proxy: [" + interfaceProxyDefinition.interfaces().toString() + "],  packageName: [" + packageName
			    + "], className: [" + className + "]";
		else
			return "Class Info-- packageName: [" + packageName + "], className: [" + className + "]";
	}

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
			    boxpiler()
			);
			return diskClassLoader[ 0 ];
		}
	}

	/**
	 * Get a class for a class name from disk
	 *
	 * @return The loaded class
	 */
	@SuppressWarnings( "unchecked" )
	public Class<IBoxRunnable> getDiskClass() {
		try {
			return ( Class<IBoxRunnable> ) getClassLoader().loadClass( FQN() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + FQN(), e );
		}
	}

	/**
	 * Get a proxy class for a class name from disk
	 *
	 * @return The loaded class
	 */
	@SuppressWarnings( "unchecked" )
	public Class<IProxyRunnable> getDiskClassProxy() {
		try {
			return ( Class<IProxyRunnable> ) getClassLoader().loadClass( FQN() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + FQN(), e );
		}
	}

	public Boolean isClass() {
		return packageName().toString().startsWith( "boxgenerated.boxclass" );
	}

}
