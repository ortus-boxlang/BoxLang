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

import java.net.URI;
import java.time.ZoneId;
import java.util.Locale;

import ortus.boxlang.runtime.application.ApplicationListener;
import ortus.boxlang.runtime.jdbc.ConnectionManager;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.services.ApplicationService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * A request-type context. I track additional things related to a request.
 */
public abstract class RequestBoxContext extends BaseBoxContext implements IJDBCCapableContext {

	private Locale					locale					= null;
	private ZoneId					timezone				= null;
	private RequestThreadManager	threadManager			= null;
	private boolean					enforceExplicitOutput	= false;
	private Long					requestTimeout			= null;
	private Long					requestStartMS			= System.currentTimeMillis();

	/**
	 * The JDBC connection manager, which tracks transaction state/context and allows a thread or request to retrieve connections.
	 */
	private ConnectionManager		connectionManager;

	/**
	 * The JDBC datasource manager, which manages connection pools. This private property will only be used if the no parent application cannot be found.
	 */
	private DataSourceManager		dataSourceManager;

	/**
	 * Application.bx listener for this request
	 * null if there is none
	 */
	ApplicationListener				applicationListener;

	/**
	 * The application service
	 */
	ApplicationService				applicationService		= getRuntime().getApplicationService();

	/**
	 * Creates a new execution context with a parent context
	 *
	 * @param parent The parent context
	 */
	protected RequestBoxContext( IBoxContext parent ) {
		super( parent );
		this.connectionManager = new ConnectionManager();
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( threadManager != null && threadManager.hasThreads() ) {
			scopes.getAsStruct( Key.contextual ).put( ThreadScope.name, threadManager.getThreadScope() );
			// loop over threads and add them to the contextual scope
			for ( Key threadName : threadManager.getThreadNames() ) {
				scopes.getAsStruct( Key.contextual ).put( threadName, threadManager.getThreadMeta( threadName ) );
			}
		}
		return super.getVisibleScopes( scopes, nearby, shallow );
	}

	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		if ( threadManager != null && threadManager.hasThreads() ) {
			// Global access to bxthread scope
			if ( key.equals( ThreadScope.name ) ) {
				return new ScopeSearchResult( threadManager.getThreadScope(), threadManager.getThreadScope(), key, true );
			}
			// Global access to threadName "scope"
			IStruct threadMeta = threadManager.getThreadMeta( key );
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
	 * This will look for an Application.cfc file in the root mapping, load it if found, and configure the Application settings
	 */
	public void loadApplicationDescriptor( URI template ) {
		// This will load the Application file and create an ApplicationListener, or an empty listener with default behavior
		applicationListener = applicationService.createApplicationListener( this, template );
	}

	/**
	 * Get the session key for this request
	 *
	 * @return The session key
	 */
	abstract public Key getSessionID();

	public ApplicationListener getApplicationListener() {
		return applicationListener;
	}

	public RequestBoxContext setApplicationListener( ApplicationListener applicationListener ) {
		this.applicationListener = applicationListener;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale( Locale locale ) {
		this.locale = locale;
	}

	public ZoneId getTimezone() {
		return timezone;
	}

	public void setTimezone( ZoneId timezone ) {
		this.timezone = timezone;
	}

	@Override
	public IStruct getConfig() {
		IStruct config = super.getConfig();

		// Apply request-specific overrides
		if ( locale != null ) {
			config.put( Key.locale, locale );
		}
		if ( timezone != null ) {
			config.put( Key.timezone, timezone );
		}
		config.put( Key.enforceExplicitOutput, enforceExplicitOutput );

		if ( requestTimeout != null ) {
			config.put( Key.requestTimeout, requestTimeout );
		}

		// There are code paths that hit this prior to intializing the applicationListener
		if ( applicationListener != null ) {
			// Make the request settings generically available in the config struct.
			// This doesn't mean we won't strategically place specific settings like mappings into specific parts
			// of the config struct, but this at least ensure everything is available for whomever wants to use it
			config.put( Key.applicationSettings, applicationListener.getSettings() );

			IStruct mappings = applicationListener.getSettings().getAsStruct( Key.mappings );
			if ( mappings != null ) {
				config.getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).putAll( mappings );
			}
		}

		return config;
	}

	/**
	 * Get the thread manager for this request.
	 * Created as needed.
	 *
	 * @return The thread manager
	 */
	public RequestThreadManager getThreadManager() {
		if ( threadManager != null ) {
			return threadManager;
		}
		synchronized ( this ) {
			if ( threadManager != null ) {
				return threadManager;
			}
			threadManager = new RequestThreadManager();
		}
		return threadManager;
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
		return requestTimeout;
	}

	/**
	 * Get the settings for this request. These are known as "application settings" since they configure the
	 * application that uses them, but they are really set every request.
	 */
	public IStruct getSettings() {
		return applicationListener.getSettings();
	}

	/**
	 * Get the time in milliseconds when the request started
	 *
	 * @return The time in milliseconds when the request started
	 */
	public Long getRequestStartMS() {
		return requestStartMS;
	}

	/**
	 * Get the ConnectionManager, which is the central point for managing database connections and transactions.
	 */
	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

}
