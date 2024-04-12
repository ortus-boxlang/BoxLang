package ortus.boxlang.compiler;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyDefinition;
import ortus.boxlang.runtime.loader.DiskClassLoader;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A Record that represents the information about a class to be compiled
 */
public record ClassInfo(
    String sourcePath,
    String packageName,
    String className,
    String boxPackageName,
    String baseclass,
    String returnType,
    BoxSourceType sourceType,
    String source,
    Path path,
    Long lastModified,
    DiskClassLoader[] diskClassLoader,
    InterfaceProxyDefinition interfaceProxyDefinition,
    IBoxpiler boxpiler ) {

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
		    null,
		    "boxgenerated.scripts",
		    "Script_" + IBoxpiler.MD5( sourceType.toString() + source ),
		    "",
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler
		);
	}

	public static ClassInfo forStatement( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    null,
		    "boxgenerated.scripts",
		    "Statement_" + IBoxpiler.MD5( sourceType.toString() + source ),
		    "",
		    "BoxScript",
		    "Object",
		    sourceType,
		    source,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler
		);
	}

	public static ClassInfo forTemplate( Path path, String packagePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		File	lcaseFile	= new File( packagePath.toString().toLowerCase() );
		String	packageName	= IBoxpiler.getPackageName( lcaseFile );
		// if package name has starting dot, remove it
		if ( packageName.startsWith( "." ) ) {
			packageName = packageName.substring( 1 );
		}
		packageName = "boxgenerated.templates" + ( packageName.equals( "" ) ? "" : "." ) + packageName;
		String className = IBoxpiler.getClassName( lcaseFile );
		return new ClassInfo(
		    path.toString(),
		    packageName,
		    className,
		    packageName,
		    "BoxTemplate",
		    "void",
		    sourceType,
		    null,
		    path,
		    path.toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler
		);
	}

	public static ClassInfo forClass( Path path, String packagePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		String boxPackagePath = packagePath;
		if ( boxPackagePath.endsWith( "." ) ) {
			boxPackagePath = boxPackagePath.substring( 0, boxPackagePath.length() - 1 );
		}
		packagePath = "boxgenerated.boxclass." + packagePath;
		// trim trailing period
		if ( packagePath.endsWith( "." ) ) {
			packagePath = packagePath.substring( 0, packagePath.length() - 1 );
		}
		String className = IBoxpiler.getClassName( path.toFile() );

		return new ClassInfo(
		    path.toString(),
		    packagePath,
		    className,
		    boxPackagePath,
		    null,
		    null,
		    sourceType,
		    null,
		    path,
		    path.toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler
		);
	}

	public static ClassInfo forClass( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    null,
		    "boxgenerated.boxclass",
		    "Class_" + IBoxpiler.MD5( source ),
		    "",
		    null,
		    null,
		    sourceType,
		    source,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler
		);
	}

	public static ClassInfo forInterfaceProxy( String name, InterfaceProxyDefinition interfaceProxyDefinition, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    null,
		    "boxgenerated.dynamicProxy",
		    "InterfaceProxy_" + name,
		    "",
		    null,
		    null,
		    null,
		    null,
		    null,
		    0L,
		    new DiskClassLoader[ 1 ],
		    interfaceProxyDefinition,
		    boxpiler
		);
	}

	public String FQN() {
		return packageName + "." + className();
	}

	public String toString() {
		if ( sourcePath != null )
			return "Class Info-- sourcePath: [" + sourcePath + "], packageName: [" + packageName + "], className: [" + className + "]";
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
			    Paths.get( BoxRuntime.getInstance().getConfiguration().compiler.classGenerationDirectory ),
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
	 * Get a Box class for a class name from disk
	 *
	 * @return The loaded class
	 */
	@SuppressWarnings( "unchecked" )
	public Class<IClassRunnable> getDiskClassClass() {
		try {
			return ( Class<IClassRunnable> ) getClassLoader().loadClass( FQN() );
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

}
