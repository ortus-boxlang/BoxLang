
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

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.EncryptionUtil;

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
		byte[] binaryData = ( byte[] ) arguments.get( Key.binary );
		return EncryptionUtil.encodeObject( binaryData, arguments.getAsString( Key.encoding ) );
	}

}
