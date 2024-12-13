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

import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A BoxLang module configuration from the boxlang.json
 * This is a configuration segment for a module
 * Each module configuration has the following properties:
 * - name: The name of the module
 * - enabled: Whether the module is enabled or not
 * - settings: The settings for the module as a struct
 */
public class ModuleConfig implements IConfigSegment {

	/**
	 * The name of the module
	 */
	public String	name;

	/**
	 * Whether the module is enabled or not
	 */
	public Boolean	enabled		= true;

	/**
	 * The settings for the module as a struct
	 */
	public IStruct	settings	= new Struct();

	/**
	 * Default constructor
	 *
	 * @param name The name of the module
	 */
	public ModuleConfig( String name ) {
		this.name = name;
	}

	/**
	 * Processes the state of the configuration segment from the configuration struct.
	 * <p>
	 * Each segment is processed individually from the initial configuration struct.
	 * This is so we can handle cascading overrides from configuration loading.
	 * <p>
	 *
	 * @param config The state of the segment as a struct
	 *
	 * @return Return itself for chaining
	 */
	public ModuleConfig process( IStruct config ) {
		this.enabled	= BooleanCaster.cast( PropertyHelper.processString( config, Key.enabled, "true" ) );
		this.settings	= StructCaster.cast( config.getOrDefault( Key.settings, new Struct() ) );
		// Process placeholders
		this.settings.forEach( ( key, value ) -> {
			if ( value instanceof String ) {
				this.settings.put( key, PlaceholderHelper.resolve( value ) );
			} else {
				this.settings.put( key, value );
			}
		} );

		return this;
	}

	/**
	 * Returns the configuration as a struct
	 *
	 * @return A struct representation of the configuration segment
	 */
	public IStruct asStruct() {
		return Struct.of(
		    Key._NAME, this.name,
		    Key.enabled, this.enabled,
		    Key.settings, new Struct( this.settings )
		);
	}

}
