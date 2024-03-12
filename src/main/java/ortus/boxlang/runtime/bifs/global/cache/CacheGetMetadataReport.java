/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.cache;

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.CacheExistsValidator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class CacheGetMetadataReport extends BIF {

	private static final Validator cacheExistsValidator = new CacheExistsValidator();

	/**
	 * Constructor
	 */
	public CacheGetMetadataReport() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.NUMERIC, Key.limit, Integer.MAX_VALUE ),
		    new Argument( false, Argument.STRING, Key.cacheName, Key._DEFAULT, Set.of( cacheExistsValidator ) )
		};
	}

	/**
	 * Get a structure of all the keys in the cache with their appropriate metadata structures.
	 * This is used to build the reporting for the cache provider
	 * Example:
	 *
	 * <pre>
	 * {
	 *    "key1": {
	 * 	  "hits": 0,
	 * 	  "lastAccessed": 0,
	 * 	  "lastUpdated": 0,
	 * 	   ...
	 *   },
	 *  "key2": {
	 * 	  "hits": 0,
	 * 	  "lastAccessed": 0,
	 * 	  "lastUpdated": 0,
	 * 	  ...
	 *  }
	 * }
	 * </pre>
	 *
	 * The {@code getStoreMetadataKeyMap} method is used to get the keys that
	 * this method returns as metadata in order to build the reports.
	 *
	 * Careful, this will be a large structure if the cache is large.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.limit The maximum number of keys to return, defaults to all keys if not passed
	 *
	 * @argument.cacheName The cache name to retrieve the id from, defaults to {@code default}
	 *
	 *
	 * @return A struct of metadata report for the keys in the cache.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ICacheProvider cache = cacheService.getCache( arguments.getAsKey( Key.cacheName ) );
		return cache.getStoreMetadataReport( arguments.getAsDouble( Key.limit ).intValue() );
	}
}
