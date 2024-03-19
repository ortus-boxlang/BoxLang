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
package ortus.boxlang.transpiler;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxScript;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transpiler Base class
 */
public abstract class Transpiler implements ITranspiler {

	private final HashMap<String, String>	properties					= new HashMap<String, String>();
	private int								tryCatchCounter				= 0;
	private int								switchCounter				= 0;
	private int								forInCounter				= 0;
	private int								lambdaCounter				= 0;
	private int								closureCounter				= 0;
	private int								lambdaContextCounter		= 0;
	private int								componentCounter			= 0;
	private int								componentOptionalCounter	= 0;
	private int								functionBodyCounter			= 0;
	private ArrayDeque<String>				currentContextName			= new ArrayDeque<>();
	private List<ImportDefinition>			imports						= new ArrayList<ImportDefinition>();
	private Map<String, BoxExpr>			keys						= new LinkedHashMap<String, BoxExpr>();

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
	public static Transpiler getTranspiler() {
		return new JavaTranspiler();
	}

	@Override
	public abstract TranspiledCode transpile( BoxNode node ) throws BoxRuntimeException;

	/**
	 * Utility method to transform a node
	 *
	 * @param node a BoxLang AST Node
	 *
	 * @return a JavaParser AST Node
	 *
	 * @throws IllegalStateException
	 */
	public Node transform( BoxNode node ) throws IllegalStateException {
		return this.transform( node, TransformerContext.NONE );
	}

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
			Class<?>		boxClass				= Class.forName( fqn, true, classLoader );
			Method			method					= boxClass.getDeclaredMethod( "getInstance" );
			BoxScript		scriptRunnable			= ( BoxScript ) method.invoke( boxClass );

			BoxRuntime		instance				= BoxRuntime.getInstance( true );
			IBoxContext		context					= new ScriptingRequestBoxContext( instance.getRuntimeContext() );

			Object			result					= scriptRunnable.invoke( context );
			// System.out.println( result );

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

	public int incrementAndGetForInCounter() {
		return ++forInCounter;
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
		return imports.stream().anyMatch( i -> token.equalsIgnoreCase( i.alias() ) || token.equalsIgnoreCase( i.className() ) );
	}

	public int incrementAndGetLambdaCounter() {
		return ++lambdaCounter;
	}

	public int incrementAndGetClosureCounter() {
		return ++closureCounter;
	}

	public int incrementAndGetLambdaContextCounter() {
		return ++lambdaContextCounter;
	}

	public int incrementAndGetComponentOptionalCounter() {
		return ++componentOptionalCounter;
	}

	public void pushComponent() {
		componentCounter++;
	}

	public void popComponent() {
		componentCounter--;
	}

	public int getComponentCounter() {
		return componentCounter;
	}

	public void setComponentCounter( int componentCounter ) {
		this.componentCounter = componentCounter;
	}

	public boolean isInsideComponent() {
		return componentCounter > 0;
	}

	public void pushfunctionBodyCounter() {
		functionBodyCounter++;
	}

	public void popfunctionBodyCounter() {
		functionBodyCounter--;
	}

	public boolean canReturn() {
		if ( functionBodyCounter > 0 ) {
			return true;
		}
		String returnType = getProperty( "returnType" );
		if ( returnType != null && !returnType.equals( "void" ) ) {
			return true;
		}
		return false;
	}

	public int registerKey( BoxExpr key ) {
		String name;
		if ( key instanceof BoxStringLiteral str ) {
			name = str.getValue();
		} else if ( key instanceof BoxIntegerLiteral intr ) {
			name = intr.getValue();
		} else {
			throw new IllegalStateException( "Key must be a string or integer literal" );
		}
		// check if exists
		if ( keys.keySet().contains( name ) ) {
			return new ArrayList<>( keys.keySet() ).indexOf( name );
		}
		keys.put( name, key );
		return keys.size() - 1;
	}

	public Map<String, BoxExpr> getKeys() {
		return keys;
	}

	public String getDateTime( LocalDateTime locaTime ) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern( "yyyy-MM-dd'T'hh:mm:ss" );

		return "LocalDateTime.parse(\"" + formatter.format( locaTime ) + "\")";
	}
}
