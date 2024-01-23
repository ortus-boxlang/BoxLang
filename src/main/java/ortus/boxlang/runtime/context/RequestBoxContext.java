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

/**
 * A request-type context. I track additional things related to a request.
 */
public abstract class RequestBoxContext extends BaseBoxContext {

	private Locale	locale		= null;
	private ZoneId	timezone	= null;

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	public RequestBoxContext( IBoxContext parent ) {
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

	public IStruct getConfig() {
		IStruct config = super.getConfig();

		// Apply request-specific overrides
		if ( locale != null ) {
			config.put( Key.locale, locale );
		}
		if ( timezone != null ) {
			config.put( Key.timezone, timezone );
		}

		return config;
	}

}
