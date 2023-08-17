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

import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;

/**
 * This class is the base class for all resolvers.
 */
public class BaseResolver implements IClassResolver {

	/**
	 * The name of a resolver
	 */
	protected String	name	= "";

	/**
	 * The prefix of a resolver
	 */
	protected String	prefix	= "";

	/**
	 * Private constructor
	 *
	 * @param name   The name of the resolver
	 * @param prefix The prefix of the resolver
	 */
	protected BaseResolver( String name, String prefix ) {
		this.name	= name;
		this.prefix	= prefix.toLowerCase();
	}

	/**
	 * Each resolver has a unique human-readable name
	 *
	 * @return The resolver name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Each resolver has a unique prefix which is used to call it. Do not add the
	 * {@code :}
	 * ex: java:, bx:, wirebox:, custom:
	 *
	 * @return The prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Each resolver has a way to resolve the class it represents.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 *
	 * @return An optional class object representing the class if found
	 */
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name ) {
		throw new UnsupportedOperationException( "Implement the [resolve] method in your own resolver" );
	}

}
