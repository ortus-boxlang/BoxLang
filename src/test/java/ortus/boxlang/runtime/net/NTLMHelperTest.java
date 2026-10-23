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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class NTLMHelperTest {

	@DisplayName( "MD4 matches known test vectors" )
	@Test
	public void testMd4() {
		assertThat( HexFormat.of().formatHex( NTLMHelper.md4( new byte[ 0 ] ) ) )
			.isEqualTo( "31d6cfe0d16ae931b73c59d7e0c089c0" );

		assertThat( HexFormat.of().formatHex( NTLMHelper.md4( "abc".getBytes( StandardCharsets.US_ASCII ) ) ) )
			.isEqualTo( "a448017aaf21d8525fc10ae87aa6729d" );

		assertThat( HexFormat.of().formatHex( NTLMHelper.md4( "message digest".getBytes( StandardCharsets.US_ASCII ) ) ) )
			.isEqualTo( "d9130a8164549fe818874806e1c7014b" );
	}

	@DisplayName( "It computes the NT password hash" )
	@Test
	public void testNtPasswordHash() {
		assertThat( HexFormat.of().formatHex( NTLMHelper.ntPasswordHash( "password" ) ) )
			.isEqualTo( "8846f7eaee8fb117ad06bdd830b7586c" );
	}

	@DisplayName( "HMAC-MD5 matches the RFC 2202 test vector" )
	@Test
	public void testHmacMd5() {
		byte[] hmacKey = new byte[ 16 ];
		Arrays.fill( hmacKey, ( byte ) 0x0b );

		assertThat( HexFormat.of().formatHex( NTLMHelper.hmacMd5( hmacKey, "Hi There".getBytes( StandardCharsets.US_ASCII ) ) ) )
			.isEqualTo( "9294727a3638bb1c13f48ef8158bfc9d" );
	}

	@DisplayName( "It derives the NTLMv2 response key" )
	@Test
	public void testNtlmV2Hash() {
		assertThat( HexFormat.of().formatHex( NTLMHelper.ntlmV2Hash( "User", "Password", "Domain" ) ) )
			.isEqualTo( "0c868a403bfd7a93a3001ef22ef02e3f" );
	}

	@DisplayName( "It builds a Type 1 message" )
	@Test
	public void testType1() {
		byte[] type1 = Base64.getDecoder().decode( NTLMHelper.createType1( "Domain", "WORKSTATION" ) );

		assertThat( type1.length ).isEqualTo( 32 );
		assertThat( Arrays.copyOfRange( type1, 0, 8 ) ).isEqualTo( "NTLMSSP\0".getBytes( StandardCharsets.US_ASCII ) );
		assertThat( ByteBuffer.wrap( type1 ).order( ByteOrder.LITTLE_ENDIAN ).getInt( 8 ) ).isEqualTo( 1 );
	}

	@DisplayName( "The NTLMv2 blob has the expected layout" )
	@Test
	public void testNtlmV2Blob() {
		byte[]	clientChallenge	= { 1, 2, 3, 4, 5, 6, 7, 8 };
		byte[]	targetInfo		= { 0, 0, 0, 0 };

		byte[]	blob			= NTLMHelper.createNtlmV2Blob( clientChallenge, targetInfo );

		assertThat( blob.length ).isEqualTo( 36 );
		assertThat( blob[ 0 ] ).isEqualTo( ( byte ) 1 );
		assertThat( blob[ 1 ] ).isEqualTo( ( byte ) 1 );

		// Timestamp occupies offsets 8..15
		boolean timestampHasValue = false;
		for ( int i = 8; i < 16; i++ ) {
			timestampHasValue |= blob[ i ] != 0;
		}
		assertThat( timestampHasValue ).isTrue();

		assertThat( Arrays.copyOfRange( blob, 16, 24 ) ).isEqualTo( clientChallenge );
		assertThat( Arrays.copyOfRange( blob, 28, 32 ) ).isEqualTo( targetInfo );
	}
}