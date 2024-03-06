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
package ortus.boxlang.runtime.validation;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.types.IStruct;

/**
 * I require a specific type
 */
public class Type implements Validator {

	public void validate( IBoxContext context, Component component, Validatable record, IStruct records ) {
		// If there is a type on the record, enforce it
		if ( record.type() != null ) {
			Object value = records.get( record.name() );
			if ( value != null ) {
				records.put( record.name(), GenericCaster.cast( context, value, record.type() ) );
			}
		}
	}

}