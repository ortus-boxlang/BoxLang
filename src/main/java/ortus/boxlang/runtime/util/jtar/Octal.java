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
 *
 * Vendored and adapted from https://github.com/xiaoxindada/jtar.
 * Original JTar copyright 2012 Kamran Zafar; Apache License 2.0.
 */
package ortus.boxlang.runtime.util.jtar;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** TAR numeric field encoding. */
public final class Octal {

	private static final long	OCTAL_MAX		= 8589934591L;
	private static final byte	LARGE_NUM_MASK	= ( byte ) 0x80;

	private Octal() {
	}

	/**
	 * Parses an octal or POSIX base-256 field.
	 * 
	 * @param header The encoded TAR header
	 * @param offset The field offset
	 * @param length The field length
	 * 
	 * @return The decoded numeric value
	 */
	public static long parseOctal( byte[] header, int offset, int length ) {
		if ( length == 12 && ( header[ offset ] & LARGE_NUM_MASK ) != 0 ) {
			return ByteBuffer.wrap( header, offset + 4, 8 ).order( ByteOrder.BIG_ENDIAN ).getLong();
		}
		long	result	= 0;
		boolean	padding	= true;
		for ( int i = offset; i < offset + length; i++ ) {
			byte value = header[ i ];
			if ( value == 0 )
				break;
			if ( ( value == ' ' || value == '0' ) && padding )
				continue;
			if ( value == ' ' )
				break;
			padding	= false;
			result	= ( result << 3 ) + value - '0';
		}
		return result;
	}

	/**
	 * Writes an octal or POSIX base-256 field.
	 * 
	 * @param value  The numeric value
	 * @param buffer The destination header buffer
	 * @param offset The field offset
	 * @param length The field length
	 * 
	 * @return The offset after the field
	 */
	public static int getOctalBytes( long value, byte[] buffer, int offset, int length ) {
		if ( value > OCTAL_MAX && length == 12 ) {
			buffer[ offset ] = LARGE_NUM_MASK;
			ByteBuffer.wrap( buffer, offset + 4, 8 ).order( ByteOrder.BIG_ENDIAN ).putLong( value );
			return offset + length;
		}
		int index = length - 1;
		buffer[ offset + index-- ] = 0;
		for ( long current = value; index >= 0; index-- ) {
			buffer[ offset + index ]	= ( byte ) ( '0' + ( current & 7 ) );
			current						>>= 3;
		}
		return offset + length;
	}

	/**
	 * Writes a TAR checksum field.
	 * 
	 * @param value  The checksum value
	 * @param buffer The destination header buffer
	 * @param offset The field offset
	 * @param length The field length
	 * 
	 * @return The offset after the field
	 */
	public static int getCheckSumOctalBytes( long value, byte[] buffer, int offset, int length ) {
		getOctalBytes( value, buffer, offset, length - 1 );
		buffer[ offset + length - 1 ] = ' ';
		return offset + length;
	}
}
