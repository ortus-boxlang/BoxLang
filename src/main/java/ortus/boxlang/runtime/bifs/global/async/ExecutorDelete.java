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

@BoxBIF( description = "Delete an executor service from the registry" )
public class ExecutorDelete extends BIF {

	/**
	 * Constructor
	 */
	public ExecutorDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key._NAME )
		};
	}

	/**
	 * Deletes an executor from the registry by name. Unknown names are ignored.
	 * Note that deleting does not guarantee the executor's threads are stopped; use {@link ExecutorShutdown} to shut an executor down.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.name The name of the executor to delete.
	 *
	 * @return Always true.
	 */
	@Override
	public Boolean _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String name = arguments.getAsString( Key._NAME );
		asyncService.deleteExecutor( name );
		return true;
	}

}
