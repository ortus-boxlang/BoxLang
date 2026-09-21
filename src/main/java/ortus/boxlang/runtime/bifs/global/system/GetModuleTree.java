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

@BoxBIF( description = "Get the module hierarchy as a tree, including modules nested inside other modules (module inception)" )
public class GetModuleTree extends BIF {

	/**
	 * Constructor
	 */
	public GetModuleTree() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, Key.module )
		};
	}

	/**
	 * Get the module hierarchy as a tree. Each node carries the module's own record data (see
	 * {@code getModuleInfo()}) plus a {@code children} struct of any modules nested inside it,
	 * recursively.
	 * <p>
	 * With no argument, returns every top-level module. Nested modules are found under their
	 * parent's {@code children} entry, not at the top level.
	 * <p>
	 * Passed a module name, returns the tree rooted at that module instead: its own data plus its
	 * nested modules, recursively. An unregistered module name returns an empty struct.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.module The name of the module to root the tree at. If not provided, the full
	 *                  tree of every top-level module is returned.
	 *
	 * @return The module tree.
	 */
	public IStruct _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String moduleName = arguments.getAsString( Key.module );

		return moduleName == null
		    ? this.moduleService.getModuleTree()
		    : this.moduleService.getModuleTree( Key.of( moduleName ) );
	}

}
