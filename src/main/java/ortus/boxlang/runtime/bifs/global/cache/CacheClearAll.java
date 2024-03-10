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
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.RegexFilter;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.cache.util.CacheExistsValidator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class CacheClearAll extends BIF {

	private static final Validator cacheExistsValidator = new CacheExistsValidator();

	/**
	 * Constructor
	 */
	public CacheClearAll() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.filter ),
		    new Argument( false, Argument.STRING, Key.cacheName, Key._DEFAULT, Set.of( cacheExistsValidator ) ),
		    new Argument( false, Argument.BOOLEAN, Key.useRegex, false ),
		};
	}

	/**
	 * Clear multiples keys in the cache based on a filter.
	 * If no cache name is provided, the default cache is used.
	 * A filter is a simple string that can contain wildcards and will leverage the {@link WildcardFilter} to match keys.
	 * Or you can use a regex filter by setting the {@code useRegex} argument to true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.filter The filter to apply to the keys, this can be a simple Wildcard filter or a regex filter.
	 *
	 * @argument.cacheName The name of the cache to get the keys from. Default is the default cache.
	 *
	 * @argument.useRegex If true, the filter will be treated as a full regular expression filter. Default is false.
	 *
	 * @return True if the keys were cleared, false otherwise
	 */
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ICacheProvider	cache		= cacheService.getCache( arguments.getAsKey( Key.cacheName ) );
		String			filter		= arguments.getAsString( Key.filter );
		Boolean			useRegex	= arguments.getAsBoolean( Key.useRegex );

		// Build the right filter
		ICacheKeyFilter	keyFilter	= useRegex ? new RegexFilter( filter ) : new WildcardFilter( filter );

		// Filter the keys
		return cache.clearAll( keyFilter );
	}
}
