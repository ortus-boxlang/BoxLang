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
package ortus.boxlang.runtime.modules;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class represents a module record
 */
public class ModuleRecord {

	/**
	 * Unique internal ID for the module
	 */
	public final String		id							= UUID.randomUUID().toString();

	/**
	 * The name of the module
	 */
	public Key				name;

	/**
	 * The version of the module
	 */
	public String			version						= "1.0.0";

	/**
	 * The author of the module
	 */
	public String			author						= "";

	/**
	 * The description of the module
	 */
	public String			description					= "";

	/**
	 * The web URL of the module
	 */
	public String			webURL						= "";

	/**
	 * The mapping of the module
	 */
	public String			mapping						= "";

	/**
	 * If the module is disabled for activation
	 */
	public boolean			disabled					= false;

	/**
	 * The settings of the module
	 */
	public Struct			settings					= new Struct();

	/**
	 * The interceptors of the module
	 */
	public List<Struct>		interceptors				= List.of();

	/**
	 * The custom interception points of the module
	 */
	public List<String>		customInterceptionPoints	= List.of();

	/**
	 * The physical path of the module
	 */
	public String			physicalPath				= "";

	/**
	 * The invocation path of the module
	 */
	public String			invocationPath				= "";

	/**
	 * The timestamp when the module was registered
	 */
	public final Instant	registrationTime			= Instant.now();

	/**
	 * The timestamp when the module was activated
	 */
	public Instant			activationTime;

	/**
	 * The time it took to startup the module
	 */
	public Instant			startupTime;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name         The name of the module
	 * @param physicalPath The physical path of the module
	 */
	public ModuleRecord( Key name, String physicalPath ) {
		this.name			= name;
		this.physicalPath	= physicalPath;
	}

	/**
	 * If the module is disabled for activation
	 *
	 * @return {@code true} if the module is disabled for activation, {@code false} otherwise
	 */
	public boolean isDisabled() {
		return disabled;
	}

	/**
	 * Get a string representation of the module record
	 */
	public String toString() {
		return asStruct().toString();
	}

	/**
	 * Get a struct representation of the module record
	 *
	 * @return A struct representation of the module record
	 */
	public IStruct asStruct() {
		return Struct.of(
		    "ID", id,
		    "name", name,
		    "version", version,
		    "author", author,
		    "description", description,
		    "webURL", webURL,
		    "mapping", mapping,
		    "disabled", disabled,
		    "settings", settings,
		    "interceptors", interceptors,
		    "customInterceptionPoints", customInterceptionPoints,
		    "physicalPath", physicalPath,
		    "invocationPath", invocationPath,
		    "registrationTime", registrationTime,
		    "activationTime", activationTime,
		    "startupTime", startupTime
		);
	}

}
