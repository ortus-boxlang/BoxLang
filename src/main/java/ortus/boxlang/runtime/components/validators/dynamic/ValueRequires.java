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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

/**
 * If this attribute is present, ensure that the required attributes are also present
 */
public class ValueRequires implements Validator {

	private Set<Key> attributeNames;

	public ValueRequires( Set<Key> attributeNames ) {
		this.attributeNames = attributeNames;
	}

	public void validate( IBoxContext context, Component component, Attribute attribute, IStruct attributes ) {
		if ( attributes.containsKey( attribute.name() ) ) {
			List<String> missingAttributes = new ArrayList<>();
			for ( Key required : attributeNames ) {
				if ( !attributes.containsKey( required ) ) {
					missingAttributes.add( required.getName() );
				}
			}
			if ( !missingAttributes.isEmpty() ) {
				String missingAttributesString = String.join( ", ", missingAttributes );
				throw new BoxValidationException( component, attribute, "requires the following attributes to be present: " + missingAttributesString );
			}
		}
	}

}