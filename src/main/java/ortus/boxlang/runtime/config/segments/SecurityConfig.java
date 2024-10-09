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
package ortus.boxlang.runtime.config.segments;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * The SecurityConfig class is a configuration segment that is used to define the security settings for the BoxLang runtime.
 */
public class SecurityConfig implements IConfigSegment {

	/**
	 * A list of disallowed imports for the runtime
	 * These are a list of regular expressions that are used to match against the import statements in the code
	 * Ex: "disallowedImports": ["java\\.lang\\.(ProcessBuilder|Reflect", "java\\.io\\.(File|FileWriter)"]
	 */
	public Set<String>			disallowedImports		= new HashSet<>();

	/**
	 * A list of regex patterns that will match expressions in any type of source, and if matched, execution will be disallowed
	 * Careful here, you can disallow a lot of things
	 * Ex: "disallowedExpressions": [ "(\s)Runtime\\.(.*)" ]
	 */
	public Set<String>			disallowedExpressions	= new HashSet<>();

	/**
	 * Disallowed BIFs in the runtime
	 * Ex: "disallowedBifs": ["createObject", "systemExecute"]
	 */
	public Set<String>			disallowedBIFs			= new HashSet<>();

	/**
	 * Disallowed Components in the runtime
	 * Ex: "disallowedComponents": [ "execute", "http" ]
	 */
	public Set<String>			disallowedComponents	= new HashSet<>();

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger	logger					= LoggerFactory.getLogger( SecurityConfig.class );

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default empty constructor
	 */
	public SecurityConfig() {
		// Default all things
	}

	/**
	 * This function takes in the name of a BIF to test if it is disallowed.
	 * The search is case-insensitive.
	 * If it's disallowed, it will throw a SecurityException, else it will return true
	 */
	public boolean isBIFDisallowed( String name ) {
		if ( this.disallowedBIFs.stream().anyMatch( name::equalsIgnoreCase ) ) {
			throw new SecurityException( "The BIF '" + name + "' is disallowed, please check your security configuration in the language configuration file." );
		}
		return true;
	}

	/**
	 * This function takes in the name of a Component to test if it is disallowed.
	 * The search is case-insensitive.
	 * If it's disallowed, it will throw a SecurityException, else it will return true
	 */
	public boolean isComponentDisallowed( String name ) {
		if ( this.disallowedComponents.stream().anyMatch( name::equalsIgnoreCase ) ) {
			throw new SecurityException(
			    "The Component '" + name + "' is disallowed, please check your security configuration in the language configuration file." );
		}
		return true;
	}

	/**
	 * Processes the configuration struct. Each segment is processed individually from the initial configuration struct.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		PropertyHelper.processListToSet( config, Key.disallowedImports, this.disallowedImports );
		PropertyHelper.processListToSet( config, Key.disallowedExpressions, this.disallowedExpressions );
		PropertyHelper.processListToSet( config, Key.disallowedBIFs, this.disallowedBIFs );
		PropertyHelper.processListToSet( config, Key.disallowedComponents, this.disallowedComponents );
		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.of(
		    Key.disallowedImports, this.disallowedImports,
		    Key.disallowedExpressions, this.disallowedExpressions,
		    Key.disallowedBIFs, this.disallowedBIFs,
		    Key.disallowedComponents, this.disallowedComponents
		);
	}

}
