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
package ortus.boxlang.runtime.cache.providers;

import ortus.boxlang.runtime.cache.util.ICacheStats;
import ortus.boxlang.runtime.types.IStruct;

/**
 * A BoxLang cache provider that can talk to any cache implementation.
 */
public interface ICacheProvider {

	/**
	 * Get the stats object
	 */
	public ICacheStats getStats();

	/**
	 * Get the name of the cache provider
	 */
	public String getName();

	/**
	 * Get the cache provider type
	 */
	public String getType();

	/**
	 * Get the cache configuration structure
	 */
	public IStruct getConfig();

}
