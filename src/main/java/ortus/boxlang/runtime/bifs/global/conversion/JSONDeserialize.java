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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.JSONUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
public class JSONDeserialize extends BIF {

	/**
	 * Constructor
	 */
	public JSONDeserialize() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.json ),
		    new Argument( false, "boolean", Key.strictMapping, true ),
		    new Argument( false, "string", Key.useCustomSerializer )
		};
	}

	/**
	 * Converts a JSON (JavaScript Object Notation) string data representation into data, such as a structure or array.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.json The JSON string to convert to data.
	 *
	 * @argument.strictMapping A Boolean value that specifies whether to convert the JSON strictly. If true, everything becomes structures.
	 *
	 * @argument.useCustomSerializer A string that specifies the name of a custom serializer to use. (Not used)
	 *
	 * @return The data representation of the JSON string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	json			= arguments.getAsString( Key.json );
		Boolean	strictMapping	= arguments.getAsBoolean( Key.strictMapping );
		Object	result			= JSONUtil.fromJSON( json );
		return JSONUtil.mapToBLTypes( result, strictMapping );
	}

}
