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
package ortus.boxlang.runtime.net;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NetworkUtilTest {

	@Test
	@DisplayName( "It can validate IPv4 literals" )
	void testIsIpv4() {
		assertThat( NetworkUtil.isIpv4( "127.0.0.1" ) ).isTrue();
		assertThat( NetworkUtil.isIpv4( "192.168.1.10" ) ).isTrue();
		assertThat( NetworkUtil.isIpv4( "255.255.255.255" ) ).isTrue();

		assertThat( NetworkUtil.isIpv4( null ) ).isFalse();
		assertThat( NetworkUtil.isIpv4( "" ) ).isFalse();
		assertThat( NetworkUtil.isIpv4( "256.1.1.1" ) ).isFalse();
		assertThat( NetworkUtil.isIpv4( "192.168.1" ) ).isFalse();
		assertThat( NetworkUtil.isIpv4( "localhost" ) ).isFalse();
		assertThat( NetworkUtil.isIpv4( "::1" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can validate IPv6 literals" )
	void testIsIpv6() {
		assertThat( NetworkUtil.isIpv6( "::1" ) ).isTrue();
		assertThat( NetworkUtil.isIpv6( "2001:db8::1" ) ).isTrue();
		assertThat( NetworkUtil.isIpv6( "fe80::1" ) ).isTrue();
		assertThat( NetworkUtil.isIpv6( "2001:0db8:85a3:0000:0000:8a2e:0370:7334" ) ).isTrue();

		assertThat( NetworkUtil.isIpv6( null ) ).isFalse();
		assertThat( NetworkUtil.isIpv6( "" ) ).isFalse();
		assertThat( NetworkUtil.isIpv6( "2001:db8:::1" ) ).isFalse();
		assertThat( NetworkUtil.isIpv6( "gggg::1" ) ).isFalse();
		assertThat( NetworkUtil.isIpv6( "localhost" ) ).isFalse();
		assertThat( NetworkUtil.isIpv6( "127.0.0.1" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can resolve local network metadata" )
	void testLocalNetworkMetadata() {
		assertThat( NetworkUtil.getLocalIPAddress() ).isNotNull();
		assertThat( NetworkUtil.getLocalIPAddress() ).isNotEmpty();
		assertThat( NetworkUtil.getLocalHostname() ).isNotNull();
		assertThat( NetworkUtil.getLocalHostname() ).isNotEmpty();

		String localMac = NetworkUtil.getLocalMacAddress();
		assertThat( localMac ).isNotNull();
		if ( !localMac.isEmpty() ) {
			assertThat( localMac ).matches( "^[0-9A-F]{2}(?:-[0-9A-F]{2}){5}$" );
		}
	}

	@Test
	@DisplayName( "It can load and cache all local IP addresses" )
	void testGetLocalIps() {
		String[] refreshedIps = NetworkUtil.getLocalIps( true );

		assertThat( refreshedIps.length ).isAtLeast( 1 );
		for ( String ip : refreshedIps ) {
			assertThat( NetworkUtil.isIpv4( ip ) || NetworkUtil.isIpv6( ip ) ).isTrue();
		}

		String[] cachedIps = NetworkUtil.getLocalIps( false );
		assertThat( Arrays.asList( cachedIps ) ).containsExactlyElementsIn( Arrays.asList( refreshedIps ) );

		cachedIps[ 0 ] = "__mutated__";
		String[] checkCacheIntegrity = NetworkUtil.getLocalIps( false );
		assertThat( Arrays.asList( checkCacheIntegrity ) ).doesNotContain( "__mutated__" );
	}

}
