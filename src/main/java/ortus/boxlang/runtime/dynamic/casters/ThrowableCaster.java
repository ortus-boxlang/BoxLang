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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.CustomException;

/**
 * I handle casting anything to a Throwable
 */
public class ThrowableCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Throwable.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Throwable
	 *
	 * @return The Throwable value
	 */
	public static CastAttempt<Throwable> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Throwable, throwing exception if we fail
	 *
	 * @param object The value to cast to a Throwable
	 *
	 * @return The Throwable value
	 */
	public static Throwable cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Throwable
	 *
	 * @param object The value to cast to a Throwable
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Throwable value
	 */
	public static Throwable cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Throwable." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		if ( object != null ) {
			if ( object instanceof Throwable t ) {
				return t;
			}

			CastAttempt<IStruct>	structObjectCast	= StructCaster.attempt( object );
			String					message				= null;
			String					detail				= null;
			String					errorcode			= null;
			String					type				= null;
			Object					extendedinfo		= null;
			Throwable				oCause				= null;
			if ( structObjectCast.wasSuccessful() ) {
				IStruct oStruct = structObjectCast.get();
				if ( oStruct.containsKey( Key.message ) && oStruct.get( Key.message ) != null ) {
					message = StringCaster.cast( oStruct.get( Key.message ) );
				} else {
					throw new BoxRuntimeException( "Cannot throw struct object as it does not contain at least a message key" );
				}
				if ( oStruct.containsKey( Key.detail ) ) {
					detail = StringCaster.cast( oStruct.get( Key.detail ), false );
				}
				if ( oStruct.containsKey( Key.errorcode ) ) {
					errorcode = StringCaster.cast( oStruct.get( Key.errorcode ), false );
				}
				if ( oStruct.containsKey( Key.type ) ) {
					type = StringCaster.cast( oStruct.get( Key.type ), false );
				}
				if ( oStruct.containsKey( Key.extendedinfo ) ) {
					extendedinfo = oStruct.get( Key.extendedinfo );
				}
				// If we don't have an extendedInfo, then store the original stack trace in the extended info
				if ( oStruct.containsKey( Key.stackTrace ) && oStruct.get( Key.stackTrace ) != null
				    && ( extendedinfo == null || extendedinfo instanceof String ) ) {
					extendedinfo	= extendedinfo == null ? "" : extendedinfo;
					extendedinfo	= ( String ) extendedinfo + ( ! ( ( String ) extendedinfo ).isBlank() ? "\n" : "" )
					    + StringCaster.cast( oStruct.get( Key.stackTrace ) );
				}
				if ( oStruct.containsKey( Key.cause ) ) {
					oCause = cast( oStruct.get( Key.cause ), false );
				}
				return new CustomException(
				    message,
				    detail,
				    errorcode,
				    type == null ? "Custom" : type,
				    extendedinfo,
				    oCause
				);
			}
		}

		// Do we throw?
		if ( fail ) {
			throw new BoxCastException( "Can't cast " + object.getClass().getName() + " to a Throwable." );
		}

		return null;
	}

}
