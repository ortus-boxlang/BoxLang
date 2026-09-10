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

import java.net.URL;
import java.util.Arrays;

/**
 * Utilities for producing stable class-loader cache keys.
 */
public final class ClassLoaderUtil {

	private ClassLoaderUtil() {
	}

	/**
	 * Hashes values in canonical sorted order.
	 *
	 * @param values The values to hash
	 *
	 * @return A stable hash for the values regardless of input order
	 */
	public static String hashSorted( Object[] values ) {
		String[] sortedValues = Arrays.stream( values )
		    .map( Object::toString )
		    .sorted()
		    .toArray( String[]::new );
		return EncryptionUtil.hash( Arrays.toString( sortedValues ) );
	}

	/**
	 * Hashes URLs in canonical sorted order.
	 *
	 * @param urls The URLs to hash
	 *
	 * @return A stable hash for the URLs regardless of input order
	 */
	public static String hashSorted( URL[] urls ) {
		return hashSorted( ( Object[] ) urls );
	}

}
