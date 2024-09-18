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
package ortus.boxlang.runtime.validation.dynamic;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.GreaterThan;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * I require a numeric record that cannot be greater than the number I'm instantiated with
 */
public class Max implements Validator {

	private Number max;

	public Max( Number max ) {
		this.max = max;
	}

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		// If it was passed...
		if ( records.get( record.name() ) != null ) {
			// then make sure it's not greater than our threshold
			if ( GreaterThan.invoke( NumberCaster.cast( records.get( record.name() ) ), this.max ) ) {
				throw new BoxValidationException( caller, record, "cannot be greater than [" + StringCaster.cast( this.max ) + "]." );
			}
		}
	}

}