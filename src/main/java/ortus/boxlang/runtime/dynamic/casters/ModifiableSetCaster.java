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
package ortus.boxlang.runtime.dynamic.casters;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableSet;

/**
 * Casts to a modifiable {@link BoxSet}, rejecting {@link UnmodifiableSet} up front so a
 * mutating BIF can fail fast with a clear error instead of throwing deep in the call stack.
 */
public class ModifiableSetCaster implements IBoxCaster {

	public static CastAttempt<BoxSet> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	public static BoxSet cast( Object object ) {
		return cast( object, true );
	}

	public static BoxSet cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Set." );
			}
			return null;
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof UnmodifiableSet ) {
			throw new BoxCastException( "Can't cast UnmodifiableSet to a modifiable Set." );
		}

		return SetCaster.cast( object, fail );
	}

}
