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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
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
			return base64_or_object;
		}

		String string = StringCaster.cast( base64_or_object );
		return java.util.Base64.getDecoder().decode( string );
	}
}
