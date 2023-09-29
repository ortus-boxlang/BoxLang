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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

/**
 * I handle casting anything to a collection
 */
public class CollectionCaster {

	/**
	 * Tests to see if the value can be cast to a collection.
	 * Returns a {@code CastAttempt<T>} which will contain the result if casting was
	 * was successfull, or can be interogated to proceed otherwise.
	 *
	 * @param object The value to cast to a collection
	 *
	 * @return The collection value
	 */
	public static CastAttempt<Collection<Object>> attempt( Object object ) {
		return CastAttempt.ofNullable( cast( object, false ) );
	}

	/**
	 * Used to cast anything to a collection, throwing exception if we fail
	 *
	 * @param object The value to cast to a collection
	 *
	 * @return The collection value
	 */
	public static Collection<Object> cast( Object object ) {
		return cast( object, true );
	}

	/**
	 * Used to cast anything to a collection
	 *
	 * @param object The value to cast to a collection
	 * @param fail   True to throw exception when failing.
	 *
	 * @return The collection value
	 */
	@SuppressWarnings( "unchecked" )
	public static Collection<Object> cast( Object object, Boolean fail ) {
		if ( object == null ) {
			if ( fail ) {
				throw new ApplicationException( "Can't cast null to a Collection." );
			} else {
				return null;
			}
		}
		object = DynamicObject.unWrap( object );

		if ( object instanceof IScope ) {
			IScope scope = ( IScope ) object;
			return scope.keySet()
			    .stream()
			    .map( k -> k.getName() )
			    .collect( Collectors.toList() );
		}

		if ( object instanceof Map ) {
			return ( ( Map<Object, Object> ) object ).keySet();
		}

		if ( object instanceof Collection ) {
			return ( Collection<Object> ) object;
		}

		if ( object.getClass().isArray() ) {
			Object[] array = ( Object[] ) object;
			return Arrays.asList( array );
		}

		if ( fail ) {
			throw new ApplicationException(
			    String.format( "Can't cast [%s] to a Collection.", object.getClass().getName() )
			);
		} else {
			return null;
		}
	}

}
