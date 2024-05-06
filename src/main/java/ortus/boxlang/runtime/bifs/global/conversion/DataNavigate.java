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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.DataNavigator;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;

@BoxBIF
public class DataNavigate extends BIF {

	/**
	 * Constructor
	 */
	public DataNavigate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.data )
		};
	}

	/**
	 * Constructs a fluent data navigator based on different types of data.
	 * <p>
	 * Available Input Types:
	 * <ul>
	 * <li>String: A JSON string</li>
	 * <li>File Path: A path to a JSON file as a string</li>
	 * <li>File Path: A path to a JSON file as a Java Path</li>
	 * <li>Struct: A structure</li>
	 * <li>Map: A Java map</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.json The JSON string to convert to CFML data.
	 *
	 * @argument.strictMapping A Boolean value that specifies whether to convert the JSON strictly. If true, everything becomes structures.
	 *
	 * @argument.useCustomSerializer A string that specifies the name of a custom serializer to use. (Not used)
	 *
	 * @return The CFML data representation of the JSON string.
	 */
	public Navigator _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// JSON string, Struct, Maps, File paths, More!
		return DataNavigator.of( arguments.get( Key.data ) );
	}

}
