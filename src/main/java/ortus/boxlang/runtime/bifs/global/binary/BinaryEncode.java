
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

package ortus.boxlang.runtime.bifs.global.binary;

import java.util.Base64;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class BinaryEncode extends BIF {

	/**
	 * Constructor
	 */
	public BinaryEncode() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.binary ),
		    new Argument( true, "string", Key.encoding )
		};
	}

	/**
	 * Encodes binary data to a string with the specified algorithm
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key		encodingKey	= Key.of( arguments.getAsString( Key.encoding ) );
		byte[]	binaryData	= ( byte[] ) arguments.get( Key.binary );

		// HEX encoding
		if ( encodingKey.equals( Key.encodingHex ) ) {
			StringBuilder sb = new StringBuilder( binaryData.length * 2 );
			for ( byte b : binaryData )
				sb.append( String.format( "%02x", b ) );
			return sb.toString();
		}
		// UU encoding
		else if ( encodingKey.equals( Key.encodingUU ) ) {
			return Base64.getMimeEncoder().encodeToString( binaryData );
		}
		// Base64 encoding
		else if ( encodingKey.equals( Key.encodingBase64 ) ) {
			return Base64.getEncoder().encodeToString( binaryData );
		}
		// Base64 URL encoding
		else if ( encodingKey.equals( Key.encodingBase64Url ) ) {
			return Base64.getUrlEncoder().encodeToString( binaryData );
		}
		// Invalid encoding
		else {
			throw new BoxRuntimeException(
			    String.format(
			        "The encoding argument [%s] is not a valid encoding type for the function BinaryEncode",
			        encodingKey.getName()
			    )
			);
		}

	}

}
