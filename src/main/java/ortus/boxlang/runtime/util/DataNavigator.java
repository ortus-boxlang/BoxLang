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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * This utility class is a fluent class that can navigate
 * data structures from many incoming sources.
 */
public class DataNavigator {

	/**
	 * Builds a navigator from a file path which must be JSON
	 *
	 * @param filePath The path to a JSON file. This can be a String or a Path
	 *
	 * @return A navigator for the JSON file
	 */
	public static Navigator ofPath( Object filePath ) {
		if ( filePath instanceof String castedFilePath ) {
			filePath = Paths.get( castedFilePath );
		}

		if ( filePath instanceof Path castedFilePath ) {
			return new Navigator( castedFilePath );
		}

		throw new BoxRuntimeException( "The file path must be a String or a Path" );
	}

	/**
	 * Builds a navigator from a JSON string
	 *
	 * @param json The JSON string
	 *
	 * @return A navigator for the JSON string
	 */
	public static Navigator ofJson( String json ) {
		// Parse the JSON
		Object data = JSONUtil.fromJSON( json );

		// We can only do structs for now
		if ( data instanceof Map<?, ?> map ) {
			return new Navigator( Struct.fromMap( map ) );
		}

		throw new BoxRuntimeException( "The JSON data must be a Map and it's a [" + TypeUtil.getObjectName( data ) + "]" );
	}

	/**
	 * Builds out a navigator from an incoming data structure.
	 * <p>
	 * This can be a Map, a Struct, a JSON string, a file path to a JSON file, etc.
	 *
	 * @param data The data to navigate
	 *
	 * @return A navigator for the data
	 */
	public static Navigator of( Object data ) {

		// Is this a Path
		if ( data instanceof Path ) {
			return ofPath( data );
		}

		// Is this a valid file path?
		if ( data instanceof String filePath && FileSystemUtil.isValidFilePath( filePath ) ) {
			return ofPath( filePath );
		}

		// Is this a JSON String?
		if ( data instanceof String ) {
			return ofJson( StringCaster.cast( data ) );
		}

		// Structs
		if ( data instanceof IStruct struct ) {
			return new Navigator( struct );
		}

		// Raw Maps
		if ( data instanceof Map<?, ?> map ) {
			return new Navigator( Struct.fromMap( map ) );
		}

		// Queries?

		throw new BoxRuntimeException(
		    "The data is not a valid type for navigation. \n" +
		        "It must be a Map, a Struct, a JSON string, or file path to a JSON file, etc."
		);
	}

	/**
	 * The Data Navigator Fluent Goodness Class
	 */
	public static class Navigator {

		/**
		 * The data structure to navigate
		 */
		private IStruct	config;

		/**
		 * The segment to navigate to
		 */
		private IStruct	segment;

		/**
		 * --------------------------------------------------------------------------
		 * Path segment marker types
		 * --------------------------------------------------------------------------
		 */

		/** Depth-first search for the first key match anywhere in the tree: {@code ..key} */
		private record RecursiveKey( String name ) {
		}

		/**
		 * 1-based inclusive slice of an array: {@code [start:end]}.
		 * Either bound may be {@code null} to mean "from the start" or "to the end" respectively.
		 */
		private record SliceSegment( Integer start, Integer end ) {
		}

		/** Filter applied to array elements: {@code [?(@.key op value)]} */
		private record FilterSegment( String key, String op, Object value ) {
		}

		/** All values of a struct or all elements of an array: {@code .*} or {@code [*]} */
		private enum Wildcard {
			INSTANCE
		}

		/**
		 * --------------------------------------------------------------------------
		 * Constructor(s)
		 * --------------------------------------------------------------------------
		 */

		/**
		 * Construct a navigator from a file path
		 *
		 * @param filePath The path to the JSON file
		 */
		public Navigator( Path filePath ) {
			this.parseFile( filePath );
		}

		/**
		 * Construct a navigator from a data structure
		 *
		 * @param data The data structure to navigate
		 */
		public Navigator( IStruct data ) {
			this.config = data;
		}

		/**
		 * --------------------------------------------------------------------------
		 * Methods
		 * --------------------------------------------------------------------------
		 */

