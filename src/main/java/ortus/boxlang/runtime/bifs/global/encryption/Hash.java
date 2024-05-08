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

import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.EncryptionUtil;

@BoxBIF
@BoxBIF( alias = "Hash40" )
@BoxMember( type = BoxLangType.STRING, name = "hash" )
@BoxMember( type = BoxLangType.STRUCT, name = "hash" )
@BoxMember( type = BoxLangType.ARRAY, name = "hash" )
@BoxMember( type = BoxLangType.DATETIME, name = "hash" )

public class Hash extends BIF {

	private static final String		DEFAULT_ALGORITHM	= "MD5";
	private static final String		DEFAULT_ENCODING	= "utf-8";
	private static final Integer	DEFAULT_ITERATIONS	= 1;

	// The hash item object - non-local so we can reassign it in streams
	private static Object			hashItem			= null;

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
	 * Creates an algorithmic hash of an object
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @throws IOException
	 * @throws JSONObjectException
	 *
	 * @argument.input The item to be hashed
	 *
	 * @argument.algorithm The supported {@link java.security.MessageDigest } algorithm (case-insensitive)
	 *
	 * @argument.encoding Applicable to strings ( default "utf-8" )
	 *
	 * @argument.iterations The number of iterations to re-digest the object ( default 1 );
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		hashItem = arguments.get( Key.input );
		byte[]	hashBytes		= null;
		Integer	iterations		= arguments.getAsInteger( Key.numIterations );
		String	algorithm		= arguments.getAsString( Key.algorithm );
		String	charset			= arguments.getAsString( Key.encoding );

		Key		bifMethodKey	= arguments.getAsKey( BIF.__functionName );
		if ( bifMethodKey.equals( Key.hash40 ) ) {
			algorithm = "SHA1";
		}

		if ( hashItem instanceof String ) {
			hashBytes = arguments
			    .getAsString( Key.input )
			    .getBytes(
			        Charset.forName( charset )
			    );
		} else if ( hashItem instanceof java.io.Serializable ) {
			try {
				hashBytes = SerializationUtils.serialize( ( java.io.Serializable ) hashItem );
			} catch ( SerializationException ns ) {
				try {
					hashBytes = JSONUtil.getJSONBuilder().asString( hashItem ).getBytes();
				} catch ( IOException e ) {
					throw new BoxIOException( "The object provided could not be serialized to a byte array", e );
				}
			}
		} else {
			hashBytes = hashItem.toString().getBytes( Charset.forName( arguments.getAsString( Key.encoding ) ) );
		}

		return EncryptionUtil.hash( hashBytes, algorithm, iterations );

	}

}
