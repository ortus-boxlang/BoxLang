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

@BoxBIF( description = "Perform cache operations" )
public class Cache extends BIF {

	private static final Validator cacheExistsValidator = new CacheExistsValidator();

	/**
	 * Constructor
	 */
	public Cache() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.cacheName, Key._DEFAULT, Set.of( cacheExistsValidator ) )
		};
	}

	/**
	 * The entry BIF into BoxLang caching. It retrieves a named cache registered in the cache service. If no cache name is passed, the
	 * <code>default</code> cache is returned. When called from an application, a cache registered for that application (using the application's
	 * cache naming) takes precedence over a global cache of the same name. An unknown cache name throws a validation exception.
	 * <p>
	 * Once you have the cache instance you can use its methods (<code>set()</code>, <code>get()</code>, <code>getKeys()</code>,
	 * <code>clearAll()</code>,
	 * <code>lookup()</code>, and so on). Every cache adheres to the {@link ICacheProvider} interface.
	 *
	 * <pre>
	 * cache().set( "key", "value" )
	 * cache( "myCache" ).get( "key" )
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.cacheName The name of the cache to retrieve. Defaults to the "default" cache.
	 *
	 * @throws BoxValidationException If the cache does not exist.
	 *
	 * @return The cache provider instance that was requested by name.
	 */
	public ICacheProvider _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the requested cache, validator makes sure the cache exists and the name is a key.
		return context.getApplicationCache( arguments.getAsKey( Key.cacheName ) );
	}
}
