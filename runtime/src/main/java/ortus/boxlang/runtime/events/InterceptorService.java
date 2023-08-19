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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.BoxRuntime;

/**
 * The interceptor service is responsible for managing all events in BoxLang.
 * A developer will register an interceptor with the service, and the service will
 * invoke the interceptor when the event is fired.
 */
public class InterceptorService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger			logger	= LoggerFactory.getLogger( InterceptorService.class );

	/**
	 * Singleton instance
	 */
	private static InterceptorService	instance;

	/**
	 * The list of interception states we can listen for
	 */
	private static List<String>			states	= new ArrayList<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private InterceptorService() {
		// Any initialization code can be placed here
		getInstance();
	}

	/**
	 * Get an instance of the service
	 *
	 * @return The singleton instance
	 */
	public static synchronized InterceptorService getInstance() {
		if ( instance == null ) {
			instance = new InterceptorService();
		}
		return instance;
	}

}
