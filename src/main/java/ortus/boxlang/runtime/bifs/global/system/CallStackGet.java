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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

@BoxBIF
public class CallStackGet extends BIF {

	/**
	 * Constructor
	 */
	public CallStackGet() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.NUMERIC, Key.maxFrames, -1 )
		};
	}

	/**
	 * Returns an array of structs by default of the current tag context.
	 * Each struct contains template name, line number, and function name (if applicable).
	 * This is a snapshot of all function calls or invocations.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 *
	 * @return An array of structs containing the call stack.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Get the max frames
		int maxFrames = arguments.getAsDouble( Key.maxFrames ).intValue();
		return ExceptionUtil.getTagContext( maxFrames );
	}
}
