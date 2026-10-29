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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Builds the NTLM Type 1 and Type 3 messages and parses the Type 2 challenge
 * for HTTP authentication (NTLMv2 only). Used by BoxHttpClient when
 * authType="NTLM".
 * MD4 is included only because NTLM requires it and the JDK has none.
 */
public class NTLMHelper {

	/**
	 * Additive constant for MD4 round 2
	 */
	private static final int	ROUND_2_CONSTANT	= 0x5a827999;

	/**
	 * Additive constant for MD4 round 3
	 */
	private static final int	ROUND_3_CONSTANT	= 0x6ed9eba1;

	/**
	 * Creates the Base64 encoded Type 1 (negotiate) message.
	 *
	 * @param domain      The NTLM domain, not placed in the message
	 * @param workstation The NTLM workstation, not placed in the message
	 *
	 * @return The Base64 encoded Type 1 message
	 */
	public static String createType1( String domain, String workstation ) {
		ByteArrayOutputStream message = new ByteArrayOutputStream();

		// NTLM signature: "NTLMSSP\0"
		message.writeBytes( "NTLMSSP\0".getBytes( StandardCharsets.US_ASCII ) );

		// Message type: 1 (NEGOTIATE_MESSAGE), little-endian
		writeIntLE( message, 1 );

		// Negotiate flags
		int flags = 0x00000001  // NEGOTIATE_UNICODE
		    | 0x00000004  // REQUEST_TARGET
		    | 0x00000200  // NEGOTIATE_NTLM
		    | 0x00008000  // NEGOTIATE_ALWAYS_SIGN
		    | 0x00080000  // NEGOTIATE_EXTENDED_SESSIONSECURITY
		    | 0x00800000  // NEGOTIATE_TARGET_INFO
		    | 0x20000000  // NEGOTIATE_128
		    | 0x80000000; // NEGOTIATE_56

		writeIntLE( message, flags );

		// Domain and workstation security buffers.
		// This implementation advertises neither field, so both are empty.
		writeShortLE( message, 0 ); // DomainNameLen
		writeShortLE( message, 0 ); // DomainNameMaxLen
		writeIntLE( message, 0 );   // DomainNameBufferOffset

		writeShortLE( message, 0 ); // WorkstationNameLen
		writeShortLE( message, 0 ); // WorkstationNameMaxLen
		writeIntLE( message, 0 );   // WorkstationNameBufferOffset

		return Base64.getEncoder().encodeToString( message.toByteArray() );
	}

	/**
	 * Decodes and validates the Base64 encoded Type 2 (challenge) message.
	 *
	 * @param challenge The Base64 encoded Type 2 message from the server
	 *
	 * @return The entire decoded Type 2 message
	 *
	 * @throws IllegalArgumentException If the value is not Base64, is too short, or is not a Type 2 message
	 */
	public static byte[] parseType2( String challenge ) {
		byte[] message;

		try {
			message = java.util.Base64.getDecoder().decode( challenge );
		} catch ( IllegalArgumentException e ) {
			throw new IllegalArgumentException(
			    "Invalid Base64 NTLM Type 2 message", e
			);
		}

		// Minimum fixed Type 2 message size: 48 bytes
		if ( message.length < 48 ) {
			throw new IllegalArgumentException(
			    "NTLM Type 2 message is too short"
			);
		}

		// Validate NTLMSSP signature
		byte[] signature = {
		    'N', 'T', 'L', 'M', 'S', 'S', 'P', 0
		};

		for ( int i = 0; i < signature.length; i++ ) {
			if ( message[ i ] != signature[ i ] ) {
				throw new IllegalArgumentException(
				    "Invalid NTLM message signature"
				);
			}
		}

		// Validate message type: 2 = CHALLENGE_MESSAGE
		java.nio.ByteBuffer buffer = java.nio.ByteBuffer
		    .wrap( message )
		    .order( java.nio.ByteOrder.LITTLE_ENDIAN );

		if ( buffer.getInt( 8 ) != 2 ) {
			throw new IllegalArgumentException(
			    "NTLM message is not a Type 2 challenge"
			);
		}
		return message;
	}

