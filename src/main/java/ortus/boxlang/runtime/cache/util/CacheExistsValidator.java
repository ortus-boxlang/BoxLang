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
package ortus.boxlang.runtime.cache.util;

import java.util.Arrays;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

/**
 * I ensure that the passed cachename is a valid registered cache.
 */
public class CacheExistsValidator implements Validator {

	private static final CacheService cacheService = BoxRuntime.getInstance().getCacheService();

	public void validate( IBoxContext context, Key caller, Validatable record, IStruct records ) {
		Key cacheName = ( Key ) records.getOrDefault( record.name(), record.defaultValue() );

		if ( !cacheService.hasCache( cacheName ) ) {
			throw new BoxValidationException(
			    caller,
			    record,
			    "Cache " + cacheName + " does not exist. Available caches are: " + Arrays.toString( cacheService.getRegisteredCaches() )
			);
		}
	}

}
