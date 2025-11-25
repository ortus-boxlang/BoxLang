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

import java.util.List;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
		BoxRuntime	runtime		= BoxRuntime.getInstance();
		boolean		isInModule	= context.getComponents().length > 0
		    && context.getComponents()[ context.getComponents().length - 1 ].getAsKey( Key._NAME ).equals( Key.component );
		context.pushTemplate( this );
		try {
			// Announcements

			runtime.announce(
			    BoxEvent.PRE_TEMPLATE_INVOKE,
			    () -> Struct.ofNonConcurrent(
			        Key.context, context,
			        Key.template, this,
			        Key.templatePath, this.getRunnablePath()
			    )
			);

			_invoke( context );

		} catch ( AbortException e ) {
			// Module components have their own checks
			if ( isInModule && ( e.isTemplate() || e.isLoop() || e.isTag() ) ) {
				throw e;
			}
			if ( e.isLoop() ) {
				throw new BoxValidationException( "You cannot use the 'loop' method of the exit component outside of a custom tag." );
			}
			// Swallowing aborts here if type="page" and exits of type template
			// Ignoring showerror in case for now
			if ( e.isRequest() ) {
				context.flushBuffer( true );
				throw e;
			}
		} catch ( Throwable e ) {
			context.flushBuffer( false );
			throw e;
		} finally {

			// Announce
			runtime.announce(
			    BoxEvent.POST_TEMPLATE_INVOKE,
			    () -> Struct.ofNonConcurrent(
			        Key.context, context,
			        Key.template, this,
			        Key.templatePath, this.getRunnablePath()
			    )
			);

			context.popTemplate();
		}

	}

	/**
	 * The imports for this runnable
	 */
	public abstract List<ImportDefinition> getImports();

	/**
	 * This method is called by the invoke method, it is the actual implementation of the template
	 *
	 * @param context The context to invoke the template with
	 *
	 */
	public abstract void _invoke( IBoxContext context );

	// ITemplateRunnable implementation methods

	/**
	 * The path to the template
	 */
	public abstract ResolvedFilePath getRunnablePath();
}
