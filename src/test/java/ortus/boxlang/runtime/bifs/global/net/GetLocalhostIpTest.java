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
package ortus.boxlang.runtime.bifs.global.net;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.net.NetworkUtil;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class GetLocalhostIpTest {

	private static BoxRuntime	instance;
	private IBoxContext			context;
	private IScope				variables;

	@BeforeAll
	public static void setUpAll() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setUp() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@Test
	@DisplayName( "It returns the primary localhost IP as a string" )
	public void testGetLocalhostIpSingle() {
		instance.executeSource(
		    "result = getLocalhostIp();",
		    this.context
		);

		String result = this.variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isNotNull();
		assertThat( result ).isNotEmpty();
		assertThat( NetworkUtil.isIpv4( result ) || NetworkUtil.isIpv6( result ) ).isTrue();
	}

	@Test
	@DisplayName( "It returns all localhost IPs when all=true" )
	public void testGetLocalhostIpAll() {
		instance.executeSource(
		    "result = getLocalhostIp( all = true );",
		    this.context
		);

		Object result = this.variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( Array.class );

		Array ips = ( Array ) result;
		assertThat( ips.size() ).isAtLeast( 1 );
		for ( Object ipValue : ips ) {
			String ip = String.valueOf( ipValue );
			assertThat( NetworkUtil.isIpv4( ip ) || NetworkUtil.isIpv6( ip ) ).isTrue();
		}
	}

	@Test
	@DisplayName( "It supports refresh argument while returning all localhost IPs" )
	public void testGetLocalhostIpAllWithRefresh() {
		instance.executeSource(
		    "result = getLocalhostIp( all = true, refresh = true );",
		    this.context
		);

		Object result = this.variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( Array.class );
		assertThat( ( ( Array ) result ).size() ).isAtLeast( 1 );
	}

}
