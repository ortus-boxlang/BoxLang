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

import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.SessionBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I represent an Application listener. I am the base class for a class-based listner, template-based listener, or default listener
 */
public abstract class ApplicationListener {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	protected Key				appName		= null;

	protected RequestBoxContext	context;

	/**
	 * All Application settings (which are really set per-request). This includes any "expected" ones from the BoxLog core, plus any additional settings
	 * that a module or add-on may be looking for. This also determines default values for all settings.
	 */
	// TODO: allow modules to contribute to the default application settings. Perhaps copy these from the application service
	protected IStruct			settings	= Struct.of(
	    "applicationTimeout", 1,
	    "blockedExtForFileUpload", "",
	    "clientManagement", false,
	    "clientStorage", "",
	    "clientTimeout", 1,
	    "component", "",
	    "componentPaths", new Array(),
	    "customTagPaths", new Array(),
	    "datasource", "",
	    "datasources", new Struct(),
	    "defaultDatasource", "",	// TODO: Move to the compat module.
	    "invokeImplicitAccessor", false,
	    "locale", "en_US",
	    "mails", new Array(),
	    "mappings", Struct.of(),
	    "name", "",
	    "scriptProtect", "none",
	    "secureJson", false,
	    "sessionManagement", false,
	    "sessionStorage", "",
	    "sessionTimeout", 1,
	    "setClientCookies", true,
	    "setDomainCookies", true,
	    "source", "",
	    "timezone", "Etc/UTC",
	    "triggerDataMember", false
	);

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param context The request context
	 */
	public ApplicationListener( RequestBoxContext context ) {
		this.context = context;
		context.setApplicationListener( this );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the settings for this application
	 *
	 * @return The settings for this application
	 */
	public IStruct getSettings() {
		return this.settings;
	}

	/**
	 * Update the settings for this application
	 *
	 * @param settings The settings to update
	 */
	public void updateSettings( IStruct settings ) {
		this.settings.addAll( settings );
		// If the settings have changed, see if the app and session contexts need updated or initialized as well
		defineApplication( context );
	}

	/**
	 * Define the application context
	 *
	 * @param context The request context
	 */
	public void defineApplication( RequestBoxContext context ) {
		String		appNameString	= StringCaster.cast( settings.get( Key._NAME ) );
		Application	thisApp;
		if ( appNameString != null && !appNameString.isEmpty() ) {
			this.appName = Key.of( appNameString );
			// Check for existing app context
			ApplicationBoxContext existingApplicationContext = context.getParentOfType( ApplicationBoxContext.class );
			// If there's none, let's add it
			if ( existingApplicationContext == null ) {
				thisApp = context.getRuntime().getApplicationService().getApplication( Key.of( this.appName ) );
				context.injectTopParentContext( new ApplicationBoxContext( thisApp ) );
				// Only starts the first time
				thisApp.start( context );
				// if there's one, but with a different name, replace it
			} else if ( !existingApplicationContext.getApplication().getName().equals( appName ) ) {
				thisApp = context.getRuntime().getApplicationService().getApplication( Key.of( this.appName ) );
				existingApplicationContext.updateApplication( thisApp );
				thisApp.start( context );
			} else {
				thisApp = existingApplicationContext.getApplication();
			}

			// Check for existing session context
			SessionBoxContext	existingSessionContext		= context.getParentOfType( SessionBoxContext.class );
			boolean				sessionManagementEnabled	= BooleanCaster.cast( settings.get( Key.sessionManagement ) );
			if ( existingSessionContext == null ) {
				// if session management is enabled, add it
				if ( sessionManagementEnabled ) {
					// If there's none, let's add it
					Session thisSession = thisApp.getSession( context.getSessionID() );
					context.injectTopParentContext( new SessionBoxContext( thisSession ) );
					// Only starts the first time
					thisSession.start( context );
				}
			} else {
				if ( sessionManagementEnabled ) {
					// Ensure we have the right session (app name could have changed)
					existingSessionContext.updateSession( thisApp.getSession( context.getSessionID() ) );
					// Only starts the first time
					existingSessionContext.getSession().start( context );
				} else {
					// If session management is disabled, remove it
					context.removeParentContext( SessionBoxContext.class );
				}
			}
		} else {
			// If there's no name, remove the app context
			context.removeParentContext( ApplicationBoxContext.class );
			// also remove any session context
			context.removeParentContext( SessionBoxContext.class );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * Abstract Methods to be implemented by the concrete classes
	 * --------------------------------------------------------------------------
	 */
	abstract public void onRequest( IBoxContext context, Object[] args );

	abstract public boolean onRequestStart( IBoxContext context, Object[] args );

	abstract public void onRequestEnd( IBoxContext context, Object[] args );

	abstract public void onAbort( IBoxContext context, Object[] args );

	// TODO: Rename. We don't support this yet ...
	// abstract public boolean onCFCRequest( IBoxContext context, Object[] args );

	abstract public void onSessionStart( IBoxContext context, Object[] args );

	abstract public void onSessionEnd( IBoxContext context, Object[] args );

	abstract public void onApplicationStart( IBoxContext context, Object[] args );

	abstract public void onApplicationEnd( IBoxContext context, Object[] args );

	abstract public boolean onError( IBoxContext context, Object[] args );

	abstract public boolean onMissingTemplate( IBoxContext context, Object[] args );
}
