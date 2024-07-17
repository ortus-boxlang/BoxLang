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
package ortus.boxlang.runtime.bifs.global.async;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;

@BoxBIF
public class ExecutorStatus extends BIF {

	/**
	 * Constructor
	 */
	public ExecutorStatus() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key._NAME )
		};
	}

	/**
	 * Get a struct map of a specific executor and its stats.
	 *
	 * If no parameters are passed, it will return a struct map of all executors and their stats.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the executor to get.
	 *
	 * @throws KeyNotFoundException If the executor is not found.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String name = arguments.getAsString( Key._NAME );

		if ( name == null ) {
			return asyncService.getExecutorStatusMap();
		}

		return asyncService.getExecutorStatusMap( name );
	}

}
