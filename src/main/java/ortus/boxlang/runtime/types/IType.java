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
package ortus.boxlang.runtime.types;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import ortus.boxlang.runtime.types.meta.BoxMeta;

/**
 * Represents a base type
 *
 * Type Hierarchy
 * - Struct
 * - sorted, ordered, etc
 * - Array
 * - XML
 * - Query
 * - Simple
 * - String
 * - Numeric
 * - Boolean
 * - List
 * - Date
 */
public interface IType {

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	public String asString();

	static Set<IType> createIdentitySetForType() {
		return Collections.newSetFromMap( new IdentityHashMap<>() );
	}

	default int computeHashCode( Set<IType> visited ) {
		return this.hashCode();
	}

	public BoxMeta getBoxMeta();
}
