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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class GetBoxCacheFilter extends BIF {

	/**
	 * Constructor
	 */
	public GetBoxCacheFilter() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.filter ),
		    new Argument( false, Argument.BOOLEAN, Key.useRegex, false )
		};
	}

	/**
	 * This method creates a cache filter that can be used to operate on a cache instance and its keys.
	 * The filter can be used to clear, get, or remove keys from the cache.
	 *
	 * All filters must be adhere to the {@link ICacheKeyFilter} interface.
	 *
	 * Example:
	 *
	 * <pre>
	 * getBoxCache().clear( getBoxCacheFilter( "foo*" ) );
	 * getBoxCache().clear( getBoxCacheFilter( ".*foo.*", true ) );
	 *
	 * You can also create your own custom cache filter by using a closure/lambda that
	 * accepts a {@code
	 * Key
	 * } and returns a boolean.
	 *
	 * Example:
	 *
	 * <pre>
	 * getBoxCache().clear( key -> key.getName().startsWith( "foo" ) );
	 * getBoxCache().clear( key -> key.getName().matches( ".*foo.*" ) );
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.filter The string pattern to match against.
	 *
	 * @argument.useRegex Use the regex filter instead of the wildcard filter.
	 *
	 * @return The cache instance that was requested by name.
	 */
	public ICacheKeyFilter _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Boolean	useRegex	= arguments.getAsBoolean( Key.useRegex );
		String	filter		= arguments.getAsString( Key.filter );

		// Build the right filter
		return useRegex ? new RegexFilter( filter ) : new WildcardFilter( filter );
	}
}
