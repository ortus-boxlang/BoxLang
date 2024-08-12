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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ThreadBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.AbortException;
import ortus.boxlang.runtime.util.RequestThreadManager;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
public class ThreadNew extends BIF {

	/**
	 * Constructor
	 */
	public ThreadNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.FUNCTION, Key.runnable ),
		    new Argument( false, Argument.STRUCT, Key.attributes, new Struct() ),
		    new Argument( false, Argument.STRING, Key._NAME, "" ),
		    new Argument( false, Argument.STRING, Key.priority, "normal", Set.of( Validator.valueOneOf( "high", "low", "normal" ) ) )
		};
	}

	/**
	 * Creates a new thread of execution based on the passed closure/lambda.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @arguments.runnable The closure/lambda to execute in the new thread.
	 *
	 * @arguments.attributes A struct of data to bind into the thread's scope.
	 *
	 * @argument.threadName The name of the thread to track it, if not provided a default name will be generated.
	 *
	 * @argument.priority The priority of the thread. Possible values are "high", "low", and "normal". Default is "normal".
	 *
	 * @return The newly created thread object if you want to monitor it.
	 */
	@Override
	public Thread _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Function				task			= arguments.getAsFunction( Key.runnable );
		String					name			= arguments.getAsString( Key._NAME );
		String					priority		= arguments.getAsString( Key.priority );
		IStruct					attributes		= arguments.getAsStruct( Key.attributes );
		RequestThreadManager	threadManager	= context.getParentOfType( RequestBoxContext.class ).getThreadManager();
		final Key				nameKey			= RequestThreadManager.ensureThreadName( name );
		ThreadBoxContext		tContext		= threadManager.createThreadContext( context, nameKey );

		// Startup the thread
		return threadManager.startThread(
		    // The thread context to run in
		    tContext,
		    // The name of the thread as a key
		    nameKey,
		    // The thread priority
		    priority,
		    // The Runnable Proxy
		    () -> {
			    StringBuffer buffer		= new StringBuffer();
			    Throwable	exception	= null;
			    Logger		logger		= LoggerFactory.getLogger( ThreadNew.class );
			    try {
				    // Execute the function using the thread context
				    tContext.invokeFunction( task );
			    } catch ( AbortException e ) {
				    // We log it so we can potentially find out why it was aborted
				    logger.debug( "Thread [{}] aborted at stacktrace: {}", nameKey.getName(), e.getStackTrace() );
			    } catch ( Throwable e ) {
				    exception = e;
				    logger.error( "Thread [{}] terminated with exception: {}", nameKey.getName(), e.getMessage() );
				    logger.error( "-> Exception", e );
			    } finally {
				    threadManager.completeThread(
				        nameKey,
				        buffer.toString(),
				        exception,
				        java.lang.Thread.interrupted()
				    );
			    }
		    },
		    // The Struct of data to bind into the thread's scope
		    attributes
		);
	}

}
