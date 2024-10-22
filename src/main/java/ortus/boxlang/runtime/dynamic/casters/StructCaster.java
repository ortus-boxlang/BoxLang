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

import java.util.Map;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.StructMapWrapper;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

/**
 * I handle casting anything to a Struct
 */
@SuppressWarnings( "unchecked" )
public class StructCaster implements IBoxCaster {

	/**
	 * Tests to see if the value can be cast to a Struct.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a Struct
	 *
	 * @return The Struct value
	 */
	public static CastAttempt<IStruct> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a Struct, throwing exception if we fail
	 *
	 * @param object The value to cast to a Struct
	 *
	 * @return The Struct value
	 */
	public static IStruct cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a Struct
	 *
	 * @param object The value to cast to a Struct
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The Struct value
	 */
	public static IStruct cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new BoxCastException( "Can't cast null to a Struct." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof Throwable t ) {
			return ExceptionUtil.throwableToStruct( t );
		}

		if ( object instanceof ArgumentsScope args ) {
			return args;
		}

		if ( object instanceof IStruct col ) {
			return col;
		}

		if ( object instanceof Map<?, ?> map ) {
			return StructMapWrapper.of( ( Map<Object, Object> ) map );
		}

		if ( fail ) {
			throw new BoxCastException(
			    String.format( "Can't cast [%s] to a Struct.", object.getClass().getName() )
			);
		} else {
			return null;
		}
	}

}
