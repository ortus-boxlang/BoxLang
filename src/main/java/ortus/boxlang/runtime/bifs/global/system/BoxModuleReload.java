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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;

@BoxBIF
public class BoxModuleReload extends BIF {

	/**
	 * Constructor
	 */
	public BoxModuleReload() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.module )
		};
	}

	/**
	 * Reload all the registered modules in the runtime or a specific module.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.module The name of the module to reload. If not provided, all modules are reloaded.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String moduleName = arguments.getAsString( Key.module );

		if ( moduleName == null ) {
			this.moduleService.reloadAll();
		} else {
			this.moduleService.reload( Key.of( moduleName ) );
		}

		return null;

	}

}
