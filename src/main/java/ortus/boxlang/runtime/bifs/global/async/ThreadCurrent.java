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
import ortus.boxlang.runtime.types.Argument;

@BoxBIF( description = "Returns the current thread object" )
public class ThreadCurrent extends BIF {

	/**
	 * Constructor
	 */
	public ThreadCurrent() {
		super();
		declaredArguments = new Argument[] {};
	}

	/**
	 * Returns the currently executing thread.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @return The current {@link Thread} object. This can be used to inspect thread properties such as
	 *         {@link Thread#getName()}, {@link Thread#getId()}, {@link Thread#getState()}, or to check
	 *         {@link Thread#isVirtual()}.
	 *
	 * @example
	 *
	 *          <pre>{@code
	 * // Get the current thread and print its name
	 * t = threadCurrent();
	 * println( "Running on thread: " + t.getName() );
	 * println( "Is virtual? " & t.isVirtual() );
	 * }</pre>
	 */
	@Override
	public Thread _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return Thread.currentThread();
	}

}
