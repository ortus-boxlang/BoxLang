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
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<String> imports ) {
		throw new UnsupportedOperationException( "Implement the [resolve] method in your own resolver" );
	}

	/**
	 * Tries to resolve the class name using the imports list given. If the class
	 * name is not found, it will return the original class name.
	 *
	 * @param context   The current context of execution
	 * @param className The name of the class to resolve
	 * @param imports   The list of imports to use
	 *
	 * @return The resolved class name or the original class name if not found
	 */
	public static String resolveFromImport( IBoxContext context, String className, List<String> imports ) {
		return imports.stream()
		    // Remove the resolvers by prefix
		    .map( thisImport -> thisImport.replaceAll( "[^:]+:", "" ) )
		    // Discover import
		    .filter( thisImport -> {
			    String[] parts		= thisImport.split( "\\." );
			    String[] aliasParts	= thisImport.split( "\\s+" );
			    String	shortName	= parts[ parts.length - 1 ];
			    String	aliasName	= aliasParts[ aliasParts.length - 1 ];

			    return shortName.equalsIgnoreCase( className ) || aliasName.equalsIgnoreCase( className );
		    } )
		    // Return the first one, the first one wins
		    .findFirst()
		    .map( thisImport -> thisImport.split( "(?i) as " )[ 0 ] )
		    // Nothing found, return the original class name
		    .orElse( className );
	}

}
