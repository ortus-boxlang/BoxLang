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

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;

import javax.script.ScriptContext;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;

/**
 * This context represents the context of a JSR scripting execution
 */
public class JSRScriptingRequestBoxContext extends ScriptingRequestBoxContext {

	private ScriptContext JSRScriptingContext;

	/**
	 * Creates a new execution context with a parent context
	 *
	 * @param parent The parent context
	 */
	public JSRScriptingRequestBoxContext( IBoxContext parent ) {
		super( parent );
	}

	/**
	 * Creates a new execution context with a parent context, and template
	 *
	 * @param parent The parent context
	 */
	public JSRScriptingRequestBoxContext( IBoxContext parent, URI template ) {
		super( parent, template );
	}

	/**
	 * Creates a new execution context with a template
	 *
	 * @param template The template to use
	 */
	public JSRScriptingRequestBoxContext( URI template ) {
		super( template );
	}

	/**
	 * Creates a new execution context
	 */
	public JSRScriptingRequestBoxContext() {
		super();
	}

	public void setJSRScriptingContext( ScriptContext JSRScriptingContext ) {
		this.JSRScriptingContext = JSRScriptingContext;
		Writer		writer		= JSRScriptingContext.getWriter();
		PrintStream	printStream	= new PrintStream( new WriterOutputStream( writer ), true );
		setOut( printStream );
	}

	/**
	 * Flush the buffer to the output stream
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	@Override
	public IBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		String output;
		for ( StringBuffer buf : buffers ) {
			synchronized ( buf ) {
				output = buf.toString();
				buf.setLength( 0 );
			}
			try {
				JSRScriptingContext.getWriter().write( output );
			} catch ( IOException e ) {
				e.printStackTrace();
			}
		}
		return this;
	}

}
