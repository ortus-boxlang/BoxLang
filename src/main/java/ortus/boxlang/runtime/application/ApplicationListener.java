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
package ortus.boxlang.runtime.application;

import ortus.boxlang.runtime.context.ClassBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.AbortException;

/**
 * I represent an Application listener. I wrap a potential Application.cfc instance, delegting to it, where possible and providing default
 * implementations otherwise
 */
public class ApplicationListener {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Application.cfc listener for this request
	 * null if there is none
	 */
	IClassRunnable listener = null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param listener An Application class instance
	 */
	public ApplicationListener( IClassRunnable listener ) {
		this.listener = listener;
	}

	/**
	 * Constructor
	 *
	 */
	public ApplicationListener() {
	}

	public IClassRunnable getListener() {
		return listener;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public void onRequest( IBoxContext context, Object[] args ) {
		if ( listener != null && listener.getVariablesScope().containsKey( Key.onRequest ) ) {
			System.out.println( "onRequest defined in application listener: " + args[ 0 ] );
			listener.dereferenceAndInvoke( context, Key.onRequest, args, false );
		} else if ( listener != null ) {
			// Default includes template inside the CFC's context
			ClassBoxContext cbc = new ClassBoxContext( context, listener );
			try {
				cbc.includeTemplate( ( String ) args[ 0 ] );
				cbc.flushBuffer( false );
			} catch ( AbortException e ) {
				cbc.flushBuffer( true );
				throw e;
			} catch ( Throwable e ) {
				cbc.flushBuffer( true );
				throw e;
			} finally {
				cbc.flushBuffer( true );
			}
			System.out.println( "including file in application listener context: " + args[ 0 ] );
		} else {
			System.out.println( "including file in current context: " + args[ 0 ] + " " + context.getClass().getName() );
			context.includeTemplate( ( String ) args[ 0 ] );
		}

	}

	public boolean onRequestStart( IBoxContext context, Object[] args ) {
		if ( listener != null && listener.getVariablesScope().containsKey( Key.onRequestStart ) ) {
			return BooleanCaster.cast( listener.dereferenceAndInvoke( context, Key.onRequestStart, args, false ) );
		}
		// Default implmentation if there is no Application.cfc or it has no onRequestStart method.
		System.out.println( "default onRequestStart: " + args[ 0 ] );
		return true;
	}

	public void onSessionStart( IBoxContext context, Object[] args ) {
		if ( listener != null && listener.getVariablesScope().containsKey( Key.onSessionStart ) ) {
			listener.dereferenceAndInvoke( context, Key.onSessionStart, args, false );
		}
	}

	public boolean onApplicationStart( IBoxContext context, Object[] args ) {
		if ( listener != null && listener.getVariablesScope().containsKey( Key.onApplicationStart ) ) {
			return BooleanCaster.cast( listener.dereferenceAndInvoke( context, Key.onApplicationStart, args, false ) );
		}
		return true;
	}
}
