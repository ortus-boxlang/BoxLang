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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ClassLocator.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This resolver is in charge of resolving and returning BoxLang classes. It will follow the
 * rules of checking runtime mappings and the current template directory for the class requesting
 * resolution.
 * <p>
 * In order to access it you must go via the @{link ClassLocator} class, as the ClassLocator
 * controls all the resolvers in the runtime.
 * <p>
 * Example:
 *
 * <pre>
 * ClassLocator.getJavaResolver();
 * or
 * ClassLocator.getResolver( ClassLocator.JAVA_PREFIX );
 * </pre>
 */
public class BoxResolver extends BaseResolver {

	/**
	 * Empty list of imports
	 */
	private static final List<ImportDefinition>	EMPTY_IMPORTS	= List.of();
	BoxLangLogger								logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param classLocator The class locator to use
	 */
	public BoxResolver( ClassLocator classLocator ) {
		super( "BoxResolver", "bx", classLocator );
		this.logger = this.runtime.getLoggingService().getLogger( "boxresolver" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Resolvers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get all the valid extensions we can process.
	 * This list does NOT include the dot.
	 */
	public Set<String> getValidExtensions() {
		return BoxRuntime.getInstance().getConfiguration().validClassExtensions;
	}

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches.
	 *
	 * @param context   The current context of execution
	 * @param name      The name of the class to resolve
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return An optional class object representing the class if found
	 */
	public Optional<ClassLocation> resolve( IBoxContext context, String name, boolean loadClass ) {
		return resolve( context, name, EMPTY_IMPORTS, loadClass );
	}

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
		return resolve( context, name, EMPTY_IMPORTS, true );
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
		return resolve( context, name, imports, true );
	}

	/**
	 * Each resolver has a way to resolve the class it represents.
	 * This method will be called by the {@link ClassLocator} class
	 * to resolve the class if the prefix matches with imports.
	 *
	 * @param context   The current context of execution
	 * @param name      The name of the class to resolve
	 * @param imports   The list of imports to use
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return An optional class object representing the class if found
	 */
	public Optional<ClassLocation> resolve( IBoxContext context, String name, List<ImportDefinition> imports, boolean loadClass ) {
		// turn / into .
		name	= name.replace( "../", "DOT_DOT_SLASH" )
		    .replace( "/", "." )
		    .replace( "DOT_DOT_SLASH", "../" ).trim();

		// and trim leading and trailing dots
		name	= name.startsWith( "." ) && !name.startsWith( "../" ) ? name.substring( 1 ) : name;
		name	= name.endsWith( "." ) ? name.substring( 0, name.length() - 1 ) : name;

		final String fullyQualifiedName = expandFromImport( context, name, imports );
		// System.out.println( "--=--------> fullyQualifiedName: " + fullyQualifiedName );

		return findFromModules( context, fullyQualifiedName, imports, loadClass )
		    .or( () -> findFromLocal( context, fullyQualifiedName, imports, loadClass ) );
	}

	/**
	 * Load a class from the registered runtime module class loaders
	 *
	 * @param context            The current context of execution
	 * @param fullyQualifiedName The fully qualified path of the class to load
	 * @param imports            The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( IBoxContext context, String fullyQualifiedName, List<ImportDefinition> imports ) {
		return findFromModules( context, fullyQualifiedName, imports, true );
	}

	/**
	 * This tries to load a BoxLang class from registered modules using the {@code fullyQualifiedName@moduleName} provided.
	 * If the class is not found, it will return an empty Optional.
	 * <p>
	 * If there is no module name, then we return an empty Optional, because it will delegate to the {@link #findFromLocal} method,
	 * which will look for the class in the current template directory, or using module mappings.
	 *
	 *
	 * @param context   The current context of execution
	 * @param name      The fully qualified path of the class to load
	 * @param imports   The list of imports to use
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromModules( IBoxContext context, String fullyQualifiedName, List<ImportDefinition> imports, boolean loadClass ) {
		// Do we have a explicit module name? path.to.Class@moduleName
		String[] parts = fullyQualifiedName.split( "@" );

		// If we have a module name, then we need to load the class from the module explicitly
		if ( parts.length == 2 ) {
			// fullyQualifiedName = parts[ 0 ];
			// moduleName = parts[ 1 ];
			return findFromModule( parts[ 0 ], Key.of( parts[ 1 ] ), imports, context );
		}

		return Optional.ofNullable( null );
	}

	/**
	 * Find a class from a specific module explicitly.
	 *
	 * @param fullyQualifiedName The fully qualified path of the class to load in the module root
	 * @param moduleName         The name of the module to look in
	 * @param imports            The list of imports to use
	 * @param context            The current context of execution
	 *
	 * @throws BoxRuntimeException If the module is not found
	 *
	 * @return The ClassLocation record wrapped in an optional if found, empty otherwise
	 */
	public Optional<ClassLocation> findFromModule( String fullyQualifiedName, Key moduleName, List<ImportDefinition> imports, IBoxContext context ) {
		ModuleService moduleService = BoxRuntime.getInstance().getModuleService();

		// Verify the module exists, else throw up, as it was an explicit call
		if ( !moduleService.hasModule( moduleName ) ) {
			throw new BoxRuntimeException(
			    String.format(
			        "Module requested [%s] not found when looking for [%s]. Valid modules are: [%s]",
			        moduleName.getName(),
			        fullyQualifiedName,
			        moduleService.getModuleNames()
			    )
			);
		}

		// Get the module record and the physical path
		ModuleRecord	moduleRecord	= moduleService.getModuleRecord( moduleName );
		String			finalSlashName	= getFullyQualifiedSlashName( fullyQualifiedName );

		// See if path exists in this parent directory with a valid extension
		Path			targetPath		= findExistingPathWithValidExtension( moduleRecord.physicalPath, finalSlashName );
		if ( targetPath != null ) {
			ResolvedFilePath resolvedFilePath = ResolvedFilePath.of( targetPath );
			return Optional.of( new ClassLocation(
			    resolvedFilePath.getBoxFQN().getClassName(),
			    targetPath.toAbsolutePath().toString(),
			    resolvedFilePath.getBoxFQN().getPackageString(),
			    ClassLocator.TYPE_BX,
			    RunnableLoader.getInstance().loadClass( resolvedFilePath, context ),
			    moduleName.getName(),
			    false
			) );
		}

		return Optional.empty();
	}

	/**
	 * Load a class from the configured directory byte code
	 *
	 * @param context            The current context of execution
	 * @param fullyQualifiedName The fully qualified path of the class to load
	 * @param imports            The list of imports to use
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromLocal( IBoxContext context, String fullyQualifiedName, List<ImportDefinition> imports ) {
		return findFromLocal( context, fullyQualifiedName, imports, true );
	}

	/**
	 * Load a class from the configured directory byte code
	 *
	 * @param context            The current context of execution
	 * @param fullyQualifiedName The fully qualified path of the class to load
	 * @param imports            The list of imports to use
	 * @param loadClass          When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return The loaded class or null if not found
	 */
	public Optional<ClassLocation> findFromLocal( IBoxContext context, String fullyQualifiedName, List<ImportDefinition> imports, boolean loadClass ) {
		final String finalSlashName = getFullyQualifiedSlashName( fullyQualifiedName );
		// Try to find the class using:
		// 1. Relative to the current template
		// 2. A mapping
		// 3. Custom tag directory or component/class path
		return findByRelativeLocation( context, finalSlashName, name, imports, loadClass )
		    // TODO: both of these method call context.getConfig(), but ideally we just call it once
		    // For classes found in a lookup directory, it will result in getConfig() being called twice.
		    .or( () -> findByMapping( context, finalSlashName, name, imports, loadClass ) )
		    .or( () -> findByLookupDirectory( context, finalSlashName, name, imports, loadClass ) );
	}

	/**
	 * Find a class by mapping resolution
	 *
	 * @param context   The current context of execution
	 * @param slashName The name of the class to find using slahes instead of dots
	 * @param name      The original dot notation name of the class to find
	 * @param imports   The list of imports to use
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return An Optional of {@link ClassLocation} if found, {@link Optional#empty()} otherwise
	 */
	private Optional<ClassLocation> findByMapping(
	    IBoxContext context,
	    String slashName,
	    String name,
	    List<ImportDefinition> imports,
	    boolean loadClass ) {

		// Look for a mapping that matches the start of the path
		IStruct mappings = context.getConfig().getAsStruct( Key.mappings );

		// System.out.println( "mappings: " + mappings );
		// System.out.println( "slashName: " + slashName );
		this.logger.debug( "Resolving [{}], mappings: [{}]", slashName, mappings );

		// Maybe if we have > 20 mappings we should use parallel streams

		return mappings
		    .entrySet()
		    .stream()
		    // Filter out mappings that don't match the start of the mapping path
		    .filter( entry -> StringUtils.startsWithIgnoreCase( slashName, entry.getKey().getName() ) )
		    // Map it to a Stream<Path> object representing the paths to the classes
		    .flatMap( entry -> {
			    // Generate multiple paths here
			    List<ResolvedFilePath> paths = new ArrayList<>();
			    for ( String extension : getValidExtensions() ) {
				    Path absolutePath = Path
				        .of( StringUtils.replaceOnceIgnoreCase( slashName, entry.getKey().getName(), entry.getValue() + "/" ) + "." + extension )
				        .normalize();
				    // Verify that the file exists
				    absolutePath = FileSystemUtil.pathExistsCaseInsensitive( absolutePath );
				    if ( absolutePath != null ) {
					    try {
						    String mappingName		= entry.getKey().getName();
						    String mappingDirectory	= entry.getValue().toString();
						    String relativePath;

						    // Java not smart enough to ignore a path part of just / and it will wind up with \\ in windows
						    if ( mappingName.equals( "/" ) || mappingName.equals( "\\" ) ) {
							    relativePath = Paths.get( mappingDirectory ).toRealPath().relativize( absolutePath ).toString();
						    } else {
							    relativePath = Paths
							        .get( mappingName, Paths.get( mappingDirectory ).toRealPath().relativize( absolutePath ).toString() )
							        .toString();
						    }

						    paths.add(
						        ResolvedFilePath.of(
						            mappingName,
						            mappingDirectory,
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
			    return new ClassLocation(
			        possibleMatch.getBoxFQN().getClassName(),
			        possibleMatch.absolutePath().toAbsolutePath().toString(),
			        possibleMatch.getBoxFQN().getPackageString(),
			        ClassLocator.TYPE_BX,
			        loadClass ? RunnableLoader.getInstance().loadClass( possibleMatch, context ) : null,
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
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return An Optional of {@link ClassLocation} if found, {@link Optional#empty()} otherwise
	 */
	private Optional<ClassLocation> findByRelativeLocation(
	    IBoxContext context,
	    String slashName,
	    String name,
	    List<ImportDefinition> imports,
	    boolean loadClass ) {

		// Check if the class exists in the directory of the currently-executing template
		ResolvedFilePath resolvedFilePath = context.findClosestTemplate();
		if ( resolvedFilePath != null ) {
			Path template = resolvedFilePath.absolutePath();

			if ( template != null && !template.toString().equalsIgnoreCase( "unknown" ) ) {
				// Get the parent directory of the template, verify it exists, else we are done
				Path parentPath = template.getParent();
				if ( parentPath != null ) {
					// See if path exists in this parent directory with a valid extension
					Path targetPath = findExistingPathWithValidExtension( parentPath, slashName );
					if ( targetPath != null ) {

						ResolvedFilePath newResolvedFilePath = resolvedFilePath
						    .newFromRelative( parentPath.relativize( Paths.get( targetPath.toString() ) ).toString() );

						return Optional.of( new ClassLocation(
						    newResolvedFilePath.getBoxFQN().getClassName(),
						    targetPath.toAbsolutePath().toString(),
						    newResolvedFilePath.getBoxFQN().getPackageString(),
						    ClassLocator.TYPE_BX,
						    loadClass ? RunnableLoader.getInstance().loadClass( newResolvedFilePath, context ) : null,
						    "",
						    false
						) );
					}
				}
			}
		}

		return Optional.empty();
	}

	/**
	 * Find a class in lookup directories. This includes custom tag paths and component/class paths
	 *
	 * @param context   The current context of execution
	 * @param slashName The name of the class to find using slahes instead of dots
	 * @param name      The original dot notation name of the class to find
	 * @param imports   The list of imports to use
	 * @param loadClass When false, the class location is returned with informatino about where the class was found, but the class is not loaded and will be null.
	 *
	 * @return An Optional of {@link ClassLocation} if found, {@link Optional#empty()} otherwise
	 */
	private Optional<ClassLocation> findByLookupDirectory(
	    IBoxContext context,
	    String slashName,
	    String name,
	    List<ImportDefinition> imports,
	    boolean loadClass ) {

		List<Path> lookupPaths = new ArrayList<Path>();
		lookupPaths
		    .addAll( context.getConfig().getAsArray( Key.customTagsDirectory ).stream().map( String::valueOf ).map( Path::of ).toList() );
		lookupPaths.addAll( context.getConfig().getAsArray( Key.classPaths ).stream().map( String::valueOf ).map( Path::of ).toList() );

		Optional<Path> result = lookupPaths
		    .stream()
		    .map( cp -> findExistingPathWithValidExtension( cp, slashName ) )
		    .filter( path -> path != null )
		    .findFirst();

		if ( !result.isPresent() ) {
			return Optional.empty();
		}
		Path				foundPath			= result.get();

		ResolvedFilePath	newResolvedFilePath	= ResolvedFilePath.of( "", "", slashName, foundPath );

		return Optional.of( new ClassLocation(
		    newResolvedFilePath.getBoxFQN().getClassName(),
		    foundPath.toAbsolutePath().toString(),
		    newResolvedFilePath.getBoxFQN().getPackageString(),
		    ClassLocator.TYPE_BX,
		    loadClass ? RunnableLoader.getInstance().loadClass( newResolvedFilePath, context ) : null,
		    "",
		    false
		) );

	}

	/**
	 * Find an existing path with a valid extension
	 *
	 * @param parentPath The parent path to search in
	 * @param slashName  The name of the class to find using slahes instead of dots
	 *
	 * @return The path if found, null otherwise
	 */
	private Path findExistingPathWithValidExtension( Path parentPath, String slashName ) {
		for ( String extension : getValidExtensions() ) {
			Path	targetPath	= parentPath.resolve( slashName.substring( 1 ) + "." + extension ).normalize();

			Path	result		= FileSystemUtil.pathExistsCaseInsensitive( targetPath );
			if ( result != null ) {
				return result;
			}
		}
		return null;
	}

	/**
	 * This method will take the fully qualified class name and convert all
	 * periods (.) to (/) slashes. It will also prepend a slash (/) if it is not,
	 * already present.
	 *
	 * This is useful for converting a fully qualified class name to a path that
	 * can be used to look up a class in the file system.
	 *
	 * @param fullyQualifiedName The fully qualified class name to convert
	 *
	 * @return The fully qualified class name with periods converted to slashes
	 */
	private String getFullyQualifiedSlashName( String fullyQualifiedName ) {
		// Convert package dot name to a lookup path
		String slashName = fullyQualifiedName.replace( "../", "DOT_DOT_SLASH" )
		    .replace( ".", "/" )
		    .replace( "DOT_DOT_SLASH", "../" );

		// prepend / if not already present
		if ( !slashName.startsWith( "/" ) ) {
			slashName = "/" + slashName;
		}

		return slashName;
	}

	/**
	 * Checks if the import has the given class name as a multi-import
	 *
	 * @param context    The current context of execution
	 * @param thisImport The import to check
	 * @param className  The class name to check
	 *
	 * @return True if the import has the class name, false otherwise
	 */
	@Override
	public boolean importHasMulti( IBoxContext context, ImportDefinition thisImport, String className ) {
		String packageSlashName = getFullyQualifiedSlashName( thisImport.getPackageName() );

		// This verifies that the package exists, else we need to expand it
		if ( !FileSystemUtil.exists( packageSlashName ) ) {
			packageSlashName = FileSystemUtil.expandPath( context, packageSlashName ).absolutePath().toString();
		}

		// Get the stream of class files in the package
		// If it finds a class that matches the class name, then it returns true, else false
		return FileSystemUtil.listDirectory(
		    packageSlashName,
		    false,
		    null,
		    "name",
		    "file"
		)
		    .anyMatch( path -> path.getFileName().toString().startsWith( className + "." ) );

	}

}
