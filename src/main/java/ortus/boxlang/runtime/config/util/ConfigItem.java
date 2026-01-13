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
package ortus.boxlang.runtime.config.util;

import java.io.Serializable;
import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * Represents a configuration item with validation support
 *
 * @param name              The name of the config item
 * @param required          Whether the config item is required
 * @param type              The type of the config item
 * @param defaultExpression The default value of the config item as a Lambda to be evaluated at runtime
 * @param validators        Validators for the config item
 * @param description       Description of the config item
 */
public record ConfigItem(
    Key name,
    boolean required,
    String type,
    DefaultExpression defaultExpression,
    Set<Validator> validators,
    String description ) implements Validatable, Serializable {

	// Serializable
	private static final long serialVersionUID = 1L;

	/**
	 * Canonical constructor
	 */
	public ConfigItem( Key name, boolean required, String type, DefaultExpression defaultExpression, Set<Validator> validators, String description ) {
		this.name				= name;
		this.required			= required;
		this.type				= type;
		this.defaultExpression	= defaultExpression;
		this.validators			= validators;
		this.description		= description;
	}

	/**
	 * Factory method with only name
	 *
	 * @param name The name of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name ) {
		return new ConfigItem( name, false, "any", null, Set.of(), null );
	}

	/**
	 * Factory method with name and description
	 *
	 * @param name        The name of the config item
	 * @param description Description of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, String type, String description ) {
		return new ConfigItem( name, false, type, null, Set.of(), description );
	}

	/**
	 * Factory method with name and type
	 *
	 * @param name The name of the config item
	 * @param type The type of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, String type ) {
		return new ConfigItem( name, false, type, null, Set.of(), null );
	}

	/**
	 * Factory method with name and required
	 *
	 * @param name     The name of the config item
	 * @param required Whether the config item is required
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required ) {
		return new ConfigItem( name, required, "any", null, Set.of(), null );
	}

	/**
	 * Factory method with name, required, and type
	 *
	 * @param name     The name of the config item
	 * @param required Whether the config item is required
	 * @param type     The type of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type ) {
		return new ConfigItem( name, required, type, null, Set.of(), null );
	}

	/**
	 * Factory method with name, required, type, and description
	 *
	 * @param name        The name of the config item
	 * @param required    Whether the config item is required
	 * @param type        The type of the config item
	 * @param description Description of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, String description ) {
		return new ConfigItem( name, required, type, null, Set.of(), description );
	}

	/**
	 * Factory method with name, required, type, and validators
	 *
	 * @param name       The name of the config item
	 * @param required   Whether the config item is required
	 * @param type       The type of the config item
	 * @param validators Validators for the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, Set<Validator> validators ) {
		return new ConfigItem( name, required, type, null, validators, null );
	}

	/**
	 * Factory method with name, required, type, validators, and description
	 *
	 * @param name        The name of the config item
	 * @param required    Whether the config item is required
	 * @param type        The type of the config item
	 * @param validators  Validators for the config item
	 * @param description Description of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, Set<Validator> validators, String description ) {
		return new ConfigItem( name, required, type, null, validators, description );
	}

	/**
	 * Factory method with name, required, type, and default expression
	 *
	 * @param name              The name of the config item
	 * @param required          Whether the config item is required
	 * @param type              The type of the config item
	 * @param defaultExpression The default expression to evaluate at runtime
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, DefaultExpression defaultExpression ) {
		return new ConfigItem( name, required, type, defaultExpression, Set.of(), null );
	}

	/**
	 * Factory method with name, required, type, default expression, and description
	 *
	 * @param name              The name of the config item
	 * @param required          Whether the config item is required
	 * @param type              The type of the config item
	 * @param defaultExpression The default expression to evaluate at runtime
	 * @param description       Description of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, DefaultExpression defaultExpression, String description ) {
		return new ConfigItem( name, required, type, defaultExpression, Set.of(), description );
	}

	/**
	 * Factory method with all parameters (canonical)
	 *
	 * @param name              The name of the config item
	 * @param required          Whether the config item is required
	 * @param type              The type of the config item
	 * @param defaultExpression The default expression to evaluate at runtime
	 * @param validators        Validators for the config item
	 * @param description       Description of the config item
	 *
	 * @return A new ConfigItem instance
	 */
	public static ConfigItem of( Key name, boolean required, String type, DefaultExpression defaultExpression, Set<Validator> validators, String description ) {
		return new ConfigItem( name, required, type, defaultExpression, validators, description );
	}

	/**
	 * Check if this config item has a default expression
	 *
	 * @return true if a default expression is set
	 */
	@Override
	public boolean hasDefaultValue() {
		return defaultExpression != null;
	}

	/**
	 * Get the default value by evaluating the default expression
	 *
	 * @param context The context to evaluate the expression in
	 *
	 * @return The default value, or null if no default expression is set
	 */
	@Override
	public Object getDefaultValue( IBoxContext context ) {
		if ( this.defaultExpression != null ) {
			return this.defaultExpression.evaluate( context );
		}
		return null;
	}

	/**
	 * Build description for error messages
	 * 
	 * @return The built description
	 */
	public String buildDescription() {
		if ( description != null && !description.isBlank() ) {
			return String.format( "Configuration item [%s] in %s ", name().toString(), description );
		} else {
			return String.format( "Configuration item [%s]", name().toString() );
		}
	}

}
