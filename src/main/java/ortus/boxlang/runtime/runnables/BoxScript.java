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
package ortus.boxlang.runtime.runnables;

import java.time.LocalDateTime;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Struct;

public abstract class BoxScript implements IScriptRunnable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The source to the script
	 */
	protected String		source;

	/**
	 * The version of the runtime that compiled this class
	 */
	protected long			compileVersion;

	/**
	 * The date the source was compiled
	 */
	protected LocalDateTime	compiledOn;

	/**
	 * The AST of the source
	 */
	protected Object		ast;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invoke a BoxLang source
	 *
	 * @param context The context to invoke the source with
	 *
	 */
	public Object invoke( IBoxContext context ) {
		InterceptorService	interceptorService	= InterceptorService.getInstance();

		// Announcements
		Struct				data				= Struct.of(
		    "context", context,
		    "source", this
		);
		interceptorService.announce( "preSourceInvoke", data );
		try {
			return _invoke( context );
		} finally {
			interceptorService.announce( "postSourceInvoke", data );
		}

	}

	/**
	 * This method is called by the invoke method, it is the actual implementation of the source
	 *
	 * @param context The context to invoke the source with
	 *
	 */
	public abstract Object _invoke( IBoxContext context );

	// ISourceRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return this.compileVersion;
	}

	/**
	 * The date the source was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return this.compiledOn;
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return this.ast;
	}

	/**
	 * The source to the script
	 */
	public String getRunnableSource() {
		return this.source;
	}

}
