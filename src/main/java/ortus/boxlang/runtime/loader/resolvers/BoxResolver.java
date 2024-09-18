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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This resolver deals with BoxLang classes only.
 */
public class BoxResolver extends BaseResolver {

	/**
	 * Singleton instance
	 */
	protected static BoxResolver				instance;

	/**
	 * Flag for whether FS is case sensitive or not to short circuit case insensitive path resolution
	 */
	private static boolean						isCaseSensitiveFS	= caseSensitivityCheck();

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
		name	= name.replace( "../", "DOT_DOT_SLASH" )
		    .replace( "/", "." )
		    .replace( "DOT_DOT_SLASH", "../" ).trim();

		// and trim leading and trailing dots
		name	= name.startsWith( "." ) && !name.startsWith( "../" ) ? name.substring( 1 ) : name;
		name	= name.endsWith( "." ) ? name.substring( 0, name.length() - 1 ) : name;

		final String fullyQualifiedName = expandFromImport( context, name, imports );

		return findFromModules( context, fullyQualifiedName, imports )
		    .or( () -> findFromLocal( context, fullyQualifiedName, imports ) );
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
		String slashName = name.replace( "../", "DOT_DOT_SLASH" )
		    .replace( ".", "/" )
		    .replace( "DOT_DOT_SLASH", "../" );

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
		IStruct mappings = context.getConfig().getAsStruct( Key.mappings );

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
			    List<ResolvedFilePath> paths = new ArrayList<ResolvedFilePath>();
			    for ( String extension : VALID_EXTENSIONS ) {
				    Path absolutePath = Path.of( StringUtils.replaceOnceIgnoreCase( slashName, entry.getKey().getName(), entry.getValue() + "/" ) + extension )
				        .normalize();
				    // Verify that the file exists
				    absolutePath = pathExists( absolutePath );
				    if ( absolutePath != null ) {
					    try {
						    String relativePath = Paths
						        .get( entry.getKey().getName(), Paths.get( entry.getValue().toString() ).toRealPath().relativize( absolutePath ).toString() )
						        .toString();
						    paths.add(
						        ResolvedFilePath.of(
						            entry.getKey().getName(),
						            entry.getValue().toString(),
						            relativePath,
						            absolutePath
						        )
						    );
					    } catch ( IOException e ) {
						    // Ignore these errors-- file either got deleted or there was a disk issue
					    }
				    }
			    }

			    return paths.stream();
		    } )
		    // Map it to a ClassLocation object
		    .map( possibleMatch -> {

			    // System.out.println( "found: " + possibleMatch.absolutePath().toAbsolutePath().toString() );
			    // System.out.println( "found package: " + possibleMatch.getPackage().toString() );
			    var className = FilenameUtils.getBaseName( possibleMatch.absolutePath().toString() );
			    return new ClassLocation(
			        className,
			        possibleMatch.absolutePath().toAbsolutePath().toString(),
			        possibleMatch.getPackage().toString(),
			        ClassLocator.TYPE_BX,
			        RunnableLoader.getInstance().loadClass( possibleMatch, context ),
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
		ResolvedFilePath resolvedFilePath = context.findClosestTemplate();
		if ( resolvedFilePath != null ) {
			Path template = resolvedFilePath.absolutePath();

			if ( template != null ) {
				// Get the parent directory of the template, verify it exists, else we are done
				Path parentPath = template.getParent();
				// System.out.println( "parentPath: " + parentPath );
				if ( parentPath != null ) {
					// See if path exists in this parent directory with a valid extension
					Path targetPath = findExistingPathWithValidExtension( parentPath, slashName );
					if ( targetPath != null ) {

						ResolvedFilePath newResolvedFilePath = resolvedFilePath
						    .newFromRelative( parentPath.relativize( Paths.get( targetPath.toString() ) ).toString() );

						System.out.println( "FilenameUtils.getBaseName( newResolvedFilePath.absolutePath().toString() ): "
						    + FilenameUtils.getBaseName( newResolvedFilePath.absolutePath().toString() ) );
						System.out.println( "targetPath.toAbsolutePath().toString(): " + targetPath.toAbsolutePath().toString() );
						System.out.println( "newResolvedFilePath.getPackage().toString(): " + newResolvedFilePath.getPackage().toString() );

						return Optional.of( new ClassLocation(
						    FilenameUtils.getBaseName( newResolvedFilePath.absolutePath().toString() ),
						    targetPath.toAbsolutePath().toString(),
						    newResolvedFilePath.getPackage().toString(),
						    ClassLocator.TYPE_BX,
						    RunnableLoader.getInstance().loadClass( newResolvedFilePath, context ),
						    "",
						    false
						) );
					}
				}
			}
		}
		return Optional.empty();
	}

	private Path findExistingPathWithValidExtension( Path parentPath, String slashName ) {
		for ( String extension : VALID_EXTENSIONS ) {
			Path	targetPath	= parentPath.resolve( slashName.substring( 1 ) + extension ).normalize();

			Path	result		= pathExists( targetPath );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	/**
	 * Performs case insensitive path resolution
	 * 
	 * @param path The path to check
	 * 
	 * @return The resolved path or null if not found
	 */
	Path pathExists( Path path ) {
		Boolean defaultCheck = Files.exists( path );
		if ( defaultCheck ) {
			try {
				return path.toRealPath();
			} catch ( IOException e ) {
				return null;
			}
		}
		if ( isCaseSensitiveFS ) {
			String		realPath		= "";
			String[]	pathSegments	= path.toString().replace( '\\', '/' ).split( "/" );
			if ( pathSegments.length > 0 && pathSegments[ 0 ].contains( ":" ) ) {
				realPath = pathSegments[ 0 ];
			}
			Boolean first = true;
			for ( String thisSegment : pathSegments ) {
				// Skip windows drive letter
				if ( realPath == pathSegments[ 0 ] && pathSegments[ 0 ].contains( ":" ) && first ) {
					first = false;
					continue;
				}
				// Skip empty segments
				if ( thisSegment.length() == 0 ) {
					continue;
				}

				Boolean		found		= false;
				String[]	children	= new File( realPath + "/" ).list();
				// This will happen if we have a matched file in the middle of a path like /foo/index.cfm/bar
				if ( children == null ) {
					return null;
				}
				for ( String thisChild : children ) {
					// We're taking the FIRST MATCH. Buyer beware
					if ( thisSegment.equalsIgnoreCase( thisChild ) ) {
						realPath	+= "/" + thisChild;
						found		= true;
						break;
					}
				}
				// If we made it through the inner loop without a match, we've hit a dead end
				if ( !found ) {
					return null;
				}
			}
			// If we made it through the outer loop, we've found a match
			Path realPathFinal = Paths.get( realPath );
			return realPathFinal;
		}
		return null;
	}

	private static boolean caseSensitivityCheck() {
		try {
			File	currentWorkingDir	= new File( System.getProperty( "user.home" ) );
			File	case1				= new File( currentWorkingDir, "case1" );
			File	case2				= new File( currentWorkingDir, "Case1" );
			case1.createNewFile();
			if ( case2.createNewFile() ) {
				case1.delete();
				case2.delete();
				return true;
			} else {
				case1.delete();
				return false;
			}
		} catch ( Throwable e ) {
			e.printStackTrace();
		}
		return true;
	}

}
