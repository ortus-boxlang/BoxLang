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

import java.nio.file.Path;
import java.time.LocalDateTime;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Struct;

public abstract class BoxTemplate implements ITemplateRunnable {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invoke a BoxLang template
	 *
	 * @param context The context to invoke the template with
	 *
	 */
	public void invoke( IBoxContext context ) {
		BoxRuntime runtime = BoxRuntime.getInstance();

		context.pushTemplate( this );
		try {
			// Announcements
			Struct data = Struct.of(
			    "context", context,
			    "template", this,
			    "templatePath", this.getRunnablePath()
			);
			runtime.announce( "preTemplateInvoke", data );

			_invoke( context );

			// Announce
			runtime.announce( "postTemplateInvoke", data );
		} finally {
			context.popTemplate();
		}

	}

	/**
	 * This method is called by the invoke method, it is the actual implementation of the template
	 *
	 * @param context The context to invoke the template with
	 *
	 */
	public abstract void _invoke( IBoxContext context );

	// ITemplateRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public abstract long getRunnableCompileVersion();

	/**
	 * The date the template was compiled
	 */
	public abstract LocalDateTime getRunnableCompiledOn();

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public abstract Object getRunnableAST();

	/**
	 * The path to the template
	 */
	public abstract Path getRunnablePath();
}
