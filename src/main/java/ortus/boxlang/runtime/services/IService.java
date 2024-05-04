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

import ortus.boxlang.runtime.scopes.Key;

/**
 * The BoxLang service interface.
 * If you implement this interface, your class will be treated as a BoxLang service.
 * This means that it will be automatically registered with the BoxLang runtime and
 * will be available for use in BoxLang code.
 * <p>
 * Every module is scaned for services and registered in the {@link ortus.boxlang.runtime.BoxRuntime}
 */
public interface IService {

	/**
	 * Get the unique service name
	 */
	public Key getName();

	/**
	 * The startup event is fired when the runtime starts up
	 */
	public void onStartup();

	/**
	 * The shutdown event is fired when the runtime shuts down
	 *
	 * @param force Whether the shutdown is forced
	 */
	public void onShutdown( Boolean force );

}
