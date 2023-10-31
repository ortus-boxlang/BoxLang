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
package ortus.boxlang.runtime.types.meta;

import ortus.boxlang.runtime.scopes.Key;

@FunctionalInterface
public interface IChangeListener {

	/**
	 * Call when a value is being changed in an IListenable
	 *
	 * @param key      The key of the value being changed. For arrays, the key will be 1-based
	 * @param newValue The new value (null if being removed)
	 * @param oldValue The old value (null if being added)
	 *
	 * @return The new value to be set (you can override)
	 */
	Object notify( Key key, Object newValue, Object oldValue );

}
