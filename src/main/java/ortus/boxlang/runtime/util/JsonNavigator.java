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
import java.util.Map;
import java.util.function.Consumer;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;

/**
 * A utility class for working with a box.json file
 */
public class JsonNavigator {

	/**
	 * Build out a descriptor for a box.json file
	 *
	 * @param filePath The path to the box.json file
	 *
	 * @return A descriptor for the box.json file
	 */
	public static Navigator of( String filePath ) {
		return new Navigator( filePath );
	}

	/**
	 * Build out a descriptor for a box.json file
	 *
	 * @param filePath A Path to the box.json file
	 *
	 * @return A descriptor for the box.json file
	 */
	public static Navigator of( Path filePath ) {
		return new Navigator( filePath );
	}

	/**
	 * The JSON Navigator
	 */
	public static class Navigator {

		private Path	filePath;
		private IStruct	config;
		private IStruct	segment;

		public Navigator( String filePath ) {
			this( Paths.get( filePath ) );
		}

		public Navigator( Path filePath ) {
			this.filePath = filePath;
			this.parseFile();
		}

		/**
		 * Check if a key exists in the box.json file and if present execute a consumer
		 *
		 * @param key      The key to check for
		 * @param consumer The consumer to execute if the key exists
		 *
		 * @return The descriptor again so you can chain calls
		 */
		public Navigator ifPresent( String key, Consumer<Object> consumer ) {
			IStruct	navConfig	= this.segment == null ? this.config : this.segment;
			Key		targetKey	= Key.of( key );

			if ( navConfig.containsKey( targetKey ) ) {
				consumer.accept( navConfig.get( targetKey ) );
			}

			return this;
		}

		/**
		 * Check if a key exists in the box.json file and if present execute a consumer
		 *
		 * @param key      The key to check for
		 * @param consumer The consumer to execute if the key exists
		 * @param orElse   The runnable to execute if the key does not exist
		 *
		 * @return The descriptor again so you can chain calls
		 */
		public Navigator ifPresentOrElse( String key, Consumer<Object> consumer, Runnable orElse ) {
			IStruct	navConfig	= this.segment == null ? this.config : this.segment;
			Key		targetKey	= Key.of( key );

			if ( navConfig.containsKey( targetKey ) ) {
				consumer.accept( navConfig.get( targetKey ) );
			} else {
				orElse.run();
			}

			return this;
		}

		/**
		 * Verify if a path exists in the box.json file
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
		 * Root the navigation of the box.json file on an object(Struct).
		 * If the navigation path does not exist, the segment will not throw an exception
		 * but will be set to an empty struct so you can continue to navigate
		 *
		 * @param path The path to the object in the box.json file
		 *
		 * @return The descriptor again.
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
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Object get( String key, Object defaultValue ) {
			if ( this.segment == null ) {
				return this.config.getOrDefault( Key.of( key ), defaultValue );
			}
			return this.segment.getOrDefault( Key.of( key ), defaultValue );
		}

		/**
		 * Get a value from the box.json file using nested keys if passed
		 *
		 * @param key One or more keys to navigate the box.json file
		 *
		 * @return The value of the key(s)
		 */
		public Object get( String... key ) {
			IStruct	navConfig	= this.segment == null ? this.config : this.segment;
			Object	lastResult	= null;

			for ( String targetKey : key ) {

				// If the path does not exist then we can't navigate it
				if ( !navConfig.containsKey( targetKey ) ) {
					throw new BoxRuntimeException(
					    "The key [" + targetKey + "] does not exist in the json contents. Top level keys are: " + navConfig.keySet()
					);
				}

				// Get the item
				lastResult = navConfig.get( Key.of( targetKey ) );

				// If the lastResult is a Map then we can navigate it, prep for further iterations
				if ( lastResult instanceof Map<?, ?> ) {
					navConfig = StructCaster.cast( lastResult );
				}

			}
			return lastResult;
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Key getAsKey( String key, Object defaultValue ) {
			return Key.of( StringCaster.cast( this.get( key, defaultValue ) ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Key getAsKey( String key ) {
			return Key.of( StringCaster.cast( this.get( key ) ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public String getAsString( String key, Object defaultValue ) {
			return StringCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public String getAsString( String key ) {
			return this.getAsString( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Boolean getAsBoolean( String key, Object defaultValue ) {
			return BooleanCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Boolean getAsBoolean( String key ) {
			return this.getAsBoolean( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Integer getAsInteger( String key, Object defaultValue ) {
			return IntegerCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Integer getAsInteger( String key ) {
			return this.getAsInteger( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Long getAsLong( String key, Object defaultValue ) {
			return LongCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Long getAsLong( String key ) {
			return this.getAsLong( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Double getAsDouble( String key, Object defaultValue ) {
			return DoubleCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Double getAsDouble( String key ) {
			return this.getAsDouble( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public IStruct getAsStruct( String key, Object defaultValue ) {
			return StructCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public IStruct getAsStruct( String key ) {
			return this.getAsStruct( key, null );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key          The key to get the value for
		 * @param defaultValue The default value to return if the key does not exist
		 */
		public Array getAsArray( String key, Object defaultValue ) {
			return ArrayCaster.cast( this.get( key, defaultValue ) );
		}

		/**
		 * Get a value from the box.json file
		 * The value can be seeded using a ${code from} method call.
		 *
		 * @param key The key to get the value for
		 */
		public Array getAsArray( String key ) {
			return this.getAsArray( key, null );
		}

		/**
		 * Parse the file and seed the config as a struct
		 */
		private void parseFile() {
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
