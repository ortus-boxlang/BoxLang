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
package ortus.boxlang.runtime.bifs.global.cli;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;

@BoxBIF
public class CLIExit extends BIF {

	/**
	 * Constructor
	 */
	public CLIExit() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "numeric", Key.exitCode, 0 )
		};
	}

	/**
	 * Exits the CLI with the specified exit code.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.exitCode By convention, a nonzero status code, indicates abnormal termination. Deault code is 0.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		System.exit( arguments.getAsInteger( Key.exitCode ) );
		return null;
	}

}
