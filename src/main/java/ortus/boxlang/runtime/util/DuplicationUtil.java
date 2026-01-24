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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
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
import ortus.boxlang.runtime.util.conversion.ObjectMarshaller;

/**
 * This class is responsible for duplicating objects in the BoxLang runtime
 */
public class DuplicationUtil {

	// ThreadLocal to keep track of seen objects in the current thread
	private static final ThreadLocal<IdentityHashMap<Object, Object>> visitedObjects = ThreadLocal.withInitial( IdentityHashMap::new );

	/**
	 * Duplicate an object according to type and deep flag
	 *
	 * @param target The object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @deprecated Use {@link #duplicate(Object, Boolean, IBoxContext)} instead.
	 *
	 * @return A new object copy
	 */
	@Deprecated
	public static Object duplicate( Object target, Boolean deep ) {
		return duplicate( target, deep, RequestBoxContext.getCurrent() );
	}

	/**
	 * Duplicate an object according to type and deep flag
	 *
	 * @param target  The object to duplicate
	 * @param deep    Flag to do a deep copy on all nested objects, if true
	 * @param context The context in which the duplication is being performed
	 *
	 * @return A new object copy
	 */
	public static Object duplicate( Object target, Boolean deep, IBoxContext context ) {
		target = DynamicObject.unWrap( target );

		if ( target == null ) {
			return null;
		}

		var visited = visitedObjects.get();
		if ( visited.containsKey( target ) ) {
			return visited.get( target );
		}

		if ( ClassUtils.isPrimitiveOrWrapper( target.getClass() ) ) {
			return target;
		} else if ( target instanceof String || target instanceof Number || target instanceof Character ) {
			return target;
		} else if ( target instanceof Enum<?> || target instanceof Class<?> ) {
			return target;
		} else if ( target instanceof IClassRunnable icr ) {
			return duplicateClass( icr, deep, context );
		} else if ( target instanceof XML xml ) {
			return duplicateXML( xml );
		} else if ( target instanceof IStruct str ) {
			return duplicateStruct( str, deep, context );
		} else if ( target instanceof Array arr ) {
			return duplicateArray( arr, deep, context );
		} else if ( target instanceof Query arr ) {
			return duplicateQuery( arr, deep, context );
		} else if ( target instanceof DateTime dateTimeInstance ) {
			return dateTimeInstance.clone();
		} else if ( target instanceof Optional optionalInstance ) {
			return Optional.ofNullable( duplicate( ( ( Optional<?> ) optionalInstance ).orElse( null ), deep, context ) );
		} else if ( target instanceof Function ) {
			// functions should never be duplicated
			return target;
		} else if ( target instanceof Serializable ) {
			// Once we get here duplication is deep but very slow, but many java classes like ArrayList and all HashMaps implement this class
			// If a new type is created, add a custom routine above for duplication
			// Use this instead of generic serializers as this uses our BL class loader aware object input stream
			// TODO: I'm not sure that we want the interceptions to be announced from here.
			return ObjectMarshaller.deserialize( context, ObjectMarshaller.serialize( context, target ) );
		} else {
			throw new BoxRuntimeException(
			    String.format(
			        "Duplication was requested on the class [%s] but we don't know how to proceed",
			        target.getClass().getSimpleName()
			    )
			);
		}
	}

