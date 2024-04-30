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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.application.ApplicationClassListener;
import ortus.boxlang.runtime.application.ApplicationDefaultListener;
import ortus.boxlang.runtime.application.ApplicationListener;
import ortus.boxlang.runtime.application.ApplicationTemplateListener;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
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
	 * TODO: timeout applications
	 */
	private Map<Key, Application>	applications							= new ConcurrentHashMap<>();

	/**
	 * Extensions to search for application descriptor templates
	 */
	// TODO: contribute cfc from compat extension
	private Set<String>				applicationDescriptorClassExtensions	= new HashSet<>( Arrays.asList( "bx", "cfc" ) );

	/**
	 * Extensions to search for application descriptor classes
	 */
	// TODO: contribute cfc from compat extension
	private Set<String>				applicationDescriptorExtensions			= new HashSet<>( Arrays.asList( "bxm", "cfm" ) );

	/**
	 * The types of application listeners we support: Application classes and Application templates
	 */
	private enum ApplicationDescriptorType {
		CLASS,
		TEMPLATE
	}

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( ApplicationService.class );

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
		Application thisApplication = applications.computeIfAbsent( name, k -> new Application( name ) );

		// logger.info( "ApplicationService.getApplication() - {}", name );

		return thisApplication;
	}

	/**
	 * Terminates an application by name
	 *
	 * @param name The name of the application
	 *
	 */
	public void removeApplication( Key name ) {
		if ( applications.containsKey( name ) ) {
			applications.remove( name );
		}
		// logger.info( "ApplicationService.removeApplication() - {}", name );
	}

	/**
	 * Check if an application exists
	 *
	 * @param name The name of the application
	 *
	 * @return True if the application exists
	 */
	boolean hasApplication( Key name ) {
		return applications.containsKey( name );
	}

	/**
	 * Get the names of all registered applications
	 *
	 * @return The names of all applications
	 */
	String[] getApplicationNames() {
		return applications.keySet()
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
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		// logger.info( "ApplicationService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force If true, forces the shutdown of the scheduler
	 */
	@Override
	public void onShutdown( Boolean force ) {
		// loop over applications and shutdown as the runtime is going down.
		applications.values().parallelStream().forEach( Application::shutdown );
	}

	/**
	 * Create an ApplicationListener based on the template path
	 *
	 * @param context  The request context requesting the application
	 * @param template The template path to search for an Application descriptor
	 *
	 * @return The ApplicationListener in the template path or a new one if not found
	 */
	public ApplicationListener createApplicationListener( RequestBoxContext context, URI template ) {
		ApplicationListener			listener;
		ApplicationDescriptorSearch	searchResult	= null;
		if ( template != null ) {

			// Look for an Application descriptor based on our lookup rules
			String	directoryOfTemplate	= null;
			String	packagePath			= "";
			String	rootMapping			= context.getConfig().getAsStruct( Key.runtime ).getAsStruct( Key.mappings ).getAsString( Key._slash );
			if ( template.isAbsolute() ) {
				directoryOfTemplate	= new File( template ).getParent();
				searchResult		= fileLookup( directoryOfTemplate );
			} else {
				directoryOfTemplate = new File( template.toString() ).getParent();
				while ( directoryOfTemplate != null ) {
					if ( directoryOfTemplate.equals( File.separator ) ) {
						searchResult = fileLookup( rootMapping );
					} else {
						searchResult = fileLookup( Paths.get( rootMapping, directoryOfTemplate ).toString() );
					}
					if ( searchResult != null ) {
						// set packagePath to the relative path from the rootMapping to the directoryOfTemplate with slashes replaced with dots
						packagePath = directoryOfTemplate.replace( File.separator, "." );
						if ( packagePath.endsWith( "." ) ) {
							packagePath = packagePath.substring( 0, packagePath.length() - 1 );
						}
						// trim leading .
						if ( packagePath.startsWith( "." ) ) {
							packagePath = packagePath.substring( 1 );
						}
						break;
					}
					directoryOfTemplate = new File( directoryOfTemplate ).getParent();
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
					                "/",
					                rootMapping,
					                packagePath.replaceAll( "\\.", File.separator ) + File.separator
					                    + searchResult.path().getFileName(),
					                searchResult.path()
					            ),
					            context
					        )
					)
					    .invokeConstructor( context )
					    .getTargetInstance(),
					    context
					);
				} else {
					// If we found a template, return a new empty ApplicationListener
					listener = new ApplicationTemplateListener(
					    RunnableLoader.getInstance().loadTemplateAbsolute(
					        context,
					        ResolvedFilePath.of(
					            "/",
					            rootMapping,
					            packagePath.replaceAll( "\\.", Matcher.quoteReplacement( File.separator ) ) + File.separator
					                + searchResult.path().getFileName(),
					            searchResult.path()
					        )
					    ),
					    context
					);
				}
			} else {
				// If we didn't find an Application, return a new empty ApplicationListener
				listener = new ApplicationDefaultListener( context );
			}
		} else {
			// If we didn't have a template, return a new empty ApplicationListener
			listener = new ApplicationDefaultListener( context );
		}

		// Announce event so modules can hook in
		announce(
		    BoxEvent.AFTER_APPLICATION_LISTENER_LOAD,
		    Struct.of(
		        "listener", listener,
		        "context", context,
		        "template", template
		    )
		);

		// Now that the settings are in place, actually define the app (and possibly session) in this request
		listener.defineApplication();

		return listener;
	}

	/**
	 * Search a directory for all known file extensions.
	 * TODO: Cache this lookup when in a production mode
	 */
	private ApplicationDescriptorSearch fileLookup( String path ) {
		// Look for a class first
		for ( var extension : applicationDescriptorClassExtensions ) {
			var descriptorPath = Paths.get( path, "Application." + extension );
			if ( descriptorPath.toFile().exists() ) {
				return new ApplicationDescriptorSearch( descriptorPath, ApplicationDescriptorType.CLASS );
			}
		}
		// Then a template
		for ( var extension : applicationDescriptorExtensions ) {
			var descriptorPath = Paths.get( path, "Application." + extension );
			if ( descriptorPath.toFile().exists() ) {
				return new ApplicationDescriptorSearch( descriptorPath, ApplicationDescriptorType.TEMPLATE );
			}
		}
		// Nothing found in this directory
		return null;
	}

	private record ApplicationDescriptorSearch( Path path, ApplicationDescriptorType type ) {
	}

}
