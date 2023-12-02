package ortus.boxlang.transpiler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transpiler Base class
 */
public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String>	properties			= new HashMap<String, String>();
	private int								tryCatchCounter		= 0;
	private int								switchCounter		= 0;
	private ArrayDeque<String>				currentContextName	= new ArrayDeque<>();
	private List<ImportDefinition>			imports				= new ArrayList<ImportDefinition>();

	/**
	 * Set a property
	 *
	 * @param key   key of the Property
	 * @param value value of the Property
	 */
	public void setProperty( String key, String value ) {
		properties.put( key, value );
	}

	/**
	 * Get a Propoerty
	 *
	 * @param key key of the Property
	 *
	 * @return the value of the property or null if not defined
	 */
	public String getProperty( String key ) {
		return ( String ) properties.get( key );
	}

	/**
	 * Returns a instance of the transpiler
	 *
	 * @param config configuration object
	 *
	 * @return a subclass of Transpiler
	 */
	public static Transpiler getTranspiler( Configuration config ) {
		return new JavaTranspiler();
	}

	@Override
	public abstract TranspiledCode transpile( BoxNode node ) throws ApplicationException;

	public abstract Node transform( BoxNode node );

	public abstract Node transform( BoxNode node, TransformerContext context );

	@Override
	public void run( String fqn, List<String> classPath ) throws Throwable {
		List<URL> finalClassPath = new ArrayList<>();
		for ( String path : classPath ) {
			try {
				finalClassPath.add( new File( path ).toURI().toURL() );

			} catch ( MalformedURLException e ) {
				throw new RuntimeException( e );
			}
		}

		try {
			URL[]			classLoaderClassPath	= finalClassPath.toArray( new URL[ 0 ] );
			URLClassLoader	classLoader				= new URLClassLoader(
			    classLoaderClassPath,
			    this.getClass().getClassLoader()
			);
			Class			boxClass				= Class.forName( fqn, true, classLoader );
			Method			method					= boxClass.getDeclaredMethod( "getInstance" );
			BoxScript		scriptRunnable			= ( BoxScript ) method.invoke( boxClass );

			BoxRuntime		instance				= BoxRuntime.getInstance( true );
			IBoxContext		context					= new ScriptingBoxContext( instance.getRuntimeContext() );
			IScope			variables				= context.getScopeNearby( VariablesScope.name );

			Object			result					= scriptRunnable.invoke( context );
			System.out.println( result );

		} catch ( Throwable e ) {
			throw e;
		}

	}

	public int incrementAndGetTryCatchCounter() {
		return ++tryCatchCounter;
	}

	public int incrementAndGetSwitchCounter() {
		return ++switchCounter;
	}

	public void pushContextName( String name ) {
		currentContextName.push( name );
	}

	public String popContextName() {
		return currentContextName.pop();
	}

	public String peekContextName() {
		return currentContextName.peek();
	}

	public void addImport( String importString ) {
		imports.add( ImportDefinition.parse( importString ) );
	}

	public boolean matchesImport( String token ) {
		/*
		 * Not supporting
		 * - java:System
		 * - java:java.lang.System
		 * - java.lang.System
		 * 
		 * right now, just
		 * 
		 * - System
		 * 
		 * as all the other options require grammar changes or are more complicated to recognize
		 */
		return imports.stream().anyMatch( i -> {
			if ( token.equalsIgnoreCase( i.alias() ) ) {
				return true;
			}

			String	className		= i.className();
			int		lastDotIndex	= className.lastIndexOf( "." );
			if ( lastDotIndex != -1 ) {
				className = className.substring( lastDotIndex + 1 );
			}

			if ( token.equalsIgnoreCase( className ) ) {
				return true;
			}

			return false;
		} );
	}

}
