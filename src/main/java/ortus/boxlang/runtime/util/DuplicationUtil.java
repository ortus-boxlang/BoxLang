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
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.SerializationUtils;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This class is responsible for duplicating objects in the BoxLang runtime
 */
public class DuplicationUtil {

	/**
	 * Duplicate an object according to type and deep flag
	 *
	 * @param target The object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new object copy
	 */
	public static Object duplicate( Object target, Boolean deep ) {
		target = DynamicObject.unWrap( target );
		if ( target == null ) {
			return null;
		} else if ( ClassUtils.isPrimitiveOrWrapper( target.getClass() ) ) {
			return target;
		} else if ( target instanceof String || target instanceof Number || target instanceof Character ) {
			return target;
		} else if ( target instanceof Enum<?> || target instanceof Class<?> ) {
			return target;
		} else if ( target instanceof IClassRunnable icr ) {
			return duplicateClass( icr, deep );
		} else if ( target instanceof XML xml ) {
			return duplicateXML( xml );
		} else if ( target instanceof IStruct str ) {
			return duplicateStruct( str, deep );
		} else if ( target instanceof Array arr ) {
			return duplicateArray( arr, deep );
		} else if ( target instanceof Query arr ) {
			return duplicateQuery( arr, deep );
		} else if ( target instanceof DateTime dateTimeInstance ) {
			return dateTimeInstance.clone();
		} else if ( target instanceof Function ) {
			// functions should never be duplicated
			return target;
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

	private static IClassRunnable duplicateClass( IClassRunnable originalClass, Boolean deep ) {
		IClassRunnable newClass;
		try {
			newClass = originalClass.getClass().getConstructor().newInstance();
		} catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
		    | SecurityException e ) {
			throw new BoxRuntimeException( "An exception occurred while duplicating the class", e );
		}

		// variables scope
		if ( deep ) {
			newClass.getVariablesScope().putAll( duplicateStruct( originalClass.getVariablesScope(), deep ) );
		} else {
			newClass.getVariablesScope().putAll( originalClass.getVariablesScope() );
		}

		// this scope
		if ( deep ) {
			newClass.getThisScope().putAll( duplicateStruct( originalClass.getThisScope(), deep ) );
		} else {
			newClass.getThisScope().putAll( originalClass.getThisScope() );
		}

		// super scope
		if ( originalClass.getSuper() != null ) {
			IClassRunnable newSuper = duplicateClass( originalClass.getSuper(), deep );
			newSuper.setChild( newClass );
			newClass._setSuper( newSuper );

		}

		// interfaces (these are singletons with no instance state, so nothing to really duplicate)
		newClass.getInterfaces().addAll( originalClass.getInterfaces() );

		return newClass;
	}

	private static XML duplicateXML( XML target ) {
		return target.clone();
	}

	/**
	 * Duplicate a Struct object
	 *
	 * @param target The Struct object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new Struct copy
	 */
	public static IStruct duplicateStruct( IStruct target, Boolean deep ) {
		var entries = target.entrySet().stream();

		if ( target.getType().equals( Struct.TYPES.LINKED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            Entry::getKey,
			            entry -> {
				            Object val = entry.getValue();
				            // If it's a null value, we need to wrap it, concurrent maps don't accept nulls.
				            if ( val == null ) {
					            val = new NullValue();
				            }
				            return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep )
				                : val instanceof Array ? duplicateArray( ArrayCaster.cast( val ), deep ) : val;
			            },
			            ( existingValue, newValue ) -> existingValue, // Keep the existing value in case of a conflict,
			            LinkedHashMap<Key, Object>::new
			        )
			    )
			);
		} else if ( target.getType().equals( Struct.TYPES.SORTED ) ) {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toMap(
			            Entry::getKey,
			            entry -> {
				            Object val = entry.getValue();
				            return processStructAssignment( val, deep );
			            },
			            ( existingValue, newValue ) -> existingValue, // Keep the existing value in case of a conflict,
			            ConcurrentSkipListMap<Key, Object>::new
			        )
			    )
			);
		} else {
			return new Struct(
			    target.getType(),
			    entries.collect(
			        Collectors.toConcurrentMap(
			            Entry::getKey,
			            entry -> processStructAssignment( entry.getValue(), deep ),
			            ( existingValue, newValue ) -> existingValue // Keep the existing value in case of a conflict
			        )
			    )
			);
		}
	}

	/**
	 * Process a struct assignment for duplication
	 *
	 * @param val  The value to duplicate
	 * @param deep Flag to do a deep copy on all nested objects, if true
	 *
	 * @return The duplicated value
	 */
	public static Object processStructAssignment( Object val, Boolean deep ) {
		// If it's a null value, we need to wrap it, concurrent maps don't accept nulls.
		if ( val == null ) {
			return new NullValue();
		}
		if ( !deep ) {
			return val;
		}
		return duplicate( val, deep );
	}

	/**
	 * Duplicate an Array object
	 *
	 * @param target The Array object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new Array copy
	 */
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
