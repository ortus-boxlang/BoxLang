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
import ortus.boxlang.runtime.types.BoxStringBuilder;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * Caster for the {@link BoxStringBuilder} type.
 * Strict casting — only accepts BoxStringBuilder instances.
 */
public class StringBuilderCasterStrict implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a {@link BoxStringBuilder}.
	 *
	 * @param object The value to test
	 *
	 * @return A CastAttempt containing the BoxStringBuilder if successful
	 */
	public static CastAttempt<BoxStringBuilder> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Strict cast — throws if the value is not a BoxStringBuilder.
	 *
	 * @param object The value to cast
	 *
	 * @return The BoxStringBuilder
	 */
	public static BoxStringBuilder cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Strict cast to a BoxStringBuilder (does not wrap raw Java StringBuilder).
	 *
	 * @param object The value to cast
	 * @param fail   True to throw on failure, false to return null
	 *
	 * @return The BoxStringBuilder, or null if fail is false and the cast fails
	 */
	public static BoxStringBuilder cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a StringBuilder." );
			}
			return null;
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof BoxStringBuilder bsb ) {
			return bsb;
		}

		if ( fail ) {
			throw new BoxCastException( String.format( "Can't cast [%s] to a StringBuilder.", TypeUtil.getObjectName( object ) ) );
		}
		return null;
	}

}
