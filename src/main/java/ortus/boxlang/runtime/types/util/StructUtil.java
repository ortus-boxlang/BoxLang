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
package ortus.boxlang.runtime.types.util;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.operators.StringCompare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.EncryptionUtil;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class StructUtil {

	public static final Key scopeAll = Key.of( "all" );

	/**
	 * Method to invoke a function for every item in a struct
	 *
	 * @param struct          The struct to iterate
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 * @param ordered         Boolean as to whether to maintain order in parallel execution
	 */
	public static void each(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads,
	    Boolean ordered ) {

		Stream<Map.Entry<Key, Object>>		entryStream	= struct.entrySet().stream();

		Consumer<Map.Entry<Key, Object>>	exec;
		if ( callback.requiresStrictArguments() ) {
			exec = item -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			);
		} else {
			exec = item -> callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			);
		}

		if ( !parallel ) {
			entryStream.forEach( exec );
		} else if ( ordered ) {
			AsyncService.buildExecutor(
			    "StructEach_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEachOrdered( exec ) );
		} else {
			AsyncService.buildExecutor(
			    "StructEach_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEach( exec ) );
		}

	}

	/**
	 * Method to test if any item in the struct meets the criteria in the callback
	 *
	 * @param struct          The struct to test
	 * @param callback        The callback test to apply
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean some(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		Stream<Map.Entry<Key, Object>>		entryStream	= struct.entrySet().stream();
		Predicate<Map.Entry<Key, Object>>	test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		return !parallel
		    ? ( Boolean ) entryStream.anyMatch( test )
		    : ( Boolean ) AsyncService.buildExecutor(
		        "structSome_" + UUID.randomUUID().toString(),
		        AsyncService.ExecutorType.FORK_JOIN,
		        maxThreads
		    ).submitAndGet( () -> entryStream.parallel().anyMatch( test ) );

	}

	/**
	 * Method to test if any item in the struct meets the criteria in the callback
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return The boolean value as to whether the test is met
	 */
	public static Boolean every(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		Stream<Map.Entry<Key, Object>>		entryStream	= struct.entrySet().stream();
		Predicate<Map.Entry<Key, Object>>	test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		return !parallel
		    ? entryStream.dropWhile( test ).toArray().length == 0
		    : BooleanCaster.cast(
		        AsyncService.buildExecutor(
		            "ArrayEvery_" + UUID.randomUUID().toString(),
		            AsyncService.ExecutorType.FORK_JOIN,
		            maxThreads
		        ).submitAndGet( () -> entryStream.parallel().dropWhile( test ).toArray().length == 0 )
		    );

	}

	/**
	 * Method to filter a struct with a function callback and context
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	@SuppressWarnings( "unchecked" )
	public static IStruct filter(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		Stream<Map.Entry<Key, Object>>		entryStream		= struct.entrySet().stream();
		Stream<Map.Entry<Key, Object>>		filteredStream	= null;
		Predicate<Map.Entry<Key, Object>>	test;
		if ( callback.requiresStrictArguments() ) {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue() }
			) );
		} else {
			test = item -> BooleanCaster.cast( callbackContext.invokeFunction(
			    callback,
			    new Object[] { item.getKey().getName(), item.getValue(), struct }
			) );
		}

		if ( !parallel ) {
			filteredStream = entryStream.filter( test );
		} else {
			filteredStream = ( Stream<Map.Entry<Key, Object>> ) AsyncService.buildExecutor(
			    "StructFilter_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().filter( test ) );
		}

		return filteredStream.collect( BLCollector.toStruct( struct.getType() ) );

	}

	/**
	 * Method to map a struct to a new struct
	 *
	 * @param struct          The struct object to filter
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param parallel        Whether to process the filter in parallel
	 * @param maxThreads      Optional max threads for parallel execution
	 *
	 * @return A filtered array
	 */
	public static Struct map(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		Stream<Map.Entry<Key, Object>>		entryStream	= struct.entrySet().stream();
		Struct								result		= new Struct( struct.getType() );

		Consumer<Map.Entry<Key, Object>>	exec;
		if ( callback.requiresStrictArguments() ) {
			exec = item -> result.put(
			    item.getKey(),
			    callbackContext.invokeFunction(
			        callback,
			        new Object[] { item.getKey().getName(), item.getValue() }
			    )
			);
		} else {
			exec = item -> result.put(
			    item.getKey(),
			    callbackContext.invokeFunction(
			        callback,
			        new Object[] { item.getKey().getName(), item.getValue(), struct }
			    )
			);
		}
		if ( !parallel ) {
			entryStream.forEach( exec );
		} else if ( struct.getType().equals( IStruct.TYPES.LINKED ) ) {
			AsyncService.buildExecutor(
			    "StructMap_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEachOrdered( exec ) );
		} else {
			AsyncService.buildExecutor(
			    "StructMap_" + UUID.randomUUID().toString(),
			    AsyncService.ExecutorType.FORK_JOIN,
			    maxThreads
			).submitAndGet( () -> entryStream.parallel().forEach( exec ) );
		}
		return result;

	}

	/**
	 * Method to reduce a struct to an accumulated object
	 *
	 * @param struct          The struct object to reduce
	 * @param callback        The callback Function object
	 * @param callbackContext The context in which to execute the callback
	 * @param initialValue    The initial value of the accumulation
	 *
	 * @return the new object reduction
	 */
	public static Object reduce(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext,
	    Object initialValue ) {
		BiFunction<Object, Map.Entry<Key, Object>, Object> reduction;
		if ( callback.requiresStrictArguments() ) {
			reduction = ( acc, item ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, item.getKey().getName(), item.getValue() } );
		} else {
			reduction = ( acc, item ) -> callbackContext.invokeFunction( callback,
			    new Object[] { acc, item.getKey().getName(), item.getValue(), struct } );
		}

		return struct.entrySet().stream()
		    .reduce(
		        initialValue,
		        reduction,
		        ( acc, intermediate ) -> acc
		    );

	}

	/**
	 * Performs a stort of a struct and returns the top-level keys in the sorted order
	 *
	 * @param struct    the struct to sort
	 * @param sortType  the textual sort directive ( numeric, text, textNoCase )
	 * @param sortOrder the sort order (asc, desc)
	 * @param path      An optional nested string path to perform the sort on
	 *
	 * @return an array containing the sorted keys
	 */
	public static Array sort(
	    IStruct struct,
	    String sortType,
	    String sortOrder,
	    String path ) {
		if ( path == null ) {
			Key typeKey = Key.of( sortType + sortOrder );
			if ( !getCommonComparators().containsKey( typeKey ) ) {
				throw new BoxRuntimeException(
				    String.format(
				        "The sort directive [%s,%s] is not a valid struct sorting directive",
				        sortType,
				        sortOrder
				    )
				);
			}
			return new Array(
			    struct.keySet()
			        .stream()
			        .sorted( getCommonComparators().get( typeKey ) )
			        .map( k -> k.getName() )
			        .toArray()
			);
		} else {
			Boolean isDescending = Key.of( sortOrder ).equals( Key.of( "desc" ) );
			return new Array( struct.entrySet().stream().sorted(
			    ( a, b ) -> Compare.invoke(
			        StructUtil.getAtPath( StructCaster.cast( isDescending ? b.getValue() : a.getValue() ), path ),
			        StructUtil.getAtPath( StructCaster.cast( isDescending ? a.getValue() : b.getValue() ), path ),
			        sortType.toLowerCase().contains( "nocase" ) ? false : true
			    )
			).map( e -> e.getKey().getName() ).toArray()
			);
		}

	}

	/**
	 * Performs a stort of a struct with a callback funciton and returns the ordered struct keys
	 *
	 * @param struct          the struct to sort
	 * @param callback        the callback sort function which is passed the arguments (k1,k2) for comparison
	 * @param callbackContext the IBoxContext to execute the callback within
	 *
	 * @return an array containing the sorted keys
	 */
	public static Array sort(
	    IStruct struct,
	    Function callback,
	    IBoxContext callbackContext ) {

		return new Array(
		    struct.keySet()
		        .stream()
		        .map( k -> k.getName() )
		        .sorted(
		            ( a, b ) -> IntegerCaster.cast( callbackContext.invokeFunction(
		                callback,
		                new Object[] { a, b }
		            ) )
		        )
		        .toArray()
		);
	}

	/**
	 * Retreives the final value of a nested string path within a struct
	 *
	 * @param struct the struct to search within
	 * @param path   the string path representation ( e.g. foo.bar.baz )
	 *
	 * @return The found object or null
	 */
	public static Object getAtPath(
	    IStruct struct,
	    String path ) {
		String[]	parts	= path.split( "\\." );
		Object		ref		= null;
		Key			refName	= Key.of( parts[ 0 ] );
		if ( struct.containsKey( refName ) ) {
			ref = struct.get( refName );
			for ( int i = 1; i < parts.length - 1; i++ ) {
				ref = StructCaster.cast( ref ).get( Key.of( parts[ i ] ) );
				if ( ref == null )
					break;
			}
			return ref;
		} else {
			return null;
		}
	}

	/**
	 * Returns an array of keys
	 *
	 * @param struct
	 * @param key
	 *
	 * @return
	 */
	public static Stream<IStruct> findKey( IStruct struct, String key ) {
		int		keyLength	= key.length();
		IStruct	flatMap		= toFlatMap( struct );
		return flatMap.entrySet()
		    .stream()
		    .filter( entry -> {
			    String stringKey = entry.getKey().getName().toLowerCase();
			    // We look for the key at the end of the string ( nested ) or at the beginning of the string for top-level keys
			    return StringUtils.right( stringKey, keyLength ).equals( key.toLowerCase() )
			        || StringUtils.left( stringKey, keyLength ).equals( key.toLowerCase() );
		    } )
		    .map( entry -> {
			    Struct	returnStruct	= new Struct( Struct.TYPES.LINKED );
			    String	keyName			= entry.getKey().getName();
			    String[] keyParts		= entry.getKey().getName().split( "\\." );
			    String	flatMapParent	= keyName.substring( 0, keyName.lastIndexOf( "." ) );
			    returnStruct.put(
			        Key.owner,
			        keyParts.length > 1
			            ? unFlattenKeys( flatMap, true, false ).get( Key.of( flatMapParent ) )
			            : struct
			    );
			    returnStruct.put(
			        Key.path,
			        "." + keyName
			    );
			    returnStruct.put(
			        Key.value,
			        entry.getValue()
			    );
			    return returnStruct;
		    } );

	}

	public static Stream<IStruct> findValue( IStruct struct, Object value ) {
		IStruct flatMap = toFlatMap( struct );
		return flatMap.entrySet()
		    .stream()
		    .filter( entry -> Compare.invoke( value, entry.getValue() ) == 0 )
		    .map( entry -> {
			    Struct	returnStruct	= new Struct( Struct.TYPES.LINKED );
			    String	keyName			= entry.getKey().getName();
			    String[] keyParts		= entry.getKey().getName().split( "\\." );
			    String	parentName		= keyName;
			    if ( keyParts.length > 1 ) {
				    parentName = keyName.substring( 0, keyName.lastIndexOf( "." ) );
			    }
			    final String finalParent = parentName;
			    returnStruct.put(
			        Key.owner,
			        keyParts.length > 1
			            ? unFlattenKeys(
			                flatMap.entrySet().stream()
			                    .filter( mapEntry -> mapEntry.getKey().getName().contains( finalParent )
			                    ).map(
			                        mapEntry -> new AbstractMap.SimpleEntry<Key, Object>(
			                            Key.of( mapEntry.getKey().getName().replace( finalParent + ".", "" ) ), mapEntry.getValue() )
			                    )
			                    .collect( BLCollector.toStruct() ),
			                true,
			                false
			            )
			            : struct
			    );
			    // TODO: This dot prefix is silly given the context this function operates in. Deprecate the dot prefix in a future release.
			    returnStruct.put(
			        Key.path,
			        "." + keyName
			    );
			    returnStruct.put(
			        Key.key,
			        keyParts[ keyParts.length - 1 ]
			    );
			    return returnStruct;
		    } );

	}

	/**
	 * Performs a deep merge on two structs and will only add top level and deep values not present in the recipient
	 *
	 * @param recipient The struct to merge into
	 * @param merge     The struct to merge from
	 *
	 * @return the recipient struct merged
	 */
	public static IStruct deepMerge( IStruct recipient, IStruct merge ) {
		return deepMerge( recipient, merge, false );
	}

	/**
	 * Performs a deep merge on two structs. If override is set to true, it will override all keys in the recipeient with keys from the merge struct.
	 * If set to false, it will only add top level and deep values not present in the recipient
	 *
	 * @param recipient The struct to merge into
	 * @param merge     The struct to merge from
	 * @param override  Whether to override the recipient keys with the merge keys
	 *
	 * @return the recipient struct merged
	 */
	public static IStruct deepMerge( IStruct recipient, IStruct merge, boolean override ) {
		merge.entrySet().forEach(
		    entry -> {
			    if ( entry.getValue() instanceof IStruct mergeStruct && recipient.get( entry.getKey() ) instanceof IStruct recipStruct ) {
				    StructUtil.deepMerge( recipStruct, mergeStruct, override );
			    } else if ( entry.getValue() instanceof Array merageArray && recipient.get( entry.getKey() ) instanceof Array recipArray ) {
				    merageArray.stream().forEach( item -> {
					    if ( !recipArray.contains( entry.getValue() ) ) {
						    recipArray.add( entry.getValue() );
					    }
				    } );
			    } else {
				    if ( override ) {
					    recipient.put( entry.getKey(), entry.getValue() );
				    } else {
					    recipient.putIfAbsent( entry.getKey(), entry.getValue() );
				    }
			    }
		    }
		);
		return recipient;
	}

	/**
	 * Flattens a struct in to a struct containing dot-delmited keys for nested structs
	 *
	 * @param struct
	 *
	 * @return a flattened map of the struct
	 */
	public static IStruct toFlatMap( IStruct struct ) {
		return new Struct(
		    struct.getType(),
		    struct.entrySet().stream()
		        .filter( entry -> entry.getValue() != null )
		        .flatMap( StructUtil::flattenEntry )
		        .collect( LinkedHashMap<Key, Object>::new, ( m, entry ) -> m.put( entry.getKey(), entry.getValue() ), LinkedHashMap::putAll )
		);
	}

	/**
	 * Method to recursively flatten an entry set containing structs
	 *
	 * @param entry the individual entry set from the stream
	 *
	 * @return the stream object for further operations
	 */
	public static Stream<Map.Entry<Key, Object>> flattenEntry( Map.Entry<Key, Object> entry ) {

		java.util.function.Function<Map.Entry<Key, Object>, AbstractMap.SimpleEntry<Key, Object>> flattener = e -> new AbstractMap.SimpleEntry<Key, Object>(
		    Key.of(
		        entry.getKey().getName() + "." + e.getKey().getName()
		    ),
		    e.getValue()
		);

		if ( entry.getValue() instanceof Map ) {
			IStruct nested = StructCaster.cast( entry.getValue() );
			return nested.entrySet().stream()
			    .map( flattener )
			    .flatMap( StructUtil::flattenEntry );
		}
		return Stream.of( entry );
	}

	/**
	 * Translates a struct with dot-delimited keys in to nested struct
	 *
	 * @param struct     The struct to de-flatten
	 * @param deep       Whether to recurse in to nested keys to unflatten
	 * @param retainKeys Whether to retain the original keys
	 *
	 * @return a flattened map of the struct
	 */
	public static IStruct unFlattenKeys( IStruct struct, boolean deep, boolean retainKeys ) {
		String	key;
		Object	value;
		int		index;
		for ( Key k : struct.getKeys() ) {
			key		= k.getName();
			value	= struct.get( k );
			if ( deep && value instanceof IStruct )
				unFlattenKeys( StructCaster.cast( value ), deep, retainKeys );
			if ( ( index = key.indexOf( '.' ) ) != -1 ) {
				unFlattenKey( index, k, key, struct, retainKeys );
			}
		}

		return struct;

	}

	/**
	 * Method to recursively un-flatten a struct with keys in dot-notation
	 *
	 * @param index      the initial key index
	 * @param key        the struct key to de-flatten
	 * @param keyValue   the string representation of the key
	 * @param original   the original struct to start the deflattening
	 * @param retainKeys whether to retain the original flattened keys
	 */
	public static void unFlattenKey( int index, Key key, String keyValue, IStruct original, boolean retainKeys ) {

		String	left;
		Object	value		= original.get( key );
		IStruct	destination	= original;
		if ( !retainKeys )
			original.remove( key );
		do {
			left		= keyValue.substring( 0, index );
			keyValue	= keyValue.substring( index + 1 );
			Key destinationKey = Key.of( left );
			if ( !destination.containsKey( destinationKey ) ) {
				destination.put( destinationKey, new Struct() );
			}
			destination = destination.getAsStruct( destinationKey );
		} while ( ( index = keyValue.indexOf( '.' ) ) != -1 );
		// final put of the last key in the delimited string
		destination.put( Key.of( keyValue ), value );
	}

	/**
	 * Convert a struct to a query string
	 * Example:
	 *
	 * <pre>
	 * { foo: "bar", baz: "qux" } -> "foo=bar&amp;baz=qux"
	 * </pre>
	 *
	 * @param struct    The struct to convert
	 * @param delimiter The delimiter to use between key-value pairs
	 *
	 * @return The query string
	 */
	public static String toQueryString( IStruct struct, String delimiter ) {
		return struct.entrySet()
		    .stream()
		    .map( entry -> EncryptionUtil.urlEncode( entry.getKey().getName().trim() ) + "=" + EncryptionUtil.urlEncode( entry.getValue().toString().trim() ) )
		    .collect( Collectors.joining( delimiter ) );
	}

	/**
	 * Convert a struct to a query string using the default delimiter of {@code "&"}
	 *
	 * @param struct The struct to convert
	 *
	 * @return The query string
	 */
	public static String toQueryString( IStruct struct ) {
		return toQueryString( struct, "&" );
	}

	/**
	 * Convert a query string to a struct
	 * Example:
	 *
	 * <pre>
	 * "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
	 * </pre>
	 *
	 * @param target    The query string to convert
	 * @param delimiter The delimiter to use between key-value pairs
	 *
	 * @return The struct
	 */
	public static IStruct fromQueryString( String target, String delimiter ) {
		target = target.trim();

		// Empty string should return an empty struct
		if ( target.length() == 0 ) {
			return new Struct( Struct.TYPES.LINKED );
		}

		// If the string starts with ? remove it
		if ( target.startsWith( "?" ) ) {
			target = target.substring( 1 );
		}

		// parse the string into a struct: Example: "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
		return new Struct(
		    Struct.TYPES.LINKED,
		    Stream.of( target.split( delimiter ) )
		        .map( pair -> pair.split( "=" ) )
		        .collect(
		            Collectors.toMap(
		                pair -> Key.of( EncryptionUtil.urlDecode( pair[ 0 ] ).trim() ),
		                pair -> pair.length > 1 ? EncryptionUtil.urlDecode( pair[ 1 ] ).trim() : ""
		            )
		        )
		);
	}

	/**
	 * Convert a query string to a struct using the default delimiter of {@code "&"}.
	 * Example:
	 *
	 * <pre>
	 * "foo=bar&amp;baz=qux" -> { foo: "bar", baz: "qux" }
	 * </pre>
	 *
	 * @param target The query string to convert.
	 *
	 * @return The struct representing the parsed query string.
	 */
	public static IStruct fromQueryString( String target ) {
		return fromQueryString( target, "&" );
	}

	public static HashMap<Key, Comparator<Key>> getCommonComparators() {
		return getCommonComparators( LocalizationUtil.COMMON_LOCALES.get( Key.of( "US" ) ) );
	}

	public static HashMap<Key, Comparator<Key>> getCommonComparators( Locale locale ) {
		return new HashMap<Key, Comparator<Key>>() {

			{
				put( Key.of( "textAsc" ), ( a, b ) -> StringCompare
				    .invoke( StringCaster.cast( a ), StringCaster.cast( b ), true, locale ) );
				put( Key.of( "textDesc" ), ( b, a ) -> StringCompare
				    .invoke( StringCaster.cast( a ), StringCaster.cast( b ), true, locale ) );
				put( Key.of( "textNoCaseAsc" ),
				    ( a, b ) -> StringCompare.invoke( StringCaster.cast( a ),
				        StringCaster.cast( b ), false, locale ) );
				put( Key.of( "textNoCaseDesc" ),
				    ( b, a ) -> StringCompare.invoke( StringCaster.cast( a ),
				        StringCaster.cast( b ), false, locale ) );
				put( Key.of( "numericAsc" ),
				    ( a, b ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getOriginalValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getOriginalValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
				put( Key.of( "numericDesc" ),
				    ( b, a ) -> {
					    CastAttempt<Number> aNum = NumberCaster.attempt( a.getOriginalValue() );
					    CastAttempt<Number> bNum = null;
					    return aNum.wasSuccessful()
					        // lazy cast second value if first is a number
					        && ( bNum = NumberCaster.attempt( b.getOriginalValue() ) ).wasSuccessful()
					            ? Compare.invoke(
					                aNum.get(),
					                bNum.get()
					            )
					            : Compare.invoke( a.toString(), b.toString(), true );
				    }
				);
			}
		};
	}

}
