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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.SessionBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I represent an Application listener. I am the base class for a class-based listner, template-based listener, or default listener
 */
public abstract class BaseApplicationListener {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The application name
	 */
	protected Key				appName		= null;

	/**
	 * The application linked to this listener
	 */
	protected Application		application;

	/**
	 * The request context bound to this listener
	 */
	protected RequestBoxContext	context;

	/**
	 * All Application settings (which are really set per-request). This includes any "expected" ones from the BoxLog core, plus any additional settings
	 * that a module or add-on may be looking for. This also determines default values for all settings.
	 */
	protected IStruct			settings	= Struct.of(
	    "applicationTimeout", 1,
	    "clientManagement", false,
	    "clientStorage", "cookie",
	    "clientTimeout", 1,
	    "component", "",
	    "componentPaths", new Array(),
	    "customTagPaths", new Array(),
	    "datasource", "",
	    "datasources", new Struct(),
	    "defaultDatasource", "",
	    "invokeImplicitAccessor", false,
	    "javaSettings", Struct.of(
	        "loadPaths", new Array(),
	        "loadSystemClassPath", false,
	        "reloadOnChange", false
	    ),
	    "locale", BoxRuntime.getInstance().getConfiguration().runtime.locale.toString(),
	    "mappings", Struct.of(),
	    "name", "",
	    "secureJson", false,
	    "secureJsonPrefix", "",
	    "sessionManagement", false,
	    "sessionStorage", "memory",
	    "sessionTimeout", 1,
	    "clientTimeout", 20,
	    "setClientCookies", true,
	    "setDomainCookies", true,
	    "source", "",
	    "timezone", BoxRuntime.getInstance().getConfiguration().runtime.timezone.getId(),
	    "triggerDataMember", false
	);

	/**
	 * Logger
	 */
	private static final Logger	logger		= LoggerFactory.getLogger( BaseApplicationListener.class );

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
	protected BaseApplicationListener( RequestBoxContext context ) {
		this.context = context;
		context.setApplicationListener( this );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the (constructed and parsed) application name. Can only be used after the application has been defined.
	 */
	public Key getAppName() {
		return this.appName;
	}

	/**
	 * Get the linked application
	 *
	 * @return The linked application
	 */
	public Application getApplication() {
		return this.application;
	}

	/**
	 * Verifies if the application is defined or not
	 *
	 * @return true if the application is defined, false otherwise
	 */
	public boolean isApplicationDefined() {
		return this.application != null;
	}

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
		defineApplication();
	}

	/**
	 * Define the application context
	 */
	public void defineApplication() {
		String appNameString = StringCaster.cast( settings.get( Key._NAME ) );

		// Only create it if we have a name
		if ( appNameString != null && !appNameString.isEmpty() ) {
			this.appName = Key.of( appNameString );
			// Check for existing app context
			ApplicationBoxContext existingApplicationContext = context.getParentOfType( ApplicationBoxContext.class );

			// If there's none, then this creates a new application
			if ( existingApplicationContext == null ) {
				this.application = context.getRuntime().getApplicationService().getApplication( this.appName );
				context.injectTopParentContext( new ApplicationBoxContext( this.application ) );
				// Only starts the first time
				try {
					this.application.start( context );
				} catch ( Throwable e ) {
					// Note this will remove the application even if the user has an abort;
					// which means you basically can't start the app if you are aborting inside of it
					// Since this is most likely the case of testing, it's probably ok.
					context.getRuntime().getApplicationService().removeApplication( this.appName );
					throw e;
				}
				// if there's one, but with a different name, replace it
			} else if ( !existingApplicationContext.getApplication().getName().equals( appName ) ) {
				this.application = context.getRuntime().getApplicationService().getApplication( this.appName );
				existingApplicationContext.updateApplication( this.application );
				this.application.start( context );
			} else {
				this.application = existingApplicationContext.getApplication();
			}

			// Check for existing session context
			SessionBoxContext	existingSessionContext		= context.getParentOfType( SessionBoxContext.class );
			boolean				sessionManagementEnabled	= BooleanCaster.cast( settings.get( Key.sessionManagement ) );
			if ( existingSessionContext == null ) {
				// if session management is enabled, add it
				if ( sessionManagementEnabled ) {
					initializeSession( context.getSessionID() );
				}
			} else {
				if ( sessionManagementEnabled ) {
					// Ensure we have the right session (app name could have changed)
					existingSessionContext.updateSession( this.application.getSession( context.getSessionID() ) );
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
	 * Session related methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Rotate a session
	 */
	public void rotateSession() {
		SessionBoxContext sessionContext = context.getParentOfType( SessionBoxContext.class );
		if ( sessionContext != null ) {
			Session			existing		= sessionContext.getSession();
			SessionScope	existingScope	= existing.getSessionScope();
			context.resetSession();
			sessionContext = context.getParentOfType( SessionBoxContext.class );
			SessionScope newScope = sessionContext.getSession().getSessionScope();
			// Transfer existing keys which were added to the scope
			existingScope.entrySet().stream().forEach( entry -> newScope.putIfAbsent( entry.getKey(), entry.getValue() ) );
		}
	}

	/**
	 * Invalidate a session
	 *
	 * @param newID The new session identifier
	 */
	public void invalidateSession( Key newID ) {
		Session terminalSession = context.getParentOfType( SessionBoxContext.class ).getSession();
		context.getParentOfType( ApplicationBoxContext.class ).getApplication().getSessionsCache().clearQuiet( terminalSession.getID().getName() );
		terminalSession.shutdown();
		initializeSession( newID );
	}

	/**
	 * Intializes a new session
	 *
	 * @param newID The new session identifier
	 */
	public void initializeSession( Key newID ) {
		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Session					newSession	= appContext.getApplication().getSession( newID );
		context.removeParentContext( SessionBoxContext.class );
		context.injectTopParentContext( new SessionBoxContext( newSession ) );
		newSession.start( context );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life-Cycle Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Handle the onRequest event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onRequest( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onRequest,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onRequestStart event
	 *
	 * @param context The context
	 * @param args    The arguments
	 *
	 * @return true if the request should continue, false otherwise
	 */
	public boolean onRequestStart( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onRequestStart,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);

		return true;
	}

	/**
	 * Handle the onRequestEnd event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onRequestEnd( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onRequestEnd,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onAbort event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onAbort( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onAbort,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onClassRequest event
	 *
	 * @param context The context
	 * @param args    The arguments
	 *
	 * @return true if the request should continue, false otherwise
	 */
	public boolean onClassRequest( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onClassRequest,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);

		return true;
	}

	/**
	 * Handle the onSessionStart event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onSessionStart( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onSessionStart,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onSessionEnd event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onSessionEnd( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onSessionEnd,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onApplicationStart event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onApplicationStart( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onApplicationStart,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onApplicationEnd event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onApplicationEnd( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onApplicationEnd,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);
	}

	/**
	 * Handle the onError event
	 *
	 * @param context The context
	 * @param args    The arguments
	 *
	 * @return true if the error was handled, false otherwise
	 */
	public boolean onError( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.onError,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);

		return true;
	}

	/**
	 * Handle the onMissingTemplate event
	 *
	 * @param context The context
	 * @param args    The arguments
	 *
	 * @return true if the missing template was handled, false otherwise
	 */
	public boolean onMissingTemplate( IBoxContext context, Object[] args ) {
		this.application.announce(
		    Key.missingTemplate,
		    Struct.of(
		        "context", context,
		        "args", args,
		        "application", this.application,
		        "listener", this
		    ),
		    context
		);

		return true;
	}
}
