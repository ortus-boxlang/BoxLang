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

import java.util.Set;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * If this record is present, ensure that the required records are also present
 */
public class RequiresOneOf implements Validator {

	private Set<Key> recordNames;

	public RequiresOneOf( Set<Key> recordNames ) {
		this.recordNames = recordNames;
	}

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		if ( records.containsKey( record.name() ) ) {
			boolean foundOne = false;
			for ( Key required : recordNames ) {
				if ( records.containsKey( required ) ) {
					foundOne = true;
					break;
				}
			}
			if ( !foundOne ) {
				String missingRecordsString = String.join( ", ", recordNames.stream().map( Key::getName ).toList() );
				throw new BoxValidationException( caller, record, "requires at least one of the following records to be present: " + missingRecordsString );
			}
		}
	}

}