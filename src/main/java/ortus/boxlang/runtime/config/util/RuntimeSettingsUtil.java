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
package ortus.boxlang.runtime.config.util;

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;
import ortus.boxlang.runtime.util.ConfigSecretUtil;

/**
 * Helpers for the user-defined {@code runtimeSettings} struct: placeholder and secret resolution,
 * deep merging of application overrides over the global settings, read-only freezing and dotted-path lookup.
 */
public class RuntimeSettingsUtil {

	/**
	 * The separator used in dotted-path lookups
	 */
	public static final char PATH_SEPARATOR = '.';

	/**
	 * Private constructor, static utility class
	 */
	private RuntimeSettingsUtil() {
	}

	/**
	 * Deep copies a settings tree, resolves {@code ${env.X}} / {@code ${X:default}} placeholders and
	 * decrypts {@code bxsecret:} values, then freezes the result. The source is never mutated.
	 *
	 * @param settings The raw settings struct
	 *
	 * @return A resolved, unmodifiable struct
	 */
	public static IStruct resolve( IStruct settings ) {
		Object resolved = PlaceholderHelper.resolveAll(
		    copy( settings ),
		    PlaceholderHelper.getPlaceholderMap(),
		    ConfigSecretUtil::decryptIfEncrypted
		);
		return ( IStruct ) freeze( ConfigSecretUtil.decryptValues( resolved ) );
	}

	/**
	 * Resolves the {@code runtimeSettings} key of an application settings struct in place, if present and a struct.
	 *
	 * @param appSettings The application settings
	 */
	public static void resolveInPlace( IStruct appSettings ) {
		if ( appSettings.get( Key.runtimeSettings ) instanceof IStruct raw ) {
			appSettings.put( Key.runtimeSettings, resolve( raw ) );
		}
	}

	/**
	 * Deep merges an override struct over a base struct. Nested structs merge by key, any other
	 * value (including arrays) in the override replaces the base value. Neither input is mutated.
	 *
	 * @param base     The base settings, may be null
	 * @param override The overriding settings, may be null
	 *
	 * @return A new unmodifiable merged struct
	 */
	public static IStruct merge( IStruct base, IStruct override ) {
		return ( IStruct ) freeze( mergeInto( copy( base ), override ) );
	}

	/**
	 * Retrieves a value by dotted path, for example {@code features.beta}. A literal key that
	 * contains dots wins over path traversal.
	 *
	 * @param settings The settings struct
	 * @param path     The key or dotted path
	 *
	 * @return The value, or null if not found
	 */
	public static Object get( IStruct settings, String path ) {
		if ( settings == null ) {
			return null;
		}
		Key literal = Key.of( path );
		if ( settings.containsKey( literal ) ) {
			return settings.get( literal );
		}
		Object	current	= settings;
		int		start	= 0;
		while ( start <= path.length() ) {
			int		end		= path.indexOf( PATH_SEPARATOR, start );
			String	segment	= path.substring( start, end < 0 ? path.length() : end );
			if ( current instanceof IStruct struct && struct.containsKey( Key.of( segment ) ) ) {
				current = struct.get( Key.of( segment ) );
			} else if ( current instanceof Array array && isIndex( segment ) ) {
				int index = Integer.parseInt( segment );
				if ( index < 1 || index > array.size() ) {
					return null;
				}
				current = array.get( index - 1 );
			} else {
				return null;
			}
			if ( end < 0 ) {
				return current;
			}
			start = end + 1;
		}
		return null;
	}

	/**
	 * Returns true if the path resolves to a value
	 *
	 * @param settings The settings struct
	 * @param path     The key or dotted path
	 *
	 * @return true if found
	 */
	public static boolean has( IStruct settings, String path ) {
		return get( settings, path ) != null;
	}

	/**
	 * Recursively makes structs and arrays unmodifiable.
	 *
	 * @param value The value tree
	 *
	 * @return The frozen tree
	 */
	@SuppressWarnings( "unchecked" )
	public static Object freeze( Object value ) {
		if ( value instanceof Map<?, ?> rawMap ) {
			IStruct frozen = new Struct();
			( ( Map<Object, Object> ) rawMap ).forEach( ( k, v ) -> frozen.put( k instanceof Key key ? key : Key.of( k.toString() ), freeze( v ) ) );
			return UnmodifiableStruct.fromStruct( frozen );
		} else if ( value instanceof List<?> rawList ) {
			Array frozen = new Array();
			rawList.forEach( v -> frozen.add( freeze( v ) ) );
			return UnmodifiableArray.fromList( frozen );
		}
		return value;
	}

	/**
	 * Deep copies structs, maps, arrays and lists into modifiable BoxLang types. Other values are shared.
	 */
	@SuppressWarnings( "unchecked" )
	private static IStruct copy( IStruct source ) {
		IStruct copy = new Struct();
		if ( source != null ) {
			source.forEach( ( k, v ) -> copy.put( k, copyValue( v ) ) );
		}
		return copy;
	}

	@SuppressWarnings( "unchecked" )
	private static Object copyValue( Object value ) {
		if ( value instanceof Map<?, ?> rawMap ) {
			IStruct copy = new Struct();
			( ( Map<Object, Object> ) rawMap ).forEach( ( k, v ) -> copy.put( k instanceof Key key ? key : Key.of( k.toString() ), copyValue( v ) ) );
			return copy;
		} else if ( value instanceof List<?> rawList ) {
			Array copy = new Array();
			rawList.forEach( v -> copy.add( copyValue( v ) ) );
			return copy;
		}
		return value;
	}

	private static IStruct mergeInto( IStruct target, IStruct override ) {
		if ( override == null ) {
			return target;
		}
		override.forEach( ( key, value ) -> {
			if ( value instanceof IStruct overrideChild && target.get( key ) instanceof IStruct targetChild ) {
				target.put( key, mergeInto( copy( targetChild ), overrideChild ) );
			} else {
				target.put( key, copyValue( value ) );
			}
		} );
		return target;
	}

	private static boolean isIndex( String segment ) {
		if ( segment.isEmpty() ) {
			return false;
		}
		for ( int i = 0; i < segment.length(); i++ ) {
			if ( !Character.isDigit( segment.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}
}
