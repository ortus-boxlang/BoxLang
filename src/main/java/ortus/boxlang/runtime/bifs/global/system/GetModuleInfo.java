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
import ortus.boxlang.runtime.types.Struct;

@BoxBIF
public class GetModuleInfo extends BIF {

	/**
	 * Constructor
	 */
	public GetModuleInfo() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.module )
		};
	}

	/**
	 * Get the module record for a loaded module in BoxLang. If the module
	 * doesn't exist, an empty struct is returned.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.module The name of the module to get the record for.
	 *
	 * @return The module record for the requested module.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key moduleName = Key.of( arguments.get( Key.module ) );

		if ( this.moduleService.hasModule( moduleName ) == false ) {
			return new Struct();
		}

		return this.moduleService.getModuleRecord( moduleName ).asStruct();
	}

}
