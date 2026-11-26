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

@BoxBIF( description = "Navigate through complex data structures" )
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
	 * Constructs a fluent data navigator based on different types of data. Use the navigator to safely walk nested data with dot paths,
	 * array indices, and JSONPath-style expressions, extract typed values, and provide defaults without null checks.
	 * <p>
	 * Available Input Types:
	 * <ul>
	 * <li>String: A JSON string</li>
	 * <li>String: A file path to a JSON file</li>
	 * <li>Path: A Java Path to a JSON file</li>
	 * <li>Struct: A BoxLang structure</li>
	 * <li>Map: A Java map</li>
	 * </ul>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.data The data to navigate: a JSON string, a path to a JSON file, a Java Path, a struct, or a map.
	 *
	 * @return A fluent navigator over the data. Use methods such as from(), get(), getOrDefault(), has(), and query() to navigate it.
	 */
	public Navigator _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// JSON string, Struct, Maps, File paths, More!
		return DataNavigator.of( arguments.get( Key.data ) );
	}

}
