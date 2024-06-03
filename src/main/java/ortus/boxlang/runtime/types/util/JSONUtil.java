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
package ortus.boxlang.runtime.types.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.extension.javatime.JacksonJrJavaTimeExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JacksonJrExtension;
import com.fasterxml.jackson.jr.ob.api.ExtensionContext;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.conversion.BoxJsonProvider;

/**
 * Utility class for JSON operations based on our library of choice.
 */
public class JSONUtil {

	/**
	 * The JSON builder library we use
	 */
	private static final JSON JSON_BUILDER = JSON.builder(
	    // Use a custom factory with enabled parsing features
	    new JsonFactory()
	        .enable( JsonParser.Feature.ALLOW_COMMENTS )
	        .enable( JsonParser.Feature.ALLOW_YAML_COMMENTS )
	)
	    // Enable JSON features
	    // https://fasterxml.github.io/jackson-jr/javadoc/jr-objects/2.8/com/fasterxml/jackson/jr/ob/JSON.Feature.html
	    .enable(
	        JSON.Feature.PRETTY_PRINT_OUTPUT,
	        JSON.Feature.USE_BIG_DECIMAL_FOR_FLOATS,
	        JSON.Feature.USE_FIELDS,
	        JSON.Feature.WRITE_NULL_PROPERTIES
	    )
	    // Add Jackson annotation support
	    .register( JacksonAnnotationExtension.std )
	    // Add JavaTime Extension
	    .register( new JacksonJrJavaTimeExtension() )
	    // Add Custom Serializers/ Deserializers
	    .register( new JacksonJrExtension() {

		    @Override
		    protected void register( ExtensionContext extensionContext ) {
			    extensionContext.insertProvider( new BoxJsonProvider() );
		    }

	    } )
	    // Yeaaaahaaa!
	    .build();

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
	public static JSON getJSONBuilder() {
		return JSON_BUILDER;
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to one of simple mappings ({@link java.util.Map}
	 * for JSON Objects, {@link java.util.List} for JSON Arrays,
	 * {@link java.lang.String}
	 * for JSON Strings, null for JSON null, {@link java.lang.Boolean} for JSON
	 * booleans
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
	public static Object fromJSON( Object json ) {
		try {
			// Now parse the JSON
			return JSON_BUILDER.anyFrom( json );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse JSON " + json.toString(), e );
		}
	}

	/**
	 * Read method that will take given JSON Source (of one of supported types),
	 * read contents and map it to a Java Bean of given type.
	 *
	 * <a href=
	 * "https://github.com/FasterXML/jackson-jr#readingwriting-simple-objects-beans-listarrays-thereof">Read
	 * more here:</a>
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
	public static <T> T fromJSON( Class<T> clazz, Object json ) {
		try {
			return JSON_BUILDER.beanFrom( clazz, json );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse JSON into " + clazz.getSimpleName(), e );
		}
	}

}
