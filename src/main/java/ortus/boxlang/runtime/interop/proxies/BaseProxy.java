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
package ortus.boxlang.runtime.interop.proxies;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.util.RequestThreadManager;

/**
 * This is a BoxLang proxy class for functional interfaces so we can use them in BoxLang
 * via type casting and coercion.
 */
public abstract class BaseProxy {

	/**
	 * The target function or box class that this proxy is wrapping.
	 */
	protected Object				target;

	/**
	 * The default method to execute on the target class runnable
	 */
	protected Key					defaultMethod;

	/**
	 * The context that created this proxy.
	 */
	protected IBoxContext			context;

	/**
	 * App context if any
	 */
	protected ApplicationBoxContext	appContext;

	/**
	 * The logger for this proxy.
	 */
	protected Logger				logger;

	/**
	 * The thread manager for this proxy.
	 */
	protected RequestThreadManager	threadManager;

	/**
	 * Constructor for the proxy.
	 *
	 * @param target  The target function / object to wrap.
	 * @param context The context that created this proxy.
	 * @param method  The method to execute on the target.
	 *
	 * @throws IllegalArgumentException If the target is null or not a function or IClassRunnable.
	 */
	protected BaseProxy( Object target, IBoxContext context, String method ) {

		// Verify the target is not null
		if ( target == null ) {
			throw new IllegalArgumentException( "Target cannot be null" );
		}

		// The target must be a Function or IClassRunnable else, throw an exception
		if ( ! ( target instanceof Function ) && ! ( target instanceof IClassRunnable ) ) {
			throw new IllegalArgumentException( "Target must be a Function or IClassRunnable" );
		}

		this.target = target;

		// Store default method if passed
		if ( method != null && !method.isEmpty() ) {
			this.defaultMethod = Key.of( method );
		} else {
			this.defaultMethod = Key.run;
		}

		this.context		= context;
		this.appContext		= context.getParentOfType( ApplicationBoxContext.class );
		this.threadManager	= new RequestThreadManager();
		prepLogger( BaseProxy.class );
	}

	/**
	 * Constructor for the proxy.
	 *
	 * @param target  The target function / object to wrap.
	 * @param context The context that created this proxy.
	 */
	protected BaseProxy( Object target, IBoxContext context ) {
		this( target, context, "run" );
	}

	/**
	 * Utility Methods
	 */

	/**
	 * This invoke is ONLY for IClassRunnables to invoke a method on a target
	 *
	 * @param @method The method to invoke on a target
	 * @param args    The arguments to pass to the function
	 *
	 * @return The result of the function
	 */
	protected Object invoke( Key method, Object... args ) {
		return getDynamicTarget().dereferenceAndInvoke(
		    this.context,
		    method,
		    args,
		    false
		);
	}

	/**
	 * Invoke using our function or callable strategy
	 *
	 * @param args The arguments to pass to the function
	 *
	 * @return The result of the function
	 */
	protected Object invoke( Object... args ) {
		if ( isFunctionTarget() ) {
			return this.context.invokeFunction(
			    this.target,
			    args
			);
		} else {
			return getDynamicTarget().dereferenceAndInvoke(
			    this.context,
			    this.defaultMethod,
			    args,
			    false
			);
		}
	}

	/**
	 * Is the target a function instance
	 *
	 * @return True if the target is a function
	 */
	protected Boolean isFunctionTarget() {
		return this.target instanceof Function;
	}

	/**
	 * Is the target a class runnable
	 *
	 * @return True if the target is a class runnable
	 */
	protected Boolean isClassRunnableTarget() {
		return this.target instanceof IClassRunnable;
	}

	/**
	 * Get the target as a function
	 *
	 * @return The target as a function
	 */
	protected Function getAsFunction() {
		return ( Function ) this.target;
	}

	/**
	 * Get the target as a IClassRunnable
	 */
	protected IClassRunnable getDynamicTarget() {
		return ( IClassRunnable ) this.target;
	}

	/**
	 * Get the default method
	 */
	protected Key getDefaultMethod() {
		return this.defaultMethod;
	}

	/**
	 * Prep logger for class
	 *
	 * @param clazz
	 */
	protected void prepLogger( Class<?> clazz ) {
		this.logger = LoggerFactory.getLogger( clazz );
	}

	/**
	 * Get the configured logger
	 *
	 * @return The logger
	 */
	protected Logger getLogger() {
		return this.logger;
	}

	/**
	 * Get the current thread
	 */
	protected Thread getCurrentThread() {
		return Thread.currentThread();
	}

	/**
	 * Get the current thread name
	 */
	protected String getCurrentThreadName() {
		return Thread.currentThread().getName();
	}

	/**
	 * Check if I am in the fork join pool
	 */
	protected boolean isInForkJoinPool() {
		return getCurrentThread().getName().startsWith( "ForkJoinPool" );
	}

	/**
	 * Get the thread manager for the proxy
	 *
	 * @return The thread manager
	 */
	protected RequestThreadManager getThreadManager() {
		return this.threadManager;
	}

	/**
	 * Are we in a thread
	 */
	protected boolean isInThread() {
		return this.threadManager.isInThread();
	}
}
