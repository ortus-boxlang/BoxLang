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
package ortus.boxlang.runtime.logging;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This is a helper class to assist with cross logging and tracing across
 * libraries we use. This can also help for getting future logging frameworks
 * embedded in BoxLang.
 */
public class LogLevel {

	// Standard Logging Libraries log levels.
	public static final Key	OFF		= Key.of( "off" );
	public static final Key	FATAL	= Key.of( "fatal" );
	public static final Key	ERROR	= Key.of( "error" );
	public static final Key	WARN	= Key.of( "warn" );
	public static final Key	INFO	= Key.of( "info" );
	public static final Key	DEBUG	= Key.of( "debug" );
	public static final Key	TRACE	= Key.of( "trace" );

	/**
	 * Convert a human-readable log level to a LogLevel Key
	 *
	 * @param level The string to convert into a LogLevel Key
	 * @param safe  If true, then it will return null instead of the exception
	 *
	 * @throws IllegalArgumentException If the string is not a valid LogLevel
	 *
	 * @return The LogLevel Key
	 */
	public static Key valueOf( String level, Boolean safe ) {
		switch ( level.toLowerCase() ) {
			case "off" :
				return OFF;
			case "fatal" :
			case "fatal information" :
				return FATAL;
			case "error" :
				return ERROR;
			case "warn" :
			case "warning" :
				return WARN;
			case "info" :
			case "information" :
				return INFO;
			case "debug" :
				return DEBUG;
			case "trace" :
				return TRACE;
			default : {
				if ( safe ) {
					return null;
				}
				throw new IllegalArgumentException( "Invalid log level: " + level );
			}
		}
	}

	/**
	 * Validate if the incoming string level is valid or not
	 *
	 * @return True if the level is valid, false otherwise
	 */
	public static boolean isValid( String level ) {
		return valueOf( level, true ) == null ? false : true;
	}

	/**
	 * Get the levels as an array of keys
	 *
	 * @return The levels as an array of keys
	 */
	public static Key[] getLevels() {
		return new Key[] { OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE };
	}

	/**
	 * Get the levels as a list of strings
	 */
	public static List<String> getLevelsList() {
		return Arrays.stream( getLevels() ).map( Key::getName ).collect( Collectors.toList() );
	}

	/**
	 * Get the levels as a list of strings
	 */
	public static String[] getLevelsStrings() {
		return getLevelsList().toArray( new String[ 0 ] );
	}

}
