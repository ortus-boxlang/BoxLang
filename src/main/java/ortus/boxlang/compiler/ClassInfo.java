package ortus.boxlang.compiler;

import java.net.URL;
import java.nio.file.Paths;

import ortus.boxlang.compiler.parser.BoxSourceType;
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
    ResolvedFilePath resolvedFilePath ) {

	/**
	 * Hash Code
	 */
	public int hashCode() {
		return fqn().toString().hashCode();
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

	public static ClassInfo forScript( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.scripts", "Script_" + IBoxpiler.MD5( sourceType.toString() + source ) ),
		    BoxFQN.of( "" ),
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
		    FQN.of( "boxgenerated.scripts", "Statement_" + IBoxpiler.MD5( sourceType.toString() + source ) ),
		    BoxFQN.of( "" ),
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
		return new ClassInfo(
		    resolvedFilePath.getFQN( "boxgenerated.templates" ),
		    resolvedFilePath.getBoxFQN( "boxgenerated.templates" ),
		    "BoxTemplate",
		    "void",
		    sourceType,
		    null,
		    resolvedFilePath.absolutePath().toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath
		);
	}

	public static ClassInfo forClass( ResolvedFilePath resolvedFilePath, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    resolvedFilePath.getFQN( "boxgenerated.boxclass" ),
		    resolvedFilePath.getBoxFQN(),
		    null,
		    null,
		    sourceType,
		    null,
		    resolvedFilePath.absolutePath().toFile().lastModified(),
		    new DiskClassLoader[ 1 ],
		    null,
		    boxpiler,
		    resolvedFilePath
		);
	}

	public static ClassInfo forClass( String source, BoxSourceType sourceType, IBoxpiler boxpiler ) {
		return new ClassInfo(
		    FQN.of( "boxgenerated.boxclass", "Class_" + IBoxpiler.MD5( source ) ),
		    BoxFQN.of( "" ),
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
		    FQN.of( "boxgenerated.dynamicProxy", "InterfaceProxy_" + name ),
		    BoxFQN.of( "" ),
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

	public String toString() {
		if ( resolvedFilePath != null )
			return "Class Info-- sourcePath: [" + resolvedFilePath.absolutePath().toString() + "], fqn: [" + fqn().toString() + "]";
		else if ( sourceType != null )
			return "Class Info-- type: [" + sourceType + "], fqn: [" + fqn().toString() + "]";
		else if ( interfaceProxyDefinition != null )
			return "Class Info-- interface proxy: [" + interfaceProxyDefinition.interfaces().toString() + "],  fqn: [" + fqn().toString()
			    + "]";
		else
			return "Class Info-- fqn: [" + fqn().toString() + "]";
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
			    boxpiler(),
			    classPoolName()
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
			return ( Class<IBoxRunnable> ) getClassLoader().loadClass( fqn().toString() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn().toString(), e );
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
			return ( Class<IProxyRunnable> ) getClassLoader().loadClass( fqn().toString() );
		} catch ( ClassNotFoundException e ) {
			throw new BoxRuntimeException( "Error compiling source " + fqn().toString(), e );
		}
	}

	public String packageName() {
		return fqn().getPackageString();
	}

	public String className() {
		return fqn().getClassName();
	}

	public Boolean isClass() {
		return packageName().toString().startsWith( "boxgenerated.boxclass" );
	}

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

}
