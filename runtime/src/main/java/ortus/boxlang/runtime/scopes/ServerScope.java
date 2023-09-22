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

import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * represents boxlang server scope container
 * Note, this doesn't have to be a "web", it can reprsesent any long-running runtime which
 * processes one or more "requests" for execution.
 */
public class ServerScope extends BaseScope {

	/**
	 * These keys cannot be set once the scope is initialized
	 */
	private static final List<Key>	unmodifiableKeys	= List.of(
	    Key.of( "coldfusion" ),
	    Key.of( "os" ),
	    Key.of( "lucee" ),
	    Key.of( "separator" ),
	    Key.of( "java" ),
	    Key.of( "servlet" ),
	    Key.of( "system" )
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
	public static final Key			name				= Key.of( "server" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ServerScope() {
		super( ServerScope.name );

		seedScope();
		intialized = true;
	}

	@Override
	public Object put( Key key, Object value ) {
		if ( intialized && unmodifiableKeys.contains( key ) ) {
			throw new ApplicationException( "Cannot modify key " + key + " in server scope" );
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
		put( Key.of( "coldfusion" ), Struct.of(
		    // TODO: Compat?
		) );

		// TODO: switch to immutable struct
		put( Key.of( "os" ), Struct.of(
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
		put( Key.of( "lucee" ), Struct.of(
		    // TODO: Compat?
		) );

		// TODO: switch to immutable struct
		put( Key.of( "separator" ), Struct.of(
		    "path", System.getProperty( "path.separator", "" ),
		    "file", System.getProperty( "file.separator", "" ),
		    "line", System.getProperty( "line.separator", "" )
		) );

		Runtime rt = Runtime.getRuntime();
		// TODO: switch to immutable struct
		put( Key.of( "java" ), Struct.of(
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
		put( Key.of( "servlet" ), Struct.of(
		    "name", ""
		) );

		Struct	env		= new Struct( ( Map ) System.getenv() );
		Struct	props	= new Struct( ( Map ) System.getProperties() );

		put( Key.of( "system" ), Struct.of(
		    // TODO: create wrapper struct that gives live view of env vars, not just a copy
		    "environment", env,
		    // TODO: create wrapper struct that gives live view of system properties, not just a copy
		    "properties", props
		) );
	}

}
