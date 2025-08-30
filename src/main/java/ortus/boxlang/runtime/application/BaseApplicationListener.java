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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
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
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.services.InterceptorService;
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
 * I represent an Application listener. I am the base class for a class-based listener, template-based listener, or default listener.
 *
 * A listener is a class (Application.bx) that observes and responds to specific events or actions within an application.
 * It acts as a bridge between the application and the runtime, enabling custom behavior to be executed when certain events occur.
 *
 * In this context, the listener is responsible for handling application lifecycle events, request events, session events,
 * and other application-specific actions, providing a way to customize and extend the behavior of the application.
 */
public abstract class BaseApplicationListener {

	/**
	 * --------------------------------------------------------------------------
	 * Constants
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The available request pool interceptors
	 */
	private static final Key[]				REQUEST_INTERCEPTION_POINTS	= List.of(
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
	 * Runtime
	 */
	private static final BoxRuntime			runtime						= BoxRuntime.getInstance();

	/**
	 * Interceptor Service
	 */
	private static final InterceptorService	interceptorService			= runtime.getInterceptorService();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The application name
	 */
	protected Key							appName						= null;

	/**
	 * The application linked to this listener
	 */
	protected Application					application;

	/**
	 * The request context bound to this listener
	 */
	protected RequestBoxContext				context;

	/**
	 * The listener's interception pool
	 */
	protected InterceptorPool				interceptorPool;

	/**
	 * The template, if any, which initiated this request.
	 * For a web request, this is the URI
	 * For a scripting request, this is the file being executed
	 * Null for ad-hoc code execution.
	 */
	protected ResolvedFilePath				baseTemplatePath			= null;

	/**
	 * All Application settings (which are really set per-request). This includes any "expected" ones from the BoxLog core, plus any additional settings
	 * that a module or add-on may be looking for. This also determines default values for all settings.
	 * <p>
	 * You can find the majority of defaults in the {@link Configuration} class.
	 */
	protected IStruct						settings					= Struct.of(
	    // Security settings
	    "allowedFileOperationExtensions", runtime.getConfiguration().security.allowedFileOperationExtensions,
	    // Application settings
	    "applicationTimeout", runtime.getConfiguration().applicationTimeout,
	    // Cache Definitions
	    "caches", new Struct(),
	    // Class Paths, using both for compat
	    "classPaths", new Array(),
	    "componentPaths", new Array(),
	    // Component Paths
	    "customComponentPaths", new Array(),
	    // Datasource settings
	    "datasource", runtime.getConfiguration().defaultDatasource,
	    "defaultDatasource", runtime.getConfiguration().defaultDatasource,
	    "datasources", new Struct(),
	    // Security settings
	    "disallowedFileOperationExtensions", runtime.getConfiguration().security.disallowedFileOperationExtensions,
	    // Invocation settings
	    "invokeImplicitAccessor", runtime.getConfiguration().invokeImplicitAccessor,
	    // Java Settings
	    "javaSettings", Struct.of(
	        "loadPaths", new Array(),
	        "loadSystemClassPath", false,
	        "reloadOnChange", false
	    ),
	    // Locale for the application
	    "locale", runtime.getConfiguration().locale.toString(),
	    // Mappings
	    "mappings", Struct.of(),
	    // Dynamic Schedulers
	    "schedulers", new Array(),
	    // Default Session Management settings
	    "sessionManagement", runtime.getConfiguration().sessionManagement,
	    "sessionStorage", runtime.getConfiguration().sessionStorage,
	    "sessionTimeout", runtime.getConfiguration().sessionTimeout,
	    // Cookie Management
	    "setClientCookies", runtime.getConfiguration().setClientCookies,
	    "setDomainCookies", runtime.getConfiguration().setDomainCookies,
	    // These are auto-calculated at runtime
	    "class", "",
	    "name", "",
	    "source", "",
	    // end auto-calculated
	    "timezone", runtime.getConfiguration().timezone.getId(),
	    // Stil Considering if they will be core or a module
	    "secureJson", false,
	    "secureJsonPrefix", ""
	);

	/**
	 * Logger
	 */
	protected BoxLangLogger					logger;

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
	protected BaseApplicationListener( RequestBoxContext context, ResolvedFilePath baseTemplatePath ) {
		this.logger				= runtime.getLoggingService().APPLICATION_LOGGER;
		this.context			= context;
		this.baseTemplatePath	= baseTemplatePath;
		context.setApplicationListener( this );
		this.interceptorPool = new InterceptorPool( Key.appListener, runtime ).registerInterceptionPoint( REQUEST_INTERCEPTION_POINTS );
		// Ensure our thread is at least using the runtime CL. If there is an application defined later, this may get updated to a more specific request CL.
		Thread.currentThread().setContextClassLoader( runtime.getRuntimeLoader() );
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
	 * Gets the Request Context
	 */
	public RequestBoxContext getRequestContext() {
		return this.context;
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
		context.clearConfigCache();
	}

	/**
	 * Define the application context. This is called every time on every request by the Application Service
	 * to ensure that the application context is properly defined and initialized.
	 *
	 * <h2>Events Announced</h2>
	 * <ul>
	 * <li><code>onApplicationDefined</code> : Once the application is correctly created</li>
	 * </ul>
	 *
	 * @see ApplicationService
	 */
	public void defineApplication() {
		String appNameString = StringCaster.cast( this.settings.get( Key._NAME ) );

		try {
			// Only create it if we have a name, else there is no application
			if ( appNameString != null && !appNameString.isEmpty() ) {
				// Setup the app name for the listener
				this.appName = Key.of( appNameString );
				// Startup app and services
				createOrUpdateApplication();
				createOrUpdateClassLoaderPaths();
				createOrUpdateCaches();
				createOrUpdateSchedulers();
				createOrUpdateSessionManagement();
			}
			// Cleanups
			else {
				context.removeParentContext( ApplicationBoxContext.class );
				context.removeParentContext( SessionBoxContext.class );
			}

			// Announce application defined
			BoxRuntime.getInstance().getInterceptorService().announce(
			    BoxEvent.ON_APPLICATION_DEFINED,
			    Struct.of(
			        "listener", this,
			        "context", this.context
			    ) );
		} catch ( Throwable e ) {
			// Log the error
			logger.error( "Error defining application [{}] => {}", this.appName, e.getMessage(), e );
			this.application = null;
			// If there was an error, we need to remove the application context
			context.removeParentContext( ApplicationBoxContext.class );
			context.removeParentContext( SessionBoxContext.class );
			throw e;
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
		URL[]				loadPathsUrls	= getJavaSettingsLoadPaths( context );
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
	 * @param requestContext The request context which can contain all the necessary information to expand the paths
	 *
	 * @return The expanded load paths as URLs
	 */
	public URL[] getJavaSettingsLoadPaths( RequestBoxContext requestContext ) {
		// Get the Application.cfc settings for the current request
		IStruct				thisSettings			= requestContext.getApplicationListener().getSettings();

		// Get the source location to resolve pathing
		String				source					= StringCaster.cast( thisSettings.get( Key.source ) );
		ResolvedFilePath	listenerResolvedPath	= ResolvedFilePath.of( source );

		// logger.debug( "Listener resolved path: {}", listenerResolvedPath );

		// Get the defined paths, and expand them using BL rules.
		IStruct				javaSettings			= thisSettings.getAsStruct( Key.javaSettings );
		Array				loadPaths				= ArrayCaster.cast( javaSettings.getOrDefault( Key.loadPaths, new Array() ) )
		    .stream()
		    .map( item -> FileSystemUtil.expandPath( requestContext, ( String ) item, listenerResolvedPath ).absolutePath().toString() )
		    .collect( BLCollector.toArray() );

		// Inflate them to what we need now
		return DynamicClassLoader.inflateClassPaths( loadPaths );
	}

	/**
	 * Update or create the application class loader paths
	 */
	private void createOrUpdateClassLoaderPaths() {
		this.application.startupClassLoaderPaths( this.context );
	}

	/**
	 * Update or create the application caches
	 */
	private void createOrUpdateCaches() {
		this.application.startupAppCaches( this.context );
	}

	/**
	 * Update or create the application schedulers
	 */
	private void createOrUpdateSchedulers() {
		this.application.startupAppSchedulers( this.context );
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
				existingSessionContext.updateSession( this.application.getOrCreateSession( this.context.getSessionID(), this.context ) );
				// Only starts the first time
				existingSessionContext.getSession().start( this.context );
			} else {
				// If session management is disabled, remove it
				this.context.removeParentContext( SessionBoxContext.class );
			}
		}
	}

	/**
	 * This method is called to create or update the application context.
	 * If the application has expired, it will be shutdown and restarted.
	 * If the application is not defined, it will be created.
	 * <p>
	 * Once created, it will inject a new @{link ApplicationBoxContext} into the request context.
	 * <p>
	 * It will call the {@link Application#start(IBoxContext)} method to start the application.
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
			Session			existing		= sessionContext.getSession();
			SessionScope	currentScope	= existing.getSessionScope();
			IStruct			existingScope	= currentScope != null ? new Struct( currentScope ) : new Struct();

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
		SessionBoxContext sessionContext = context.getParentOfType( SessionBoxContext.class );
		if ( sessionContext == null ) {
			throw new BoxRuntimeException( "No session to invalidate.  Is session management enabled?" );
		}
		Session terminalSession = sessionContext.getSession();
		context.getParentOfType( ApplicationBoxContext.class ).getApplication().getSessionsCache().clear( terminalSession.getID().getName() );
		terminalSession.shutdown( this );
		initializeSession( newID );
	}

	/**
	 * Initializes a new session, also called by every new request via the {@link BaseApplicationListener#defineApplication} method
	 *
	 * @param newID The new session identifier
	 */
	public void initializeSession( Key newID ) {
		Session targetSession = this.context
		    .getApplicationContext()
		    .getApplication()
		    .getOrCreateSession( newID, this.context );
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
	 * @param state      The state to announce
	 * @param data       The data to announce
	 * @param appContext The application context
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
		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onRequest,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onRequest,
		    eventArgs,
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
		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onRequestStart,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onRequestStart,
		    eventArgs,
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
		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onRequestEnd,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onRequestEnd,
		    eventArgs,
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

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onAbort,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onAbort,
		    eventArgs,
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
		_onClassRequest( context, args );
	}

	/**
	 * Handle the onClassRequest event. This extra method is to get around an issue with needing to call this logic from more than one place.
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	private void _onClassRequest( IBoxContext context, Object[] args ) {
		logger.trace( "Fired onClassRequest ...................." );

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
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

		// Announce globally
		interceptorService.announce(
		    Key.onClassRequest,
		    eventArgs,
		    context
		);
	}

	/**
	 * Handle the onClassRequest event when there is no Application class. This is here to easily share
	 * between the default and template application listeners.
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	protected void onClassRequestSimple( IBoxContext context, Object[] args ) {
		_onClassRequest( context, args );
		String				className		= ( String ) args[ 0 ];
		Struct				params			= ( Struct ) args[ 1 ];

		IClassRunnable		classInstance	= loadClassInstance( context, className );
		String				methodName		= null;
		ScopeSearchResult	scopeSearch		= context.scopeFind( Key.method, context.getDefaultAssignmentScope(), false );
		if ( scopeSearch.value() != null ) {
			methodName = StringCaster.cast( scopeSearch.value() );
		} else {
			classRequestNoMethod( context, classInstance );
			return;
		}

		invokeClassRequest(
		    context,
		    classInstance,
		    methodName,
		    params,
		    null,
		    true
		);
	}

	/**
	 * Handle the invocation of a class request
	 *
	 * @param context          The context
	 * @param classInstance    The possible class instance to execute
	 * @param methodName       The method name to execute on the class
	 * @param namedParams      The named parameters to the class
	 * @param positionalParams The positional parameters to the class
	 * @param mustBeRemote     If the method must be remote or not
	 */
	protected void invokeClassRequest(
	    IBoxContext context,
	    IClassRunnable classInstance,
	    String methodName,
	    Struct namedParams,
	    Object[] positionalParams,
	    boolean mustBeRemote ) {

		Object result = null;

		// Check method is marked as remote
		if ( mustBeRemote
		    && ! ( classInstance.getThisScope().get( Key.of( methodName ) ) instanceof Function func && func.getAccess().equals( Function.Access.REMOTE ) ) ) {
			throw new BoxRuntimeException( "[" + methodName + "] is not marked as remote method on the class." );
		}
		// The method itself will push the template, but for expandPath to work correctly, we need the BASE template to be the CFC we're executing,
		// which may be different than the class the method is in, if it's in the super class
		context.pushTemplate( classInstance );
		try {
			// Direct invocation of a remote method will use named args, but invocation of the listener's onClassRequest method will use positional args
			if ( namedParams != null ) {
				result = classInstance.dereferenceAndInvoke( context, Key.of( methodName ), namedParams, false );
			} else {
				result = classInstance.dereferenceAndInvoke( context, Key.of( methodName ), positionalParams, false );
			}
		} finally {
			context.popTemplate();
		}

		String				returnFormat	= null;
		ScopeSearchResult	scopeSearch		= context.scopeFind( Key.returnFormat, context.getDefaultAssignmentScope(), false );
		if ( scopeSearch.value() != null ) {
			returnFormat = StringCaster.cast( scopeSearch.value() );
		}
		// If there was no override, see if a remote method set it via annotation
		if ( returnFormat == null ) {
			// Any time a remote function is executed thaty has a returnFormat annotation, this request context attachment is set.
			// This will basically represent the returnFormat of the LAST remote function which was executed on this request.
			returnFormat = Optional.ofNullable( context.getRequestContext().getAttachment( Key.returnFormat ) )
			    .map( Object::toString )
			    .orElse( null );
		}

		// If there was still no override, default to the config, which by default in BoxLang is JSON
		if ( returnFormat == null ) {
			returnFormat = context.getRuntime().getConfiguration().defaultRemoteMethodReturnFormat;
		}

		// Regardless of how we found it, set it in the request context so we can use it later
		context.getRequestContext().putAttachment( Key.returnFormat, returnFormat );

		if ( result != null ) {
			String stringResult;
			// switch on returnFormat
			switch ( returnFormat.toLowerCase() ) {
				case "json" :
					stringResult = ( String ) context.invokeFunction( Key.JSONSerialize, new Object[] { result, "struct" } );
					break;
				case "wddx" :
				case "xml" :
					if ( context.getRuntime().getModuleService().hasModule( Key.wddx ) ) {
						DynamicObject WDDXUtil = context.getRuntime()
						    .getClassLocator()
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
	 * @param context       The context
	 * @param classInstance The class instance
	 */
	protected void classRequestNoMethod( IBoxContext context, IClassRunnable classInstance ) {
		// If there is no method and we're in debug mode, dump the CFC
		if ( context.getRuntime().inDebugMode() ) {
			context.invokeFunction(
			    Key.dump,
			    new Object[] {
			        classInstance
			    }
			);
		} else {
			context.writeToBuffer( "Method not specified, enable debug to see class details." );
		}
	}

	/**
	 * Helper method to create the instance of our class so we can reuse this logic.
	 *
	 * @param className The class name to load
	 *
	 * @return The class instance
	 */
	protected IClassRunnable loadClassInstance( IBoxContext context, String className ) {
		Object possibleClassInstance = context.invokeFunction( Key.createObject, new Object[] { className } );
		if ( possibleClassInstance instanceof IClassRunnable icr ) {
			return icr;
		}

		throw new BoxRuntimeException( "The path must be a class and not an interface." );
	}

	/**
	 * Handle the onSessionStart event
	 *
	 * @param context The context
	 * @param args    The arguments
	 */
	public void onSessionStart( IBoxContext context, Object[] args ) {
		logger.trace( "Fired onSessionStart ...................." );

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onSessionStart,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onSessionStart,
		    eventArgs,
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

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onSessionEnd,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onSessionEnd,
		    eventArgs,
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

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onError,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onError,
		    eventArgs,
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

		IStruct eventArgs = Struct.of(
		    "context", context,
		    "args", args,
		    "application", this.application,
		    "listener", this
		);

		// Announce locally
		this.interceptorPool.announce(
		    Key.onMissingTemplate,
		    eventArgs,
		    context
		);

		// Announce globally
		interceptorService.announce(
		    Key.onMissingTemplate,
		    eventArgs,
		    context
		);

		return true;
	}

	public ResolvedFilePath getBaseTemplatePath() {
		return this.baseTemplatePath;
	}
}
