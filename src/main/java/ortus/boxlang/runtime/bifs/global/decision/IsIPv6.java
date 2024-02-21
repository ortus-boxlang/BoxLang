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
package ortus.boxlang.runtime.bifs.global.decision;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class IsIPv6 extends BIF {

	/**
	 * Constructor
	 */
	public IsIPv6() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, "string", Key.hostname ),
		};
	}

	/**
	 * Determine whether the given hostname supports IPv6.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.ip String representing the IP address to test.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		try {
			InetAddress[] addresses;
			if ( arguments.containsKey( Key.hostname ) ) {
				String hostOrIP = arguments.getAsString( Key.hostname );
				addresses = InetAddress.getAllByName( hostOrIP );
			} else {
				addresses = InetAddress.getAllByName( InetAddress.getLocalHost().getHostName() );
			}
			return Arrays.stream( addresses )
			    .anyMatch( Inet6Address.class::isInstance );
		} catch ( UnknownHostException e ) {
			return false;
		}
	}

}