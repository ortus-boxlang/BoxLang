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

import java.io.Serializable;

import ortus.boxlang.runtime.types.immutable.IImmutable;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

/**
 * I represent a null value, so we can store it in a ConcurrentHashMap
 */
public class NullValue implements IType, IImmutable, Serializable {

	/**
	 * Serializable
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * Metadata object
	 */
	public BoxMeta				$bx;

	/**
	 * Constructor
	 */
	public NullValue() {
		// we are empty as we represent null
	}

	/**
	 * Represent as string
	 *
	 * @return The string representation
	 */
	public String asString() {
		return "[null]";
	}

	/**
	 * The metadata object
	 *
	 * @return The metadata object
	 */
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

	/**
	 * We can't convert `null` back to mutable. So it remains as is.
	 *
	 * @return The mutable type
	 */
	public IType toMutable() {
		return this;
	}

}
