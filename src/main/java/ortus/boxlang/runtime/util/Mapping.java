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

import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * I represent the a mapping such as /foo/ or /foo/bar/ which resolves to an absolute directory path.
 *
 */
public record Mapping( String name, String path, boolean external ) {

	/**
	 * Factor method to create a new Mapping instance.
	 * Mapping names such as foo/bar will be forced to have leading and trailing slashes.
	 * /foo/bar/
	 *
	 * @param name     The mapping name
	 * @param path     The mapping path
	 * @param external Is this mapping externally accessible?
	 *
	 * @return A new Mapping instance.
	 */
	public static Mapping of( String name, String path, boolean external ) {
		return new Mapping(
		    cleanName( name ),
		    path,
		    external
		);
	}

	/**
	 * Factory method to create a new external Mapping instance
	 *
	 * @param name The mapping name
	 * @param path The mapping path
	 *
	 * @return A new Mapping instance.
	 */
	public static Mapping ofExternal( String name, String path ) {
		return Mapping.of(
		    name,
		    path,
		    true
		);
	}

	/**
	 * Factory method to create a new internal Mapping instance
	 *
	 * @param name The mapping name
	 * @param path The mapping path
	 *
	 * @return A new Mapping instance.
	 */
	public static Mapping ofInternal( String name, String path ) {
		return Mapping.of(
		    name,
		    path,
		    false
		);
	}

	/**
	 * Factory method to create a new Mapping instance from data.
	 * This method will attempt to extract the mapping path and external flag from the data.
	 * If the data is a Struct, it will extract the path and external flag.
	 * If the data is not a Struct, it will assume the data is a string representing the mapping path and default the external flag.
	 * 
	 * @param name The mapping name
	 * @param data The data to extract the mapping path and external flag from.
	 * 
	 * @return A new Mapping instance.
	 */
	public static Mapping fromData( String name, Object data, boolean defaultExternal ) {
		// If the data is a Struct, we can extract the mapping path and external flag
		return StructCaster.attempt( data ).map( s -> Mapping.of(
		    name,
		    s.getAsAttempt( Key.path ).map( StringCaster::cast ).orThrow( "Path is required for mapping" ),
		    s.getAsAttempt( Key.external ).map( BooleanCaster::cast ).getOrDefault( defaultExternal )
		) )
		    // Otherwise, we assume the data is a string representing the mapping path (and default external)
		    .orElseGet( () -> Mapping.of(
		        name,
		        StringCaster.cast( data ),
		        defaultExternal
		    ) );
	}

	/**
	 * Convert this Mapping to a Struct representation.
	 * 
	 * @return A Struct representation of this Mapping.
	 */
	public IStruct toStruct() {
		return Struct.of(
		    Key._NAME, this.name,
		    Key.path, this.path,
		    Key.external, this.external
		);
	}

	/**
	 * Convert this Mapping to a Struct representation without the name.
	 * 
	 * @return A Struct representation of this Mapping without the name.
	 */
	public IStruct toStructNoName() {
		return Struct.of(
		    Key.path, this.path,
		    Key.external, this.external
		);
	}

	public static String cleanName( String name ) {
		if ( !name.startsWith( "/" ) ) {
			name = "/" + name;
		}
		if ( !name.endsWith( "/" ) ) {
			name += "/";
		}
		return name;
	}

	@Override
	public String toString() {
		return path;
	}

}