		/**
		 * Verifies if the segment or the data structure is empty or not
		 *
		 * @return True if the segment or the data structure is empty, false otherwise
		 */
		public boolean isEmpty() {
			return this.segment == null ? this.config.isEmpty() : this.segment.isEmpty();
		}

		/**
		 * Verifies if the segment or the data structure has data. This is the inverse of {@code isEmpty()}
		 *
		 * @return True if the segment or the data structure has data, false otherwise
		 */
		public boolean isPresent() {
			return !this.isEmpty();
		}

		/**
		 * Check if a key exists in the data segment and if present execute a consumer.
		 *
		 * @param key      The key to check for
		 * @param consumer The consumer to execute if the key exists
		 *
		 * @return The navigator again so you can chain calls
		 */
		public Navigator ifPresent( String key, Consumer<Object> consumer ) {
			IStruct navConfig = this.segment == null ? this.config : this.segment;

			if ( navConfig.containsKey( key ) ) {
				consumer.accept( get( key ) );
			}

			return this;
		}

		/**
		 * Check if a key exists in the data segment and if present execute a consumer.
		 * If the key does not exist then execute the orElse runnable.
		 *
		 * @param key      The key to check for
		 * @param consumer The consumer to execute if the key exists
		 * @param orElse   The runnable to execute if the key does not exist
		 *
		 * @return The navigator again so you can chain calls
		 */
		public Navigator ifPresentOrElse( String key, Consumer<Object> consumer, Runnable orElse ) {
			IStruct navConfig = this.segment == null ? this.config : this.segment;

			if ( navConfig.containsKey( key ) ) {
				consumer.accept( get( key ) );
			} else {
				orElse.run();
			}

			return this;
		}

		/**
		 * Verify if a path exists in the data structure.
		 * When called with a single argument that contains {@code .} or {@code [}, the argument
		 * is treated as a path expression (e.g. {@code "boxlang.settings.hello"}, {@code "list[1]"},
		 * {@code "..key"}).
		 *
		 * <h2>Examples</h2>
		 *
		 * <pre>
		 * navigator.has( "simpleKey" )                     // checks for "simpleKey" at the current segment
		 * navigator.has( "nested.key.path" )                // checks for "nested" then "key" then "path"
		 * navigator.has( "list[1]" )                        // checks for "list" then index 1 (1-based)
		 * navigator.has( "..recursiveKey" )                // checks for "recursiveKey" anywhere in the tree
		 * navigator.has( "items[*].name" )                 // checks for "items" then any element then "name"
		 * navigator.has( "array[1:3]" )                    // checks for "array" then indices 1 through 3 (inclusive)
		 * navigator.has( "users[?(@.active == true)].email" ) // checks for "users" then active users then "email"
		 * navigator.has( "key1", "key2", "key3" )          // checks for "key1" then "key2" then "key3"
		 * navigator.has( "..key1" ) 					  // checks for "key1" anywhere in the tree
		 * </pre>
		 *
		 * @param path The path(s) to verify (nested keys accepted, or a single path expression)
		 *
		 * @return True if the key exists, false otherwise
		 */
		public boolean has( String path ) {
			return this.has( new String[] { path } );
		}

		/**
		 * Verify if a path exists in the data structure.
		 * When called with a single argument that contains {@code .} or {@code [}, the argument
		 * is treated as a path expression (e.g. {@code "boxlang.settings.hello"}, {@code "list[1]"},
		 * {@code "..key"}).
		 *
		 * @param path The path(s) to verify (nested keys accepted, or a single path expression)
		 *
		 * @return True if the key exists, false otherwise
		 */
		public boolean has( String... path ) {
			// Single path expression — delegate to the path-aware navigator
			if ( path.length == 1 ) {
				String normalizedPath = normalizePathExpression( path[ 0 ] );
				if ( isPathExpression( normalizedPath ) ) {
					return !navigateForQuery( parsePath( normalizedPath ) ).isEmpty();
				}
			}

			IStruct	navConfig	= this.segment == null ? this.config : this.segment;
			Object	lastResult	= null;

			for ( String targetKey : path ) {

				// If the path does not exist then we can't navigate it
				if ( !navConfig.containsKey( targetKey ) ) {
					return false;
				}

				// Get the item
				lastResult = navConfig.get( Key.of( targetKey ) );

				// If the lastResult is a Map then we can navigate it, prep for further iterations
				if ( lastResult instanceof Map<?, ?> ) {
					navConfig = StructCaster.cast( lastResult );
				}

			}
			return true;
		}

