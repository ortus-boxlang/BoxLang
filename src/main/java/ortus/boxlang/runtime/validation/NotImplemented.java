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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * I ensure this value is not passed yet since we don't implement this feature.
 */
public class NotImplemented implements Validator {

	public void validate(IBoxContext context, Key caller, Validatable record, IStruct records) {
		Object targetValue = records.get(record.name());

		if (targetValue != null) {

			// If the value is a string and EMPTY, then ignore it
			// LEEWAY!
			if( targetValue instanceof String castedTargetValue && castedTargetValue.isEmpty() ) {
				return;
			}

			throw new BoxValidationException(caller, record, "is not implemented yet and should not be provided.");
		}
	}

}
