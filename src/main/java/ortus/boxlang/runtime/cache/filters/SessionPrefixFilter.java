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
package ortus.boxlang.runtime.cache.filters;

import ortus.boxlang.runtime.application.Session;

/**
 * This filter will match any session that has a prefix which in our case
 * is the name of the application that the session belongs to.
 */
public class SessionPrefixFilter extends WildcardFilter {

	/**
	 * Create a new widlcard filter with a case-insensitive widlcard
	 *
	 * @param prefix The widlcard to use
	 */
	public SessionPrefixFilter( String prefix ) {
		this( prefix, true );
	}

	/**
	 * Create a new prefix filter
	 *
	 * @param prefix     The prefix to use
	 * @param ignoreCase Whether the prefix should be case-sensitive
	 */
	public SessionPrefixFilter( String prefix, boolean ignoreCase ) {
		super( prefix + Session.ID_CONCATENATOR + "*", ignoreCase );
	}

}