		/**
		 * Check if an exact key exists in the data segment.
		 * This method does not interpret dots or brackets as path separators.
		 *
		 * @param key The exact key to check for
		 *
		 * @return True if the exact key exists, false otherwise
		 */
		public boolean hasByKey( String key ) {
			IStruct navConfig = this.segment == null ? this.config : this.segment;
			return navConfig.containsKey( Key.of( key ) );
		}

		/**
		 * Safely navigate the data structure to a segment without blowing up.
		 *
		 * @param path The path to the object in the data structure. When called with a single argument that contains {@code .} or {@code [}, the argument
		 *
		 * @return The navigator with the segment set
		 *         is treated as a path expression (e.g. {@code "boxlang.settings.hello"}, {@code "list[1]"},
		 */
		public Navigator from( String path ) {
			return from( new String[] { path } );
		}

		/**
		 * Safely navigate the data structure to a segment without blowing up.
		 * If the path does not exist then a new empty struct is returned as the segment.
		 *
		 * @param path The path to the object in the data structure
		 *
		 * @return The navigator with the segment set
		 */
		public Navigator from( String... path ) {
			IStruct	navConfig	= this.config;
			Object	lastResult	= null;

			for ( String thisPath : path ) {

				// If the path does not exist then we can't navigate it
				if ( !navConfig.containsKey( thisPath ) ) {
					lastResult = new Struct();
				} else {
					// Get the segment
					lastResult = navConfig.get( Key.of( thisPath ) );
				}

				// If it's not a map/struct then we can't navigate it, blow up
				if ( ! ( lastResult instanceof Map<?, ?> ) ) {
					throw new BoxRuntimeException( "The requested segment is not a Struct, but a [" + TypeUtil.getObjectName( lastResult ) + "]" );
				}

				// Set the navigable segment
				this.segment	= StructCaster.cast( lastResult );
				navConfig		= this.segment;
			}

			return this;
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Object get( String key, Object defaultValue ) {
			Object result = get( key );
			return result == null ? defaultValue : result;
		}

		/**
		 * Get a value from data structure using nested keys if passed.
		 * If the value is null or does not exist, return the provided default.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the value is null or does not exist
		 *
		 * @return The value for the key, or the default value
		 */
		public Object getOrDefault( String key, Object defaultValue ) {
			return this.get( key, defaultValue );
		}

		/**
		 * Get a value from data structure using nested keys if passed
		 *
		 * @param key The key to get the value for.
		 *
		 * @throws BoxRuntimeException If the key does not exist
		 *
		 * @return The value of the key(s)
		 */
		public Object getOrThrow( String key ) {
			return this.getOrThrow( new String[] { key } );
		}

		/**
		 * Get a value from data structure using nested keys if passed
		 * If the key does not exist then throw an exception
		 *
		 * @param key One or more keys to retrieve the value for
		 *
		 * @throws BoxRuntimeException If the key does not exist
		 *
		 * @return The value of the key(s)
		 */
		public Object getOrThrow( String... key ) {
			Object result = this.get( key );
			if ( result == null ) {
				throw new BoxRuntimeException( "The key [" + key + "] does not exist in the json contents. Top level keys are: " + this.config.keySet() );
			}
			return result;
		}

		/**
		 * Get a value from data structure using nested keys if passed.
		 * When called with a single argument that contains {@code .} or {@code [}, the argument
		 * is treated as a path expression (e.g. {@code "boxlang.settings.hello"}, {@code "list[1]"},
		 * {@code "..key"}).
		 *
		 * @param key One or more keys to navigate the data structure, or a single path expression
		 *
		 * @return The value of the key(s) or null if it does not exist
		 */
		public Object get( String key ) {
			return this.get( new String[] { key } );
		}

		/**
		 * Get a value from data structure using nested keys if passed.
		 * When called with a single argument that contains {@code .} or {@code [}, the argument
		 * is treated as a path expression (e.g. {@code "boxlang.settings.hello"}, {@code "list[1]"},
		 * {@code "..key"}).
		 *
		 * @param key One or more keys to navigate the data structure, or a single path expression
		 *
		 * @return The value of the key(s) or null if it does not exist
		 */
		public Object get( String... key ) {
			// Single path expression — delegate to the path-aware navigator
			if ( key.length == 1 ) {
				String normalizedPath = normalizePathExpression( key[ 0 ] );
				if ( isPathExpression( normalizedPath ) ) {
					return navigateSegments( parsePath( normalizedPath ) );
				}
			}

			IStruct	navConfig	= this.segment == null ? this.config : this.segment;
			Object	lastResult	= null;

			for ( String targetKey : key ) {

				// If the path does not exist then we can't navigate it
				if ( !navConfig.containsKey( targetKey ) ) {
					lastResult = null;
					break;
				}

				// Get the item
				lastResult = navConfig.get( Key.of( targetKey ) );

				// If the lastResult is a Map then we can navigate it, prep for further iterations
				if ( lastResult instanceof Map<?, ?> ) {
					navConfig = StructCaster.cast( lastResult );
				}

			}

			// Auto-Casting
			if ( lastResult instanceof Map<?, ?> map ) {
				return StructCaster.cast( map );
			}
			if ( lastResult instanceof List<?> list ) {
				return ArrayCaster.cast( list );
			}
			if ( lastResult instanceof Object[] array ) {
				return ArrayCaster.cast( array );
			}

			return lastResult;
		}

		/**
		 * Get a value from the data structure by exact key.
		 * This method does not interpret dots or brackets as path separators.
		 *
		 * @param key The exact key to retrieve
		 *
		 * @return The value of the exact key, or null if it does not exist
		 */
		public Object getByKey( String key ) {
			IStruct navConfig = this.segment == null ? this.config : this.segment;

			if ( !navConfig.containsKey( Key.of( key ) ) ) {
				return null;
			}

			Object result = navConfig.get( Key.of( key ) );

			if ( result instanceof Map<?, ?> map ) {
				return StructCaster.cast( map );
			}
			if ( result instanceof List<?> list ) {
				return ArrayCaster.cast( list );
			}
			if ( result instanceof Object[] array ) {
				return ArrayCaster.cast( array );
			}

			return result;
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Key getAsKey( String key, Object defaultValue ) {
			return Key.of( StringCaster.cast( this.get( key, defaultValue ) ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Key getAsKey( String key ) {
			return this.getAsKey( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Key getAsKey( String... key ) {
			return Key.of( StringCaster.cast( this.get( key ) ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public String getAsString( String key, Object defaultValue ) {
			return StringCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public String getAsString( String key ) {
			return this.getAsString( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public String getAsString( String... key ) {
			return StringCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Boolean getAsBoolean( String key, Object defaultValue ) {
			return BooleanCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Boolean getAsBoolean( String key ) {
			return this.getAsBoolean( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Boolean getAsBoolean( String... key ) {
			return BooleanCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Integer getAsInteger( String key, Object defaultValue ) {
			return IntegerCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Integer getAsInteger( String key ) {
			return this.getAsInteger( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Integer getAsInteger( String... key ) {
			return IntegerCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 *
		 * @return The value as a date
		 */
		public DateTime getAsDate( String key, Object defaultValue ) {
			return DateTimeCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public DateTime getAsDate( String key ) {
			return this.getAsDate( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public DateTime getAsDate( String... key ) {
			return DateTimeCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Long getAsLong( String key, Object defaultValue ) {
			return LongCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Long getAsLong( String key ) {
			return this.getAsLong( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Long getAsLong( String... key ) {
			return LongCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Double getAsDouble( String key, Object defaultValue ) {
			return DoubleCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Double getAsDouble( String key ) {
			return this.getAsDouble( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Double getAsDouble( String... key ) {
			return DoubleCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public IStruct getAsStruct( String key, Object defaultValue ) {
			return StructCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public IStruct getAsStruct( String key ) {
			return this.getAsStruct( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public IStruct getAsStruct( String... key ) {
			return StructCaster.cast( this.get( key ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Array getAsArray( String key, Object defaultValue ) {
			return ArrayCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Array getAsArray( String key ) {
			return this.getAsArray( new String[] { key } );
		}

		/**
		 * Get a value from data structure
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Array getAsArray( String... key ) {
			return ArrayCaster.cast( this.get( key ) );
		}

		/**
		 * Query the data structure using a path expression and return all matching values as a BoxLang {@link Array}.
		 * Supports dot notation, bracket indexing, recursive descent ({@code ..key}),
		 * wildcards ({@code .*} or {@code [*]}), array slicing ({@code [1:3]}),
		 * and filter expressions ({@code [?(@.key op value)]}).
		 *
		 * @param path The path expression to evaluate
		 *
		 * @return A BoxLang {@link Array} of all matching values; empty if no matches are found
		 */
		public Array query( String path ) {
			return navigateForQuery( parsePath( normalizePathExpression( path ) ) );
		}

		/**
		 * Normalizes a path expression by trimming surrounding whitespace.
		 * Path expressions may contain insignificant whitespace around segment boundaries,
		 * but quoted filter values remain untouched.
		 *
		 * @param path The raw path expression
		 *
		 * @return The normalized path expression
		 */
		private String normalizePathExpression( String path ) {
			return path == null ? "" : path.trim();
		}

		/**
		 * Returns true if the given string is a path expression (contains {@code .} or {@code [}).
		 * Plain keys without dots or brackets use the existing direct-lookup path.
		 *
		 * @param key The key to test
		 *
		 * @return Whether the key should be treated as a path expression
		 */
		private boolean isPathExpression( String key ) {
			return key.contains( "." ) || key.contains( "[" );
		}

		/**
		 * Parses a dot/bracket path expression into an ordered list of typed segment descriptors.
		 * Supports:
		 * <ul>
		 * <li>{@code key.sub} — nested struct keys</li>
		 * <li>{@code list[1]} — 1-based array index</li>
		 * <li>{@code ..key} — depth-first recursive descent (first match)</li>
		 * <li>{@code .*} or {@code [*]} — wildcard (all values / all elements)</li>
		 * <li>{@code [1:3]} — 1-based inclusive slice</li>
		 * <li>{@code [?(@.key op value)]} — filter expression</li>
		 * </ul>
		 *
		 * @param path The path expression to parse
		 *
		 * @return Ordered list of segment descriptors (String, Integer, RecursiveKey, Wildcard, SliceSegment, or FilterSegment)
		 */
		private List<Object> parsePath( String path ) {
			path = normalizePathExpression( path );
			List<Object>	segments	= new ArrayList<>();
			int				i			= 0;
			int				len			= path.length();

			while ( i < len ) {
				char c = path.charAt( i );

				if ( Character.isWhitespace( c ) ) {
					i++;
					continue;
				}

				if ( c == '.' ) {
					if ( i + 1 < len && path.charAt( i + 1 ) == '.' ) {
						// ".." — recursive descent; read the key that follows
						i += 2;
						int start = i;
						while ( i < len && path.charAt( i ) != '.' && path.charAt( i ) != '[' ) {
							i++;
						}
						String key = path.substring( start, i ).trim();
						if ( !key.isEmpty() ) {
							segments.add( new RecursiveKey( key ) );
						}
					} else {
						// Single dot separator — read the next key
						i++;
						int start = i;
						while ( i < len && path.charAt( i ) != '.' && path.charAt( i ) != '[' ) {
							i++;
						}
						String key = path.substring( start, i ).trim();
						if ( key.equals( "*" ) ) {
							segments.add( Wildcard.INSTANCE );
						} else if ( !key.isEmpty() ) {
							segments.add( key );
						}
					}
				} else if ( c == '[' ) {
					// Bracket segment — read until matching ']'
					int end = path.indexOf( ']', i );
					if ( end == -1 ) {
						throw new BoxRuntimeException( "Unclosed '[' in path expression: " + path );
					}
					String inner = path.substring( i + 1, end ).trim();
					i = end + 1;

					if ( inner.equals( "*" ) ) {
						segments.add( Wildcard.INSTANCE );
					} else if ( inner.contains( ":" ) ) {
						String[]	parts		= inner.split( ":", 2 );
						String		startStr	= parts[ 0 ].trim();
						String		endStr		= parts[ 1 ].trim();
						Integer		sliceStart	= startStr.isEmpty() ? null : IntegerCaster.cast( startStr );
						Integer		sliceEnd	= endStr.isEmpty() ? null : IntegerCaster.cast( endStr );
						segments.add( new SliceSegment( sliceStart, sliceEnd ) );
					} else if ( inner.startsWith( "?" ) ) {
						segments.add( parseFilter( inner ) );
					} else {
						segments.add( IntegerCaster.cast( inner.trim() ) );
					}
				} else {
					// Plain key at the start of the expression (no leading dot)
					int start = i;
					while ( i < len && path.charAt( i ) != '.' && path.charAt( i ) != '[' ) {
						i++;
					}
					String key = path.substring( start, i ).trim();
					if ( key.equals( "*" ) ) {
						segments.add( Wildcard.INSTANCE );
					} else if ( !key.isEmpty() ) {
						segments.add( key );
					}
				}
			}

			return segments;
		}

		/**
		 * Parses the content inside {@code [?( ... )]} into a {@link FilterSegment}.
		 * Supports operators {@code ==}, {@code !=}, {@code >=}, {@code <=}, {@code >}, {@code <},
		 * and a bare {@code @.key} existence check.
		 *
		 * @param inner The text inside the brackets, e.g. {@code ?(@.active == true)}
		 *
		 * @return The parsed filter segment
		 */
		private FilterSegment parseFilter( String inner ) {
			// Strip "?(" prefix and ")" suffix
			String		expr	= inner.substring( 2, inner.length() - 1 ).trim();
			// Try each operator longest-first to avoid partial matches (>= before >)
			String[]	ops		= { "==", "!=", ">=", "<=", ">", "<" };
			for ( String op : ops ) {
				int idx = expr.indexOf( op );
				if ( idx > 0 ) {
					String	keyPart		= expr.substring( 0, idx ).trim();
					String	valuePart	= expr.substring( idx + op.length() ).trim();
					String	key			= keyPart.startsWith( "@." ) ? keyPart.substring( 2 ) : keyPart;
					return new FilterSegment( key, op, parseFilterValue( valuePart ) );
				}
			}
			// No operator — existence check
			String key = expr.startsWith( "@." ) ? expr.substring( 2 ) : expr;
			return new FilterSegment( key, "exists", null );
		}

		/**
		 * Parses a filter value literal into its Java type:
		 * quoted string, boolean, null, or number (Double). Falls back to String.
		 *
		 * @param s The raw value string from the filter expression
		 *
		 * @return The parsed value
		 */
		private Object parseFilterValue( String s ) {
			if ( ( s.startsWith( "\"" ) && s.endsWith( "\"" ) ) || ( s.startsWith( "'" ) && s.endsWith( "'" ) ) ) {
				return s.substring( 1, s.length() - 1 );
			}
			if ( "true".equals( s ) )
				return Boolean.TRUE;
			if ( "false".equals( s ) )
				return Boolean.FALSE;
			if ( "null".equals( s ) )
				return null;
			try {
				return Double.parseDouble( s );
			} catch ( NumberFormatException e ) {
				return s;
			}
		}

		/**
		 * Walks a parsed segment list from the current navigation root, returning a single value.
		 * Multi-result segments (Wildcard, SliceSegment, FilterSegment) return {@code null};
		 * use {@link #navigateForQuery(List)} for those.
		 *
		 * @param segments The parsed path segments
		 *
		 * @return The value at the end of the path, or {@code null} if any segment is absent
		 */
		private Object navigateSegments( List<Object> segments ) {
			Object current = this.segment == null ? this.config : this.segment;

			for ( Object seg : segments ) {
				if ( current == null )
					return null;

				// Auto-cast to BoxLang native types before each step
				if ( current instanceof Map<?, ?> )
					current = StructCaster.cast( current );
				if ( current instanceof List<?> || current instanceof Object[] )
					current = ArrayCaster.cast( current );

				if ( seg instanceof String key ) {
					if ( ! ( current instanceof IStruct struct ) || !struct.containsKey( key ) )
						return null;
					current = struct.get( Key.of( key ) );
				} else if ( seg instanceof Integer idx ) {
					if ( ! ( current instanceof Array arr ) || idx < 1 || idx > arr.size() )
						return null;
					current = arr.getAt( idx );
				} else if ( seg instanceof RecursiveKey rk ) {
					current = findFirst( current, rk.name() );
				} else {
					// Wildcard / Slice / Filter require multi-value navigation — not supported in get()
					return null;
				}
			}

			// Final auto-cast of the result
			if ( current instanceof Map<?, ?> map )
				return StructCaster.cast( map );
			if ( current instanceof List<?> list )
				return ArrayCaster.cast( list );
			if ( current instanceof Object[] arr )
				return ArrayCaster.cast( arr );

			return current;
		}

		/**
		 * Depth-first search returning the first value whose key matches {@code key} anywhere in the tree.
		 *
		 * @param node The root node to search from
		 * @param key  The key name to find
		 *
		 * @return The first matching value, or {@code null} if not found
		 */
		private Object findFirst( Object node, String key ) {
			if ( node instanceof Map<?, ?> )
				node = StructCaster.cast( node );
			if ( node instanceof List<?> || node instanceof Object[] )
				node = ArrayCaster.cast( node );

			if ( node instanceof IStruct struct ) {
				if ( struct.containsKey( key ) )
					return struct.get( Key.of( key ) );
				for ( Object value : struct.values() ) {
					Object found = findFirst( value, key );
					if ( found != null )
						return found;
				}
			} else if ( node instanceof Array arr ) {
				for ( int i = 1; i <= arr.size(); i++ ) {
					Object found = findFirst( arr.getAt( i ), key );
					if ( found != null )
						return found;
				}
			}

			return null;
		}

		/**
		 * Walks a parsed segment list, fanning out at every multi-result segment, and collects
		 * all matching values into a BoxLang {@link Array}.
		 *
		 * @param segments The parsed path segments (may include Wildcard, SliceSegment, FilterSegment)
		 *
		 * @return An {@link Array} of all matched values (may be empty)
		 */
		private Array navigateForQuery( List<Object> segments ) {
			List<Object> workingSet = new ArrayList<>();
			workingSet.add( this.segment == null ? this.config : this.segment );

			for ( Object seg : segments ) {
				List<Object> nextSet = new ArrayList<>();

				for ( Object current : workingSet ) {
					if ( current == null )
						continue;
					if ( current instanceof Map<?, ?> )
						current = StructCaster.cast( current );
					if ( current instanceof List<?> || current instanceof Object[] )
						current = ArrayCaster.cast( current );

					if ( seg instanceof String key ) {
						if ( current instanceof IStruct struct && struct.containsKey( key ) ) {
							Object val = struct.get( Key.of( key ) );
							nextSet.add( val );
						}
					} else if ( seg instanceof Integer idx ) {
						if ( current instanceof Array arr && idx >= 1 && idx <= arr.size() ) {
							nextSet.add( arr.getAt( idx ) );
						}
					} else if ( seg instanceof RecursiveKey rk ) {
						collectAll( current, rk.name(), nextSet );
					} else if ( seg instanceof Wildcard ) {
						if ( current instanceof IStruct struct ) {
							nextSet.addAll( struct.values() );
						} else if ( current instanceof Array arr ) {
							for ( int i = 1; i <= arr.size(); i++ ) {
								nextSet.add( arr.getAt( i ) );
							}
						}
					} else if ( seg instanceof SliceSegment ss ) {
						if ( current instanceof Array arr ) {
							int	start	= Math.max( 1, ss.start() != null ? ss.start() : 1 );
							int	end		= Math.min( arr.size(), ss.end() != null ? ss.end() : arr.size() );
							for ( int i = start; i <= end; i++ ) {
								nextSet.add( arr.getAt( i ) );
							}
						}
					} else if ( seg instanceof FilterSegment fs ) {
						if ( current instanceof Array arr ) {
							for ( int i = 1; i <= arr.size(); i++ ) {
								Object elem = arr.getAt( i );
								if ( elem instanceof Map<?, ?> )
									elem = StructCaster.cast( elem );
								if ( elem instanceof IStruct struct && matchesFilter( struct, fs ) ) {
									nextSet.add( elem );
								}
							}
						}
					}
				}

				workingSet = nextSet;
			}

			// Build the result array with auto-casting
			Array result = new Array();
			for ( Object item : workingSet ) {
				if ( item instanceof Map<?, ?> )
					item = StructCaster.cast( item );
				if ( item instanceof List<?> || item instanceof Object[] )
					item = ArrayCaster.cast( item );
				result.add( item );
			}
			return result;
		}

		/**
		 * Depth-first search collecting every value whose key matches {@code key} anywhere in the tree.
		 * Unlike {@link #findFirst}, this accumulates all matches.
		 *
		 * @param node    The root node to search from
		 * @param key     The key name to find
		 * @param results The list to append matching values into
		 */
		private void collectAll( Object node, String key, List<Object> results ) {
			if ( node instanceof Map<?, ?> )
				node = StructCaster.cast( node );
			if ( node instanceof List<?> || node instanceof Object[] )
				node = ArrayCaster.cast( node );

			if ( node instanceof IStruct struct ) {
				if ( struct.containsKey( key ) )
					results.add( struct.get( Key.of( key ) ) );
				for ( Object value : struct.values() ) {
					collectAll( value, key, results );
				}
			} else if ( node instanceof Array arr ) {
				for ( int i = 1; i <= arr.size(); i++ ) {
					collectAll( arr.getAt( i ), key, results );
				}
			}
		}

		/**
		 * Evaluates a {@link FilterSegment} against a struct element.
		 * Supported operators: {@code ==}, {@code !=}, {@code >}, {@code >=}, {@code <}, {@code <=},
		 * and {@code exists} (bare key check).
		 *
		 * @param struct The struct element to test
		 * @param fs     The filter descriptor
		 *
		 * @return {@code true} if the element satisfies the filter
		 */
		private boolean matchesFilter( IStruct struct, FilterSegment fs ) {
			if ( "exists".equals( fs.op() ) )
				return struct.containsKey( fs.key() );
			if ( !struct.containsKey( fs.key() ) )
				return false;
			Object	actual		= struct.get( Key.of( fs.key() ) );
			Object	expected	= fs.value();
			return switch ( fs.op() ) {
				case "==" -> Objects.equals( actual, expected ) || compareValues( actual, expected ) == 0;
				case "!=" -> !Objects.equals( actual, expected );
				case ">" -> compareValues( actual, expected ) > 0;
				case ">=" -> compareValues( actual, expected ) >= 0;
				case "<" -> compareValues( actual, expected ) < 0;
				case "<=" -> compareValues( actual, expected ) <= 0;
				default -> false;
			};
		}

		/**
		 * Compares two values numerically, falling back to lexicographic string comparison.
		 * {@code null} is treated as less than any non-null value; two {@code null}s are equal.
		 *
		 * @param a First value
		 * @param b Second value
		 *
		 * @return Negative, zero, or positive
		 */
		private int compareValues( Object a, Object b ) {
			if ( a == null && b == null )
				return 0;
			if ( a == null )
				return -1;
			if ( b == null )
				return 1;
			try {
				return Double.compare( DoubleCaster.cast( a ), DoubleCaster.cast( b ) );
			} catch ( Exception e ) {
				return StringCaster.cast( a ).compareTo( StringCaster.cast( b ) );
			}
		}

		/**
		 * Parse the file and seed the config as a struct
		 *
		 * @param filepath The path to the file
		 */
		private void parseFile( Path filePath ) {
			try {
				Object rawConfig = JSONUtil.fromJSON(
				    Files.readString( filePath.toAbsolutePath(), StandardCharsets.UTF_8 )
				);
				if ( rawConfig instanceof Map<?, ?> rawMap ) {
					this.config = Struct.fromMap( rawMap );
				}
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}

	}

}
