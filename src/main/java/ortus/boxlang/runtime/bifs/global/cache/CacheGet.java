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

import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.CacheExistsValidator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class CacheGet extends BIF {

	/**
	 * Constructor
	 */
	public CacheGet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.id ),
		    new Argument( false, Argument.STRING, Key.cacheName, Key._DEFAULT, Set.of( new CacheExistsValidator() ) ),
		    new Argument( false, Argument.ANY, Key.defaultValue )
		};
	}

	/**
	 * Get an item from the cache. If the item is not found, the default value will be returned if provided, else null will be returned.
	 * By default, the {@code cacheName} is set to {@code default}.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.id The cache id to retrieve
	 *
	 * @argument.cacheName The cache name to retrieve the id from, defaults to {@code default}
	 *
	 * @argument.defaultValue The default value to return if the id is not found in the cache
	 *
	 * @return The value of the object in the cache or null if not found, or the default value if provided
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the requested cache
		ICacheProvider cache = cacheService.getCache( arguments.getAsKey( Key.cacheName ) );
		// Get the value
		Optional<Object> results = cache.get( arguments.getAsString( Key.id ) );
		// If we have a value return it, else do we have a defaultValue, else return null
		return results.orElse( arguments.get( Key.defaultValue ) );

	}
}
