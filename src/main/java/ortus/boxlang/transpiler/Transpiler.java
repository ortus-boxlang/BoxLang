package ortus.boxlang.transpiler;

import com.github.javaparser.ast.Node;
import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxExpression;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Transpiler Base class
 */
public abstract class Transpiler implements ITranspiler {

	private final HashMap properties = new HashMap<String, String>();

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

			scriptRunnable.invoke( context );

		} catch ( Throwable e ) {
			throw e;
		}

	}

}
