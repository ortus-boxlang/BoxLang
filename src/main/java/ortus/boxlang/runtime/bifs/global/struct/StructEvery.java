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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructEvery extends BIF {

	/**
	 * Constructor
	 */
	public StructEvery() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "struct", Key.struct ),
		    new Argument( true, "function", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads )
		};
	}

	/**
	 * Used to iterate over a struct and test whether every item in the struct meets the test.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct to test
	 *
	 * @argument.callback The function used to test. The function will be passed 3 arguments: the key, the value, the struct.
	 *
	 * @argument.parallel Specifies whether the items can be executed in parallel
	 *
	 * @argument.maxThreads The maximum number of threads to use when parallel = true
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct target = arguments.getAsStruct( Key.struct );
		return StructUtil.every(
		    target,
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads )
		);
	}

}
