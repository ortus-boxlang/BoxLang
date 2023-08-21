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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.events.InterceptorState;
import ortus.boxlang.runtime.functions.FunctionNamespace;
import ortus.boxlang.runtime.functions.BIF;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.ClassDiscovery;

/**
 * The {@code FunctionService} is in charge of managing the runtime's built-in functions.
 * It will also be used by the module services to register functions.
 */
public class FunctionService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private static final String					FUNCTIONS_PACKAGE	= "ortus.boxlang.runtime.functions";

	/**
	 * Logger
	 */
	private static final Logger					logger				= LoggerFactory.getLogger( FunctionService.class );

	/**
	 * Singleton instance
	 */
	private static FunctionService				instance;

	/**
	 * The set of global functions registered with the service
	 */
	private static Map<Key, BIF>				globalFunctions		= new ConcurrentHashMap<>();

	/**
	 * The set of namespaced functions registered with the service
	 */
	private static Map<Key, FunctionNamespace>	namespaces			= new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private FunctionService() {
	}

	/**
	 * Get an instance of the service
	 *
	 * @return The singleton instance
	 */
	public static synchronized FunctionService getInstance() {
		if ( instance == null ) {
			instance = new FunctionService();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	public static void onStartup() {
		logger.info( "FunctionService.onStartup()" );
		// Load global functions
		try {
			loadGlobalFunctions();
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Cannot load global functions", e );
		}
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	public static void onConfigurationLoad() {
		logger.info( "FunctionService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	public static void onShutdown() {
		logger.info( "FunctionService.onShutdown()" );
	}

	public static void loadGlobalFunctions() throws IOException {
		globalFunctions = ClassDiscovery.getClassFilesAsStream( FUNCTIONS_PACKAGE + ".global" )
		        .collect(
		                Collectors.toConcurrentMap(
		                        Key::of,
		                        value -> null,  // We lazy load all functions, this is just discovery
		                        ( existingValue, newValue ) -> existingValue,
		                        ConcurrentHashMap::new
		                )
		        );
	}
}
