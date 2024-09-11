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
package ortus.boxlang.runtime.scopes;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;
import ortus.boxlang.runtime.util.NetworkUtil;

/**
 * Represents the BoxLang "server" scope container
 * <p>
 * Note, this doesn't have to be a "web", it can reprsesent any long-running runtime which
 * processes one or more "requests" for execution.
 * </p>
 * <p>
 * Unmodifiables keys are : os, separator, java, system
 * </p>
 */
public class ServerScope extends BaseScope {

	private static final Logger		logger				= LoggerFactory.getLogger( ServerScope.class );

	/**
	 * These keys cannot be set once the scope is initialized
	 */
	private static final List<Key>	unmodifiableKeys	= List.of(
	    Key.coldfusion,
	    Key.java,
	    Key.os,
	    Key.separator,
	    Key.system
	);

	/**
	 * Unmodifiable keys can be modified up until this switches to true
	 */
	private boolean					initialized			= false;

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key			name				= Key.server;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ServerScope() {
		super( ServerScope.name );
		seedScope();
	}

	/**
	 * Initialize the Server scope, so now modules can collaborate
	 */
	@Override
	public IScope initialize() {
		// announce the scope creation
		BoxRuntime.getInstance().announce(
		    BoxEvent.ON_SERVER_SCOPE_CREATION,
		    Struct.of(
		        "scope", this,
		        "name", ServerScope.name
		    )
		);
		this.initialized = true;

		logger.debug( "Server Scope Constructed and Initialized" );

		return this;
	}

	/**
	 * Put a value into the scope container and throw an exception if the key is unmodifiable.
	 * Unmodifiables keys are : coldfusion, os, separator, java, system
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 */
	@Override
	public Object put( Key key, Object value ) {
		if ( this.initialized && unmodifiableKeys.contains( key ) ) {
			throw new BoxRuntimeException( String.format( "Cannot modify key %s in server scope", key ) );
		}
		return super.put( key, value );
	}

	/**
	 * Create default keys always present in the server scope
	 * - coldfusion
	 * - os
	 * - separator
	 * - java
	 * - system
	 */
	private void seedScope() {
		BoxRuntime runtime = BoxRuntime.getInstance();

		// BoxLang Version Info
		put(
		    Key.boxlang,
		    new ImmutableStruct( runtime.getVersionInfo() )
		);

		put( Key.os, ImmutableStruct.of(
		    "additionalinformation", "",
		    "arch", System.getProperty( "os.arch", "" ),
		    "archModel", System.getProperty( "os.arch", "" ),
		    "hostname", NetworkUtil.getLocalHostname(),
		    "ipAddress", NetworkUtil.getLocalIPAddress(),
		    "macAddress", NetworkUtil.getLocalMacAddress(),
		    "name", System.getProperty( "os.name" ),
		    "version", System.getProperty( "os.version" )
		) );

		put( Key.separator, ImmutableStruct.of(
		    "path", System.getProperty( "path.separator", "" ),
		    "file", System.getProperty( "file.separator", "" ),
		    "line", System.getProperty( "line.separator", "" )
		) );

		Runtime javaRuntime = Runtime.getRuntime();
		put( Key.java, ImmutableStruct.of(
		    "archModel", System.getProperty( "os.arch", "" ),
		    "executionPath", System.getProperty( "user.dir", "" ),
		    "executionCommand", System.getProperty( "sun.java.command", "" ),
		    "executionArgs", System.getProperty( "boxlang.cliArgs", "" ),
		    "freeMemory", javaRuntime.freeMemory(),
		    "maxMemory", javaRuntime.maxMemory(),
		    "totalMemory", javaRuntime.totalMemory(),
		    "vendor", System.getProperty( "java.vendor", "" ),
		    "version", System.getProperty( "java.version", "" )
		) );

		IStruct	env		= ImmutableStruct.fromMap( System.getenv() );
		IStruct	props	= ImmutableStruct.fromMap( System.getProperties() );
		put( Key.system, ImmutableStruct.of(
		    // TODO: create wrapper struct that gives live view of env vars, not just a copy
		    "environment", env,
		    // TODO: create wrapper struct that gives live view of system properties, not just a copy
		    "properties", props
		) );

	}

}
