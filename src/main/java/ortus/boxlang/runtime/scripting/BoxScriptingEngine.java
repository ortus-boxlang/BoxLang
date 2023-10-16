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

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;

public class BoxScriptingEngine implements ScriptEngine, Compilable {

	private IBoxContext			boxContext;
	private BoxScriptingFactory	boxScriptingFactory;
	private BoxRuntime			boxRuntime;
	private ScriptContext		scriptContext;

	public BoxScriptingEngine( BoxScriptingFactory boxScriptingFactory ) {
		this.boxScriptingFactory	= boxScriptingFactory;
		this.boxContext				= new ScriptingBoxContext( BoxRuntime.getInstance().getRuntimeContext() );
		this.boxRuntime				= BoxRuntime.getInstance();
		this.scriptContext			= new BoxScriptingContext( boxContext );
	}

	public Object eval( String script, ScriptContext context ) throws ScriptException {
		scriptContext = context;
		return eval( script );
	}

	public Object eval( Reader reader, ScriptContext context ) throws ScriptException {
		scriptContext = context;
		return eval( reader );
	}

	public Object eval( String script ) throws ScriptException {
		return boxRuntime.executeStatement( script, boxContext );
	}

	public Object eval( Reader reader ) throws ScriptException {
		return eval( reader.toString() );
	}

	public Bindings createBindings() {
		return new SimpleBindings();
	}

	public ScriptContext getContext() {
		return scriptContext;
	}

	public void setContext( ScriptContext context ) {
		scriptContext = context;
	}

	public ScriptEngineFactory getFactory() {
		return boxScriptingFactory;
	}

	@Override
	public Object eval( String script, Bindings n ) throws ScriptException {
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( script );
	}

	@Override
	public Object eval( Reader reader, Bindings n ) throws ScriptException {
		setBindings( n, ScriptContext.ENGINE_SCOPE );
		return eval( reader );
	}

	@Override
	public void put( String key, Object value ) {
		getBindings( ScriptContext.ENGINE_SCOPE ).put( key, value );
	}

	@Override
	public Object get( String key ) {
		return getBindings( ScriptContext.ENGINE_SCOPE ).get( key );
	}

	@Override
	public Bindings getBindings( int scope ) {
		return scriptContext.getBindings( scope );
	}

	@Override
	public void setBindings( Bindings bindings, int scope ) {
		scriptContext.setBindings( bindings, scope );

	}

	@Override
	public CompiledScript compile( String script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script ) );
	}

	@Override
	public CompiledScript compile( Reader script ) throws ScriptException {
		return new BoxCompiledScript( this, RunnableLoader.getInstance().loadStatement( script.toString() ) );
	}

	// get boxcontext
	public IBoxContext getBoxContext() {
		return boxContext;
	}
}