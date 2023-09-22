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

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

/**
 * Utility class for JSON operations based on our library of choice.
 */
public class JsonUtil {

	/**
	 * The JSON builder library we use
	 */
	private static final JSON JSON_BUILDER = JSON.builder().register( JacksonAnnotationExtension.std ).build();

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Returns the JSON builder library we use
	 *
	 * @see com.fasterxml.jackson.jr.ob.JSON
	 *
	 * @return The JSON builder
	 */
	public static JSON getJsonBuilder() {
		return JSON_BUILDER;
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to one of simple mappings ({@link java.util.Map}
	 * for JSON Objects, {@link java.util.List} for JSON Arrays, {@link java.lang.String}
	 * for JSON Strings, null for JSON null, {@link java.lang.Boolean} for JSON booleans
	 * and {@link java.lang.Number} for JSON numbers.
	 *
	 * Supported source types include:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li><code>byte[]</code></li>
	 * <li><code>char[]</code></li>
	 * </ul>
	 *
	 * @param json The JSON to parse
	 *
	 * @return The parsed JSON in raw Java format
	 */
	public static Object fromJson( Object json ) {
		try {
			return JSON_BUILDER.anyFrom( json );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to parse JSON", e );
		}
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to a Java Bean of given type.
	 *
	 * <a href="https://github.com/FasterXML/jackson-jr#readingwriting-simple-objects-beans-listarrays-thereof">Read more here:</a>
	 *
	 * Supported source types include:
	 * <ul>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link java.io.Reader}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.lang.String}</li>
	 * <li><code>byte[]</code></li>
	 * <li><code>char[]</code></li>
	 * </ul>
	 *
	 * @param clazz The POJO Java Beans class to parse the JSON into
	 * @param json  The JSON to parse
	 *
	 * @return The parsed JSON into the given class passsed
	 */
	public static <T> T fromJson( Class<T> clazz, Object json ) {
		try {
			return JSON_BUILDER.beanFrom( clazz, json );
		} catch ( Exception e ) {
			throw new RuntimeException( "Failed to parse JSON into " + clazz.getSimpleName(), e );
		}
	}

}
