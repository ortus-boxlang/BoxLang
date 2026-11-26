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

package ortus.boxlang.runtime.bifs.global.encryption;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.lang3.SerializationException;
import org.apache.commons.lang3.SerializationUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

@BoxBIF( description = "Generate a hash value for a string" )
@BoxBIF( alias = "Hash40" )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "hash" )
@BoxMember( type = BoxLangType.STRUCT, name = "hash" )
@BoxMember( type = BoxLangType.ARRAY, name = "hash" )
@BoxMember( type = BoxLangType.DATETIME, name = "hash" )
@BoxMember( type = BoxLangType.SET, name = "hash" )

public class Hash extends BIF {

	private static final String		DEFAULT_ALGORITHM	= "MD5";
	private static final String		QUICK_ALGORITHM		= "quick";
	private static final String		DEFAULT_ENCODING	= "utf-8";
	private static final Integer	DEFAULT_ITERATIONS	= 1;

	/**
	 * Constructor
	 */
	public Hash() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.input ),
		    new Argument( false, "string", Key.algorithm, DEFAULT_ALGORITHM ),
		    new Argument( false, "string", Key.encoding, DEFAULT_ENCODING ),
		    new Argument( false, "integer", Key.numIterations, DEFAULT_ITERATIONS )
		};
	}

	/**
	 * Creates a hexadecimal hash (message digest) of a value. Strings are hashed using the given encoding, binary values are hashed as-is, and
	 * any other object is serialized first. The result is a lowercase hex string.
	 *
	 * The special algorithm `quick` returns an insecure, fast 64-bit hash (16 hex characters). This BIF is also registered as `Hash40`, which
	 * always uses the SHA1 algorithm, and as a member function on strings, structs, arrays, dates and sets, e.g. `"hello".hash( "SHA-256" )`.
	 *
	 * @argument.input The value to hash.
	 *
	 * @argument.algorithm Optional. A java.security.MessageDigest algorithm name (case-insensitive) such as MD5, SHA, SHA-256 or SHA-512, or
	 *                     `quick`. Defaults to MD5. An unknown algorithm throws an exception.
	 *
	 * @argument.encoding Optional. The charset used to convert strings to bytes. Defaults to utf-8.
	 *
	 * @argument.numIterations Optional. The number of times the digest is re-applied to its own output. Defaults to 1.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The hash as a lowercase hexadecimal string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	hashItem			= arguments.get( Key.input );
		byte[]	hashBytes			= null;
		Integer	iterations			= arguments.getAsInteger( Key.numIterations );
		String	algorithm			= arguments.getAsString( Key.algorithm );
		String	charset				= arguments.getAsString( Key.encoding );
		boolean	isQuickAlgorithm	= algorithm.trim().toLowerCase().equals( QUICK_ALGORITHM );

		if ( isQuickAlgorithm ) {
			return EncryptionUtil.generate64BitHash( StringCaster.cast( hashItem ), 16 );
		}

		Key bifMethodKey = arguments.getAsKey( BIF.__functionName );
		if ( bifMethodKey.equals( Key.hash40 ) ) {
			algorithm = "SHA1";
		}
		if ( hashItem instanceof byte[] itemBytes ) {
			hashBytes = itemBytes;
		} else if ( hashItem instanceof String itemString ) {
			hashBytes = itemString.getBytes( Charset.forName( charset ) );
		} else if ( hashItem instanceof java.io.Serializable ) {
			try {
				hashBytes = SerializationUtils.serialize( ( java.io.Serializable ) hashItem );
			} catch ( SerializationException ns ) {
				try {
					hashBytes = JSONUtil.getJSONBuilder( false ).asString( hashItem ).getBytes();
				} catch ( IOException e ) {
					throw new BoxIOException( "The object provided could not be serialized to a byte array", e );
				}
			}
		} else {
			try {
				hashBytes = ObjectMarshaller.serialize( context, hashItem );
			} catch ( BoxIOException e ) {
				// fallback to a stringification and get the bytes if serialization fails
				hashBytes = hashItem.toString().getBytes( Charset.forName( arguments.getAsString( Key.encoding ) ) );
			}
		}

		return EncryptionUtil.hash( hashBytes, algorithm, iterations );

	}

}
