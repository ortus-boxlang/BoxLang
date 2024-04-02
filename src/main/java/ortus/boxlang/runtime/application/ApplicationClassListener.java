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

import java.util.function.Function;

import ortus.boxlang.runtime.context.ClassBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * I represent an Application listener that wraps an Application class instance, delegting to it, where possible and providing default
 * implementations otherwise
 */
public class ApplicationClassListener extends ApplicationListener {

	/**
	 * Application.cfc listener for this request
	 */
	private IClassRunnable listener = null;

	/**
	 * Constructor
	 *
	 * @param listener An Application class instance
	 */
	public ApplicationClassListener( IClassRunnable listener, RequestBoxContext context ) {
		super( context );
		this.listener = listener;
		this.settings
		    .putAll( listener.getThisScope().entrySet().stream().filter( e -> ! ( e.getValue() instanceof Function ) ).collect( BLCollector.toStruct() ) );
		this.settings.put( "source", listener.getRunnablePath() );
		this.settings.put( "component", listener.getRunnablePath() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public void onRequest( IBoxContext context, Object[] args ) {
		if ( listener.getVariablesScope().containsKey( Key.onRequest ) ) {
			listener.dereferenceAndInvoke( context, Key.onRequest, args, false );
		} else {
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
		}
	}

	public boolean onRequestStart( IBoxContext context, Object[] args ) {
		if ( listener.getVariablesScope().containsKey( Key.onRequestStart ) ) {
			Object result = listener.dereferenceAndInvoke( context, Key.onRequestStart, args, false );
			if ( result != null ) {
				return BooleanCaster.cast( result );
			}
			// Null or no return value means true
			return true;
		}
		// Default implementation if there is no Application.cfc or it has no onRequestStart method.
		return true;
	}

	public void onSessionStart( IBoxContext context, Object[] args ) {
		if ( listener.getVariablesScope().containsKey( Key.onSessionStart ) ) {
			listener.dereferenceAndInvoke( context, Key.onSessionStart, args, false );
		}
	}

	public boolean onApplicationStart( IBoxContext context, Object[] args ) {
		if ( listener.getVariablesScope().containsKey( Key.onApplicationStart ) ) {
			return BooleanCaster.cast( listener.dereferenceAndInvoke( context, Key.onApplicationStart, args, false ) );
		}
		return true;
	}

}
