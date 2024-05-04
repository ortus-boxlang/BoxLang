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
import java.util.Map;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Represents the BoxLang "server" scope container
 * <p>
 * Note, this doesn't have to be a "web", it can reprsesent any long-running runtime which
 * processes one or more "requests" for execution.
 * </p>
 * <p>
 * Unmodifiables keys are : coldfusion, os, lucee, separator, java, servlet, system
 * </p>
 */
public class ServerScope extends BaseScope {

	/**
	 * These keys cannot be set once the scope is initialized
	 */
	private static final List<Key>	unmodifiableKeys	= List.of(
	    Key.coldfusion,
	    Key.java,
	    Key.lucee,
	    Key.os,
	    Key.separator,
	    Key.servlet,
	    Key.system
	);

	/**
	 * Unmodifiable keys can be modified up until this switches to true
	 */
	private boolean					intialized			= false;

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
		this.intialized = true;
	}

	/**
	 * Put a value into the scope container and throw an exception if the key is unmodifiable.
	 * Unmodifiables keys are : coldfusion, os, lucee, separator, java, servlet, system
	 *
	 * @param key   The key to set
	 * @param value The value to set
	 */
	@Override
	public Object put( Key key, Object value ) {
		if ( this.intialized && unmodifiableKeys.contains( key ) ) {
			throw new BoxRuntimeException( String.format( "Cannot modify key %s in server scope", key ) );
		}
		return super.put( key, value );
	}

	/**
	 * Create default keys always present in the server scope
	 * - coldfusion
	 * - os
	 * - lucee
	 * - separator
	 * - java
	 * - servlet
	 * - system
	 */
	@SuppressWarnings( "unchecked" )
	private void seedScope() {

		// TODO: switch to immutable struct
		put( Key.coldfusion, Struct.of(
		    // TODO: Compat?
		    "productName", "BoxLang",
		    "productVersion", "0.0.0"
		) );

		// TODO: switch to immutable struct
		put( Key.os, Struct.of(
		    "additionalinformation", "",
		    "arch", System.getProperty( "os.arch", "" ),
		    "archModel", System.getProperty( "os.arch", "" ),
		    "buildnumber", "",
		    // TODO: Lucee-only. Does it even belong?
		    "hostname", "",
		    // TODO: watch out for performance issues
		    "macAddress", "",
		    "name", System.getProperty( "os.name" ),
		    "version", System.getProperty( "os.version" )
		) );

		// TODO: switch to immutable struct
		put( Key.lucee, Struct.of(
		    // TODO: Compat?
		    "version", "0.0.0"
		) );

		// TODO: switch to immutable struct
		put( Key.separator, Struct.of(
		    "path", System.getProperty( "path.separator", "" ),
		    "file", System.getProperty( "file.separator", "" ),
		    "line", System.getProperty( "line.separator", "" )
		) );

		Runtime rt = Runtime.getRuntime();
		// TODO: switch to immutable struct
		put( Key.java, Struct.of(
		    "archModel", System.getProperty( "os.arch", "" ),
		    "executionPath", System.getProperty( "user.dir", "" ),
		    "freeMemory", rt.freeMemory(),
		    "maxMemory", rt.maxMemory(),
		    "totalMemory", rt.totalMemory(),
		    "vendor", System.getProperty( "java.vendor", "" ),
		    "version", System.getProperty( "java.version", "" )

		) );

		// TODO: Move this to web module later
		// TODO: switch to immutable struct
		put( Key.servlet, Struct.of(
		    "name", ""
		) );

		IStruct	env		= new Struct( ( Map ) System.getenv() );
		IStruct	props	= new Struct( ( Map ) System.getProperties() );

		put( Key.system, Struct.of(
		    // TODO: create wrapper struct that gives live view of env vars, not just a copy
		    "environment", env,
		    // TODO: create wrapper struct that gives live view of system properties, not just a copy
		    "properties", props
		) );
	}

}
