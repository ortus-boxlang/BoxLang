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
package ortus.boxlang.runtime.scripting;

import java.io.Reader;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;

/**
 * The BoxScriptingEngine is the JSR-223 implementation for BoxLang. It is the
 * entry point for executing BoxLang code on the JVM.
 *
 * @see ScriptEngine
 */
public class BoxScriptingEngine implements ScriptEngine, Compilable, Invocable {

	private JSRScriptingRequestBoxContext	boxContext;
	private BoxScriptingFactory				boxScriptingFactory;
	private BoxRuntime						boxRuntime;
	private ScriptContext					scriptContext;

	/**
	 * Constructor for the BoxScriptingEngine
	 *
	 * @param boxScriptingFactory The factory for the BoxScriptingEngine
	 * @param debug               Whether to run in debug mode, defaults to false
	 *
	 * @see BoxScriptingFactory
	 */
	public BoxScriptingEngine( BoxScriptingFactory boxScriptingFactory, Boolean debug ) {
		this.boxScriptingFactory	= boxScriptingFactory;
		this.boxContext				= new JSRScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );
		this.scriptContext			= new BoxScriptingContext( boxContext );
		boxContext.setJSRScriptingContext( this.scriptContext );
		this.boxRuntime = BoxRuntime.getInstance( debug );
	}

	/**
	 * Constructor for the BoxScriptingEngine
	 *
	 * @param boxScriptingFactory The factory for the BoxScriptingEngine
	 *
	 * @see BoxScriptingFactory
	 */
	public BoxScriptingEngine( BoxScriptingFactory boxScriptingFactory ) {
		this( boxScriptingFactory, false );
	}

	/**
	 * Get the BoxRuntime for the BoxScriptingEngine
	 *
	 * @return The BoxRuntime for the BoxScriptingEngine
	 */
	public BoxRuntime getRuntime() {
		return this.boxRuntime;
	}

	/**
	 * Evaluate a script in the context of the ScriptContext
	 *
	 * @param script  The script to evaluate
	 * @param context The context to evaluate the script in
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( String script, ScriptContext context ) throws ScriptException {
		this.scriptContext = context;
		return eval( script );
	}

	/**
	 * Evaluate a script in the context of the ScriptContext
	 *
	 * @param reader  The reader to read the script from
	 * @param context The context to evaluate the script in
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( Reader reader, ScriptContext context ) throws ScriptException {
		this.scriptContext = context;
		return eval( reader );
	}

	/**
	 * Evaluate a script bound only to the top-level BoxRuntime context
	 *
	 * @param reader The reader to read the script from
	 *
	 * @return The result of the script evaluation
	 */
	public Object eval( Reader reader ) throws ScriptException {
		return eval( reader.toString() );
	}

	/**
	 * Evaluate a script using the given Bindings
	 *
	 * @param script The script to evaluate
	 * @param n      The Bindings to use
	 *
	 * @return The result of the script evaluation
	 */
	@Override
	public Object eval( String script, Bindings n ) throws ScriptException {
		// Seed the bindings into the engine scope = variables scope
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( script );
	}

	@Override
	public Object eval( Reader reader, Bindings n ) throws ScriptException {
		// Seed the bindings into the engine scope = variables scope
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( reader );
	}

	/**
	 * Evaluate a script bound only to the top-level BoxRuntime context
	 *
	 * @param script The script to evaluate
	 *
	 * @return The buffer from the BoxContext
	 */
	public Object eval( String script ) throws ScriptException {
		return boxRuntime.executeStatement( script, this.boxContext );
	}

	/**
	 * Create a new Bindings object
	 *
	 * @return A new Bindings object
	 */
	public Bindings createBindings() {
		return new SimpleBindings();
	}

	/**
	 * Create a new Bindings object with the given map
	 *
	 * @param m The map to seed the Bindings with
	 *
	 * @return A new Bindings object with the given map
	 */
	public Bindings creatBindings( Map<String, Object> m ) {
		return new SimpleBindings( m );
	}

	@Override
	public void put( String key, Object value ) {
		getBindings( ScriptContext.ENGINE_SCOPE ).put( key, value );
	}

	@Override
	public Object get( String key ) {
		return getBindings( ScriptContext.ENGINE_SCOPE ).get( key );
	}

	/**
	 * Get the defaults bindings which is the variables scope = engine scope
	 *
	 * @return The bindings for the given scope if found, else null
	 */
	public Bindings getBindings() {
		return this.scriptContext.getBindings( ScriptContext.ENGINE_SCOPE );
	}

	/**
	 * Helper method to get the request scope bindings
	 */
	public Bindings getRequestBindings() {
		return this.scriptContext.getBindings( BoxScriptingContext.REQUEST_SCOPE );
	}

	/**
	 * Helper method to get the server scope bindings
	 */
	public Bindings getServerBindings() {
		return this.scriptContext.getBindings( ScriptContext.GLOBAL_SCOPE );
	}

	/**
	 * Get the bindings for the given scope
	 *
	 * @param scope The scope to get the bindings for
	 *
	 * @return The bindings for the given scope if found, else null
	 */
	@Override
	public Bindings getBindings( int scope ) {
		return this.scriptContext.getBindings( scope );
	}

	/**
	 * Set the bindings for the given scope
	 *
	 * @param bindings The bindings to set
	 * @param scope    The scope to set the bindings for
	 *
	 * @throws IllegalArgumentException If the scope is invalid
	 */
	@Override
	public void setBindings( Bindings bindings, int scope ) {
		this.scriptContext.setBindings( bindings, scope );
	}

	/**
	 * Get the ScriptContext for the BoxScriptingEngine
	 *
	 * @return The ScriptContext for the BoxScriptingEngine
	 */
	public ScriptContext getContext() {
		return this.scriptContext;
	}

	/**
	 * Set the ScriptContext for the BoxScriptingEngine
	 *
	 * @param context The ScriptContext to set
	 */
	public void setContext( ScriptContext context ) {
		this.scriptContext = context;
	}

	@Override
	public ScriptEngineFactory getFactory() {
		return this.boxScriptingFactory;
	}

	/**
	 * Compile a script
	 *
	 * @param script The script to compile
	 *
	 * @return The compiled script
	 */
	@Override
	public CompiledScript compile( String script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script ) );
	}

	/**
	 * Compile a script
	 *
	 * @param script The script to compile
	 *
	 * @return The compiled script
	 *
	 */
	@Override
	public CompiledScript compile( Reader script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script.toString() ) );
	}

	/**
	 * Get the BoxContext for the BoxScriptingEngine
	 *
	 * @return The BoxContext for the BoxScriptingEngine
	 */
	public JSRScriptingRequestBoxContext getBoxContext() {
		return this.boxContext;
	}

	/**
	 * This is used when you eval a script that is a BoxLang object definition.
	 *
	 * @param thiz The object to invoke the method on
	 * @param name The name of the method to invoke
	 * @param args The positional arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 *
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 */
	@Override
	public Object invokeMethod( Object thiz, String name, Object... args ) throws ScriptException, NoSuchMethodException {

		if ( thiz == null ) {
			throw new ScriptException( "Cannot invoke method on null object" );
		}

		// This will handle any sort of referencable object, including member methods on data types
		return Referencer.getAndInvoke( getBoxContext(), thiz, Key.of( name ), args, false );
	}

	/**
	 * This is used when you eval a script that is a BoxLang function definition, so you can invoke it.
	 *
	 * @param name The name of the function to invoke
	 * @param args The positional arguments to pass to the function
	 *
	 * @return The result of the function invocation
	 *
	 * @throws ScriptException
	 * @throws NoSuchMethodException
	 */
	@Override
	public Object invokeFunction( String name, Object... args ) throws ScriptException, NoSuchMethodException {
		return boxContext.invokeFunction( Key.of( name ), args );
	}

	/**
	 * Returns an implementation of an interface using functions compiled in the interpreter.
	 *
	 * @param clasz The interface to create the dynamic proxy
	 *
	 * @return An implementation of the interface using functions compiled in the interpreter
	 */
	@Override
	public <T> T getInterface( Class<T> clasz ) {
		return buildGenericProxy( getBindings( ScriptContext.ENGINE_SCOPE ), clasz );
	}

	/**
	 * Builds a dynamic proxy from the passed in object that maps to the given interface.
	 *
	 * @param thiz  The object to create the proxy from. This can be a BoxLang object, structure, or a Map or function.
	 * @param clasz The interface to create the dynamic proxy
	 *
	 * @return An implementation of the interface using functions compiled in the interpreter
	 */
	@SuppressWarnings( "unchecked" )
	@Override
	public <T> T getInterface( Object thiz, Class<T> clasz ) {
		if ( thiz instanceof IClassRunnable icr ) {
			return ( T ) InterfaceProxyService.createProxy( getBoxContext(), icr, Array.of( clasz ) );
		} else if ( thiz instanceof Map<?, ?> map ) {
			return buildGenericProxy( map, clasz );
		}
		return null;
	}

	/**
	 * Returns an implementation of an interface using functions compiled in the interpreter.
	 *
	 * @param map   The map to use as the basis for the proxy which represents the bindings and function to map
	 * @param clasz The interface to create the dynamic proxy
	 *
	 * @return An implementation of the interface using functions compiled in the interpreter
	 */
	@SuppressWarnings( "unchecked" )
	private <T> T buildGenericProxy( Map<?, ?> map, Class<T> clasz ) {
		// Create a dummy Box Class
		IClassRunnable dummyBoxClass = ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
		    class {}
		    """, getBoxContext(), BoxSourceType.BOXSCRIPT )
		)
		    .invokeConstructor( getBoxContext() )
		    .getTargetInstance();

		// Populate it with all the variables in our current binding
		dummyBoxClass.getVariablesScope().addAll( map );
		dummyBoxClass.getThisScope().addAll( map );

		return ( T ) InterfaceProxyService.createProxy( getBoxContext(), dummyBoxClass, Array.of( clasz ) );
	}
}
