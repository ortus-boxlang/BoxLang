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

import java.time.ZoneId;
import java.util.Locale;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * A request-type context. I track additional things related to a request.
 */
public abstract class RequestBoxContext extends BaseBoxContext {

	private Locale					locale					= null;
	private ZoneId					timezone				= null;
	private RequestThreadManager	threadManager			= null;
	private boolean					enforceExplicitOutput	= false;
	private Long					requestTimeout			= null;
	private Long					requestStartMS			= System.currentTimeMillis();

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	protected RequestBoxContext( IBoxContext parent ) {
		super( parent );
	}

	// getters/setters for request-specific data

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
	 * Get the time in milliseconds when the request started
	 * 
	 * @return The time in milliseconds when the request started
	 */
	public Long getRequestStartMS() {
		return requestStartMS;
	}

}
