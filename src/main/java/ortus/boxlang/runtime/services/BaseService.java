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
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.util.Timer;

/**
 * A base service class that all services should extend.
 */
public abstract class BaseService {

	/**
	 * The timer utility class
	 */
	private static final Timer	timerUtil	= new Timer();

	/**
	 * The runtime singleton link
	 */
	protected BoxRuntime		runtime;

	/**
	 * Runtime Service Constructor
	 *
	 * @param runtime The runtime singleton
	 */
	protected BaseService( BoxRuntime runtime ) {
		this.runtime = runtime;
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
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	public void onStartup() {
		throw new ApplicationException( "onStartup() not implemented" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	public void onConfigurationLoad() {
		throw new ApplicationException( "onConfigurationLoad() not implemented" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	public void onShutdown() {
		throw new ApplicationException( "onShutdown() not implemented" );
	}

}
