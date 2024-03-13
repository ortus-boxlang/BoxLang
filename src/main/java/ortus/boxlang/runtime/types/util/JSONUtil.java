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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Utility class for JSON operations based on our library of choice.
 */
public class JSONUtil {

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
			// Strip out comments
			if ( json instanceof File ) {
				json = stripComments( ( File ) json );
			} else if ( json instanceof Path ) {
				json = stripComments( ( Path ) json );
			} else if ( json instanceof InputStream ) {
				json = stripComments( ( InputStream ) json );
			} else if ( json instanceof String ) {
				json = stripComments( ( String ) json );
			} else if ( json instanceof URL ) {
				// Conver the URL to a File Object
				json = stripComments( ( ( URL ) json ).getFile() );
			}
			// Now parse the JSON
			return JSON_BUILDER.anyFrom( json );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse JSON", e );
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

	/**
	 * Strip comments from a JSON file
	 *
	 * @param filePath The path to the JSON file
	 *
	 * @return The JSON string without comments
	 *
	 * @throws IOException If the file cannot be read
	 */
	public static String stripComments( String filePath ) throws IOException {
		return stripComments( new File( filePath ) );
	}

	/**
	 * Strip comments from a JSON file
	 *
	 * @param filePath A Path object
	 *
	 * @return The JSON string without comments
	 *
	 * @throws IOException If the file cannot be read
	 */
	public static String stripComments( Path filePath ) throws IOException {
		return stripComments( filePath.toFile() );
	}

	/**
	 * Strip comments from a JSON InputStream
	 *
	 * @param inputStream The input stream to read the JSON from
	 *
	 * @return The JSON string without comments
	 *
	 * @throws IOException If the file cannot be read
	 */
	public static String stripComments( InputStream inputStream ) throws IOException {
		try ( BufferedReader reader = new BufferedReader( new InputStreamReader( inputStream ) ) ) {
			return visitReader( reader );
		}
	}

	/**
	 * Strip comments from a JSON file
	 *
	 * @param file A file object
	 *
	 * @return The JSON string without comments
	 *
	 * @throws IOException If the file cannot be read
	 */
	public static String stripComments( File file ) throws IOException {
		try ( BufferedReader reader = new BufferedReader( new FileReader( file ) ) ) {
			return visitReader( reader );
		}
	}

	/**
	 * Strip comments from a JSON string
	 *
	 * @param json The JSON string to strip comments from
	 *
	 * @return The JSON string without comments
	 */
	public static String stripCommentsFromString( String json ) {
		return visitReader( new BufferedReader( new StringReader( json ) ) );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Visit reader for stripping comments
	 *
	 * @param reader The reader to visit
	 *
	 * @return The JSON string without comments
	 */
	private static String visitReader( BufferedReader reader ) {
		return reader
		    .lines()
		    .parallel()
		    .map( JSONUtil::stripLineComments )
		    .collect( StringBuilder::new, StringBuilder::append, StringBuilder::append )
		    .toString();
	}

	/**
	 * Strips single-line comments from a line
	 *
	 * TODO: Add multi-line comments in the future
	 *
	 * @param line The line to strip comments from
	 *
	 * @return The line without comments
	 */
	private static String stripLineComments( String line ) {
		// Remove single-line comments starting with "//"
		return line.replaceAll( "\\/\\/.*$", "" );
	}

}
