/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.net;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.net.NetworkUtil;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

@BoxBIF( description = "Returns the local IP address(es) of the machine" )
public class GetLocalhostIp extends BIF {

	private static final Key	ALL_KEY		= Key.of( "all" );
	private static final Key	REFRESH_KEY	= Key.of( "refresh" );

	/**
	 * Constructor
	 */
	public GetLocalhostIp() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.BOOLEAN, ALL_KEY, false ),
		    new Argument( false, Argument.BOOLEAN, REFRESH_KEY, false )
		};
	}

	/**
	 * Returns the localhost IP address (string), or all local IP addresses (array) if the 'all' argument is set to true.
	 *
	 * The lookup for ALL IP addresses is cached by default, but can be forced to refresh with the 'refresh' argument.
	 * The 'refresh' argument will clear the cache and perform a new lookup, when called with 'all' set to true.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.all (optional, boolean) If true, returns an array of all local IP addresses. Default is false (returns only the primary local IP).
	 *
	 * @argument.refresh (optional, boolean) If true, forces a refresh of the local IP address cache. Default is false (uses cached value if available).
	 *
	 * @return A string representing the local IP address, or an array of strings if 'all' is true.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		boolean	all		= arguments.getAsBoolean( ALL_KEY );
		boolean	refresh	= arguments.getAsBoolean( REFRESH_KEY );

		if ( all ) {
			return Array.fromArray( NetworkUtil.getLocalIps( refresh ) );
		} else {
			return NetworkUtil.getLocalIPAddress();
		}
	}

}
