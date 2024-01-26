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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * This resolver deals with BoxLang classes only.
 */
public class BoxResolver extends BaseResolver {

	/**
	 * Singleton instance
	 */
	protected static BoxResolver		instance;

	/**
	 * List of valid class extensions
	 */
	private static final List<String>	VALID_EXTENSIONS	= List.of( ".bx", ".cfc" );

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
	 * @param context The current context of execution
	 * @param name    The fully qualified path of the class to load
	 * @param imports The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromLocal( IBoxContext context, String name, List<ImportDefinition> imports ) {
		// Convert package dot name to a lookup path
		String slashName = "/" + name.replace( ".", "/" );

		// Find the class using:
		// 1. Relative to the current template
		// 2. A mapping
		return findByRelativeLocation( context, slashName, name, imports )
		    .or( () -> findByMapping( context, slashName, name, imports ) );
	}

	/**
	 * Find a class by mapping resolution
	 *
	 * @param context   The current context of execution
	 * @param slashName The name of the class to find using slahes instead of dots
	 * @param name      The original dot notation name of the class to find
	 * @param imports   The list of imports to use
	 *
	 * @return An Optional of {@link ClassLocation} if found, {@link Optional#empty()} otherwise
	 */
	private Optional<ClassLocation> findByMapping(
	    IBoxContext context,
	    String slashName,
	    String name,
	    List<ImportDefinition> imports ) {

		// Look for a mapping that matches the start of the path
		IStruct mappings = context.getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings );

		// System.out.println( "mappings: " + mappings );
		// System.out.println( "slashName: " + slashName );

		// Maybe if we have > 20 mappings we should use parallel streams

		return mappings
		    .entrySet()
		    .stream()
		    // Filter out mappings that don't match the start of the mapping path
		    .filter( entry -> StringUtils.startsWithIgnoreCase( slashName, entry.getKey().getName() ) )
		    // Map it to a Path object representing the path to the class
		    .map( entry -> Path.of( StringUtils.replaceOnceIgnoreCase( slashName, entry.getKey().getName(), entry.getValue() + "/" ) + ".cfc" ).normalize() )
		    // .peek( path -> System.out.println( "Class Location: " + path.toString() ) )
		    // Verify that the file exists
		    .filter( Files::exists )
		    // .peek( file -> System.out.println( "File Exists." ) )
		    // Map it to a ClassLocation object
		    .map( path -> {
			    var	className	= FilenameUtils.getBaseName( path.toString() );
			    // From original name with mappings: tests.components.User -> tests.components
			    String packageName = ClassUtils.getPackageName( name );
			    return new ClassLocation(
			        className,
			        path.toAbsolutePath().toString(),
			        packageName,
			        ClassLocator.TYPE_BX,
			        RunnableLoader.getInstance().loadClass( path, packageName, context ),
			        ""
			    );
		    } )
		    // Find the first one or return empty
		    .findFirst();
	}

	/**
	 * Find a class by relative location resolution
	 *
	 * @param context   The current context of execution
	 * @param slashName The name of the class to find using slahes instead of dots
	 * @param name      The original dot notation name of the class to find
	 * @param imports   The list of imports to use
	 *
	 * @return An Optional of {@link ClassLocation} if found, {@link Optional#empty()} otherwise
	 */
	private Optional<ClassLocation> findByRelativeLocation(
	    IBoxContext context,
	    String slashName,
	    String name,
	    List<ImportDefinition> imports ) {

		// Check if the class exists in the directory of the currently-executing template
		ITemplateRunnable template = context.findClosestTemplate();

		if ( template != null ) {
			// See if path exists in this parent directory
			Path targetPath = template.getRunnablePath().getParent().resolve( slashName.substring( 1 ) + ".cfc" );
			if ( template.getRunnablePath().getParent() != null && Files.exists( targetPath ) ) {

				String	className	= FilenameUtils.getBaseName( targetPath.toString() );
				String	packageName	= name.replace( className, "" );

				// Remove ending dot if it exists
				if ( packageName.endsWith( "." ) ) {
					packageName = packageName.substring( 0, packageName.length() - 1 );
				}

				return Optional.of( new ClassLocation(
				    className,
				    targetPath.toAbsolutePath().toString(),
				    packageName,
				    ClassLocator.TYPE_BX,
				    RunnableLoader.getInstance().loadClass( targetPath, packageName, context ),
				    ""
				) );
			}
		}
		return Optional.empty();
	}

}
