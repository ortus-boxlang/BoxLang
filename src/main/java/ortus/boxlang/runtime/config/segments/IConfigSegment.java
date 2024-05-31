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
package ortus.boxlang.runtime.config.segments;

import ortus.boxlang.runtime.types.IStruct;

/**
 * Each configuration segment is a part of the configuration file that is
 * responsible for a specific part of the configuration. This class is the
 * interface that all configuration segments must implement.
 *
 * This is to provide a common interface for all configuration segments to
 * override it's state using the state pattern
 */
public interface IConfigSegment {

	/**
	 * Processes the state of the configuration segment from the configuration struct.
	 * <p>
	 * Each segment is processed individually from the initial configuration struct.
	 * This is so we can handle cascading overrides from configuration loading.
	 * <p>
	 *
	 * @param config The state of the segment as a struct
	 *
	 * @return Return itself for chaining
	 */
	public IConfigSegment process( IStruct config );

	/**
	 * Returns the configuration as a struct
	 *
	 * @return A struct representation of the configuration segment
	 */
	public IStruct asStruct();

}
