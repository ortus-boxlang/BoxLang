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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.scopes.Key;

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
	private Map<Key, Application>	applications	= new ConcurrentHashMap<>();

	/**
	 * Logger
	 */
	private static final Logger		logger			= LoggerFactory.getLogger( ApplicationService.class );

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
		super( runtime );
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
		// TODO: Application settings
		// TODO: possible Application listener class
		Application thisApplication = applications.get( name );
		if ( thisApplication == null ) {
			synchronized ( applications ) {
				thisApplication = applications.get( name );
				if ( thisApplication == null ) {
					thisApplication = new Application( name );
					applications.put( name, thisApplication );
				}
			}
		}
		return thisApplication;
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
		logger.info( "ApplicationService.onStartup()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		// loop over applications and shutdown
		applications.values().parallelStream().forEach( Application::shutdown );
		logger.info( "ApplicationService.onShutdown()" );
	}
}
