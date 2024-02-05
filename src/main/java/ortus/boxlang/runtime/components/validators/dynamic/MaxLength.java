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
package ortus.boxlang.runtime.components.validators.dynamic;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * I require a string arg that cannot be of greater length than the threshold I'm instantiated with
 */
public class MaxLength implements Validator {

	private Number maxLength;

	public MaxLength( Number maxLength ) {
		this.maxLength = maxLength;
	}

	public void validate( IBoxContext context, Component component, Attribute attribute, IStruct attributes ) {
		// If it was passed...
		if ( attributes.get( attribute.name() ) != null ) {
			// then make sure it's not greater than our threshold
			if ( StringCaster.cast( attributes.get( attribute.name() ) ).length() > this.maxLength.doubleValue() ) {
				throw new BoxValidationException( component, attribute, "cannot be longer than [" + StringCaster.cast( this.maxLength ) + "] character(s)." );
			}
		}
	}

}