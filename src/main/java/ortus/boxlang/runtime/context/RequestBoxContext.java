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
package ortus.boxlang.runtime.context;

import java.io.PrintStream;
import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * A request-type context. I track additional things related to a request.
 */
public abstract class RequestBoxContext extends BaseBoxContext implements IJDBCCapableContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The locale for this request
	 */
	private Locale					locale					= null;

	/**
	 * The timezone for this request
	 */
	private ZoneId					timezone				= null;

	/**
	 * The thread manager for this request
	 */
	private RequestThreadManager	threadManager			= null;

	/**
	 * Flag to enforce explicit output
	 */
	private boolean					enforceExplicitOutput	= false;

	/**
	 * The request timeout in milliseconds
	 */
	private Long					requestTimeout			= null;

	/**
	 * The time in milliseconds when the request started
	 */
	private DateTime				requestStart			= new DateTime();

	/**
	 * The JDBC connection manager, which tracks transaction state/context and allows a thread or request to retrieve connections.
	 */
	private ConnectionManager		connectionManager;

	/**
	 * Application.bx listener for this request
	 * null if there is none
	 */
	private BaseApplicationListener	applicationListener;

	/**
	 * The application service
	 */
	private ApplicationService		applicationService		= getRuntime().getApplicationService();

	/**
	 * The output buffer for the script
	 */
	private PrintStream				out						= System.out;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a parent context
	 *
	 * @param parent The parent context
	 */
	protected RequestBoxContext( IBoxContext parent ) {
		super( parent );
		this.connectionManager = new ConnectionManager( this );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Set the output stream for this context
	 *
	 * @param out The output stream
	 *
	 * @return This context
	 */
	public RequestBoxContext setOut( PrintStream out ) {
		this.out = out;
		return this;
	}

	/**
	 * Get the output stream for this context
	 *
	 * @return The output stream
	 */
	public PrintStream getOut() {
		return this.out;
	}

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( this.threadManager != null && this.threadManager.hasThreads() ) {
			scopes.getAsStruct( Key.contextual ).put( ThreadScope.name, this.threadManager.getThreadScope() );
			// loop over threads and add them to the contextual scope
			for ( Key threadName : this.threadManager.getThreadNames() ) {
				scopes.getAsStruct( Key.contextual ).put( threadName, this.threadManager.getThreadMeta( threadName ) );
			}
		}
		return super.getVisibleScopes( scopes, nearby, shallow );
	}

	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		if ( this.threadManager != null && this.threadManager.hasThreads() ) {
			// Global access to bxthread scope
			if ( key.equals( ThreadScope.name ) ) {
				return new ScopeSearchResult( this.threadManager.getThreadScope(), this.threadManager.getThreadScope(), key, true );
			}
			// Global access to threadName "scope"
			IStruct threadMeta = this.threadManager.getThreadMeta( key );
			if ( threadMeta != null ) {
				return new ScopeSearchResult( threadMeta, threadMeta, key, true );
			}
		}

		if ( parent != null ) {
			return parent.scopeFind( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null, key );
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	/**
	 * This will look for an Application.bx file in the root mapping, load it if found, and configure the Application settings
	 *
	 * @param template The URI to the Application.bx file
	 */
	public void loadApplicationDescriptor( URI template ) {
		// This will load the Application file and create an ApplicationListener, or an empty listener with default behavior
		this.applicationListener = this.applicationService.createApplicationListener( this, template );
	}

	/**
	 * Get the session key for this request
	 *
	 * @return The session key
	 */
	public abstract Key getSessionID();

	/**
	 * Invalidate a session
	 */
	public abstract void resetSession();

	/**
	 * Get the application listener for this request
	 *
	 * @return The application listener
	 */
	public BaseApplicationListener getApplicationListener() {
		return this.applicationListener;
	}

	/**
	 * Set the application listener for this request
	 *
	 * @param applicationListener
	 *
	 * @return
	 */
	public RequestBoxContext setApplicationListener( BaseApplicationListener applicationListener ) {
		this.applicationListener = applicationListener;
		return this;
	}

	/**
	 * Get the locale for this request
	 *
	 * @return The locale
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Set the locale for this request
	 *
	 * @param locale The locale
	 */
	public RequestBoxContext setLocale( Locale locale ) {
		this.locale = locale;
		return this;
	}

	/**
	 * Get the timezone for this request
	 *
	 * @return The timezone
	 */
	public ZoneId getTimezone() {
		return this.timezone;
	}

	/**
	 * Set the timezone for this request
	 *
	 * @param timezone The timezone
	 *
	 * @return this context
	 */
	public RequestBoxContext setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
		return this;
	}

	/**
	 * Get the contexual config struct. Each context has a chance to add in config of their
	 * own to the struct, or override existing config with a new struct of their own design.
	 * It depends on whether the context wants its changes to exist for the rest of the entire
	 * request or only for code that executes in the current context and below.
	 *
	 * @return A struct of configuration
	 */
	@Override
	public IStruct getConfig() {
		IStruct config = super.getConfig();

		// Apply request-specific overrides
		// These can happen from BIF calls specifically
		if ( this.locale != null ) {
			config.getAsStruct( Key.runtime ).put( Key.locale, this.locale );
		}
		if ( this.timezone != null ) {
			config.getAsStruct( Key.runtime ).put( Key.timezone, this.timezone );
		}
		if ( this.requestTimeout != null ) {
			config.getAsStruct( Key.runtime ).put( Key.requestTimeout, this.requestTimeout );
		}
		config.put( Key.enforceExplicitOutput, this.enforceExplicitOutput );

		// There are code paths that hit this prior to intializing the applicationListener
		if ( this.applicationListener != null ) {
			IStruct appSettings = this.applicationListener.getSettings();
			// Make the request settings generically available in the config struct.
			// This doesn't mean we won't strategically place specific settings like mappings into specific parts
			// of the config struct, but this at least ensure everything is available for whomever wants to use it
			config.put( Key.applicationSettings, appSettings );

			// Default Datasource as string pointing to a datasource in the datasources struct
			// this.datasource = "coldbox"
			if ( appSettings.get( Key.datasource ) instanceof String castedDSN && castedDSN.length() > 0 ) {
				config.getAsStruct( Key.runtime ).put( Key.defaultDatasource, castedDSN );
			}

			// Default datasource as a inline struct
			// This is a special case where the datasource is defined inline in the Application.bx
			// Register it into the datasources struct as well as the 'bxDefaultDatasource'
			// this.datasource = { driver: "", url: "", username: "", password: "" }
			if ( appSettings.get( Key.datasource ) instanceof IStruct castedDSN ) {
				// Store the datasource in the datasources struct
				config.getAsStruct( Key.runtime ).getAsStruct( Key.datasources ).put( Key.bxDefaultDatasource, castedDSN );
				// Store the datasource name in the runtime struct as "defaultDatasource"
				config.getAsStruct( Key.runtime ).put( Key.defaultDatasource, Key.bxDefaultDatasource.getName() );
			}

			// Datasource overrides
			IStruct datasources = appSettings.getAsStruct( Key.datasources );
			if ( !datasources.isEmpty() ) {
				config.getAsStruct( Key.runtime ).getAsStruct( Key.datasources ).putAll( datasources );
			}

			// Mapping overrides
			IStruct mappings = appSettings.getAsStruct( Key.mappings );
			if ( !mappings.isEmpty() ) {
				config.getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).putAll( mappings );
			}

			// OTHER OVERRIDES go here
		}

		// Announce it so modules can do their own overrides and such
		BoxRuntime.getInstance()
		    .getInterceptorService()
		    .announce(
		        BoxEvent.ON_REQUEST_CONTEXT_CONFIG,
		        Struct.of(
		            "context", this,
		            "config", config
		        )
		    );

		return config;
	}

	/**
	 * Get the thread manager for this request.
	 * Created as needed.
	 *
	 * @return The thread manager
	 */
	public RequestThreadManager getThreadManager() {
		if ( this.threadManager != null ) {
			return this.threadManager;
		}
		synchronized ( this ) {
			if ( this.threadManager != null ) {
				return this.threadManager;
			}
			this.threadManager = new RequestThreadManager();
		}
		return this.threadManager;
	}

	/**
	 * Set the enforceExplicitOutput flag. This determines if templating output is requried to be inside an output component
	 * Get this setting by asking your local context for config item "enforceExplicitOutput"
	 *
	 * @param enforceExplicitOutput true to enforce explicit output
	 *
	 * @return this context
	 */
	public RequestBoxContext setEnforceExplicitOutput( boolean enforceExplicitOutput ) {
		this.enforceExplicitOutput = enforceExplicitOutput;
		return this;
	}

	/**
	 * Get the enforceExplicitOutput flag. This determines if templating output is requried to be inside an output component
	 *
	 * @return true if explicit output is enforced
	 */
	public boolean isEnforceExplicitOutput() {
		return enforceExplicitOutput;
	}

	/**
	 * Set the request timeout in milliseconds
	 *
	 * @param requestTimeout The timeout in milliseconds
	 *
	 * @return this context
	 */
	public RequestBoxContext setRequestTimeout( Long requestTimeout ) {
		this.requestTimeout = requestTimeout;
		return this;
	}

	/**
	 * Get the request timeout in milliseconds
	 *
	 * @return The timeout in milliseconds
	 */
	public Long getRequestTimeout() {
		return this.requestTimeout;
	}

	/**
	 * Get the settings for this request. These are known as "application settings" since they configure the
	 * application that uses them, but they are really set every request.
	 */
	public IStruct getSettings() {
		return this.applicationListener.getSettings();
	}

	/**
	 * Get the time in milliseconds when the request started
	 *
	 * @return The time in milliseconds when the request started
	 */
	public DateTime getRequestStart() {
		return this.requestStart;
	}

	/**
	 * Get the ConnectionManager, which is the central point for managing database connections and transactions.
	 */
	public ConnectionManager getConnectionManager() {
		return this.connectionManager;
	}

	/**
	 * Shutdown the ConnectionManager and release any resources.
	 */
	public void shutdownConnections() {
		this.connectionManager.shutdown();
	}

}
