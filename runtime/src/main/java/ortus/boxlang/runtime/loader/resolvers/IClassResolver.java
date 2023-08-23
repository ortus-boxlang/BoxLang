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
package ortus.boxlang.runtime.loader.resolvers;

import java.util.List;
import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;

/**
 * This interface is to implement ways to resolve classes in BoxLang.
 * Each resolver can have a name and a way to resolve the class it represents.
 * The {@link ClassLocator} class will then use the resolver and convert it to a
 * {@see DynamicObject}
 */
public interface IClassResolver {

	/**
	 * Each resolver has a unique human-readable name
	 *
	 * @return The resolver name
	 */
	public String getName();

	/**
	 * Each resolver has a unique prefix which is used to call it. Do not add the
	 * {@code :}
	 * ex: java:, bx:, wirebox:, custom:
	 *
	 * @return The prefix
	 */
	public String getPrefix();

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 *
	 * @return An optional class object representing the class if found
	 */
	public Optional<ClassLocation> resolve( IBoxContext context, String name );

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches with imports.
	 *
	 * @param context The current context of execution
	 * @param name    The name of the class to resolve
	 * @param imports The list of imports to use
	 *
	 * @return An optional class object representing the class if found
	 */
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<String> imports );

}
