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
import java.util.Map;
import java.util.Observer;
import java.util.Optional;

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.BoxResolver;

/**
 * An interceptor state is a state that is used to intercept a method call, and
 * allow observers to be notified of the method call. The observers can then
 * modify the data, or even short circuit the method call.
 */
public class InterceptorState {

	/**
	 * The state name (e.g. "preProcess", "postProcess", "onRuntimeStartup" etc.)
	 */
	private String					name;

	/**
	 * The observers for this state
	 */
	private List<DynamicObject>		observers	= new ArrayList<>();

	/**
	 * Singleton instance
	 */
	private static InterceptorState	instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	private InterceptorState( String name ) {
		this.name = name;
	}

	/**
	 * Singleton instance
	 *
	 * @return The instance
	 */
	public static synchronized InterceptorState getInstance( String name ) {
		if ( instance == null ) {
			instance = new InterceptorState( name );
		}

		return instance;
	}

	public String getName() {
		return name;
	}

	public void addObserver( DynamicObject observer ) {
		observers.add( observer );
	}

	public void removeObserver( DynamicObject observer ) {
		observers.remove( observer );
	}

	public void notifyObservers( Map<?, ?> data ) throws Throwable {
		for ( DynamicObject observer : observers ) {
			// Announce to the observer
			Optional<?> shortCircuit = observer.invoke( getName(), new Object[] { data } );
			// If the observer returns true, we short circuit the rest of the observers
			if ( shortCircuit.isPresent() && BooleanCaster.cast( shortCircuit.get() ) ) {
				break;
			}
		}
	}

}
