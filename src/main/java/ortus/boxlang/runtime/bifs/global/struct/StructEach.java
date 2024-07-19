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
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRUCT )

public class StructEach extends BIF {

	/**
	 * Constructor
	 */
	public StructEach() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "structloose", Key.struct ),
		    new Argument( true, "function:BiConsumer", Key.callback ),
		    new Argument( false, "boolean", Key.parallel, false ),
		    new Argument( false, "integer", Key.maxThreads ),
		    new Argument( false, "boolean", Key.ordered, false )
		};
	}

	/**
	 * Used to iterate over a struct and run the function closure for each key/value pair.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.struct The target struct to iterate
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the key, the value, the struct. You can alternatively pass a Java BiConsumer which will only receive the first 2 args.
	 *
	 * @argument.parallel Specifies whether the items can be executed in parallel
	 *
	 * @argument.maxThreads The maximum number of threads to use when parallel = true
	 *
	 * @argument.ordered (BoxLang only) whether parallel operations should execute and maintain order
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		IStruct target = arguments.getAsStruct( Key.struct );

		StructUtil.each(
		    target,
		    arguments.getAsFunction( Key.callback ),
		    context,
		    arguments.getAsBoolean( Key.parallel ),
		    arguments.getAsInteger( Key.maxThreads ),
		    arguments.getAsBoolean( Key.ordered )
		);

		return null;
	}

}
