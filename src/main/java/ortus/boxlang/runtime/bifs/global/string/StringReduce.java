
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

package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

@BoxBIF

public class StringReduce extends ListReduce {

	/**
	 * Constructor
	 */
	public StringReduce() {
		super();
	}

	/**
	 * Run the provided udf over all characters in a string to reduce the values to a single output
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The string to iterate
	 *
	 * @argument.callback The callback to use for the test
	 *
	 * @argument.initialValue The initial value of the reduction
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		arguments.put( Key.delimiter, "" );
		return super._invoke( context, arguments );
	}

}
