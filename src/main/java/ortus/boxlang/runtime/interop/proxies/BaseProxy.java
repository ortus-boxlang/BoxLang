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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
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

	private static final Set<Key>	javaLangObjectPublicMethods	= Set.of(
	    Key.getClass,
	    Key._hashCode,
	    Key.equals,
	    Key.toString,
	    Key.notify,
	    Key.notifyAll,
	    Key.wait
	);

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
	 * 
	 * @throws InterruptedException
	 */
	protected Object invoke( Key method, Object... args ) throws InterruptedException {
		IClassRunnable target = getDynamicTarget();
		// We need to handle the case where a proxy being passed around in the JDK code could have methods like .toString() call on them, with the expectation that
		// the method will exist, since that is true of all Java objects extending `java.lang.Object`.
		if ( javaLangObjectPublicMethods.contains( method ) && !target.getThisScope().containsKey( method ) ) {
			if ( method.equals( Key.getClass ) ) {
				return target.getClass();
			} else if ( method.equals( Key._hashCode ) ) {
				return target.hashCode();
			} else if ( method.equals( Key.equals ) ) {
				return target.equals( args[ 0 ] );
			} else if ( method.equals( Key.toString ) ) {
				return target.toString();
			} else if ( method.equals( Key.notify ) ) {
				target.notify();
				return null;
			} else if ( method.equals( Key.notifyAll ) ) {
				target.notifyAll();
				return null;
			} else if ( method.equals( Key.wait ) ) {
				if ( args.length == 0 ) {
					target.wait();
				} else if ( args.length == 1 ) {
					target.wait( ( Long ) args[ 0 ] );
				} else if ( args.length == 2 ) {
					target.wait( ( Long ) args[ 0 ], ( Integer ) args[ 1 ] );
				} else {
					throw new BoxRuntimeException( "Unknown method: " + method );
				}
				return null;
			} else {
				throw new BoxRuntimeException( "Unknown method: " + method );
			}

		} else {
			return target.dereferenceAndInvoke(
			    this.context,
			    method,
			    args,
			    false
			);
		}
	}

	/**
	 * Invoke using our function or callable strategy
	 *
	 * @param args The arguments to pass to the function
	 *
	 * @return The result of the function
	 * 
	 * @throws InterruptedException
	 */
	protected Object invoke( Object... args ) throws InterruptedException {
		if ( isFunctionTarget() ) {
			return this.context.invokeFunction(
			    this.target,
			    args
			);
		} else {
			return invoke( this.defaultMethod, args );
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
