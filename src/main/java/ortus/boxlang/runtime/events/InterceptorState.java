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
package ortus.boxlang.runtime.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;

/**
 * An interceptor state is an event state that is used to hold observers that want to listent
 * to that specific state. For example, the "preProcess" state is used to hold observers that
 * listen to "preProcess" events.
 *
 * The {@see InterceptorService} is in charge of managing all states and event registrations in BoxLang.
 */
public class InterceptorState {

	/**
	 * The interception state we represent (e.g. "preProcess", "postProcess", "onRuntimeStartup" etc.)
	 */
	private Key								name;

	/**
	 * The observers for this state
	 */
	private final List<InterceptorEntry>	observers	= new CopyOnWriteArrayList<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The state name
	 */
	public InterceptorState( String name ) {
		this( Key.of( name ) );
	}

	/**
	 * Constructor
	 *
	 * @param name The state name
	 */
	public InterceptorState( Key name ) {
		this.name = name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the state name
	 *
	 * @return The state name
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * Register an observer for this state
	 *
	 * @param observer The observer to register
	 *
	 * @return The same state
	 */
	public InterceptorState register( DynamicObject observer ) {
		InterceptorInvoker	invoker;
		Object				interceptor	= observer.unWrap();

		// This is a BoxLang class
		if ( interceptor instanceof IReferenceable ) {
			invoker = ( data, context, target ) -> ( ( IReferenceable ) target.unWrap() ).dereferenceAndInvoke(
			    context,
			    getName(),
			    new Object[] { data, context },
			    false
			);
		}
		// Java Interceptor Lambdas
		else if ( interceptor instanceof IInterceptorLambda ) {
			invoker = ( data, context, target ) -> ( ( IInterceptorLambda ) target.unWrap() ).intercept( data );
		}
		// Anything else is a Java class
		else {
			invoker = ( data, context, target ) -> target.invoke( context, getName().getName(), new Object[] { data } );
		}

		// Register the interceptor entry set
		this.observers.add( new InterceptorEntry( observer, invoker ) );
		return this;
	}

	/**
	 * Unregister an observer for this state
	 *
	 * @param observer The observer
	 *
	 * @return The same state
	 */
	public InterceptorState unregister( DynamicObject observer ) {
		// Iterate and remove the interceptor if found, by calling the equals method
		for ( InterceptorEntry entry : this.observers ) {
			if ( entry.equals( observer ) ) {
				this.observers.remove( entry );
				break;
			}
		}
		return this;
	}

	/**
	 * Check if an observer is registered for this state
	 *
	 * @param observer The observer to check
	 *
	 * @return True if the observer is registered, false otherwise
	 */
	public Boolean exists( DynamicObject observer ) {
		// Iterate and check if the interceptor is registered
		for ( InterceptorEntry entry : this.observers ) {
			if ( entry.equals( observer ) ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the number of observers registered for this state
	 *
	 * @return The number of observers registered for this state
	 */
	public int size() {
		return this.observers.size();
	}

	/**
	 * Process the state by announcing it to all observers
	 *
	 * @param data    The struct of data to pass to the observers
	 * @param context The box context to execute on
	 */
	public void announce( IStruct data, IBoxContext context ) {

		// Quick short ciruit
		if ( this.observers.isEmpty() ) {
			return;
		}

		// Process the state
		for ( InterceptorEntry entry : this.observers ) {
			// Run baby run!
			Object stopChain = entry.invoker.invoke( data, context, entry.interceptor );
			// If the observer returns true, we short circuit the rest of the observers
			if ( Boolean.TRUE.equals( stopChain ) ) {
				break;
			}
		}
	}

}