	/**
	 * Creates the Base64 encoded Type 3 (authenticate) message.
	 *
	 * @param username    The username
	 * @param password    The password
	 * @param domain      The NTLM domain, or null for none
	 * @param workstation The NTLM workstation, or null for none
	 * @param type2       The decoded Type 2 message
	 *
	 * @return The Base64 encoded Type 3 message
	 *
	 * @throws IllegalArgumentException If a required value is missing or the Type 2 message cannot be used for NTLMv2
	 */
	public static String createType3(
	    String username,
	    String password,
	    String domain,
	    String workstation,
	    byte[] type2 ) {
		if ( username == null || password == null || type2 == null ) {
			throw new IllegalArgumentException(
			    "Username, password, and Type 2 message are required"
			);
		}

		if ( type2.length < 48
		    || !Arrays.equals(
		        Arrays.copyOfRange( type2, 0, 8 ),
		        new byte[] { 'N', 'T', 'L', 'M', 'S', 'S', 'P', 0 }
		    )
		    || ByteBuffer.wrap( type2 )
		        .order( ByteOrder.LITTLE_ENDIAN )
		        .getInt( 8 ) != 2 ) {
			throw new IllegalArgumentException( "Invalid NTLM Type 2 message" );
		}

		ByteBuffer	buffer			= ByteBuffer.wrap( type2 )
		    .order( ByteOrder.LITTLE_ENDIAN );

		byte[]		serverChallenge	= Arrays.copyOfRange( type2, 24, 32 );
		int			serverFlags		= buffer.getInt( 20 );

		if ( ( serverFlags & 0x00000001 ) == 0
		    || ( serverFlags & 0x00000200 ) == 0 ) {
			throw new IllegalArgumentException(
			    "Server must negotiate NTLM and Unicode"
			);
		}

		int	targetInfoLength	= Short.toUnsignedInt( buffer.getShort( 40 ) );
		int	targetInfoOffset	= buffer.getInt( 44 );

		if ( targetInfoLength < 4
		    || targetInfoOffset < 0
		    || targetInfoOffset > type2.length
		    || targetInfoLength > type2.length - targetInfoOffset ) {
			throw new IllegalArgumentException(
			    "Invalid or missing NTLMv2 target information"
			);
		}

		byte[] targetInfo = Arrays.copyOfRange(
		    type2, targetInfoOffset, targetInfoOffset + targetInfoLength
		);

		validateTargetInfo( targetInfo );

		byte[] clientChallenge = new byte[ 8 ];
		RANDOM.nextBytes( clientChallenge );

		String		safeDomain		= domain == null ? "" : domain;
		String		safeWorkstation	= workstation == null ? "" : workstation;

		byte[]		responseKey		= ntlmV2Hash( username, password, safeDomain );
		byte[]		blob			= createNtlmV2Blob( clientChallenge, targetInfo );
		byte[]		ntResponse		= createNtlmV2Response(
		    responseKey, serverChallenge, blob
		);

		// A server-supplied timestamp means the LMv2 response should be zeroed.
		byte[]		lmResponse		= hasTimestamp( targetInfo )
		    ? new byte[ 24 ]
		    : createLmV2Response( responseKey, serverChallenge, clientChallenge );

		byte[][]	fields			= {
		    lmResponse,
		    ntResponse,
		    safeDomain.getBytes( StandardCharsets.UTF_16LE ),
		    username.getBytes( StandardCharsets.UTF_16LE ),
		    safeWorkstation.getBytes( StandardCharsets.UTF_16LE ),
		    new byte[ 0 ] // Encrypted session key
		};

		// Type 3 header without Version or MIC is 64 bytes.
		int[]		offsets			= new int[ fields.length ];
		int			nextOffset		= 64;

		for ( int i = 0; i < fields.length; i++ ) {
			offsets[ i ]	= nextOffset;
			nextOffset		+= fields[ i ].length;
		}

		ByteArrayOutputStream message = new ByteArrayOutputStream();

		message.writeBytes( new byte[] { 'N', 'T', 'L', 'M', 'S', 'S', 'P', 0 } );
		writeIntLE( message, 3 );

		for ( int i = 0; i < fields.length; i++ ) {
			writeSecurityBuffer( message, fields[ i ].length, offsets[ i ] );
		}

		int flags = serverFlags & ( 0x00000001 | 0x00000004 | 0x00000200 | 0x00008000
		    | 0x00080000 | 0x00800000 | 0x20000000 | 0x80000000 );

		writeIntLE( message, flags );

		for ( byte[] field : fields ) {
			message.writeBytes( field );
		}

		return Base64.getEncoder().encodeToString( message.toByteArray() );
	}

	/**
	 * Writes a security buffer descriptor: length, maximum length and offset.
	 *
	 * @param out    The stream to write to
	 * @param length The length of the field
	 * @param offset The offset of the field from the start of the message
	 *
	 * @throws IllegalArgumentException If the length does not fit in 16 bits
	 */
	private static void writeSecurityBuffer(
	    ByteArrayOutputStream out,
	    int length,
	    int offset ) {
		if ( length > 0xFFFF ) {
			throw new IllegalArgumentException( "NTLM field is too large" );
		}

		writeShortLE( out, length );
		writeShortLE( out, length );
		writeIntLE( out, offset );
	}

