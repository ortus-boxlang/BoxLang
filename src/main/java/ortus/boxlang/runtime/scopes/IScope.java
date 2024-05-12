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
package ortus.boxlang.runtime.scopes;

import ortus.boxlang.runtime.types.IStruct;

/**
 * All scope implementations must implement this interface
 */
public interface IScope extends IStruct {

	/**
	 * Some scopes need initialization procedures.
	 * You can implement this for just that use case
	 */
	public default IScope initialize() {
		// Some don't require initialization
		return this;
	}

	/**
	 * Gets the name of the scope
	 *
	 * @return The name of the scope
	 */
	public Key getName();

	/**
	 * Gets the name of the lock for use in the lock component. Must be unique per
	 * scope instance.
	 *
	 * @return The unique lock name for the scope
	 */
	public String getLockName();

}
