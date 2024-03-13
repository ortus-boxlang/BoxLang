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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class CacheGetMetadata extends BIF {

	private static final Validator cacheExistsValidator = new CacheExistsValidator();

	/**
	 * Constructor
	 */
	public CacheGetMetadata() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.key ),
		    new Argument( false, Argument.STRING, Key.cacheName, Key._DEFAULT, Set.of( cacheExistsValidator ) )
		};
	}

	/**
	 * Get the item metadata for a specific entry or entries.
	 * By default, the {@code cacheName} is set to {@code default}.
	 *
	 * The default metadata for a BoxCache is:
	 * - cacheName : The cachename the entry belongs to
	 * - hits : How many hits the entry has
	 * - timeout : The timeout in seconds
	 * - lastAccessTimeout : The last access timeout in seconds
	 * - created : When the entry was created
	 * - lastAccessed : When the entry was last accessed
	 * - key : The key used to cache it
	 * - metadata : Any extra metadata stored with the entry
	 * - isEternal : If the object has a timeout of 0
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.key The cache key to retrieve, or an array of keys to retrieve
	 *
	 * @argument.cacheName The cache name to retrieve the id from, defaults to {@code default}
	 *
	 * @return A struct of metadata about a cache entry
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the requested cache
		ICacheProvider cache = cacheService.getCache( arguments.getAsKey( Key.cacheName ) );

		// Single or multiple ids
		if ( arguments.get( Key.key ) instanceof Array aKeys ) {
			var results = new Struct();
			aKeys.stream().forEach( key -> results.put( Key.of( key ), cache.getCachedObjectMetadata( ( String ) key ) ) );
			return results;
		}

		// Get a single value
		return cache.getCachedObjectMetadata( arguments.getAsString( Key.key ) );

	}
}