	/**
	 * Writes a 32 bit value in little-endian order.
	 *
	 * @param out   The stream to write to
	 * @param value The value to write
	 */
	private static void writeIntLE( ByteArrayOutputStream out, int value ) {
		out.write( value & 0xFF );
		out.write( ( value >>> 8 ) & 0xFF );
		out.write( ( value >>> 16 ) & 0xFF );
		out.write( ( value >>> 24 ) & 0xFF );
	}

	/**
	 * Writes a 16 bit value in little-endian order.
	 *
	 * @param out   The stream to write to
	 * @param value The value to write
	 */
	private static void writeShortLE( ByteArrayOutputStream out, int value ) {
		out.write( value & 0xFF );
		out.write( ( value >>> 8 ) & 0xFF );
	}

	/**
	 * Computes the MD4 digest of the supplied bytes.
	 *
	 * @param input The bytes to hash
	 *
	 * @return The 16 byte MD4 digest
	 */
	public static byte[] md4( byte[] input ) {
		long	bitLength		= ( long ) input.length * 8L;
		int		paddedLength	= ( ( input.length + 9 + 63 ) / 64 ) * 64;
		byte[]	padded			= java.util.Arrays.copyOf( input, paddedLength );

		padded[ input.length ] = ( byte ) 0x80;

		// Append the original message length as a 64-bit little-endian value.
		for ( int i = 0; i < 8; i++ ) {
			padded[ paddedLength - 8 + i ] = ( byte ) ( bitLength >>> ( 8 * i ) );
		}

		int		a0		= 0x67452301;
		int		b0		= 0xefcdab89;
		int		c0		= 0x98badcfe;
		int		d0		= 0x10325476;

		int[]	x		= new int[ 16 ];

		int[]	order2	= {
		    0, 4, 8, 12, 1, 5, 9, 13,
		    2, 6, 10, 14, 3, 7, 11, 15
		};

		int[]	order3	= {
		    0, 8, 4, 12, 2, 10, 6, 14,
		    1, 9, 5, 13, 3, 11, 7, 15
		};

		int[]	shifts1	= { 3, 7, 11, 19 };
		int[]	shifts2	= { 3, 5, 9, 13 };
		int[]	shifts3	= { 3, 9, 11, 15 };

		for ( int offset = 0; offset < padded.length; offset += 64 ) {
			for ( int i = 0; i < 16; i++ ) {
				int p = offset + i * 4;
				x[ i ] = ( padded[ p ] & 0xff )
				    | ( ( padded[ p + 1 ] & 0xff ) << 8 )
				    | ( ( padded[ p + 2 ] & 0xff ) << 16 )
				    | ( ( padded[ p + 3 ] & 0xff ) << 24 );
			}

			int	a	= a0;
			int	b	= b0;
			int	c	= c0;
			int	d	= d0;

			// Round 1
			for ( int i = 0; i < 16; i++ ) {
				int	k	= i;
				int	s	= shifts1[ i % 4 ];

				switch ( i % 4 ) {
					case 0 -> a = Integer.rotateLeft(
					    a + ( ( b & c ) | ( ~b & d ) ) + x[ k ], s );
					case 1 -> d = Integer.rotateLeft(
					    d + ( ( a & b ) | ( ~a & c ) ) + x[ k ], s );
					case 2 -> c = Integer.rotateLeft(
					    c + ( ( d & a ) | ( ~d & b ) ) + x[ k ], s );
					case 3 -> b = Integer.rotateLeft(
					    b + ( ( c & d ) | ( ~c & a ) ) + x[ k ], s );
				}
			}

			// Round 2
			for ( int i = 0; i < 16; i++ ) {
				int	k	= order2[ i ];
				int	s	= shifts2[ i % 4 ];

				switch ( i % 4 ) {
					case 0 -> a = Integer.rotateLeft(
					    a + ( ( b & c ) | ( b & d ) | ( c & d ) )
					        + x[ k ] + ROUND_2_CONSTANT,
					    s );
					case 1 -> d = Integer.rotateLeft(
					    d + ( ( a & b ) | ( a & c ) | ( b & c ) )
					        + x[ k ] + ROUND_2_CONSTANT,
					    s );
					case 2 -> c = Integer.rotateLeft(
					    c + ( ( d & a ) | ( d & b ) | ( a & b ) )
					        + x[ k ] + ROUND_2_CONSTANT,
					    s );
					case 3 -> b = Integer.rotateLeft(
					    b + ( ( c & d ) | ( c & a ) | ( d & a ) )
					        + x[ k ] + ROUND_2_CONSTANT,
					    s );
				}
			}

			// Round 3
			for ( int i = 0; i < 16; i++ ) {
				int	k	= order3[ i ];
				int	s	= shifts3[ i % 4 ];

				switch ( i % 4 ) {
					case 0 -> a = Integer.rotateLeft(
					    a + ( b ^ c ^ d ) + x[ k ] + ROUND_3_CONSTANT, s );
					case 1 -> d = Integer.rotateLeft(
					    d + ( a ^ b ^ c ) + x[ k ] + ROUND_3_CONSTANT, s );
					case 2 -> c = Integer.rotateLeft(
					    c + ( d ^ a ^ b ) + x[ k ] + ROUND_3_CONSTANT, s );
					case 3 -> b = Integer.rotateLeft(
					    b + ( c ^ d ^ a ) + x[ k ] + ROUND_3_CONSTANT, s );
				}
			}

			a0	+= a;
			b0	+= b;
			c0	+= c;
			d0	+= d;
		}

		int[]	state	= { a0, b0, c0, d0 };
		byte[]	digest	= new byte[ 16 ];

		for ( int i = 0; i < state.length; i++ ) {
			for ( int j = 0; j < 4; j++ ) {
				digest[ i * 4 + j ] = ( byte ) ( state[ i ] >>> ( 8 * j ) );
			}
		}

		return digest;
	}

