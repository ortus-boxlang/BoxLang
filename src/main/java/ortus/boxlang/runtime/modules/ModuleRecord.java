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
import ortus.boxlang.runtime.services.ModuleService;
import ortus.boxlang.runtime.types.Array;
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
	 * Flag to indicate if the module has been activated or not
	 */
	public boolean			activated					= false;

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
	 * The invocation path of the module which is a composition of the
	 * {@link ModuleService#MODULE_MAPPING_PREFIX} and the module name.
	 * Example: {@code /bxModules/MyModule} is the mapping for the module
	 * the invocation path would be {@code bxModules.MyModule}
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
	 * The class loader for the module
	 */
	public ClassLoader		classLoader					= null;

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
		// Register the automatic mapping by convention: /bxModules/{name}
		this.mapping		= ModuleService.MODULE_MAPPING_PREFIX + name.getName();
		// Register the invocation path by convention: bxModules.{name}
		this.invocationPath	= ModuleService.MODULE_MAPPING_INVOCATION_PREFIX + name.getName();
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
	 * If the module is activated
	 *
	 * @return {@code true} if the module is activated, {@code false} otherwise
	 */
	public boolean isActivated() {
		return activated;
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
		    "activationTime", activationTime,
		    "activated", activated,
		    "author", author,
		    "customInterceptionPoints", Array.copyOf( customInterceptionPoints ),
		    "description", description,
		    "disabled", disabled,
		    "Id", id,
		    "interceptors", Array.copyOf( interceptors ),
		    "invocationPath", invocationPath,
		    "mapping", mapping,
		    "name", name,
		    "physicalPath", physicalPath,
		    "registrationTime", registrationTime,
		    "settings", settings,
		    "startupTime", startupTime,
		    "version", version,
		    "webURL", webURL
		);
	}

}
