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
import java.util.List;
import java.util.Map;
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

		throw new BoxRuntimeException( "The JSON data must be a Map and it's a [" + data.getClass().getName() + "]" );
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
		 * Verify if a path exists in the data structure
		 *
		 * @param path The path(s) to verify (nested keys accepted)
		 *
		 * @return True if the key exists, false otherwise
		 */
		public boolean has( String... path ) {
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
					throw new BoxRuntimeException( "The requested segment is not a Struct, but a [" + lastResult.getClass().getName() + "]" );
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
		 * Get a value from data structure using nested keys if passed
		 *
		 * @param key One or more keys to navigate the box.json file
		 *
		 * @return The value of the key(s) or null if it does not exist
		 */
		public Object get( String... key ) {
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
		public Array getAsArray( String... key ) {
			return ArrayCaster.cast( this.get( key ) );
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
