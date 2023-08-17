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
package ortus.boxlang.runtime.loader;

/**
 * This class is the base class for all resolvers.
 */
public class BaseResolver {

	/**
	 * The name of a resolver
	 */
	public static final String		NAME	= "";

	/**
	 * The prefix of a resolver
	 */
	public static final String		PREFIX	= "";

	/**
	 * Singleton
	 */
	protected static BaseResolver	instance;

	/**
	 * Private constructor
	 */
	protected BaseResolver() {
		// Base resolver does not need anything
	}

	/**
	 * Singleton instance
	 *
	 * @return The instance
	 */
	public static synchronized BaseResolver getInstance() {
		if ( instance == null ) {
			instance = new BaseResolver();
		}
		return instance;
	}

	/**
	 * Each resolver has a unique human-readable name
	 *
	 * @return The resolver name
	 */
	public String getName() {
		return NAME;
	}

	/**
	 * Each resolver has a unique prefix which is used to call it. Do not add the
	 * {@code :}
	 * ex: java:, bx:, wirebox:, custom:
	 *
	 * @return The prefix
	 */
	public String getPrefix() {
		return PREFIX;
	}

}
