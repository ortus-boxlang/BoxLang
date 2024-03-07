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

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * I require an array, struct, or string record that cannot be of shorter length than the threshold I'm instantiated with
 */
public class MinLength implements Validator {

	private final Number minLength;

	public MinLength( Number minLength ) {
		this.minLength = minLength;
	}

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		// If it was passed...
		if ( records.get( record.name() ) != null ) {
			Object				value			= records.get( record.name() );

			CastAttempt<Array>	arrayAttempt	= ArrayCaster.attempt( value );
			if ( arrayAttempt.wasSuccessful() ) {
				if ( arrayAttempt.getOrFail().size() < this.minLength.intValue() ) {
					throw new BoxValidationException( caller, record,
					    "cannot be shorter than [" + StringCaster.cast( this.minLength ) + "] item" + ( this.minLength.intValue() != 1 ? "s" : "" ) + "." );
				}
				return;
			}

			CastAttempt<IStruct> structAttempt = StructCaster.attempt( value );
			if ( structAttempt.wasSuccessful() ) {
				if ( structAttempt.getOrFail().size() < this.minLength.intValue() ) {
					throw new BoxValidationException( caller, record,
					    "cannot be shorter than [" + StringCaster.cast( this.minLength ) + "] record" + ( this.minLength.intValue() != 1 ? "s" : "" ) + "." );
				}
				return;
			}

			// then make sure it's not greater than our threshold
			if ( StringCaster.cast( records.get( record.name() ) ).length() < this.minLength.doubleValue() ) {
				throw new BoxValidationException( caller, record, "cannot be shorter than [" + StringCaster.cast( this.minLength ) + "] character(s)." );
			}
		}
	}

}