	private static IClassRunnable duplicateClass( IClassRunnable originalClass, Boolean deep, IBoxContext context ) {
		IClassRunnable newClass;
		try {
			newClass = originalClass.getClass().getConstructor().newInstance();
		} catch ( InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
		    | SecurityException e ) {
			throw new BoxRuntimeException( "An exception occurred while duplicating the class", e );
		}
		var visited = visitedObjects.get();
		visited.put( originalClass, newClass );
		try {

			// variables scope
			if ( deep ) {
				newClass.getVariablesScope().putAll( duplicateStruct( originalClass.getVariablesScope(), deep, context ) );
			} else {
				newClass.getVariablesScope().putAll( originalClass.getVariablesScope() );
			}

			// this scope
			if ( deep ) {
				newClass.getThisScope().putAll( duplicateStruct( originalClass.getThisScope(), deep, context ) );
			} else {
				newClass.getThisScope().putAll( originalClass.getThisScope() );
			}

			// super scope
			if ( originalClass.getSuper() != null ) {
				IClassRunnable newSuper = duplicateClass( originalClass.getSuper(), deep, context );
				newSuper.setChild( newClass );
				newClass._setSuper( newSuper );

			}

			// interfaces (these are singletons with no instance state, so nothing to really duplicate)
			newClass.getInterfaces().addAll( originalClass.getInterfaces() );
		} finally {
			visited.remove( originalClass );
		}
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
	 * @deprecated Use {@link #duplicateStruct(IStruct, Boolean, IBoxContext)} instead.
	 *
	 * @return A new Struct copy
	 */
	@Deprecated
	public static IStruct duplicateStruct( IStruct target, Boolean deep ) {
		return duplicateStruct( target, deep, RequestBoxContext.getCurrent() );
	}

	/**
	 * Duplicate a Struct object
	 *
	 * @param target The Struct object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new Struct copy
	 */
	public static IStruct duplicateStruct( IStruct target, Boolean deep, IBoxContext context ) {
		var		visited		= visitedObjects.get();
		IStruct	newStruct	= new Struct( target.getType() );

		visited.put( target, newStruct );
		try {
			var entries = target.entrySet().stream();

			if ( target.getType().equals( Struct.TYPES.LINKED ) ) {
				newStruct.addAll( entries.collect(
				    Collectors.toMap(
				        Entry::getKey,
				        entry -> {
					        Object val = entry.getValue();
					        // If it's a null value, we need to wrap it, concurrent maps don't accept nulls.
					        if ( val == null ) {
						        val = new NullValue();
					        }
					        return deep && val instanceof IStruct ? duplicateStruct( StructCaster.cast( val ), deep, context )
					            : val instanceof Array ? duplicateArray( ArrayCaster.cast( val ), deep, context ) : val;
				        },
				        ( existingValue, newValue ) -> existingValue, // Keep the existing value in case of a conflict,
				        LinkedHashMap<Key, Object>::new
				    )
				) );
			} else if ( target.getType().equals( Struct.TYPES.SORTED ) ) {
				newStruct.addAll( entries.collect(
				    Collectors.toMap(
				        Entry::getKey,
				        entry -> {
					        Object val = entry.getValue();
					        return processStructAssignment( val, deep, context );
				        },
				        ( existingValue, newValue ) -> existingValue, // Keep the existing value in case of a conflict,
				        ConcurrentSkipListMap<Key, Object>::new
				    )
				) );
			} else {
				newStruct.addAll( entries.collect(
				    Collectors.toConcurrentMap(
				        Entry::getKey,
				        entry -> processStructAssignment( entry.getValue(), deep, context ),
				        ( existingValue, newValue ) -> existingValue // Keep the existing value in case of a conflict
				    )
				)
				);
			}
			return newStruct;

		} finally {
			visited.remove( target );
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
	private static Object processStructAssignment( Object val, Boolean deep, IBoxContext context ) {
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
	 * @deprecated Use {@link #duplicateArray(Array, Boolean, IBoxContext)} instead.
	 *
	 * @return A new Array copy
	 */
	@Deprecated
	public static Array duplicateArray( Array target, Boolean deep ) {
		return duplicateArray( target, deep, RequestBoxContext.getCurrent() );
	}

	/**
	 * Duplicate an Array object
	 *
	 * @param target  The Array object to duplicate
	 * @param deep    Flag to do a deep copy on all nested objects, if true
	 * @param context The context in which the duplication is being performed
	 *
	 * @return A new Array copy
	 */
	public static Array duplicateArray( Array target, Boolean deep, IBoxContext context ) {
		var	visited		= visitedObjects.get();
		var	newArray	= new Array();
		visited.put( target, newArray );
		try {
			newArray.addAll(
			    target.intStream()
			        .mapToObj( idx -> deep ? ( Object ) duplicate( target.get( idx ), deep, context ) : ( Object ) target.get( idx ) )
			        .toList()
			);
			return newArray;
		} finally {
			visited.remove( target );
		}
	}

	/**
	 * Duplicate a Query object
	 *
	 * @param target The Query object to duplicate
	 * @param deep   Flag to do a deep copy on all nested objects, if true
	 *
	 * @return A new Query copy
	 */
	private static Object duplicateQuery( Query target, Boolean deep, IBoxContext context ) {
		Query	newQuery	= new Query();
		var		visited		= visitedObjects.get();
		visited.put( target, newQuery );
		try {
			return target.duplicate( newQuery, deep, context );
		} finally {
			visited.remove( target );
		}
	}

}
