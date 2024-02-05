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
package ortus.boxlang.runtime.components;

import java.util.Set;

import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Represents an attribute to a Component
 *
 * @param name         The name of the attribute
 * @param type         The type of the attribute
 * @param validators   Validators for the attribute
 * @param defaultValue The default value of the attribute
 * @param requires     Attributes that are required for this attribute to be valid
 *
 */
public record Attribute( Key name, String type, Object defaultValue, Set<Validator> validators ) {

	/**
	 * Create an attribute declaration with a name accepting any type, but no default value or validators.
	 * 
	 * @param name The name of the attribute
	 */
	public Attribute( Key name ) {
		this( name, "any" );
	}

	/**
	 * Create an attribute declaration with a name and type but no default value or validators.
	 * 
	 * @param name The name of the attribute
	 * @param type The type of the attribute
	 */
	public Attribute( Key name, String type ) {
		this( name, type, null, Set.of() );
	}

	/**
	 * Create an attribute declaration with a name and type and default value, but no validators.
	 * 
	 * @param name         The name of the attribute
	 * @param type         The type of the attribute
	 * @param defaultValue The default value of the attribute
	 */
	public Attribute( Key name, String type, Object defaultValue ) {
		this( name, type, defaultValue, Set.of() );
	}

	/**
	 * Create an attribute declaration with a name and type and validators but no default value.
	 * 
	 * @param name         The name of the attribute
	 * @param type         The type of the attribute
	 * @param defaultValue The default value of the attribute
	 * @param validators   Validators for the attribute
	 */
	public Attribute( Key name, String type, Set<Validator> validators ) {
		this( name, type, null, validators );
	}

	/**
	 * Validate myself
	 * 
	 * @param context
	 * @param component
	 * @param attributes
	 */
	public void validate( IBoxContext context, Component component, IStruct attributes ) {
		// loop over validators and call
		for ( Validator validator : this.validators() ) {
			// Automatically enforce type, if set. This always happens first.
			Validator.TYPE.validate( context, component, this, attributes );
			// Now run the rest of the validators
			validator.validate( context, component, this, attributes );
			// Automatically enforce default value, if set. This always happens last.
			Validator.DEFAULT_VALUE.validate( context, component, this, attributes );
		}
	}

}
