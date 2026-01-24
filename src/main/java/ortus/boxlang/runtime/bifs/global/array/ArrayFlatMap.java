/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.bifs.global.array.ArrayParallelUtil.ParallelSettings;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF( description = "Create a new array by transforming each element using a callback function" )
@BoxMember( type = BoxLangType.ARRAY )
public class ArrayFlatMap extends BIF {

	/**
	 * Constructor
	 */
	public ArrayFlatMap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ARRAY, Key.array ),
		    new Argument( true, "function:Function", Key.callback ),
		    new Argument( false, Argument.BOOLEAN, Key.parallel, false ),
		    new Argument( false, Argument.ANY, Key.maxThreads ),
		    new Argument( false, Argument.BOOLEAN, Key.virtual, false )
		};
	}

	/**
	 * Maps each element and flattens the result one level.
	 *
	 * <pre>
	 * values = [ 1, 2, 3 ];
	 * values.flatMap( ( value ) => [ value, value * 10 ] );
	 * // [ 1, 10, 2, 20, 3, 30 ]
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.array The array to transform
	 *
	 * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the current item, and the
	 *                    current index, and the original array. You can alternatively pass a Java Function which will only receive the 1st arg.
	 *
	 * @argument.parallel If true, the function will be invoked in parallel using multiple threads. Defaults to false.
	 *
	 * @argument.maxThreads The maximum number of threads to use when parallel is true. If not provided the common thread pool will be used. If a boolean value is passed, it will be assigned as the virtual argument.
	 *
	 * @argument.virtual If true, the function will be invoked using virtual thread. Defaults to false. Ignored if parallel is false.
	 *
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		ParallelSettings settings = ArrayParallelUtil.resolveParallelSettings( arguments );

		return ListUtil.flatten(
		    ListUtil.map(
		        arguments.getAsArray( Key.array ),
		        arguments.getAsFunction( Key.callback ),
		        context,
		        arguments.getAsBoolean( Key.parallel ),
		        settings.maxThreads(),
		        settings.virtual()
		    ),
		    1
		);
	}
}
