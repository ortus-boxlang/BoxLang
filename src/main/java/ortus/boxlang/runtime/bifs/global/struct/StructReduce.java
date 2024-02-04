
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

package ortus.boxlang.runtime.bifs.global.struct;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.util.StructUtil;

@BoxBIF

public class StructReduce extends BIF {

	/**
	 * Constructor
	 */
	public StructReduce() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( Key.initialValue )
		};
	}

	/**
	 * Run the provided udf against struct to reduce the values to a single output
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The struct to reduce
	 *
	 * @argument.callback The function to invoke for each entry in the struct. The function will be passed 4 arguments: the accumulator, they entry key,
	 *                    the
	 *                    current index, and the original struct. The function should return the new accumulator value.
	 *
	 * @argument.initialValue The initial value of the accumulator
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		return StructUtil.reduce(
		    arguments.getAsStruct( Key.struct ), arguments.getAsFunction( Key.callback ), context, arguments.get( Key.initialValue )
		);

	}

}
