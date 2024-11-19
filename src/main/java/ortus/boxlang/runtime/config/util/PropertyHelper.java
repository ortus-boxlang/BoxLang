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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.util.ListUtil;

/**
 * Helps convert from JSON to native Java/BoxLang Types
 */
public class PropertyHelper {

	/**
	 * Logger
	 */
	private static final Logger logger = LoggerFactory.getLogger( PropertyHelper.class );

	/**
	 * This processes a JSON array list to a HashSet
	 *
	 * @param config The configuration object
	 * @param key    The target key to look and process
	 * @param target The target set to populate
	 */
	@SuppressWarnings( "unchecked" )
	public static void processListToSet( IStruct config, Key key, Set<String> target ) {
		if ( config.containsKey( key ) ) {
			if ( config.get( key ) instanceof List<?> castedList ) {
				target.addAll( ( Collection<String> ) castedList );
			} else {
				logger.warn( "The property [{}] must be a JSON Array", key );
			}
		}
	}

	/**
	 * This processes:
	 * - A string to a list (comma separated or a single string)
	 * - A JSON array to a list
	 *
	 * @param config The configuration object
	 * @param key    The target key to look and process
	 * @param target The target list to populate with the values
	 */
	public static void processStringOrArrayToList( IStruct config, Key key, List<String> target ) {
		if ( config.containsKey( key ) ) {
			// If it's a string, convert it to an array
			if ( config.get( key ) instanceof String castedStringList ) {
				config.put(
				    key,
				    ListUtil.asList( PlaceholderHelper.resolve( castedStringList ), ListUtil.DEFAULT_DELIMITER )
				);
			}

			// For some reason we have to re-cast this through a stream. Attempting to cast it directly throws a ClassCastException
			ArrayCaster.cast( config.get( key ) )
			    .stream()
			    // Add each item to the incoming target
			    .map( StringCaster::cast )
			    .forEach( target::add );
		}
	}

}
