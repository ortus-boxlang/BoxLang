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
package ortus.boxlang.runtime.services;

import java.io.File;
import java.net.URI;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.application.ApplicationClassListener;
import ortus.boxlang.runtime.application.ApplicationDefaultListener;
import ortus.boxlang.runtime.application.ApplicationTemplateListener;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.BoxFQN;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I handle managing Applications
 */
public class ApplicationService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The applications for this runtime
	 */
	private Map<Key, Application>												applications				= new ConcurrentHashMap<>();

	/**
	 * Extensions to search for application descriptor classes: Application.bx, Application.cfc, etc
	 */
	private Set<String>															applicationDescriptorClassExtensions;

	/**
	 * Extensions to search for application descriptor templates: Application.bxm, Application.bxml, etc
	 */
	private Set<String>															applicationDescriptorExtensions;

	/**
	 * Cache Application.bx/bxm lookups per directory if trusted cache is enabled.
	 * This cache is server-wide and based purely on the file system absolute path being looked in.
	 */
	private final java.util.Map<String, Optional<ApplicationDescriptorSearch>>	applicationDescriptorCache	= new java.util.HashMap<>();

	/**
	 * The types of application listeners we support: Application classes and
	 * Application templates
	 */
	private enum ApplicationDescriptorType {
		CLASS,
		TEMPLATE
	}

	/**
	 * Logger
	 */
	private Logger logger;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param runtime The runtime instance
	 */
	public ApplicationService( BoxRuntime runtime ) {
		super( runtime, Key.applicationService );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Application Helper Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get an application by name, creating if neccessary
	 *
	 * @param name The name of the application
	 *
	 * @return The application
	 */
	public Application getApplication( Key name ) {
		Application thisApplication = this.applications.get( name );
		if ( thisApplication == null || thisApplication.isExpired() ) {
			// Server-wide lock on this application name. We want to block other threads from creating THIS SPECIFIC application,
			// but we don't want to block other unrelated application creations while we do this.
			synchronized ( name.getName().toLowerCase().intern() ) {
				thisApplication = this.applications.get( name );
				if ( thisApplication == null || thisApplication.isExpired() ) {
					logger.trace( "ApplicationService.getApplication() - {} creating new because {}", name,
					    thisApplication == null ? "didnt' exist" : "was expired" );
					// Create a new one
					thisApplication = new Application( name );
					this.applications.put( name, thisApplication );
				}
			}
		}
		logger.trace( "ApplicationService.getApplication() - {}", name );

		return thisApplication;
	}

	/**
	 * Terminates an application by name
	 *
	 * @param name The name of the application
	 *
	 */
	public void removeApplication( Key name ) {

		logger.trace( "ApplicationService.removeApplication() - {}", name );

		this.applications.remove( name );
	}

	/**
	 * Shuts down an application by name and removes it
	 *
	 * @param name The name of the application
	 */
	public void shutdownApplication( Key name ) {
		Application thisApp = this.applications.get( name );
		if ( thisApp != null ) {
			// remove it first so no one else can access it while it's shutting down
			this.applications.remove( name );
			// nuke it
			thisApp.shutdown( true );
		}

		logger.trace( "ApplicationService.shutdownApplication() - {}", name );
	}

	/**
	 * Check if an application exists
	 *
	 * @param name The name of the application
	 *
	 * @return True if the application exists
	 */
	public boolean hasApplication( Key name ) {
		return this.applications.containsKey( name );
	}

	/**
	 * Get the names of all registered applications
	 *
	 * @return The names of all applications
	 */
	public String[] getApplicationNames() {
		return this.applications.keySet()
		    .stream()
		    .sorted()
		    .map( Key::getName )
		    .toArray( String[]::new );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	@Override
	public void onConfigurationLoad() {
		this.logger = runtime.getLoggingService().APPLICATION_LOGGER;
	}

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		// Setup the application descriptor extensions from the runtime configuration
		this.applicationDescriptorClassExtensions = BoxRuntime.getInstance().getConfiguration().validClassExtensions;
		// Exclude wildcard "*" from template extensions so we don't try to check for Application.*
		this.applicationDescriptorExtensions = BoxRuntime.getInstance().getConfiguration().getValidTemplateExtensions().stream()
		    .filter( ( e ) -> !e.equals( "*" ) ).collect( java.util.stream.Collectors.toSet() );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// loop over applications and shutdown as the runtime is going down.
		this.applications.values().parallelStream().forEach( app -> app.shutdown( force ) );
	}

	/**
	 * Create an ApplicationListener based on the template path
	 *
	 * @param context  The request context requesting the application
	 * @param template The template path to search for an Application descriptor
	 *
	 * @return The ApplicationListener in the template path or a new one if not
	 *         found
	 */
	public BaseApplicationListener createApplicationListener( RequestBoxContext context, URI template ) {
		BaseApplicationListener		listener;
		ApplicationDescriptorSearch	searchResult	= null;
		ResolvedFilePath			templatePath	= null;
		if ( template != null ) {
			// Look for an Application descriptor based on our lookup rules
			String	directoryOfTemplate	= null;
			String	packagePath			= "";
			if ( template.isAbsolute() ) {
				templatePath		= ResolvedFilePath.of( Paths.get( template ) );
				directoryOfTemplate	= new File( template ).getParent();
				searchResult		= fileLookup( directoryOfTemplate );
			} else {
				// This may not be the actual absolute path of the file if we're including a file which is being
				// resolved via a mapping declared in the Application class, which we haven't yet created

				templatePath = FileSystemUtil.expandPath( context, template.getPath() );
				// templatePath = FileSystemUtil.expandPath( runtime.getConfiguration().mappings, template.getPath(), null, false );

				Path	rootPath			= Paths.get( templatePath.mappingPath() );
				Path	currentDirectory	= templatePath.absolutePath().getParent();
				while ( currentDirectory != null && ( currentDirectory.startsWith( rootPath ) || currentDirectory.equals( rootPath ) ) ) {
					searchResult = fileLookup( currentDirectory.toString() );
					if ( searchResult != null ) {
						// Combine the mapping name with the relative path still left to the template
						String mappingDotPath = templatePath.mappingName().equals( "/" ) ? ""
						    : templatePath.mappingName().substring( 1 ).replace( File.separator, "." );
						if ( !mappingDotPath.isBlank() && !mappingDotPath.endsWith( "." ) ) {
							mappingDotPath += ".";
						}
						packagePath = new BoxFQN(
						    mappingDotPath + currentDirectory.toString().substring( rootPath.toString().length() ).replace( File.separator, "." ) ).toString();
						break;
					}
					currentDirectory = currentDirectory.getParent();
				}
			}
			// If we found an Application class, instantiate it
			if ( searchResult != null ) {
				if ( searchResult.type() == ApplicationDescriptorType.CLASS ) {
					// If we found a class, load it and instantiate it
					listener = new ApplicationClassListener( ( IClassRunnable ) DynamicObject.of(
					    RunnableLoader.getInstance()
					        .loadClass(
					            ResolvedFilePath.of(
					                templatePath.mappingName(),
					                templatePath.mappingPath(),
					                packagePath.replace( ".", File.separator ) + File.separator
					                    + searchResult.path().getFileName(),
					                searchResult.path() ),
					            context ) )
					    // We do NOT invoke init() on the Application class for CF compat
					    .invokeConstructor( context, Key.noInit )
					    .getTargetInstance(),
					    context,
					    templatePath );
				} else {
					// If we found a template, return a new empty ApplicationListener
					listener = new ApplicationTemplateListener(
					    RunnableLoader.getInstance().loadTemplateAbsolute(
					        context,
					        ResolvedFilePath.of(
					            templatePath.mappingName(),
					            templatePath.mappingPath(),
					            packagePath.replace( ".", File.separator )
					                + File.separator
					                + searchResult.path().getFileName(),
					            searchResult.path() ) ),
					    context, templatePath );
				}
			} else {
				// If we didn't find an Application, return a new empty ApplicationListener
				listener = new ApplicationDefaultListener( context, templatePath );
			}
		} else {
			// If we didn't have a template, return a new empty ApplicationListener
			listener = new ApplicationDefaultListener( context, templatePath );
		}

		// Announce event so modules can hook in
		announce(
		    BoxEvent.BEFORE_APPLICATION_LISTENER_LOAD,
		    Struct.of(
		        Key.listener, listener,
		        Key.context, context,
		        Key.template, template
		    )
		);

		// Now that the settings are in place, actually define the app (and possibly
		// session) in this request
		context.setTimezone( null ); // reset any inherited timezone before defining the application
		listener.defineApplication();

		announce(
		    BoxEvent.AFTER_APPLICATION_LISTENER_LOAD,
		    Struct.of(
		        Key.listener, listener,
		        Key.context, context,
		        Key.template, template
		    )
		);

		return listener;
	}

	/**
	 * Search a directory for all known file extensions.
	 */
	private ApplicationDescriptorSearch fileLookup( String path ) {
		// If not caching, simply get and return
		if ( !runtime.getConfiguration().trustedCache ) {
			return _fileLookup( path );
		}

		var cachedOptional = applicationDescriptorCache.get( path );
		if ( cachedOptional != null ) {
			// Once the cache is warm, we'll always exit here.
			return cachedOptional.orElse( null );
		}

		// Only need to do this once
		synchronized ( applicationDescriptorCache ) {
			// Double check in case another thread already did the work while we were waiting
			cachedOptional = applicationDescriptorCache.get( path );
			if ( cachedOptional != null ) {
				return cachedOptional.orElse( null );
			}
			var result = _fileLookup( path );
			applicationDescriptorCache.put( path, Optional.ofNullable( result ) );
			return result;
		}

	}

	/**
	 * Search a directory for all known file extensions.
	 */
	private ApplicationDescriptorSearch _fileLookup( String path ) {

		// Look for a class first
		for ( var extension : applicationDescriptorClassExtensions ) {
			try {
				var descriptorPath = Paths.get( path, "Application." + extension );
				if ( descriptorPath.toFile().exists() ) {
					return new ApplicationDescriptorSearch( descriptorPath, ApplicationDescriptorType.CLASS );
				}
			} catch ( InvalidPathException ipe ) {
				// Skip invalid paths. This can happen if an extensions gets registered which invalid chars. No need to blow up, just ignore.
				continue;
			}
		}
		// Then a template
		for ( var extension : applicationDescriptorExtensions ) {
			try {
				var descriptorPath = Paths.get( path, "Application." + extension );
				if ( descriptorPath.toFile().exists() ) {
					return new ApplicationDescriptorSearch( descriptorPath, ApplicationDescriptorType.TEMPLATE );
				}
			} catch ( InvalidPathException ipe ) {
				// Skip invalid paths. This can happen if an extensions gets registered which invalid chars. No need to blow up, just ignore.
				continue;
			}
		}
		// Nothing found in this directory
		return null;
	}

	private record ApplicationDescriptorSearch( Path path, ApplicationDescriptorType type ) {
	}

}
