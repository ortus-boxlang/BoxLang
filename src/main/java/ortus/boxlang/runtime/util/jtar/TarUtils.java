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
 *
 * Vendored and adapted from https://github.com/xiaoxindada/jtar.
 * Original JTar copyright 2012 Kamran Zafar; Apache License 2.0.
 */
package ortus.boxlang.runtime.util.jtar;

/** TAR utility methods. */
public final class TarUtils {

	private TarUtils() {
	}

	/**
	 * Trims a character from both ends of a string.
	 * 
	 * @param value     The source value
	 * @param character The character to remove
	 * 
	 * @return The trimmed value
	 */
	public static String trim( String value, char character ) {
		int	start	= 0;
		int	end		= value.length();
		while ( start < end && value.charAt( start ) == character )
			start++;
		while ( end > start && value.charAt( end - 1 ) == character )
			end--;
		return value.substring( start, end );
	}
}
