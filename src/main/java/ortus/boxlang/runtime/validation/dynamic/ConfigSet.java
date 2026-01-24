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

import ortus.boxlang.runtime.config.util.ConfigItem;
import ortus.boxlang.runtime.config.util.ConfigUtil;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * If this record is present, ensure its nested keys obey the given config items
 */
public class ConfigSet implements Validator {

	private Set<ConfigItem> configSet;

	public ConfigSet( Set<ConfigItem> configSet ) {
		this.configSet = configSet;
	}

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		if ( records.get( record.name() ) instanceof IStruct nestedStruct ) {
			records.put( record.name(), ConfigUtil.getConfigSet( configSet, nestedStruct, context ) );
		}
	}

}