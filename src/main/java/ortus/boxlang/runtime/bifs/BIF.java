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
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Base class for all BIFs. BIFs are invoked by the runtime when a function is called.
 */
public abstract class BIF {

	/**
	 * Used to indicate that the BIF is being invoked as a member function
	 * and it will replace the first argument with the object on which it is being invoked
	 */
	public static final Key			__isMemberExecution	= Key.__isMemberExecution;

	/**
	 * Used to indicate what is the name of the function being invoked just like getCalledFunctionName() but internally
	 */
	public static final Key			__functionName		= Key.__functionName;

	/**
	 * BIF Arguments
	 */
	protected Argument[]			declaredArguments	= new Argument[] {};

	/**
	 * The runtime instance
	 */
	protected BoxRuntime			runtime				= BoxRuntime.getInstance();

	/**
	 * The function service helper
	 */
	protected FunctionService		functionService		= BoxRuntime.getInstance().getFunctionService();

	/**
	 * The interceptor service helper
	 */
	protected InterceptorService	interceptorService	= BoxRuntime.getInstance().getInterceptorService();

	/**
	 * The cache service helper
	 */
	protected CacheService			cacheService		= BoxRuntime.getInstance().getCacheService();

	/**
	 * The async service helper
	 */
	protected AsyncService			asyncService		= BoxRuntime.getInstance().getAsyncService();

	/**
	 * Invoke the BIF with the given arguments
	 *
	 * @param context   The context in which the BIF is being invoked
	 * @param arguments The arguments to the BIF
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Add any setup/teardown here that needs to run for all BIFs.
		return _invoke( context, arguments );
	}

	/**
	 * This is overridden by the concrete to provide the actual BIF implementation
	 *
	 * @param context   The context in which the BIF is being invoked
	 * @param arguments The arguments to the BIF
	 *
	 * @return The result of the invocation
	 */
	public abstract Object _invoke( IBoxContext context, ArgumentsScope arguments );

	/**
	 * Get the arguments for this BIF
	 *
	 * @return The arguments for this BIF
	 */
	public Argument[] getDeclaredArguments() {
		return declaredArguments;
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		interceptorService.announce( state, data );
	}

}
