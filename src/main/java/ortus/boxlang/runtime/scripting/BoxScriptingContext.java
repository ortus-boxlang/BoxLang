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

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import javax.script.Bindings;
import javax.script.ScriptContext;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * The BoxScriptingContext is an implementation of the JSR-223 ScriptContext
 * interface that provides a context for executing BoxLang code.
 *
 * @see ScriptContext
 */
public class BoxScriptingContext implements ScriptContext {

	private Bindings		engineScope;
	private Bindings		globalScope;
	private Bindings		requestScope;
	private Writer			errorWriter;
	private Writer			writer;
	private Reader			reader;

	/**
	 * The BoxLang request scope that doesn't exist in the traditional script contexts
	 */
	public static final int	REQUEST_SCOPE	= 999;

	/**
	 * Constructor for the BoxScriptingContext
	 *
	 * @param boxContext The BoxContext this context is associated with
	 */
	public BoxScriptingContext( IBoxContext boxContext ) {
		// The "engine" scope wraps the variables scope. It's "local" to the scripting engine instance
		this.engineScope	= new BoxScopeBindings( boxContext.getScopeNearby( VariablesScope.name ) );
		// The "request" scope is a custom bindings that wraps the BoxLang scope of the same name
		this.requestScope	= new BoxScopeBindings( boxContext.getScopeNearby( RequestScope.name ) );
		// the "global" scope wraps the server scope. It's "global" to the scripting engine factory and all engines it creates
		this.globalScope	= new BoxScopeBindings( boxContext.getScopeNearby( ServerScope.name ) );
		this.errorWriter	= new PrintWriter( System.err );
		this.writer			= new PrintWriter( System.out );
		this.reader			= new InputStreamReader( System.in );
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
		switch ( scope ) {
			case ENGINE_SCOPE :
				this.engineScope.putAll( bindings );
				break;
			case GLOBAL_SCOPE :
				this.globalScope.putAll( bindings );
				break;
			case REQUEST_SCOPE :
				this.requestScope.putAll( bindings );
				break;
			default :
				throw new IllegalArgumentException( "Invalid scope value [" + scope + "]. Valid scopes are ENGINE_SCOPE, GLOBAL_SCOPE, and REQUEST_SCOPE" );
		}
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
		if ( scope == ENGINE_SCOPE ) {
			return this.engineScope;
		} else if ( scope == GLOBAL_SCOPE ) {
			return this.globalScope;
		} else if ( scope == REQUEST_SCOPE ) {
			return this.requestScope;
		} else {
			return null;
		}
	}

	/**
	 * Set the attribute for the given scope
	 *
	 * @param name  The name of the attribute
	 * @param value The value of the attribute
	 * @param scope The scope to set the attribute for
	 */
	@Override
	public void setAttribute( String name, Object value, int scope ) {
		getBindings( scope ).put( name, value );
	}

	/**
	 * Get the attribute for the given scope
	 *
	 * @param name  The name of the attribute
	 * @param scope The scope to get the attribute for
	 *
	 * @return The attribute value if found, else null
	 */
	@Override
	public Object getAttribute( String name, int scope ) {
		return getBindings( scope ).get( name );
	}

	/**
	 * Remove the attribute for the given scope
	 *
	 * @param name  The name of the attribute
	 * @param scope The scope to remove the attribute for
	 *
	 * @return The attribute value if found, else null
	 */
	@Override
	public Object removeAttribute( String name, int scope ) {
		return getBindings( scope ).remove( name );
	}

	/**
	 * Get the attribute for the given scope by testing all scopes in order: engine, request, global
	 *
	 * @param name The name of the attribute
	 *
	 * @return The attribute value if found, else null
	 */
	@Override
	public Object getAttribute( String name ) {
		Object value = this.engineScope.get( name );
		if ( value == null ) {
			value = requestScope.get( name );
			if ( value == null ) {
				value = globalScope.get( name );
			}
		}
		return value;
	}

	/**
	 * Get the scope of the attribute by testing all scopes in order: engine, request, global
	 * If the attribute is not found in any scope, return -1
	 * If the attribute is found in the engine scope, return ENGINE_SCOPE
	 * If the attribute is found in the request scope, return REQUEST_SCOPE
	 * If the attribute is found in the global scope, return GLOBAL_SCOPE
	 *
	 * @param name The name of the attribute
	 *
	 * @return The scope of the attribute if found, else -1
	 */
	@Override
	public int getAttributesScope( String name ) {
		if ( engineScope.containsKey( name ) ) {
			return ENGINE_SCOPE;
		} else if ( requestScope.containsKey( name ) ) {
			return REQUEST_SCOPE;
		} else if ( globalScope.containsKey( name ) ) {
			return GLOBAL_SCOPE;
		} else {
			return -1;
		}
	}

	@Override
	public Writer getWriter() {
		return writer;
	}

	@Override
	public Writer getErrorWriter() {
		return errorWriter;
	}

	@Override
	public void setWriter( Writer writer ) {
		this.writer = writer;
	}

	@Override
	public void setErrorWriter( Writer writer ) {
		this.errorWriter = writer;
	}

	@Override
	public Reader getReader() {
		return reader;
	}

	@Override
	public void setReader( Reader reader ) {
		this.reader = reader;
	}

	@Override
	public List<Integer> getScopes() {
		return Arrays.asList( ENGINE_SCOPE, GLOBAL_SCOPE, REQUEST_SCOPE );
	}

}
