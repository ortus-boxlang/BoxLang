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
package ortus.boxlang.runtime.components.validators;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.dynamic.Max;
import ortus.boxlang.runtime.components.validators.dynamic.MaxLength;
import ortus.boxlang.runtime.components.validators.dynamic.Min;
import ortus.boxlang.runtime.components.validators.dynamic.MinLength;
import ortus.boxlang.runtime.components.validators.dynamic.Requires;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * I help validate attributes
 */
@FunctionalInterface
public interface Validator {

	// These are static validator which can be re-used
	public static final Validator	REQUIRED		= new Required();
	public static final Validator	NON_EMPTY		= new MinLength( 1 );
	// This validator will get the type from the attribute record
	public static final Validator	TYPE			= new Type();
	// This validator will get the default value from the attribute record
	public static final Validator	DEFAULT_VALUE	= new DefaultValue();

	/**
	 * Validate an attribute.
	 * 
	 * @param context    The current Box context
	 * @param component  The component being validated
	 * @param attribute  The specific attribute being validated
	 * @param attributes All the attributes being validated
	 */
	public void validate( IBoxContext context, Component component, Attribute attribute, IStruct attributes );

	/****************************************************************************************
	 * These are builder methods to create validators that hold some state
	 * concerning how the validation will be performed at runtime.
	 ****************************************************************************************/

	/**
	 * Builder method to create a Min validator
	 * 
	 * @param min The minimum value
	 * 
	 * @return The Min validator
	 */
	public static Validator min( Number min ) {
		return new Min( min );
	}

	/**
	 * Builder method to create a Max validator
	 * 
	 * @param max The maximum value
	 * 
	 * @return The Max validator
	 */
	public static Validator max( Number max ) {
		return new Max( max );
	}

	/**
	 * Builder method to create a MaxLength validator
	 * 
	 * @param maxLength The maximum length
	 * 
	 * @return The MaxLength validator
	 */
	public static Validator maxLength( Number maxLength ) {
		return new MaxLength( maxLength );
	}

	/**
	 * Builder method to create a MinLength validator
	 * 
	 * @param minLength The minimum length
	 * 
	 * @return The MinLength validator
	 */
	public static Validator minLength( Number minLength ) {
		return new MinLength( minLength );
	}

	/**
	 * Builder method to create a Requires validator
	 * 
	 * @param attributeNames The names of the attributes that are required if this attribute is present
	 * 
	 * @return The Requires validator
	 */
	public static Validator requires( Key... attributeNames ) {
		return new Requires( Set.of( attributeNames ) );
	}

}