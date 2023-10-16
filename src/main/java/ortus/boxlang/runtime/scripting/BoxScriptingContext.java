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

public class BoxScriptingContext implements ScriptContext {

	private Bindings		engineScope;
	private Bindings		globalScope;
	private Bindings		requestScope;
	private Writer			errorWriter;
	private Writer			writer;
	private Reader			reader;
	public static final int	REQUEST_SCOPE	= 999;

	public BoxScriptingContext( IBoxContext boxContext ) {
		// The "engine" scope wraps the variables scope. It's "local" to the scripting engine instance
		engineScope		= new BoxScopeBindings( boxContext.getScopeNearby( VariablesScope.name ) );
		// The "request" scope is a custom bindings that wraps the BoxLang scope of the same name
		requestScope	= new BoxScopeBindings( boxContext.getScopeNearby( RequestScope.name ) );
		// the "global" scope wraps the server scope. It's "global" to the scripting engine factory and all engines it creates
		globalScope		= new BoxScopeBindings( boxContext.getScopeNearby( ServerScope.name ) );
		errorWriter		= new PrintWriter( System.err );
		writer			= new PrintWriter( System.out );
		reader			= new InputStreamReader( System.in );
	}

	@Override
	public void setBindings( Bindings bindings, int scope ) {
		if ( scope == ENGINE_SCOPE ) {
			engineScope = bindings;
		} else if ( scope == GLOBAL_SCOPE ) {
			globalScope = bindings;
		} else if ( scope == REQUEST_SCOPE ) {
			requestScope = bindings;
		} else {
			throw new IllegalArgumentException( "Invalid scope value." );
		}
	}

	@Override
	public Bindings getBindings( int scope ) {
		if ( scope == ENGINE_SCOPE ) {
			return engineScope;
		} else if ( scope == GLOBAL_SCOPE ) {
			return globalScope;
		} else if ( scope == REQUEST_SCOPE ) {
			return requestScope;
		} else {
			return null;
		}
	}

	@Override
	public void setAttribute( String name, Object value, int scope ) {
		getBindings( scope ).put( name, value );
	}

	@Override
	public Object getAttribute( String name, int scope ) {
		return getBindings( scope ).get( name );
	}

	@Override
	public Object removeAttribute( String name, int scope ) {
		return getBindings( scope ).remove( name );
	}

	@Override
	public Object getAttribute( String name ) {
		Object value = engineScope.get( name );
		if ( value == null ) {
			value = requestScope.get( name );
			if ( value == null ) {
				value = globalScope.get( name );
			}
		}
		return value;
	}

	@Override
	public int getAttributesScope( String name ) {
		if ( engineScope.containsKey( name ) ) {
			return ENGINE_SCOPE;
		} else if ( globalScope.containsKey( name ) ) {
			return GLOBAL_SCOPE;
		} else if ( requestScope.containsKey( name ) ) {
			return REQUEST_SCOPE;
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
