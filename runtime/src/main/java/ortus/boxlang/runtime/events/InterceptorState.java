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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.Struct;

/**
 * An interceptor state is an event state that is used to hold observers that want to listent
 * to that specific state. For example, the "preProcess" state is used to hold observers that
 * listen to "preProcess" events.
 *
 * The {@see InterceptorService} is in charge of managing all states and event registrations in BoxLang.
 */
public class InterceptorState {

	/**
	 * The state name (e.g. "preProcess", "postProcess", "onRuntimeStartup" etc.)
	 */
	private String				name;

	/**
	 * The observers for this state
	 */
	private List<DynamicObject>	observers	= new ArrayList<>();

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
		this.name = name;
	}

	/**
	 * Get the state name
	 *
	 * @return The state name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Register an observer for this state
	 *
	 * @param observer The observer
	 *
	 * @return The same state
	 */
	public InterceptorState register( DynamicObject observer ) {
		observers.add( observer );
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
		observers.remove( observer );
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
		return observers.contains( observer );
	}

	/**
	 * Get the number of observers registered for this state
	 * 
	 * @return The number of observers registered for this state
	 */
	public int size() {
		return observers.size();
	}

	/**
	 * Process the state by announcing it to all observers
	 *
	 * @param data The struct of data to pass to the observers
	 *
	 * @throws Throwable If an error occurs while processing the state
	 */
	public void announce( Struct data ) throws Throwable {
		for ( DynamicObject observer : observers ) {
			// Announce to the observer
			Optional<?> stopChain = observer.invoke( getName(), new Object[] { data } );
			// If the observer returns true, we short circuit the rest of the observers
			if ( stopChain.isPresent() && BooleanCaster.cast( stopChain.get() ) ) {
				break;
			}
		}
	}

}
