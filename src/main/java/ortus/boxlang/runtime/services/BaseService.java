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
package ortus.boxlang.runtime.services;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.Timer;

/**
 * A base service class that all services should extend.
 */
public abstract class BaseService implements IService {

	/**
	 * The timer utility class
	 */
	protected static final Timer	timerUtil	= new Timer();

	/**
	 * The runtime singleton link
	 */
	protected BoxRuntime			runtime;

	/**
	 * The service name
	 */
	protected Key					name;

	/**
	 * Runtime Service Constructor
	 *
	 * @param runtime The runtime singleton
	 */
	protected BaseService( BoxRuntime runtime, Key name ) {
		this.runtime	= runtime;
		this.name		= name;
	}

	/**
	 * Get the timer utility class
	 *
	 * @return The timer utility class
	 */
	public Timer getTimerUtil() {
		return timerUtil;
	}

	/**
	 * Get the runtime singleton
	 *
	 * @return The runtime
	 */
	public BoxRuntime getRuntime() {
		return runtime;
	}

	/**
	 * Get the service name
	 *
	 * @return The service name
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The configuration load event is fired when the runtime loads the configuration
	 */
	public abstract void onConfigurationLoad();

	/**
	 * The startup event is fired when the runtime starts up
	 */
	public abstract void onStartup();

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether the shutdown is forced
	 */
	public abstract void onShutdown( Boolean force );

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( Key state, IStruct data ) {
		runtime.getInterceptorService().announce( state, data );
	}

	/**
	 * Announce an event with the provided {@link IStruct} of data.
	 *
	 * @param state The state key to announce
	 * @param data  The data to announce
	 */
	public void announce( BoxEvent state, IStruct data ) {
		runtime.getInterceptorService().announce( state.key(), data );
	}

	/**
	 * Announce an event with no data.
	 *
	 * @param state The state key to announce
	 */
	public void announce( Key state ) {
		announce( state, new Struct() );
	}

	/**
	 * Announce an event with no data.
	 *
	 * @param state The state key to announce
	 */
	public void announce( BoxEvent state ) {
		announce( state.key(), new Struct() );
	}

}
