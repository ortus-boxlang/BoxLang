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

import java.io.File;
import java.util.List;
import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;

/**
 * This resolver deals with BoxLang classes only.
 */
public class BoxResolver extends BaseResolver {

	/**
	 * The class directory for generated bx classes
	 */
	private String					classDirectory;

	/**
	 * Singleton instance
	 */
	protected static BoxResolver	instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private BoxResolver() {
		super( "BoxResolver", "bx" );
	}

	/**
	 * Singleton instance
	 *
	 * @return The instance
	 */
	public static synchronized BoxResolver getInstance() {
		if ( instance == null ) {
			instance = new BoxResolver();
		}

		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters & Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return the classDirectory
	 */
	public String getClassDirectory() {
		return classDirectory;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers
	 * --------------------------------------------------------------------------
	 */

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
	@Override
	public Optional<ClassLocation> resolve( IBoxContext context, String name ) {
		return resolve( context, name, List.of() );
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
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<ImportDefinition> imports ) {
		return findFromModules( context, name, imports )
		    .or( () -> findFromLocal( context, name, imports ) );
	}

	/**
	 * Load a class from the registered runtime module class loaders
	 *
	 * @param name    The fully qualified path of the class to load
	 * @param imports The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( IBoxContext context, String name, List<ImportDefinition> imports ) {
		return Optional.ofNullable( null );
	}

	/**
	 * Load a class from the configured directory byte code
	 *
	 * @param name    The fully qualified path of the class to load
	 * @param imports The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromLocal( IBoxContext context, String name, List<ImportDefinition> imports ) {
		// TODO: How do we deal with .bx vs .cfc extensions?

		String				slashName	= "/" + name.replace( ".", "/" );
		// System.out.println( "resolving: " + slashName );

		// First look and see if the CFC lives in the directory of the currently-executing template
		ITemplateRunnable	template	= context.findClosestTemplate();
		if ( template != null ) {
			// See if path exists in this parent directory
			File file;
			// System.out.println( "this template's parent: " + template.getRunnablePath().getParent().resolve( slashName.substring( 1 ) + ".cfc" ).toFile() );
			// TODO: Make case insensitive
			if ( template.getRunnablePath().getParent() != null
			    && ( file = template.getRunnablePath().getParent().resolve( slashName.substring( 1 ) + ".cfc" ).toFile() ).exists() ) {
				String	className	= file.getName().replace( ".cfc", "" );
				String	packageName	= name.replace( className, "" );
				// System.out.println( "name: " + name );
				// System.out.println( "packageName: " + packageName );
				// System.out.println( "className: " + className );
				return Optional.of( new ClassLocation(
				    className,
				    file.toURI().toString(),
				    packageName,
				    ClassLocator.TYPE_BX,
				    RunnableLoader.getInstance().loadClass( file.toPath(), packageName, context ),
				    ""
				) );
			}

		}

		// Next look for a mapping that matches the start of the path
		Struct		mappings	= context.getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings );
		List<Key>	keys		= mappings.getKeys();
		// System.out.println( "Mappings: " + mappings );
		// Longest to shortest
		keys.sort( ( s1, s2 ) -> Integer.compare( s2.getName().length(), s1.getName().length() ) );
		for ( Key key : keys ) {
			String mapping = ( String ) mappings.get( key );
			// System.out.println( "Looking in mapping: " + key.getName() + " -> " + mapping );
			if ( slashName.startsWith( key.getName() ) ) {
				// See if path exists in this parent directory
				File file;
				// TODO: Make case insensitive
				if ( ( file = new File( mapping + slashName + ".cfc" ) ).exists() ) {
					String	className	= file.getName().replace( ".cfc", "" );
					String	packageName	= name.replace( className, "" );
					return Optional.of( new ClassLocation(
					    className,
					    file.toURI().toString(),
					    packageName,
					    ClassLocator.TYPE_BX,
					    RunnableLoader.getInstance().loadClass( file.toPath(), packageName, context ),
					    ""
					) );
				}
			}
		}
		return Optional.ofNullable( null );
	}

}