	/**
	 * Computes the NT password hash used by NTLM.
	 *
	 * @param password The password
	 *
	 * @return The 16 byte NT password hash
	 */
	public static byte[] ntPasswordHash( String password ) {
		return md4( password.getBytes( StandardCharsets.UTF_16LE ) );
	}

	/**
	 * Computes HMAC-MD5 using the supplied key and data.
	 *
	 * @param key  The HMAC key
	 * @param data The data to authenticate
	 *
	 * @return The 16 byte HMAC-MD5 value
	 *
	 * @throws IllegalStateException If HMAC-MD5 is not available
	 */
	public static byte[] hmacMd5( byte[] key, byte[] data ) {
		try {
			Mac mac = Mac.getInstance( "HmacMD5" );
			mac.init( new SecretKeySpec( key, "HmacMD5" ) );
			return mac.doFinal( data );
		} catch ( java.security.GeneralSecurityException e ) {
			throw new IllegalStateException(
			    "Unable to compute HMAC-MD5", e
			);
		}
	}

	/**
	 * Derives the NTLMv2 response key.
	 *
	 * ResponseKeyNT = HMAC-MD5(
	 * NTPasswordHash,
	 * (Uppercase(username) + domain).getBytes(UTF_16LE)
	 * )
	 *
	 * @param username The username
	 * @param password The password
	 * @param domain   The domain, or an empty string
	 *
	 * @return The 16 byte NTLMv2 response key
	 */
	public static byte[] ntlmV2Hash(
	    String username,
	    String password,
	    String domain ) {
		byte[]	passwordHash	= ntPasswordHash( password );

		String	identity		= username.toUpperCase( Locale.ROOT ) + domain;

		byte[]	identityBytes	= identity.getBytes( StandardCharsets.UTF_16LE );

		return hmacMd5( passwordHash, identityBytes );
	}

	private static final SecureRandom RANDOM = new SecureRandom();

	/**
	 * Writes a 64 bit value in little-endian order.
	 *
	 * @param out   The stream to write to
	 * @param value The value to write
	 */
	private static void writeLongLE( ByteArrayOutputStream out, long value ) {
		for ( int i = 0; i < 8; i++ ) {
			out.write( ( int ) ( value >>> ( 8 * i ) ) & 0xff );
		}
	}

	/**
	 * Converts the current time to Windows FILETIME units.
	 *
	 * @return The current time in 100 nanosecond ticks since 1601-01-01
	 */
	private static long windowsFileTimeNow() {
		return ( Instant.now().toEpochMilli() + 11_644_473_600_000L ) * 10_000L;
	}

