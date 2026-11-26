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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.cache.filters.ICacheKeyFilter;
import ortus.boxlang.runtime.cache.filters.RegexFilter;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Filter cache entries using a callback function" )
public class CacheFilter extends BIF {

	/**
	 * Constructor
	 */
	public CacheFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.filter ),
		    new Argument( false, Argument.BOOLEAN, Key.useRegex, false )
		};
	}

	/**
	 * Creates a cache key filter that can be used with a cache instance to select keys, for example to get keys or clear entries with
	 * <code>getKeys( filter )</code> or <code>clearAll( filter )</code>. All filters adhere to the {@link ICacheKeyFilter} interface.
	 * <p>
	 * By default a wildcard filter is created, which matches the whole key using globbing patterns: <code>*</code> matches any number of
	 * characters,
	 * <code>?</code> matches a single character, and <code>[abc]</code> or <code>[!abc]</code> match character sets.
	 * When useRegex is true a regular expression is used instead, which must match the whole key. Both filters are case insensitive.
	 *
	 * <pre>
	 * cache().clearAll( cacheFilter( "foo*" ) )
	 * cache().clearAll( cacheFilter( ".*foo.*", true ) )
	 * </pre>
	 *
	 * You can also create your own filter by passing a closure/lambda that accepts a key and returns a boolean:
	 *
	 * <pre>
	 * cache().getKeys( ( key ) -> key.getName().startsWith( "foo" ) )
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.filter The wildcard pattern (or regular expression when useRegex is true) to match keys against.
	 *
	 * @argument.useRegex If true, the filter is a regular expression instead of a wildcard pattern. Defaults to false.
	 *
	 * @return An ICacheKeyFilter that matches the cache keys.
	 */
	public ICacheKeyFilter _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Boolean	useRegex	= BooleanCaster.cast( arguments.get( Key.useRegex ) );
		String	filter		= StringCaster.cast( arguments.get( Key.filter ) );

		// Build the right filter
		return useRegex ? new RegexFilter( filter ) : new WildcardFilter( filter );
	}
}
