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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.bifs.BIFNamespace;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

/**
 * The {@code FunctionService} is in charge of managing the runtime's built-in functions.
 * It will also be used by the module services to register functions.
 */
public class FunctionService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	private static final String		FUNCTIONS_PACKAGE	= "ortus.boxlang.runtime.bifs";

	/**
	 * Logger
	 */
	private static final Logger		logger				= LoggerFactory.getLogger( FunctionService.class );

	/**
	 * The set of global functions registered with the service
	 */
	private Map<Key, BIFDescriptor>	globalFunctions		= new ConcurrentHashMap<>();

	/**
	 * The set of namespaced functions registered with the service
	 */
	private Map<Key, BIFNamespace>	namespaces			= new ConcurrentHashMap<>();

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
	public FunctionService( BoxRuntime runtime ) {
		super( runtime );
		// Load global functions
		try {
			loadGlobalFunctions();
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Cannot load global functions", e );
		}
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
		logger.info( "FunctionService.onStartup()" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	@Override
	public void onConfigurationLoad() {
		logger.info( "FunctionService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		logger.info( "FunctionService.onShutdown()" );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Global Function Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the number of global functions registered with the service
	 *
	 * @return The number of global functions registered with the service
	 */
	public long getGlobalFunctionCount() {
		return this.globalFunctions.size();
	}

	/**
	 * Returns the names of the global functions registered with the service
	 *
	 * @return A set of global function names
	 */
	public Set<String> getGlobalFunctionNames() {
		return this.globalFunctions.keySet().stream().map( Key::getName ).collect( Collectors.toSet() );
	}

	/**
	 * Returns whether or not the service has a global function with the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return Whether or not the service has a global function with the given name
	 */
	public Boolean hasGlobalFunction( String name ) {
		return hasGlobalFunction( Key.of( name ) );
	}

	/**
	 * Returns whether or not the service has a global function with the given name
	 *
	 * @param name The key name of the global function
	 *
	 * @return Whether or not the service has a global function with the given name
	 */
	public Boolean hasGlobalFunction( Key name ) {
		return this.globalFunctions.containsKey( name );
	}

	/**
	 * Returns the global function with the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return The global function with the given name
	 *
	 * @throws KeyNotFoundException If the global function does not exist
	 */
	public BIFDescriptor getGlobalFunction( String name ) {
		return getGlobalFunction( Key.of( name ) );
	}

	/**
	 * Returns the global function with the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return The global function with the given name
	 *
	 * @throws KeyNotFoundException If the global function does not exist
	 */
	public BIFDescriptor getGlobalFunction( Key name ) {
		BIFDescriptor target = this.globalFunctions.get( name );
		if ( target == null ) {
			throw new KeyNotFoundException(
			    String.format(
			        "The global function [%s] does not exist.",
			        name
			    ) );
		}
		return target;
	}

	/**
	 * Gets the global function descriptor for the given name
	 *
	 * @param name The name of the global function
	 *
	 * @return The BIFDescriptor for the global function
	 */
	public BIFDescriptor getGlobalBIFDescriptor( String name ) {
		return getGlobalBIFDescriptor( Key.of( name ) );
	}

	/**
	 * Gets the global function descriptor for the given key
	 *
	 * @param name The key of the global function
	 *
	 * @return The BIFDescriptor for the global function
	 */
	public BIFDescriptor getGlobalBIFDescriptor( Key name ) {
		return this.globalFunctions.get( name );
	}

	/**
	 * Registers a global function with the service
	 *
	 * @param descriptor The descriptor for the global function
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	public void registerGlobalFunction( BIFDescriptor descriptor ) throws IllegalArgumentException {
		if ( hasGlobalFunction( descriptor.name ) ) {
			throw new BoxRuntimeException( "Global function " + descriptor.name + " already exists" );
		}
		this.globalFunctions.put( Key.of( descriptor.name ), descriptor );
	}

	/**
	 * Registers a global function with the service
	 *
	 * @param name     The name of the global function
	 * @param function The global function
	 * @param module   The module the global function belongs to
	 *
	 * @throws IllegalArgumentException If the global function already exists
	 */
	public void registerGlobalFunction( Key name, BIF function, String module ) throws IllegalArgumentException {
		if ( hasGlobalFunction( name ) ) {
			throw new BoxRuntimeException( "Global function " + name.getName() + " already exists" );
		}

		this.globalFunctions.put(
		    name,
		    new BIFDescriptor(
		        name.getName(),
		        ClassUtils.getCanonicalName( function.getClass() ),
		        module,
		        null,
		        true,
		        function
		    )
		);
	}

	/**
	 * Unregisters a global function with the service
	 *
	 * @param name The name of the global function
	 */
	public void unregisterGlobalFunction( Key name ) {
		this.globalFunctions.remove( name );
	}

	/**
	 * This method loads all of the global functions into the service by scanning the
	 * {@code ortus.boxlang.runtime.bifs.global} package.
	 *
	 * @throws IOException If there is an error loading the global functions
	 */
	public void loadGlobalFunctions() throws IOException {
		this.globalFunctions = ClassDiscovery
		    .getClassFilesAsStream( FUNCTIONS_PACKAGE + ".global", true )
		    .collect(
		        Collectors.toConcurrentMap(
		            value -> Key.of( ClassUtils.getShortClassName( value ) ),
		            value -> new BIFDescriptor(
		                ClassUtils.getShortClassName( value ),
		                value,
		                null,
		                null,
		                true,
		                null
		            )
		        )
		    );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Namespace Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The count of registered namespaces
	 *
	 * @return The count of registered namespaces
	 */
	public long getNamespaceCount() {
		return this.namespaces.size();
	}
}