	/**
	 * Creates the NTLMv2 client-challenge blob.
	 * targetInfo must contain valid server AV pairs, including AV_EOL.
	 *
	 * @param clientChallenge The 8 byte client challenge
	 * @param targetInfo      The server target information
	 *
	 * @return The blob
	 *
	 * @throws IllegalArgumentException If the client challenge is not 8 bytes or the target information is invalid
	 */
	static byte[] createNtlmV2Blob(
	    byte[] clientChallenge,
	    byte[] targetInfo ) {
		if ( clientChallenge == null || clientChallenge.length != 8 ) {
			throw new IllegalArgumentException(
			    "Client challenge must be 8 bytes" );
		}

		validateTargetInfo( targetInfo );

		ByteArrayOutputStream blob = new ByteArrayOutputStream();

		blob.write( 0x01 ); // RespType
		blob.write( 0x01 ); // HiRespType
		writeShortLE( blob, 0 ); // Reserved1

		writeIntLE( blob, 0 ); // Reserved2
		writeLongLE( blob, windowsFileTimeNow() ); // Timestamp

		blob.writeBytes( clientChallenge ); // ClientChallenge
		writeIntLE( blob, 0 ); // Reserved3

		blob.writeBytes( targetInfo ); // AvPairs
		writeIntLE( blob, 0 ); // Reserved4

		return blob.toByteArray();
	}

	/**
	 * Ensures target information consists of valid AV pairs ending in AV_EOL.
	 *
	 * @param targetInfo The target information bytes
	 *
	 * @throws IllegalArgumentException If the target information is missing, malformed or has no terminator
	 */
	private static void validateTargetInfo( byte[] targetInfo ) {
		if ( targetInfo == null || targetInfo.length < 4 ) {
			throw new IllegalArgumentException(
			    "NTLMv2 requires valid target information" );
		}

		int pos = 0;

		while ( pos + 4 <= targetInfo.length ) {
			int	id	= ( targetInfo[ pos ] & 0xff )
			    | ( ( targetInfo[ pos + 1 ] & 0xff ) << 8 );

			int	len	= ( targetInfo[ pos + 2 ] & 0xff )
			    | ( ( targetInfo[ pos + 3 ] & 0xff ) << 8 );

			pos += 4;

			if ( id == 0 ) {
				if ( len != 0 || pos != targetInfo.length ) {
					throw new IllegalArgumentException(
					    "Invalid NTLM target information terminator" );
				}
				return;
			}

			if ( len > targetInfo.length - pos ) {
				throw new IllegalArgumentException(
				    "Invalid NTLM target information AV pair" );
			}

			pos += len;
		}

		throw new IllegalArgumentException(
		    "NTLM target information has no AV_EOL terminator" );
	}

	/**
	 * Concatenates byte arrays in order.
	 *
	 * @param arrays The arrays to join
	 *
	 * @return A new array holding all of the bytes
	 */
	private static byte[] concat( byte[]... arrays ) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		for ( byte[] array : arrays ) {
			out.writeBytes( array );
		}

		return out.toByteArray();
	}

	/**
	 * Creates the NTLMv2 response: HMAC-MD5 of the server challenge and blob, followed by the blob.
	 *
	 * @param responseKey     The NTLMv2 response key
	 * @param serverChallenge The 8 byte server challenge
	 * @param blob            The client challenge blob
	 *
	 * @return The NT response
	 */
	private static byte[] createNtlmV2Response(
	    byte[] responseKey,
	    byte[] serverChallenge,
	    byte[] blob ) {

		byte[] proof = hmacMd5(
		    responseKey,
		    concat( serverChallenge, blob ) );

		return concat( proof, blob );
	}

	/**
	 * Creates the LMv2 response: HMAC-MD5 of the server and client challenges, followed by the client challenge.
	 *
	 * @param responseKey     The NTLMv2 response key
	 * @param serverChallenge The 8 byte server challenge
	 * @param clientChallenge The 8 byte client challenge
	 *
	 * @return The 24 byte LM response
	 */
	private static byte[] createLmV2Response(
	    byte[] responseKey,
	    byte[] serverChallenge,
	    byte[] clientChallenge ) {

		byte[] proof = hmacMd5(
		    responseKey,
		    concat( serverChallenge, clientChallenge ) );

		return concat( proof, clientChallenge );
	}

	/**
	 * Checks whether the target information contains a server timestamp.
	 *
	 * @param targetInfo The target information bytes
	 *
	 * @return True if a timestamp pair is present
	 */
	private static boolean hasTimestamp( byte[] targetInfo ) {
		int pos = 0;

		while ( pos + 4 <= targetInfo.length ) {
			int	id		= ( targetInfo[ pos ] & 0xFF )
			    | ( ( targetInfo[ pos + 1 ] & 0xFF ) << 8 );

			int	length	= ( targetInfo[ pos + 2 ] & 0xFF )
			    | ( ( targetInfo[ pos + 3 ] & 0xFF ) << 8 );

			pos += 4;

			if ( id == 0 ) {
				return false;
			}

			if ( length > targetInfo.length - pos ) {
				return false;
			}

			if ( id == 7 && length == 8 ) {
				return true;
			}

			pos += length;
		}

		return false;
	}
}
