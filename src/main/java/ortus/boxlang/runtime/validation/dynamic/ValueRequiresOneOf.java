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
import java.util.stream.Collectors;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * If this record is present, ensure that the required records are also present
 */
public class ValueRequiresOneOf implements Validator {

	private final Set<Key>	recordNames;
	private final String	value;

	public ValueRequiresOneOf( String value, Set<Key> recordNames ) {
		this.recordNames	= recordNames;
		this.value			= value;
	}

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		if ( records.containsKey( record.name() ) && records.getAsString( record.name() ).equalsIgnoreCase( this.value ) ) {
			if ( !recordNames.stream().anyMatch( records::containsKey ) ) {
				throw new BoxValidationException(
				    caller,
				    record,
				    "requires the one of the following attributes or arguments to be provided: "
				        + recordNames.stream().map( Key::getName ).collect( Collectors.joining( ", " ) )
				);
			}
		}
	}

}