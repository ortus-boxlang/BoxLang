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
package ortus.boxlang.runtime.bifs;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.Argument;

/**
 * Base class for all BIFs. BIFs are invoked by the runtime when a function is called.
 */
public abstract class BIF {

	/**
	 * Used to indicate that the BIF is being invoked as a member function
	 * and it will replace the first argument with the object on which it is being invoked
	 */
	public static final Key		__isMemberExecution	= new Key( "__isMemberExecution" );

	/**
	 * BIF Arguments
	 */
	protected Argument[]		arguments			= new Argument[] {};

	/**
	 * The function service helper
	 */
	protected FunctionService	functionService		= BoxRuntime.getInstance().getFunctionService();

	/**
	 * Invoke the BIF with the given arguments
	 *
	 * @param context   The context in which the BIF is being invoked
	 * @param arguments The arguments to the BIF
	 *
	 * @return The result of the invocation
	 */
	public abstract Object invoke( IBoxContext context, ArgumentsScope arguments );

	/**
	 * Get the arguments for this BIF
	 *
	 * @return The arguments for this BIF
	 */
	public Argument[] getArguments() {
		return arguments;
	}

}
