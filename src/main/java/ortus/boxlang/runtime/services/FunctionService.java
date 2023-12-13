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
import ortus.boxlang.runtime.functions.BIF;
import ortus.boxlang.runtime.functions.FunctionDescriptor;
import ortus.boxlang.runtime.functions.FunctionNamespace;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.util.ClassDiscovery;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
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

	private static final String				FUNCTIONS_PACKAGE	= "ortus.boxlang.runtime.functions";

	/**
	 * Logger
	 */
	private static final Logger				logger				= LoggerFactory.getLogger( FunctionService.class );

	/**
	 * The set of global functions registered with the service
	 */
	private Map<Key, FunctionDescriptor>	globalFunctions		= new ConcurrentHashMap<>();

	/**
	 * The set of namespaced functions registered with the service
	 */
	private Map<Key, FunctionNamespace>		namespaces			= new ConcurrentHashMap<>();

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
			throw new ApplicationException( "Cannot load global functions", e );
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

	public long getGlobalFunctionCount() {
		return globalFunctions.size();
	}

	public Set<String> getGlobalFunctionNames() {
		return globalFunctions.keySet().stream().map( Key::getName ).collect( Collectors.toSet() );
	}

	public Boolean hasGlobalFunction( String name ) {
		return globalFunctions.containsKey( Key.of( name ) );
	}

	public FunctionDescriptor getGlobalFunction( String name ) {
		FunctionDescriptor target = globalFunctions.get( Key.of( name ) );
		if ( target == null ) {
			throw new KeyNotFoundException(
			    String.format(
			        "The global function [%s] does not exist.",
			        name
			    ) );
		}
		return target;
	}

	public FunctionDescriptor getGlobalFunctionDescriptor( String name ) {
		return globalFunctions.get( Key.of( name ) );
	}

	public void registerGlobalFunction( FunctionDescriptor descriptor ) throws IllegalArgumentException {
		if ( hasGlobalFunction( descriptor.name ) ) {
			throw new ApplicationException( "Global function " + descriptor.name + " already exists" );
		}
		globalFunctions.put( Key.of( descriptor.name ), descriptor );
	}

	public void registerGlobalFunction( String name, BIF function, String module ) throws IllegalArgumentException {
		if ( hasGlobalFunction( name ) ) {
			throw new ApplicationException( "Global function " + name + " already exists" );
		}

		globalFunctions.put(
		    Key.of( name ),
		    new FunctionDescriptor(
		        name,
		        ClassUtils.getCanonicalName( function.getClass() ),
		        module,
		        null,
		        true,
		        DynamicObject.of( function )
		    )
		);
	}

	public void unregisterGlobalFunction( String name ) {
		globalFunctions.remove( Key.of( name ) );
	}

	public void loadGlobalFunctions() throws IOException {
		globalFunctions = ClassDiscovery
		    .getClassFilesAsStream( FUNCTIONS_PACKAGE + ".global", true )
		    .collect(
		        Collectors.toConcurrentMap(
		            value -> Key.of( ClassUtils.getShortClassName( value ) ),
		            value -> new FunctionDescriptor(
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

	public long getNamespaceCount() {
		return namespaces.size();
	}
}
