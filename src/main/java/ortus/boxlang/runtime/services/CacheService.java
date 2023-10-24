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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class CacheService extends BaseService {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Logger
	 */
	private static final Logger	logger	= LoggerFactory.getLogger( CacheService.class );

	/**
	 * Singleton instance
	 */
	private static CacheService	instance;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	private CacheService() {
		logger.info( "CacheService.onStartup()" );
	}

	/**
	 * Get an instance of the service
	 *
	 * @return The singleton instance
	 */
	public static synchronized CacheService getInstance() {
		if ( instance == null ) {
			instance = new CacheService();
		}
		return instance;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Runtime Service Event Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The startup event is fired when the runtime starts up
	 */
	@Override
	public void onStartup() {
		logger.info( "CacheService.onStartup()" );
	}

	/**
	 * The configuration load event is fired when the runtime loads its configuration
	 */
	@Override
	public void onConfigurationLoad() {
		logger.info( "CacheService.onConfigurationLoad()" );
	}

	/**
	 * The shutdown event is fired when the runtime shuts down
	 */
	@Override
	public void onShutdown() {
		logger.info( "CacheService.onShutdown()" );
	}

}
