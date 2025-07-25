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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I represent an Application listener that wraps an Application template
 */
public class ApplicationTemplateListener extends BaseApplicationListener {

	/**
	 * Application.cfm listener for this request
	 */
	private BoxTemplate listener = null;

	/**
	 * Constructor
	 *
	 * @param listener An Application class instance
	 */
	public ApplicationTemplateListener( BoxTemplate listener, RequestBoxContext context, ResolvedFilePath baseTemplatePath ) {
		super( context, baseTemplatePath );
		this.listener = listener;
		// Store the template path in the settings map
		this.settings.put( Key.source, listener.getRunnablePath().absolutePath().toString() );
		this.settings.put( Key._CLASS, listener.getRunnablePath().absolutePath().toString() );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	@Override
	public void onRequest( IBoxContext context, Object[] args ) {
		super.onRequest( context, args );
		// Then include the requested template
		context.includeTemplate( ( String ) args[ 0 ], true );
	}

	@Override
	public boolean onRequestStart( IBoxContext context, Object[] args ) {
		super.onRequestStart( context, args );
		// Run Application template
		listener.invoke( context );
		return true;
	}

	@Override
	public void onSessionStart( IBoxContext context, Object[] args ) {
		super.onSessionStart( context, args );
	}

	@Override
	public void onApplicationStart( IBoxContext context, Object[] args ) {
		super.onApplicationStart( context, args );
	}

	@Override
	public void onRequestEnd( IBoxContext context, Object[] args ) {
		super.onRequestEnd( context, args );
	}

	@Override
	public void onAbort( IBoxContext context, Object[] args ) {
		super.onAbort( context, args );
	}

	@Override
	public void onSessionEnd( IBoxContext context, Object[] args ) {
		super.onSessionEnd( context, args );
	}

	@Override
	public void onApplicationEnd( IBoxContext context, Object[] args ) {
		super.onApplicationEnd( context, args );
	}

	@Override
	public boolean onError( IBoxContext context, Object[] args ) {
		super.onError( context, args );
		return false;
	}

	@Override
	public boolean onMissingTemplate( IBoxContext context, Object[] args ) {
		super.onMissingTemplate( context, args );
		return false;
	}

	@Override
	public void onClassRequest( IBoxContext context, Object[] args ) {
		super.onClassRequestSimple( context, args );
	}

}
