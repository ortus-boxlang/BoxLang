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

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.SessionBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.DynamicClassLoader;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
	protected Key				appName						= null;

	/**
	 * The application linked to this listener
	 */
	protected Application		application;

	/**
	 * The request context bound to this listener
	 */
	protected RequestBoxContext	context;

	/**
	 * The listener's interception pool
	 */
	protected InterceptorPool	interceptorPool;

	/**
	 * The available request pool interceptors
	 */
	private static final Key[]	REQUEST_INTERCEPTION_POINTS	= List.of(
	    Key.onRequest,
	    Key.onRequestStart,
	    Key.onRequestEnd,
	    Key.onAbort,
	    Key.onClassRequest,
	    Key.onSessionStart,
	    Key.onSessionEnd,
	    Key.onApplicationStart,
	    Key.onApplicationEnd,
	    Key.onError,
	    Key.missingTemplate
	).toArray( new Key[ 0 ] );

	/**
	 * All Application settings (which are really set per-request). This includes any "expected" ones from the BoxLog core, plus any additional settings
	 * that a module or add-on may be looking for. This also determines default values for all settings.
	 * <p>
	 * You can find the majority of defaults in the {@link Configuration} class.
	 */
	protected IStruct			settings					= Struct.of(
	    "applicationTimeout", BoxRuntime.getInstance().getConfiguration().applicationTimeout,
	    // CLIENT WILL BE REMOVED IN BOXLANG
	    // Kept here for now
	    "clientManagement", false,
	    "clientStorage", "cookie",
	    "clientTimeout", 1,
	    // END: CLIENT
	    "componentPaths", new Array(),
	    "customTagPaths", new Array(),
	    "datasource", BoxRuntime.getInstance().getConfiguration().defaultDatasource,
	    "defaultDatasource", BoxRuntime.getInstance().getConfiguration().defaultDatasource,
	    "datasources", new Struct(),
	    "invokeImplicitAccessor", BoxRuntime.getInstance().getConfiguration().invokeImplicitAccessor,
	    "javaSettings", Struct.of(
	        "loadPaths", new Array(),
	        "loadSystemClassPath", false,
	        "reloadOnChange", false
	    ),
	    "locale", BoxRuntime.getInstance().getConfiguration().locale.toString(),
	    "mappings", Struct.of(),
	    "sessionManagement", BoxRuntime.getInstance().getConfiguration().sessionManagement,
	    "sessionStorage", BoxRuntime.getInstance().getConfiguration().sessionStorage,
	    "sessionTimeout", BoxRuntime.getInstance().getConfiguration().sessionTimeout,
	    "setClientCookies", BoxRuntime.getInstance().getConfiguration().setClientCookies,
	    "setDomainCookies", BoxRuntime.getInstance().getConfiguration().setDomainCookies,
	    // These are auto-calculated at runtime
	    "class", "",
	    "name", "",
	    "source", "",
	    // end auto-calculated
	    "timezone", BoxRuntime.getInstance().getConfiguration().timezone.getId(),
	    // Stil Considering if they will be core or a module
	    "secureJson", false,
	    "secureJsonPrefix", "",
	    "allowedFileOperationExtensions", BoxRuntime.getInstance().getConfiguration().allowedFileOperationExtensions,
	    "disallowedFileOperationExtensions", BoxRuntime.getInstance().getConfiguration().disallowedFileOperationExtensions
	);

	/**
	 * Logger
	 */
	private static final Logger	logger						= LoggerFactory.getLogger( BaseApplicationListener.class );

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
		this.interceptorPool = new InterceptorPool( Key.appListener, BoxRuntime.getInstance() )
		    .registerInterceptionPoint( REQUEST_INTERCEPTION_POINTS );
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
	 * Define the application context. This is called every time on every request by the Application Service
	 * to ensure that the application context is properly defined and initialized.
	 *
	 * @see ApplicationService
	 */
	public void defineApplication() {
		String appNameString = StringCaster.cast( settings.get( Key._NAME ) );

		// Only create it if we have a name
		if ( appNameString != null && !appNameString.isEmpty() ) {
			// Setup the app name for the listener
			this.appName = Key.of( appNameString );
			// Startup app and services
			createOrUpdateApplication();
			createOrUpdateClassLoaderPaths();
			createOrUpdateSessionManagement();
		} else {
			// If there's no name, remove the app context
			context.removeParentContext( ApplicationBoxContext.class );
			// also remove any session context
			context.removeParentContext( SessionBoxContext.class );
		}
	}

	/**
	 * Get the request class loader for the current request according to the application settings
	 * or the default class loader if none is defined
	 *
	 * @param context The request context asking for the class loader
	 *
	 * @return The request class loader
	 */
	public DynamicClassLoader getRequestClassLoader( RequestBoxContext context ) {
		// If the application is null, return the default
		if ( this.application == null ) {
			return BoxRuntime.getInstance().getRuntimeLoader();
		}

		// We are in app mode
		URL[]				loadPathsUrls	= getJavaSettingsLoadPaths( context.getParentOfType( ApplicationBoxContext.class ) );
		String				loaderCacheKey	= EncryptionUtil.hash( Arrays.toString( loadPathsUrls ) );
		DynamicClassLoader	target			= this.application.getClassLoader( loaderCacheKey );
		if ( target == null ) {
			target = BoxRuntime.getInstance().getRuntimeLoader();
		}

		return target;
	}

	/**
	 * This reads the javaSettings.loadPaths, expands them, and returns them as URLs of
	 * jars and classes
	 *
	 * @param appContext The application context
	 *
	 * @return The expanded load paths as URLs
	 */
	public URL[] getJavaSettingsLoadPaths( ApplicationBoxContext appContext ) {
		// Get the source location to resolve pathing
		String				source					= StringCaster.cast( this.settings.get( Key.source ) );
		ResolvedFilePath	listenerResolvedPath	= ResolvedFilePath.of( source );

		// logger.debug( "Listener resolved path: {}", listenerResolvedPath );

		// Get the defined paths, and expand them using BL rules.
		IStruct				javaSettings			= this.settings.getAsStruct( Key.javaSettings );
		Array				loadPaths				= ArrayCaster.cast( javaSettings.getOrDefault( Key.loadPaths, new Array() ) )
		    .stream()
		    .map( item -> FileSystemUtil.expandPath( appContext, ( String ) item, listenerResolvedPath ).absolutePath().toString() )
		    .collect( BLCollector.toArray() );

		// Inflate them to what we need now
		return DynamicClassLoader.inflateClassPaths( loadPaths );
	}

	/**
	 * Update or create the application class loaders according to the
	 * discovered and passed app context
	 */
	private void createOrUpdateClassLoaderPaths() {
		this.application.startupClassLoaderPaths( this.context.getParentOfType( ApplicationBoxContext.class ) );
	}

	/**
	 * Update or create the session management in an application if enabled.
	 */
	private void createOrUpdateSessionManagement() {
		// Check for existing session context
		SessionBoxContext	existingSessionContext		= this.context.getParentOfType( SessionBoxContext.class );
		boolean				sessionManagementEnabled	= BooleanCaster.cast( this.settings.get( Key.sessionManagement ) );

		// Create session management if enabled
		if ( existingSessionContext == null ) {
			// if session management is enabled, add it
			if ( sessionManagementEnabled ) {
				initializeSession( this.context.getSessionID() );
			}
		}
		// Update session management if enabled
		else {
			if ( sessionManagementEnabled ) {
				// Ensure we have the right session (app name could have changed)
				existingSessionContext.updateSession( this.application.getOrCreateSession( this.context.getSessionID() ) );
				// Only starts the first time
				existingSessionContext.getSession().start( this.context );
			} else {
				// If session management is disabled, remove it
				this.context.removeParentContext( SessionBoxContext.class );
			}
		}
	}

	/**
	 * Create or update the application according to the
	 * discovered and passed app context
	 */
	private void createOrUpdateApplication() {
		ApplicationBoxContext appContext = this.context.getParentOfType( ApplicationBoxContext.class );

		// If it exists, make sure it has not expired, else restart it
		if ( appContext != null && appContext.getApplication().isExpired() ) {
			this.context.getRuntime().getApplicationService().shutdownApplication( this.appName );
			appContext = null;
		}

		// If there's none, then this creates a new application
		if ( appContext == null ) {
			this.application = this.context.getRuntime().getApplicationService().getApplication( this.appName );
			this.context.injectTopParentContext( new ApplicationBoxContext( this.application ) );
			// Only starts the first time
			try {
				this.application.start( this.context );
			} catch ( Throwable e ) {
				// Note this will remove the application even if the user has an abort;
				// which means you basically can't start the app if you are aborting inside of it
				// Since this is most likely the case of testing, it's probably ok.
				this.context.getRuntime().getApplicationService().removeApplication( this.appName );
				throw e;
			}
		}
		// if there's one, but with a different name, replace it
		else if ( !appContext.getApplication().getName().equals( appName ) ) {
			this.application = this.context.getRuntime().getApplicationService().getApplication( this.appName );
			appContext.updateApplication( this.application );
			this.application.start( this.context );
		}
		// if there's one with the same name, use it
		else {
			this.application = appContext.getApplication();
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
			Session	existing		= sessionContext.getSession();
			IStruct	existingScope	= new Struct( existing.getSessionScope() );

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
		Session terminalSession = this.context.getParentOfType( SessionBoxContext.class ).getSession();
		context.getParentOfType( ApplicationBoxContext.class ).getApplication().getSessionsCache().clear( terminalSession.getID().getName() );
		terminalSession.shutdown( this );
		initializeSession( newID );
	}

	/**
	 * Intializes a new session, also called by every new request via the {@link BaseApplicationListener#defineApplication} method
	 *
	 * @param newID The new session identifier
	 */
	public void initializeSession( Key newID ) {
		ApplicationBoxContext	appContext		= this.context.getParentOfType( ApplicationBoxContext.class );
		Session					targetSession	= appContext.getApplication().getOrCreateSession( newID );
		this.context.removeParentContext( SessionBoxContext.class );
		this.context.injectTopParentContext( new SessionBoxContext( targetSession ) );
		targetSession.start( this.context );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Life-Cycle Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the interceptor pool for this listener
	 *
	 * @return
	 */
	public InterceptorPool getInterceptorPool() {
		return this.interceptorPool;
	}

	/**
	 * Helper to Announce an event with the provided {@link IStruct} of data and the app context
	 *
	 * @param state   The state to announce
	 * @param data    The data to announce
	 * @param context The application context
	 */
	public void announce( BoxEvent state, IStruct data, IBoxContext appContext ) {
		announce( state.key(), data, appContext );
	}

	/**
	 * Helper to Announce an event with the provided {@link IStruct} of data and the app context
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data, IBoxContext appContext ) {
		getInterceptorPool().announce( state, data, appContext );
	}

	/**
	 * Handle the onRequest event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onRequest( IBoxContext context, Object[] args ) {
		logger.trace( "Fired onRequest ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onRequestStart ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onRequestEnd ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onAbort ...................." );

		this.interceptorPool.announce(
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
	 */
	public void onClassRequest( IBoxContext context, Object[] args ) {
		logger.trace( "Fired onClassRequest ...................." );

		this.interceptorPool.announce(
		    Key.onClassRequest,
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
	 * Handle the invocation of a class request
	 *
	 * @param context               The context
	 * @param possibleClassInstance The possible class instance to execute
	 * @param methodName            The method name to execute on the class
	 * @param namedParams           The named parameters to the class
	 * @param positionalParams      The positional parameters to the class
	 * @param returnFormat          The return format for the class: json, wddx, xml, plain
	 * @param mustBeRemote          If the method must be remote or not
	 */
	protected void invokeClassRequest(
	    IBoxContext context,
	    Object possibleClassInstance,
	    String methodName,
	    Struct namedParams,
	    Object[] positionalParams,
	    String returnFormat,
	    boolean mustBeRemote ) {
		// Test the class instance
		IClassRunnable classInstance = null;
		if ( possibleClassInstance instanceof IClassRunnable icr ) {
			classInstance = icr;
		} else {
			throw new BoxRuntimeException( "The path must be a class and not an interface." );
		}
		Object result = null;

		// Check method is marked as remote
		if ( mustBeRemote
		    && ! ( classInstance.getThisScope().get( Key.of( methodName ) ) instanceof Function func && func.getAccess().equals( Function.Access.REMOTE ) ) ) {
			throw new BoxRuntimeException( "[" + methodName + "] is not marked as remote method on the class." );
		}

		// Direct invocation of a remote method will use named args, but invocation of the listener's onClassRequest method will use positional args
		if ( namedParams != null ) {
			result = classInstance.dereferenceAndInvoke( context, Key.of( methodName ), namedParams, false );
		} else {
			result = classInstance.dereferenceAndInvoke( context, Key.of( methodName ), positionalParams, false );
		}

		// If there was no override, see if a remote method set it via annotation
		if ( returnFormat == null ) {
			returnFormat = Optional.ofNullable( context.getParentOfType( RequestBoxContext.class ).getAttachment( Key.returnFormat ) )
			    .map( Object::toString )
			    .orElse( null );
		}

		// If there was still no override, default to the config, which by default in BoxLang is JSON
		if ( returnFormat == null ) {
			returnFormat = context.getRuntime().getConfiguration().defaultRemoteMethodReturnFormat;
		}

		if ( result != null ) {
			String stringResult;
			// switch on returnFormat
			switch ( returnFormat ) {
				case "json" :
					stringResult = ( String ) context.invokeFunction( Key.JSONSerialize, new Object[] { result } );
					break;
				case "wddx" :
				case "xml" :
					if ( context.getRuntime().getModuleService().hasModule( Key.wddx ) ) {
						DynamicObject WDDXUtil = ClassLocator.getInstance()
						    .load(
						        context,
						        "ortus.boxlang.modules.wddx.util.WDDXUtil@wddx",
						        ClassLocator.JAVA_PREFIX
						    );
						stringResult = ( String ) WDDXUtil.invoke( context, "serializeObject", result );
					} else {
						throw new BoxRuntimeException( "WDDX module is not installed.  Cannot serialize to WDDX." );
					}
					break;
				case "plain" :
					CastAttempt<String> stringAttempt = StringCaster.attempt( result );
					if ( stringAttempt.wasSuccessful() ) {
						stringResult = stringAttempt.get();
					} else {
						throw new BoxRuntimeException(
						    "Could not cast return value of type [" + result.getClass().getSimpleName() + "] to string for returnFormat 'plain'" );
					}
					break;
				default :
					throw new BoxRuntimeException( "Unsupported returnFormat [" + returnFormat + "]. Valid options are 'json', 'wddx', 'xml', and 'plain'" );
			}
			context.writeToBuffer( stringResult );
			// If this is a web request, we'll set the default content type in the web-support runtime since this code is core and technically runtime-agnostic, even though
			// the only place we're actually firing the onClassRequest listener right now is in the web-support runtime
		}
	}

	/**
	 * Handle the onClassRequest event when no method is specified.
	 * This will try to dump the class metadata document if debug is enabled.
	 * Else it will return a message that the method was not specified.
	 *
	 * @param context   The context
	 * @param className The class name
	 */
	protected void classRequestNoMethod( IBoxContext context, String className ) {
		// If there is no method and we're in debug mode, dump the CFC
		if ( context.getRuntime().inDebugMode() ) {
			context.invokeFunction(
			    Key.dump,
			    new Object[] {
			        context.invokeFunction(
			            Key.createObject,
			            new Object[] { className }
			        )
			    }
			);
		} else {
			context.writeToBuffer( "Method not specified, enable debug to see class details." );
		}
	}

	/**
	 * Handle the onSessionStart event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onSessionStart( IBoxContext context, Object[] args ) {
		logger.trace( "Fired onSessionStart ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onSessionEnd ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onApplicationStart ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onApplicationEnd ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onError ...................." );

		this.interceptorPool.announce(
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
		logger.trace( "Fired onMissingTemplate ...................." );

		this.interceptorPool.announce(
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
