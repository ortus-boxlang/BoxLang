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
package ortus.boxlang.runtime.context;

import java.util.function.Function;

import ortus.boxlang.runtime.types.IStruct;

/**
 * This context provides a way to override config for all downstream execution.
 */
public class ConfigOverrideBoxContext extends ParentPassthroughBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The variables scope
	 */
	protected Function<IStruct, IStruct> configOverride;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	public ConfigOverrideBoxContext( IBoxContext parent, Function<IStruct, IStruct> configOverride ) {
		super( parent );
		this.configOverride = configOverride;
	}

	/**
	 * Allow our overrides to happen
	 */
	public IStruct getConfig() {
		var config = super.getConfig();
		return configOverride.apply( config );
	}
}
