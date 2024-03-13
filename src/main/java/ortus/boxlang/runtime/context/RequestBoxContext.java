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

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.Locale;

import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.application.ApplicationListener;
import ortus.boxlang.runtime.application.Session;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.jdbc.DBManager;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThreadScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.util.BLCollector;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * A request-type context. I track additional things related to a request.
 */
public abstract class RequestBoxContext extends BaseBoxContext implements IDBManagingContext {

	private Locale					locale					= null;
	private ZoneId					timezone				= null;
	private RequestThreadManager	threadManager			= null;
	private boolean					enforceExplicitOutput	= false;
	private Long					requestTimeout			= null;
	private Long					requestStartMS			= System.currentTimeMillis();
	private DBManager				dbManager;
	// This is a general hold-ground for all settings that can be set for the duration of a request.
	// This can be done via the application component or via Application.cfc
	// TODO: Stub out some keys which should always exist?
	private IStruct					settings				= Struct.of( "mappings", Struct.of() );

	/**
	 * Application.cfc listener for this request
	 * null if there is none
	 */
	ApplicationListener				applicationListener;

	/**
	 * Creates a new execution context with a parent context
	 *
	 * @param parent The parent context
	 */
	protected RequestBoxContext( IBoxContext parent ) {
		super( parent );
		this.dbManager = new DBManager();
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

		if ( template == null ) {
			applicationListener = new ApplicationListener();
			return;
		}

		Path	descriptorPath		= null;
		String	directoryOfTemplate	= null;
		String	packagePath			= "";
		if ( template.isAbsolute() ) {
			directoryOfTemplate	= new File( template ).getParent();
			descriptorPath		= Paths.get( directoryOfTemplate, "Application.cfc" );
			if ( !descriptorPath.toFile().exists() ) {
				descriptorPath = null;
			}
		} else {
			directoryOfTemplate = new File( template.toString() ).getParent();
			String	rootMapping	= getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).getAsString( Key._slash );
			boolean	found		= false;
			while ( directoryOfTemplate != null ) {
				if ( directoryOfTemplate.equals( File.separator ) ) {
					descriptorPath = Paths.get( rootMapping, "Application.cfc" );
				} else {
					descriptorPath = Paths.get( rootMapping, directoryOfTemplate, "Application.cfc" );
				}
				if ( descriptorPath.toFile().exists() ) {
					found		= true;
					// set packagePath to the relative path from the rootMapping to the directoryOfTemplate with slashes replaced with dots
					packagePath	= directoryOfTemplate.replace( File.separator, "." );
					if ( packagePath.endsWith( "." ) ) {
						packagePath = packagePath.substring( 0, packagePath.length() - 1 );
					}
					break;
				}
				directoryOfTemplate = new File( directoryOfTemplate ).getParent();
			}
			if ( !found ) {
				descriptorPath = null;
			}
		}
		if ( descriptorPath != null ) {
			applicationListener = new ApplicationListener( ( IClassRunnable ) DynamicObject.of(
			    RunnableLoader.getInstance()
			        .loadClass( descriptorPath, packagePath, this )
			)
			    .invokeConstructor( this )
			    .getTargetInstance()
			);
			// Extract settings from the this scope of the Application.cfc
			settings.addAll( applicationListener.getListener().getThisScope().entrySet().stream().filter( e -> ! ( e.getValue() instanceof Function ) )
			    .collect( BLCollector.toStruct() ) );

			Application thisApp = getRuntime().getApplicationService().getApplication( Key.of( settings.getOrDefault( Key._NAME, "Application" ) ) );
			injectTopParentContext( new ApplicationBoxContext( thisApp ) );
			// Only starts the first time
			thisApp.start( this );

			if ( BooleanCaster.cast( settings.getOrDefault( Key.sessionManagement, false ) ) ) {
				Session thisSession = thisApp.getSession( getSessionID() );
				injectTopParentContext( new SessionBoxContext( thisSession ) );
				// Only starts the first time
				thisSession.start( this );
			}
		} else {
			applicationListener = new ApplicationListener();
		}

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

		// Make the request settings generically available in the config struct.
		// This doesn't mean we won't strategically place specific settings like mappings into specific parts
		// of the config struct, but this at least ensure everything is available for whomever wants to use it
		config.put( Key.applicationSettings, settings );

		IStruct mappings = settings.getAsStruct( Key.mappings );
		if ( mappings != null ) {
			config.getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).putAll( mappings );
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
		return settings;
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
	 * Get the DBManager, which is the central point for managing database connections and transactions.
	 */
	public DBManager getDBManager() {
		return dbManager;
	}

}
