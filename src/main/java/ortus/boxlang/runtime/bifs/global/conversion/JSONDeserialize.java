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

@BoxBIF( description = "Convert JSON string to BoxLang data" )
@BoxMember( type = BoxLangType.STRING_STRICT )
@BoxMember( type = BoxLangType.STRING )
@BoxMember( type = BoxLangType.STRING_STRICT, name = "fromJSON" )
@BoxMember( type = BoxLangType.STRING, name = "fromJSON" )
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
	 * Converts a JSON (JavaScript Object Notation) string into BoxLang data such as a struct, array, string, number or boolean.
	 * <p>
	 * JSON objects are always deserialized into ordered structs, which preserves the key order of the original JSON string.
	 * This is handy when reading a JSON file, modifying it, and writing it back out.
	 * <p>
	 * Also available as the <code>jsonDeserialize()</code> and <code>fromJSON()</code> member functions on strings.
	 *
	 * <pre>
	 * data = jsonDeserialize( '{"name":"BoxLang","tags":["jvm","dynamic"]}' )
	 * data = '[1,2,3]'.fromJSON()
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.json The JSON string to convert to data.
	 *
	 * @argument.strictMapping Whether to convert the JSON strictly. If true (default), every JSON object becomes a struct. If false, an object
	 *                         that has the shape of a serialized query (a "columns" array and a "data" array, with an optional "rowCount") is
	 *                         converted to a query.
	 *
	 * @argument.useCustomSerializer The name of a custom serializer to use. Not used.
	 *
	 * @throws BoxRuntimeException If the string is not valid JSON.
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
