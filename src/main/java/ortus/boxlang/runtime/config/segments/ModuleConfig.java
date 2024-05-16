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
public class ModuleConfig {

	/**
	 * The name of the module
	 */
	public String	name;

	/**
	 * Whether the module is disabled or not
	 */
	public Boolean	disabled	= false;

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
	 * Process the settings for the module
	 */
	public ModuleConfig process( IStruct config ) {
		// Check if the module is enabled
		if ( config.containsKey( "disabled" ) ) {
			this.disabled = BooleanCaster.cast( PlaceholderHelper.resolve( config.getOrDefault( "disabled", false ) ) );
		}

		// Store the settings
		this.settings = StructCaster.cast( config.getOrDefault( Key.settings, new Struct() ) );
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
	 * Remember that this is what the context's use to build runtime/request configs, so don't use any references
	 */
	public IStruct toStruct() {
		return Struct.of(
		    Key._NAME, this.name,
		    Key.disabled, this.disabled,
		    Key.settings, new Struct( this.settings )
		);
	}

}
