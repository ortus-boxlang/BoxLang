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
package ortus.boxlang.runtime.util;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.QueryCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.*;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.util.BLCollector;

public class DuplicationUtil {

	public static Object duplicate( Object target, Boolean deep ) {
		if ( target == null ) {
			return null;
		} else if ( ClassUtils.isPrimitiveOrWrapper( target.getClass() ) ) {
			return target;
		} else if ( target instanceof IStruct ) {
			return duplicateStruct( StructCaster.cast( target ), deep );
		} else if ( target instanceof Array ) {
			return duplicateArray( ArrayCaster.cast( target ), deep );
		} else if ( target instanceof Query ) {
			return duplicateQuery( QueryCaster.cast( target ), deep );
		} else if ( target instanceof DateTime dateTimeInstance ) {
			return dateTimeInstance.clone();
		} else if ( target instanceof Function ) {
			// functions should never be duplicated
			return target;
		} else if ( target instanceof Throwable t ) {
			return ExceptionUtil.throwableToStruct( t );
		} else if ( target instanceof Serializable ) {
			// Once we get here duplication is deep but very slow, but many java classes like ArrayList and all HashMaps implement this class
			// If a new type is created, add a custom routine above for duplication
			return SerializationUtils.clone( ( Serializable ) target );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "Duplication was requested on the class [%s] but we don't know how to proceed",
			        target.getClass().getSimpleName()
			    )
			);
		}
	}

	public static Struct duplicateStruct( IStruct target, Boolean deep ) {
		var entries = target.entrySet().stream();

		if ( target.getType().equals( Struct.TYPES.LINKED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            entry -> entry.getKey(),
			            entry -> {
				            Object val = entry.getValue();
				            return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep )
				                : val instanceof Array ? duplicateArray( ArrayCaster.cast( val ), deep ) : val;
			            },
			            ( v1, v2 ) -> {
				            throw new BoxRuntimeException( "An exception occurred while duplicating the linked HashMap" );
			            },
			            LinkedHashMap<Key, Object>::new
			        )
			    )
			);
		} else if ( target.getType().equals( Struct.TYPES.SORTED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            entry -> entry.getKey(),
			            entry -> {
				            Object val = entry.getValue();
				            return processAssignment( val, deep );
			            },
			            ( v1, v2 ) -> {
				            throw new BoxRuntimeException( "An exception occurred while duplicating the linked HashMap" );
			            },
			            ConcurrentSkipListMap<Key, Object>::new
			        )
			    )
			);
		} else {
			return new Struct(
			    target.getType(),
			    entries.collect( Collectors.toConcurrentMap( entry -> entry.getKey(), entry -> {
				    Object val = entry.getValue();
				    return processAssignment( val, deep );
			    } ) )
			);
		}
	}

	public static Object processAssignment( Object val, Boolean deep ) {
		return deep && val instanceof IStruct
		    ? duplicateStruct( StructCaster.cast( val ), deep )
		    : deep && val instanceof Array
		        ? duplicateArray( ArrayCaster.cast( val ), deep )
		        : deep && val instanceof Function
		            ? val
		            : deep && val instanceof Serializable
		                ? SerializationUtils.clone( ( Serializable ) val )
		                : val;
	}

	public static Array duplicateArray( Array target, Boolean deep ) {
		return new Array(
		    target.intStream()
		        .mapToObj( idx -> deep ? ( Object ) duplicate( target.get( idx ), deep ) : ( Object ) target.get( idx ) )
		        .toArray()
		);
	}

	/**
	 * Duplicate a Query object
	 *
	 * @param target The Query object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new Query copy
	 */
	private static Object duplicateQuery( Query target, Boolean deep ) {
		return target.duplicate( deep );
	}

}
