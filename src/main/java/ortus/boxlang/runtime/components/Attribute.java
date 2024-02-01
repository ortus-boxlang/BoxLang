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
public record Attribute( Key name, String type, Set<Validator> validators, Object defaultValue, Set<Key> requires ) {

	public Attribute( Key name ) {
		this( name, "any" );
	}

	public Attribute( Key name, String type, Object defaultValue ) {
		this( name, type, Set.of(), defaultValue );
	}

	public Attribute( Key name, String type ) {
		this( name, type, Set.of() );
	}

	public Attribute( Key name, String type, Set<Validator> validators ) {
		this( name, type, validators, null );
	}

	public Attribute( Key name, String type, Set<Validator> validators, Object defaultValue ) {
		this( name, type, validators, defaultValue, Set.of() );
	}

	// validate myself
	public void validate( IBoxContext context, Component component, IStruct attributes ) {
		// loop over validators and call
		for ( Validator validator : this.validators() ) {
			validator.validate( context, component, this, attributes );
		}
	}

}
