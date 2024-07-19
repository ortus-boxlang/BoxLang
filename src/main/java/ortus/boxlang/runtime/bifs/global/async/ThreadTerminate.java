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

import java.util.Set;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class ThreadTerminate extends BIF {

	/**
	 * Constructor
	 */
	public ThreadTerminate() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.threadName, Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Terminates the thread specified by the threadName parameter.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.threadName The name of the thread to terminate.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Key threadName = Key.of( arguments.getAsString( Key.threadName ) );
		context.getParentOfType( RequestBoxContext.class )
		    .getThreadManager()
		    .terminateThread( threadName );
		return null;
	}

}
