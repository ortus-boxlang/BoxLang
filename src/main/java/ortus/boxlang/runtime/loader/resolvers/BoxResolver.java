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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
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
	protected static BoxResolver				instance;

	/**
	 * List of valid class extensions
	 */
	// TODO: Move .cfc extension into CF compat module and contribute it at startup.
	// Need to add a setter or other similar mechanism to allow for dynamic extension
	private static List<String>					VALID_EXTENSIONS	= List.of( ".bx", ".cfc" );

	/**
	 * Empty list of imports
	 */
	private static final List<ImportDefinition>	EMPTY_IMPORTS		= List.of();

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
		return resolve( context, name, EMPTY_IMPORTS );
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
		// turn / into .
		name	= name.replace( "/", "." ).trim();
		// and trim leading and trailing dots
		name	= name.startsWith( "." ) ? name.substring( 1 ) : name;
		final String finalName = name.endsWith( "." ) ? name.substring( 0, name.length() - 1 ) : name;

		return findFromModules( context, finalName, imports )
		    .or( () -> findFromLocal( context, finalName, imports ) );
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
		String slashName = name.replace( ".", "/" );
		// prepend / if not already present
		if ( !slashName.startsWith( "/" ) ) {
			slashName = "/" + slashName;
		}
		final String finalSlashName = slashName;

		// Find the class using:
		// 1. Relative to the current template
		// 2. A mapping
		return findByRelativeLocation( context, finalSlashName, name, imports )
		    .or( () -> findByMapping( context, finalSlashName, name, imports ) );
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
		    // Map it to a Stream<Path> object representing the paths to the classes
		    .flatMap( entry -> {
			    // Generate multiple paths here
			    List<Path> paths = new ArrayList<Path>();
			    for ( String extension : VALID_EXTENSIONS ) {
				    paths.add(
				        Path.of( StringUtils.replaceOnceIgnoreCase( slashName, entry.getKey().getName(), entry.getValue() + "/" ) + extension ).normalize() );
			    }

			    return paths.stream();
		    } )
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
			        "",
			        false
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
		Path template = context.findClosestTemplate();

		if ( template != null ) {
			// Get the parent directory of the template, verify it exists, else we are done
			Path parentPath = template.getParent();
			// System.out.println( "parentPath: " + parentPath );
			if ( parentPath != null ) {
				// See if path exists in this parent directory with a valid extension
				Path targetPath = findExistingPathWithValidExtension( parentPath, slashName );
				if ( targetPath != null ) {

					String	className	= FilenameUtils.getBaseName( targetPath.toString() );
					String	packageName	= name.replace( className, "" );

					// System.out.println( "packageName: " + packageName );
					// System.out.println( "classname: " + className );
					// System.out.println( "name: " + name );

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
					    "",
					    false
					) );
				}
			}
		}

		return Optional.empty();
	}

	private Path findExistingPathWithValidExtension( Path parentPath, String slashName ) {
		for ( String extension : VALID_EXTENSIONS ) {
			Path targetPath = parentPath.resolve( slashName.substring( 1 ) + extension );
			if ( Files.exists( targetPath ) ) {
				return targetPath;
			}
		}
		return null;
	}

}
