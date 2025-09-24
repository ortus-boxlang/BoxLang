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
package ortus.boxlang.runtime.bifs.global.conversion;

import com.fasterxml.jackson.core.Base64Variants;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF( description = "Convert data to binary format" )
@BoxMember( type = BoxLangType.STRING_STRICT )
public class ToBinary extends BIF {

	/**
	 * Constructor
	 */
	public ToBinary() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.base64_or_object )
		};
	}

	/**
	 * Calculates the binary representation of Base64-encoded data.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.base64_or_object A string containing base64-encoded data.
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object base64_or_object = arguments.get( Key.base64_or_object );

		if ( base64_or_object instanceof byte[] b ) {
			return b;
		}

		String	string	= StringCaster.cast( base64_or_object ).trim();

		// Add padding if necessary
		int		padding	= string.length() % 4;
		if ( padding != 0 ) {
			string = string + "=".repeat( 4 - padding ); // Add padding
		}

		string = stripExcessBase64Padding( string );

		// return java.util.Base64.getDecoder().decode( string );
		return Base64Variants.getDefaultVariant().decode( string );
	}

	/**
	 * Strips excess padding from a Base64 string.
	 * 
	 * @param base64 The Base64 string to process.
	 * 
	 * @return The processed Base64 string without excess padding.
	 */
	private String stripExcessBase64Padding( String base64 ) {
		int len = base64.length();

		// Fast path: length is multiple of 4 and no more than 2 trailing '='
		if ( len % 4 == 0 ) {
			int padCount = 0;
			for ( int i = len - 1; i >= 0 && base64.charAt( i ) == '='; i-- ) {
				padCount++;
			}
			if ( padCount <= 2 ) {
				return base64; // Input is already valid
			}
		}

		// Count actual trailing '='
		int padStart = len;
		while ( padStart > 0 && base64.charAt( padStart - 1 ) == '=' ) {
			padStart--;
		}
		int		padCount	= len - padStart;

		// Trim to at most 2 '='
		int		maxPad		= Math.min( padCount, 2 );
		String	trimmed		= base64.substring( 0, padStart + maxPad );

		// Ensure length is multiple of 4 by stripping extra '=' if needed
		int		mod			= trimmed.length() % 4;
		if ( mod != 0 ) {
			int	excess	= mod;
			int	end		= trimmed.length();
			while ( excess > 0 && trimmed.charAt( end - 1 ) == '=' ) {
				end--;
				excess--;
			}
			trimmed = trimmed.substring( 0, end );
		}

		return trimmed;
	}

}
